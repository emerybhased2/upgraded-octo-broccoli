#!/usr/bin/env ruby
# frozen_string_literal: true

# Fetches reference names for Bloons TD 5 and Bloons TD 6 from the Bloons Wiki
# (https://bloons.fandom.com) through its public MediaWiki API:
#
#   * BTD5 tower names and each tower's upgrades (path 1 and path 2)
#   * BTD5 map (track) names, with their difficulty where the wiki lists one
#   * BTD6 difficulties and the game modes under each one
#   * BTD6 Bloon names (normal, MOAB-class, properties, other)
#   * BTD6 Boss Bloons
#
# Only names are collected. Uses the Ruby standard library only.
#
# Usage:
#   ruby scripts/fetch_bloons_data.rb                 # print a readable summary
#   ruby scripts/fetch_bloons_data.rb --json out.json # also write everything as JSON
#   ruby scripts/fetch_bloons_data.rb --json -        # JSON to stdout only

require "json"
require "net/http"
require "uri"
require "optparse"
require "time"

module BloonsData
  API = URI("https://bloons.fandom.com/api.php")
  USER_AGENT = "BloonsDataFetcher/1.0 (Ruby #{RUBY_VERSION})"

  # ------------------------------------------------------------------ wiki access

  class Wiki
    def initialize(verbose: false)
      @verbose = verbose
      @cache = {}
    end

    def get(params)
      key = params.sort.to_h
      @cache[key] ||= request(params.merge(format: "json", formatversion: 2))
    end

    # Full wikitext of a page (follows redirects), or nil if it does not exist.
    def wikitext(page)
      data = get(action: "parse", page: page, prop: "wikitext", redirects: 1)
      data.dig("parse", "wikitext")
    end

    # Wikitext of the first section whose heading matches `heading` (a String or Regexp).
    def section(page, heading)
      sections = get(action: "parse", page: page, prop: "sections", redirects: 1).dig("parse", "sections") || []
      match = sections.find do |s|
        line = BloonsData.strip_markup(s["line"])
        heading.is_a?(Regexp) ? line.match?(heading) : line.casecmp?(heading)
      end
      return nil unless match

      get(action: "parse", page: page, section: match["index"], prop: "wikitext", redirects: 1).dig("parse", "wikitext")
    end

    def category_members(category)
      members = []
      params = { action: "query", list: "categorymembers", cmtitle: "Category:#{category}", cmlimit: 500, cmnamespace: 0 }
      loop do
        data = request(params.merge(format: "json", formatversion: 2))
        members.concat(data.dig("query", "categorymembers").map { |m| m["title"] })
        cont = data.dig("continue", "cmcontinue") or break
        params = params.merge(cmcontinue: cont)
      end
      members
    end

    private

    def request(params, attempts: 4)
      uri = API.dup
      uri.query = URI.encode_www_form(params)
      warn "GET #{uri}" if @verbose
      attempts.times do |i|
        begin
          res = Net::HTTP.start(uri.host, uri.port, use_ssl: true, open_timeout: 15, read_timeout: 30) do |http|
            http.get(uri.request_uri, "User-Agent" => USER_AGENT, "Accept" => "application/json")
          end
          if res.is_a?(Net::HTTPSuccess)
            data = JSON.parse(res.body)
            raise "API error: #{data['error']['info']}" if data["error"] && data["error"]["code"] != "missingtitle"
            return data
          end
          raise "HTTP #{res.code}" unless res.code.to_i >= 500 || res.code == "429"
        rescue Net::OpenTimeout, Net::ReadTimeout, SocketError, Errno::ECONNRESET, OpenSSL::SSL::SSLError => e
          raise if i == attempts - 1
          warn "  retrying after #{e.class}" if @verbose
        end
        sleep(2**i)
      end
      raise "Giving up on #{uri}"
    end
  end

  # ------------------------------------------------------------------ wikitext helpers

  module_function

  # Turns "[[Target|Label]]", "'''bold'''", HTML tags and comments into plain text.
  def strip_markup(text)
    text.to_s
        .gsub(/<!--.*?-->/m, "")
        .gsub(/<ref[^>]*\/>|<ref[^>]*>.*?<\/ref>/m, "")
        .gsub(/\[\[(?:[^\]|]*\|)?([^\]]*)\]\]/, '\1')
        .gsub(/<[^>]+>/, "")
        .gsub(/'{2,}/, "")
        .gsub("&amp;", "&")
        .strip
  end

  # Values of `field` in every {{Template ...}} call named `template` in `text`.
  def template_field(text, template, field)
    text.to_s.scan(/\{\{\s*#{Regexp.escape(template)}\s*\n?(.*?)\n?\}\}\s*$/m).flatten.filter_map do |body|
      m = body.match(/\|\s*#{Regexp.escape(field)}\s*=\s*([^|\n]*)/)
      m && strip_markup(m[1])
    end
  end

  # "Dart Monkey (BTD5)" -> "Dart Monkey"
  def display_name(title)
    title.sub(/\s*\((?:BTD\d.*?|Track|Tower)\)\z/i, "")
  end

  # ------------------------------------------------------------------ BTD5

  # Tower article titles for BTD5, read from the wiki's tower navbox.
  def btd5_tower_pages(wiki)
    navbox = wiki.wikitext("Template:Towers").to_s.gsub(/<!--.*?-->/m, "")
    # The navbox nests several child boxes; use the one listing player towers by game.
    navbox = navbox.split(/\{\{\s*Navbox/i).find { |box| box.include?("(BTD5)|BTD5") }.to_s
    pages = []
    navbox.scan(/^\s*\|\s*group(\d+)\s*=\s*(.*?)\n\s*\|\s*list\1\s*=\s*(.*?)$/).each do |_n, group, list|
      group_is_btd5 = strip_markup(group).match?(/\ABTD5\b/)
      # walk top-level links, each optionally followed by "( per-game variants )"
      list.scan(/\[\[([^\]|]+)(?:\|[^\]]*)?\]\](\s*\((?:[^()]|\([^()]*\))*\))?/).each do |target, variants|
        if variants
          variant = variants.scan(/\[\[([^\]|]+)\|([^\]]+)\]\]/).find do |_t, label|
            label.match?(/\A(?:BTD\d-)?BTD5\z/)
          end
          pages << variant[0] if variant
        elsif group_is_btd5
          pages << target
        end
      end
    end
    pages.uniq
  end

  # Upgrade names on paths 1 and 2 of a BTD5 tower article.
  def btd5_upgrades(wiki, page)
    text = wiki.wikitext(page).to_s
    # The first "Path N" heading on a BTD5 article holds its upgrade cards; the block ends at the next heading.
    # Pages shared with other games split each path into "Path N (BTD5)" / "Path N (BTDB Mobile)".
    %w[1 2].map do |n|
      block = text[/^=+\s*Path #{n} \(BTD5\)\s*=+\s*\n(.*?)(?=^=+[^=]|\z)/m, 1] ||
              text[/^=+\s*Path #{n}\s*=+\s*\n(.*?)(?=^=+[^=]|\z)/m, 1]
      template_field(block.to_s, "Upgrade", "name").uniq
    end
  end

  def btd5_towers(wiki)
    btd5_tower_pages(wiki).filter_map do |page|
      path1, path2 = btd5_upgrades(wiki, page)
      next if path1.empty? && path2.empty? # not a buildable tower

      { "name" => display_name(page), "page" => page, "upgrades" => { "path_1" => path1, "path_2" => path2 } }
    end
  end

  def btd5_maps(wiki)
    table = wiki.wikitext("Template:Medals/All Tracks/BTD5").to_s
    maps = table.split(/^\|-/).filter_map do |row|
      cells = row.lines.map(&:strip).select { |l| l.start_with?("|") && !l.start_with?("|}") }
      next if cells.empty?

      name = strip_markup(cells[0].sub(/\A\|/, ""))
      next if name.empty? || name.start_with?("[[File")

      difficulty = cells[1] && strip_markup(cells[1].sub(/\A\|/, "")).split("|").last.to_s.strip
      { "name" => display_name(name), "difficulty" => (difficulty.nil? || difficulty.empty? ? nil : difficulty) }
    end
    known = maps.map { |m| m["name"].downcase }
    # The medal table is marked incomplete on the wiki, so add any tracks from the category it misses.
    wiki.category_members("Bloons TD 5 Tracks").each do |title|
      next if title.match?(%r{\A(?:Maps|List of|Tracks?\b)}i) || title.include?("/")

      name = display_name(title)
      next if known.include?(name.downcase)

      maps << { "name" => name, "difficulty" => nil }
      known << name.downcase
    end
    maps
  end

  # ------------------------------------------------------------------ BTD6

  def btd6_difficulties(wiki)
    text = wiki.section("Bloons TD 6", "Game Modes").to_s
    text = text.split(/^===/).first # stop before "Map Borders"
    difficulties = []
    text.each_line do |line|
      if (m = line.match(/\A;\s*'''(.+?):?'''/))
        difficulties << { "name" => strip_markup(m[1]).delete_suffix(":"), "modes" => [] }
      elsif difficulties.any? && line.match?(/\A\*(?!\*)/)
        links = line.scan(/\[\[(?![Ff]ile:)([^\]|]+)(?:\|([^\]]*))?\]\]/)
        next if links.empty?

        target, label = links.first
        difficulties.last["modes"] << strip_markup(label.to_s.empty? ? target : label)
      end
    end
    difficulties
  end

  BTD6_BLOON_SECTIONS = {
    "normal" => "Normal Bloons",
    "moab_class" => "MOAB Class Bloons",
    "properties" => %r{\ABloon properties/types\z}i,
    "other" => "Other Bloons",
    "boss_minions" => "Boss Minions"
  }.freeze

  def btd6_bloons(wiki)
    BTD6_BLOON_SECTIONS.to_h do |key, heading|
      text = wiki.section("Bloons TD 6", heading).to_s
      text = text.split(/^====\s*Other Bloons/).first if key == "properties"
      [key, template_field(text, "Bloon Card", "name").uniq]
    end
  end

  def btd6_bosses(wiki)
    text = wiki.section("Bloons TD 6", "Boss Bloons").to_s
    text.scan(/\{\{\s*Bloon Card(.*?)\}\}\s*$/m).flatten.filter_map do |body|
      name = body[/\|\s*name\s*=\s*([^|\n]*)/, 1]
      next unless name

      aka = body[/\|\s*aka\s*=\s*([^|\n]*)/, 1]
      { "name" => strip_markup(name), "title" => aka && strip_markup(aka) }
    end
  end

  # ------------------------------------------------------------------ main

  def fetch_all(verbose: false)
    wiki = Wiki.new(verbose: verbose)
    {
      "source" => "https://bloons.fandom.com (CC BY-SA)",
      "fetched_at" => Time.now.utc.iso8601,
      "btd5" => { "towers" => btd5_towers(wiki), "maps" => btd5_maps(wiki) },
      "btd6" => { "difficulties" => btd6_difficulties(wiki), "bloons" => btd6_bloons(wiki), "bosses" => btd6_bosses(wiki) }
    }
  end

  def print_summary(data, io = $stdout)
    btd5 = data["btd5"]
    btd6 = data["btd6"]
    io.puts "== BTD5 towers (#{btd5['towers'].size}) =="
    btd5["towers"].each do |t|
      io.puts "#{t['name']}"
      io.puts "  Path 1: #{t['upgrades']['path_1'].join(' > ')}"
      io.puts "  Path 2: #{t['upgrades']['path_2'].join(' > ')}"
    end
    io.puts "\n== BTD5 maps (#{btd5['maps'].size}) =="
    btd5["maps"].each { |m| io.puts "  #{m['name']}#{m['difficulty'] ? " [#{m['difficulty']}]" : ''}" }
    io.puts "\n== BTD6 difficulties =="
    btd6["difficulties"].each { |d| io.puts "  #{d['name']}: #{d['modes'].join(', ')}" }
    io.puts "\n== BTD6 Bloons =="
    btd6["bloons"].each { |group, names| io.puts "  #{group.tr('_', ' ')} (#{names.size}): #{names.join(', ')}" }
    io.puts "\n== BTD6 bosses (#{btd6['bosses'].size}) =="
    btd6["bosses"].each { |b| io.puts "  #{b['name']}#{b['title'] ? (b['title'].match?(/\Athe\b/i) ? " " : ": ") + b['title'] : ''}" }
  end
end

if $PROGRAM_NAME == __FILE__
  options = { json: nil, verbose: false }
  OptionParser.new do |o|
    o.banner = "Usage: #{File.basename(__FILE__)} [--json FILE|-] [--verbose]"
    o.on("--json FILE", "Write the data as JSON to FILE (use - for stdout)") { |f| options[:json] = f }
    o.on("-v", "--verbose", "Log each API request to stderr") { options[:verbose] = true }
  end.parse!

  data = BloonsData.fetch_all(verbose: options[:verbose])
  if options[:json] == "-"
    puts JSON.pretty_generate(data)
  else
    BloonsData.print_summary(data)
    if options[:json]
      dir = File.dirname(options[:json])
      Dir.mkdir(dir) unless Dir.exist?(dir)
      File.write(options[:json], JSON.pretty_generate(data) + "\n")
      warn "\nWrote #{options[:json]}"
    end
  end
end
