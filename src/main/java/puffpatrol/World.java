package puffpatrol;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** All the game state and rules; no drawing or input. */
final class World {
    static final int START_CASH = 650;
    private static final String[] POP_WORDS = {"pop!", "puff!", "fwip!", "plip!", "poof!"};

    enum UpgradeState { AVAILABLE, MAXED, PATH_LOCKED, XP_LOCKED }

    final GameMap map;
    final Difficulty difficulty;
    final Profile profile;
    final List<Puff> puffs = new ArrayList<>();
    final List<Tower> towers = new ArrayList<>();
    final List<Projectile> projectiles = new ArrayList<>();
    final List<Particle> particles = new ArrayList<>();
    private final List<Puff> pending = new ArrayList<>();
    final Random rnd = new Random();

    int cash = START_CASH;
    int lives;
    int round;
    long totalPops;
    boolean roundActive, autoStart, gameOver, victory, victoryAcknowledged;
    String banner;
    int bannerTicks;
    int globalSurge;

    /** Sound hook; the UI plugs in the real player. */
    Consumer<Sound.Sfx> sound = s -> { };
    /** Called after each round is cleared (the UI autosaves here). */
    Runnable onRoundEnd = () -> { };

    private List<Waves.Spawn> spawns = List.of();
    private int spawnIdx, roundTick, spawnCounter;

    World(GameMap map, Difficulty difficulty, Profile profile) {
        this.map = map;
        this.difficulty = difficulty;
        this.profile = profile;
        this.lives = difficulty.lives;
        this.cash = map.startCash;
        this.round = difficulty.startRound - 1;
    }

    void sfx(Sound.Sfx s) {
        sound.accept(s);
    }

    int displayRound() {
        return roundActive ? round : round + 1;
    }

    boolean startRound() {
        if (roundActive || gameOver) return false;
        round++;
        spawns = Waves.round(round);
        spawnIdx = 0;
        roundTick = 0;
        roundActive = true;
        return true;
    }

    boolean pausedForVictory() {
        return victory && !victoryAcknowledged;
    }

    // ---------------------------------------------------------------- the tick

    void update() {
        if (gameOver) return;
        if (bannerTicks > 0) bannerTicks--;
        if (globalSurge > 0) globalSurge--;
        if (roundActive) {
            while (spawnIdx < spawns.size() && spawns.get(spawnIdx).tick() <= roundTick) {
                Waves.Spawn s = spawns.get(spawnIdx++);
                Puff p = new Puff(s.type(), spawnCounter++ % map.tracks.size(), 0, s.hidden(), s.regrow(), s.armoured());
                p.place(map);
                puffs.add(p);
            }
            roundTick++;
        }
        for (Puff p : puffs) {
            if (p.dead) continue;
            p.update(map, difficulty.speedMul);
            if (p.regrow) regrow(p);
            if (p.slimeTicks > 0 && p.slimeDot > 0 && ++p.slimeDotTimer >= p.slimeDotEvery) {
                p.slimeDotTimer = 0;
                damage(p, p.slimeDot, DamageType.NORMAL, p.slimeSource);
            }
            if (!p.dead && p.dist >= map.length(p.track)) {
                p.dead = true;
                lives -= p.rbe();
                sfx(Sound.Sfx.LEAK);
            }
        }
        if (lives <= 0) {
            lives = 0;
            gameOver = true;
        }
        applyBuffs();
        for (Tower t : towers) t.update(this);
        for (Projectile pr : projectiles) pr.update(this);
        puffs.addAll(pending);
        pending.clear();
        puffs.removeIf(p -> p.dead);
        projectiles.removeIf(p -> p.dead);
        particles.removeIf(p -> !p.update());
        if (roundActive && spawnIdx >= spawns.size() && puffs.isEmpty()) endRound();
    }

    private void regrow(Puff p) {
        if (++p.regrowTimer < 150) return;
        p.regrowTimer = 0;
        PuffType parent = p.type.regrowParent();
        if (parent != null && parent.rbe() <= p.regrowMax.rbe()) {
            p.setLayer(parent);
            if (particles.size() < 400) particles.add(Particle.ring(p.x, p.y, p.radius() + 4, new Color(0x8BD46A)));
        }
    }

    private void applyBuffs() {
        for (Tower t : towers) t.resetBuffs();
        for (Tower l : towers) {
            if (l.type != TowerType.LANTERN) continue;
            for (Tower t : towers) {
                if (t == l || t.type == TowerType.LANTERN || Math.hypot(t.x - l.x, t.y - l.y) > l.range) continue;
                t.rRange = Math.max(t.rRange, l.giveRange);
                t.rSpeed = Math.min(t.rSpeed, l.giveSpeed);
                t.rPierce = Math.max(t.rPierce, l.givePierce);
                t.rDamage = Math.max(t.rDamage, l.giveDamage);
                t.rDetect |= l.giveDetect;
            }
        }
    }

    private void endRound() {
        roundActive = false;
        int bonus = 100 + round;
        cash += bonus;
        banner = "Round " + round + " cleared!  +" + bonus;
        bannerTicks = 150;
        for (Tower t : towers) {
            if (t.income > 0) {
                cash += t.income;
                particles.add(Particle.coin(t.x, t.y - 26, t.income));
            }
            profile.addXp(t.type, 10 + t.spent / 30);
        }
        sfx(Sound.Sfx.ROUND);
        if (round >= difficulty.finalRound && !victory) {
            victory = true;
            profile.addMedal(map.name, difficulty);
        }
        profile.save();
        onRoundEnd.run();
        if (autoStart && !pausedForVictory()) startRound();
    }

    // ---------------------------------------------------------------- towers

    boolean canAfford(int cost) {
        return cash >= cost;
    }

    int cost(TowerType t) {
        return difficulty.cost(t.cost);
    }

    int cost(Upgrade u) {
        return difficulty.cost(u.cost());
    }

    UpgradeState upgradeState(Tower t, int path) {
        Upgrade u = t.nextUpgrade(path);
        if (u == null) return UpgradeState.MAXED;
        if (t.pathLocked(path)) return UpgradeState.PATH_LOCKED;
        if (!profile.unlocked(t.type, path, t.tiers[path] + 1)) return UpgradeState.XP_LOCKED;
        return UpgradeState.AVAILABLE;
    }

    Tower place(TowerType type, double x, double y) {
        int c = cost(type);
        if (!canAfford(c) || !map.canPlace(type, x, y, towers)) return null;
        cash -= c;
        Tower t = new Tower(type, x, y);
        t.spent = c;
        towers.add(t);
        particles.add(Particle.ring(x, y, 30, new Color(0xFFF6D8)));
        sfx(Sound.Sfx.PLACE);
        return t;
    }

    boolean upgrade(Tower t, int path) {
        if (upgradeState(t, path) != UpgradeState.AVAILABLE) return false;
        int c = cost(t.nextUpgrade(path));
        if (!canAfford(c)) return false;
        cash -= c;
        t.applyUpgrade(path, c);
        particles.add(Particle.ring(t.x, t.y, 34, path == 0 ? new Color(0xFF9A8A) : new Color(0x8AC4FF)));
        sfx(Sound.Sfx.UPGRADE);
        return true;
    }

    void sell(Tower t) {
        if (towers.remove(t)) {
            cash += t.sellValue();
            particles.add(Particle.coin(t.x, t.y - 10, t.sellValue()));
            sfx(Sound.Sfx.COIN);
        }
    }

    Puff findTarget(Tower t) {
        Puff best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double range = t.effRange();
        boolean sees = t.seesHidden();
        for (Puff p : puffs) {
            if (p.dead || (p.hidden && !sees)) continue;
            if (p.x < -4 || p.y < -4 || p.x > GameMap.W + 4 || p.y > GameMap.H + 4) continue;
            double dx = p.x - t.px, dy = p.y - t.py, rr = range + p.radius();
            double d2 = dx * dx + dy * dy;
            if (d2 > rr * rr) continue;
            double s = switch (t.mode) {
                case FIRST -> p.dist;
                case LAST -> -p.dist;
                case STRONG -> p.rbe() * 10000.0 + p.dist;
                case CLOSE -> -d2;
            };
            if (s > bestScore) {
                bestScore = s;
                best = p;
            }
        }
        return best;
    }

    /** Where to aim so a projectile of the given speed meets the puff. */
    double[] lead(Tower t, Puff p, double speed) {
        double d = Math.hypot(p.x - t.px, p.y - t.py);
        double time = d / Math.max(1, speed);
        Point2D.Double q = map.pointAt(p.track, p.dist + p.currentSpeed(difficulty.speedMul) * time);
        return new double[]{q.x, q.y};
    }

    void pushBack(Puff p, double amount) {
        p.dist = Math.max(0, p.dist - amount);
        p.place(map);
    }

    // ---------------------------------------------------------------- damage

    void explode(double x, double y, Tower t) {
        blast(x, y, t.splash, t.effDamage(), t.maxHits, t, DamageType.EXPLOSIVE, true);
        if (t.cluster) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 + rnd.nextDouble() * 0.4;
                blast(x + Math.cos(a) * t.splash * 0.9, y + Math.sin(a) * t.splash * 0.9,
                        t.splash * 0.55, t.effDamage(), t.maxHits / 2, t, DamageType.EXPLOSIVE, true);
            }
        }
        sfx(Sound.Sfx.BOOM);
    }

    void blast(double x, double y, double r, int dmg, int maxHits, Tower src, DamageType type, boolean fx) {
        if (fx && particles.size() < 700) particles.add(Particle.boom(x, y, r));
        int hits = 0;
        for (Puff p : puffs) {
            if (p.dead) continue;
            if (Math.hypot(p.x - x, p.y - y) > r + p.radius() * 0.5) continue;
            damage(p, dmg, type, src);
            if (++hits >= maxHits) break;
        }
    }

    void slime(Puff p, Tower t) {
        applySlime(p, t, false);
        if (t.slimeSplash > 0) {
            int n = 0;
            for (Puff q : puffs) {
                if (q == p || q.dead || Math.hypot(q.x - p.x, q.y - p.y) > t.slimeSplashR) continue;
                applySlime(q, t, false);
                if (++n >= t.slimeSplash) break;
            }
        }
    }

    private void applySlime(Puff p, Tower t, boolean force) {
        if (p.type.isBoss() && !t.slimeBosses && !force) return;
        p.slimeTicks = Math.max(p.slimeTicks, t.slimeTicks);
        p.slimeFactor = p.type.isBoss() ? Math.max(t.slimeFactor, 0.7) : t.slimeFactor;
        if (t.slimeDot >= p.slimeDot) {
            p.slimeDot = t.slimeDot;
            p.slimeDotEvery = t.slimeDotEvery;
            p.slimeSource = t;
        }
    }

    void damage(Puff p, int dmg, DamageType type, Tower src) {
        if (p.dead) return;
        p.regrowTimer = 0;
        if (p.brittle && p.frozen > 0) dmg += 1;
        while (dmg > 0 && !p.dead) {
            if (p.type.immuneTo(type)) {
                if (rnd.nextInt(6) == 0 && particles.size() < 300) {
                    particles.add(Particle.text(p.x, p.y - p.radius() - 4, "tink", new Color(0xD8DDE6)));
                }
                return;
            }
            if (dmg < p.hp) {
                p.hp -= dmg;
                return;
            }
            dmg -= p.hp;
            popLayer(p, src);
        }
    }

    private void popLayer(Puff p, Tower src) {
        cash += 1 + (src != null ? src.coinBonus : 0);
        totalPops++;
        if (src != null) src.pops++;
        sfx(Sound.Sfx.POP);
        if (particles.size() < 700) {
            particles.add(Particle.pop(p.x, p.y, p.type.color.brighter(), p.radius(), rnd.nextDouble()));
        }
        if (rnd.nextInt(9) == 0 && particles.size() < 250) {
            particles.add(Particle.text(p.x, p.y - p.radius(), POP_WORDS[rnd.nextInt(POP_WORDS.length)], new Color(0xFFF8E7)));
        }
        PuffType[] ch = p.type.children();
        if (ch.length == 0) {
            p.dead = true;
            return;
        }
        double gap = p.type.isBoss() ? 16 : 7;
        for (int i = 1; i < ch.length; i++) {
            Puff c = new Puff(ch[i], p.track, Math.max(0, p.dist - gap * i), p.hidden, p.regrow, p.armoured);
            c.regrowMax = p.regrowMax;
            c.place(map);
            pending.add(c);
        }
        p.setLayer(ch[0]);
        p.frozen = 0;
        p.brittle = false;
    }

    // ---------------------------------------------------------------- abilities

    /** The abilities owned right now, in a stable order. */
    List<Ability> abilities() {
        List<Ability> out = new ArrayList<>();
        for (Ability a : Ability.values()) {
            for (Tower t : towers) {
                if (t.ability == a) {
                    out.add(a);
                    break;
                }
            }
        }
        return out;
    }

    /** Ticks until the soonest tower with this ability is ready (0 = ready), or -1 if none owns it. */
    int abilityCooldown(Ability a) {
        int best = -1;
        for (Tower t : towers) {
            if (t.ability == a && (best < 0 || t.abilityCooldown < best)) best = t.abilityCooldown;
        }
        return best;
    }

    boolean activate(Ability a) {
        Tower t = null;
        for (Tower c : towers) if (c.ability == a && c.abilityCooldown == 0) t = c;
        if (t == null || gameOver) return false;
        t.abilityCooldown = a.cooldownTicks;
        t.anim = 14;
        sfx(Sound.Sfx.ABILITY);
        banner = a.label + "!";
        bannerTicks = 70;
        switch (a) {
            case QUILL_BLIZZARD -> {
                for (int i = 0; i < 48; i++) {
                    Projectile q = new Projectile(Projectile.Kind.QUILL, t, i * Math.PI * 2 / 48, t.x, t.y - 8, 11, 280);
                    q.damage = t.effDamage() + 2;
                    q.pierce = 8;
                    projectiles.add(q);
                }
            }
            case SWARM_SURGE -> {
                t.surgeTicks = 480;
                t.surgeFactor = 0.25;
            }
            case ACORN_AVALANCHE -> {
                List<Puff> strongest = byStrength();
                for (int i = 0; i < Math.min(12, strongest.size()); i++) {
                    Puff p = strongest.get(i);
                    blast(p.x, p.y, 80, 50, 30, t, DamageType.EXPLOSIVE, true);
                }
                sfx(Sound.Sfx.BOOM);
            }
            case OWL_SQUADRON -> {
                for (int i = 0; i < 30; i++) {
                    List<Puff> s = byStrength();
                    if (s.isEmpty()) break;
                    Puff p = s.get(0);
                    if (i % 3 == 0) particles.add(Particle.streak(t.x, t.y - 14, p.x, p.y, new Color(255, 246, 200)));
                    damage(p, t.effDamage(), DamageType.NORMAL, t);
                }
            }
            case DEEP_WINTER -> {
                for (Puff p : puffs) {
                    if (p.dead) continue;
                    if (p.type.isBoss()) {
                        p.slowTicks = 240;
                        p.slowFactor = 0.4;
                    } else if (!p.type.freezeImmune()) {
                        p.frozen = Math.max(p.frozen, 240);
                        p.brittle |= t.brittle;
                    }
                }
                particles.add(Particle.ring(GameMap.W / 2.0, GameMap.H / 2.0, 520, new Color(0xBFE6FF)));
            }
            case GREAT_CROAK -> {
                for (Puff p : puffs) {
                    if (p.dead) continue;
                    if (p.type.isBoss()) damage(p, 300, DamageType.MAGIC, t);
                    else pushBack(p, 150);
                }
                particles.add(Particle.ring(t.x, t.y, 400, new Color(0xC9A8FF)));
            }
            case HARVEST_MOON -> {
                int amount = t.income * 3;
                cash += amount;
                particles.add(Particle.coin(t.x, t.y - 30, amount));
                sfx(Sound.Sfx.COIN);
            }
            case SLIME_FLOOD -> {
                for (Puff p : puffs) if (!p.dead) applySlime(p, t, true);
                particles.add(Particle.ring(GameMap.W / 2.0, GameMap.H / 2.0, 520, new Color(0x9BD46A)));
            }
            case THORN_STORM -> {
                for (int i = 0; i < 30; i++) {
                    int tr = rnd.nextInt(map.tracks.size());
                    Point2D.Double p = map.pointAt(tr, rnd.nextDouble() * map.length(tr) * 0.95);
                    Projectile b = Projectile.burr(t, p.x, p.y, p.x, p.y);
                    b.travel = 0;
                    b.vx = 0;
                    b.vy = 0;
                    b.life = t.burrLife;
                    projectiles.add(b);
                }
            }
            case BONFIRE_NIGHT -> {
                globalSurge = 600;
                particles.add(Particle.ring(t.x, t.y, 300, new Color(0xFFC86A)));
            }
            case HORNET_HURRICANE -> {
                t.surgeTicks = 600;
                t.surgeFactor = 0.25;
            }
            case TIDAL_WAVE -> {
                for (Puff p : puffs) {
                    if (p.dead) continue;
                    damage(p, 25, DamageType.NORMAL, t);
                    if (!p.dead && !p.type.isBoss()) pushBack(p, 100);
                }
                particles.add(Particle.ring(GameMap.W / 2.0, GameMap.H / 2.0, 520, new Color(0x5BA8D8)));
            }
            case PINECONE_BARRAGE -> {
                for (int i = 0; i < 40; i++) {
                    double bx, by;
                    List<Puff> alive = puffs.stream().filter(p -> !p.dead).toList();
                    if (!alive.isEmpty()) {
                        Puff p = alive.get(rnd.nextInt(alive.size()));
                        bx = p.x;
                        by = p.y;
                    } else {
                        int tr = rnd.nextInt(map.tracks.size());
                        Point2D.Double q = map.pointAt(tr, rnd.nextDouble() * map.length(tr));
                        bx = q.x;
                        by = q.y;
                    }
                    blast(bx, by, 45, 5, 15, t, DamageType.EXPLOSIVE, true);
                }
                sfx(Sound.Sfx.BOOM);
            }
            case CAVE_IN -> {
                for (Puff p : puffs) {
                    if (p.dead) continue;
                    damage(p, 10, DamageType.NORMAL, t);
                    if (p.dead) continue;
                    if (p.type.isBoss()) p.stun = Math.max(p.stun, 120);
                    else pushBack(p, 200);
                }
                particles.add(Particle.ring(t.x, t.y, 400, new Color(0xA8865A)));
                sfx(Sound.Sfx.THUMP);
            }
        }
        return true;
    }

    private List<Puff> byStrength() {
        List<Puff> l = new ArrayList<>();
        for (Puff p : puffs) if (!p.dead) l.add(p);
        l.sort(Comparator.comparingInt(Puff::rbe).reversed());
        return l;
    }
}
