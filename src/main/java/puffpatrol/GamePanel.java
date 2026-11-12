package puffpatrol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class GamePanel extends JPanel {
    static final int PW = GameMap.W, PH = GameMap.H, SW = 240, W = PW + SW;

    private static final Font TITLE_FONT = new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 56);
    private static final Font SUB_FONT = new Font(Font.SERIF, Font.ITALIC, 18);
    private static final Font H_FONT = new Font(Font.SERIF, Font.BOLD, 16);
    private static final Font B_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font S_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    private static final Font STAT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private static final Color WOOD = new Color(0x8B6B4A);
    private static final Color GOOD = new Color(0x5E9A45);
    private static final Color BAD = new Color(0xC0453B);

    private enum Screen { TITLE, PLAYING }

    private final List<GameMap> maps = Maps.all();
    private Screen screen = Screen.TITLE;
    private World world;
    private TowerType placing;
    private Tower selected;
    private int hoverShop = -1;
    private boolean fast;
    private int mx = -100, my = -100;
    private long lastNanos;
    private double accumulator;
    private final Timer timer;

    // sidebar layout
    private final Rectangle[] shopRects = new Rectangle[TowerType.values().length];
    private final Rectangle[] upgradeRects = {new Rectangle(PW + 12, 400, 216, 58), new Rectangle(PW + 12, 462, 216, 58)};
    private final Rectangle targetRect = new Rectangle(PW + 12, 524, 106, 30);
    private final Rectangle sellRect = new Rectangle(PW + 122, 524, 106, 30);
    private final Rectangle startRect = new Rectangle(PW + 12, 572, 156, 56);
    private final Rectangle autoRect = new Rectangle(PW + 172, 572, 56, 56);
    private final Rectangle[] mapCards = {new Rectangle(120, 230, 380, 304), new Rectangle(540, 230, 380, 304)};
    private final Rectangle continueRect = new Rectangle(PW / 2 - 150, 360, 300, 44);
    private final Rectangle titleRect = new Rectangle(PW / 2 - 150, 414, 300, 44);

    GamePanel() {
        setPreferredSize(new Dimension(W, PH));
        setFocusable(true);
        setBackground(Art.PARCHMENT);
        for (int i = 0; i < shopRects.length; i++) {
            shopRects[i] = new Rectangle(PW + 12 + (i % 2) * 110, 64 + (i / 2) * 76, 104, 70);
        }
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mx = e.getX();
                my = e.getY();
                hoverShop = -1;
                for (int i = 0; i < shopRects.length; i++) if (shopRects[i].contains(mx, my)) hoverShop = i;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                click(e.getX(), e.getY(), SwingUtilities.isRightMouseButton(e), e.isShiftDown());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                key(e);
            }
        });
        timer = new Timer(15, e -> tick());
    }

    void start() {
        lastNanos = System.nanoTime();
        timer.start();
        requestFocusInWindow();
    }

    /** Starts a game directly (used by tools and tests). */
    World newGame(GameMap map) {
        world = new World(map);
        screen = Screen.PLAYING;
        placing = null;
        selected = null;
        fast = false;
        return world;
    }

    void select(Tower t) {
        selected = t;
    }

    void setMouse(int x, int y) {
        mx = x;
        my = y;
    }

    void setPlacing(TowerType t) {
        placing = t;
    }

    private void tick() {
        long now = System.nanoTime();
        accumulator += Math.min(0.25, (now - lastNanos) / 1e9);
        lastNanos = now;
        double step = 1.0 / 60;
        while (accumulator >= step) {
            accumulator -= step;
            if (world != null && !world.gameOver && !world.pausedForVictory()) {
                for (int i = 0; i < (fast ? 3 : 1); i++) world.update();
            }
        }
        if (selected != null && world != null && !world.towers.contains(selected)) selected = null;
        repaint();
    }

    // ------------------------------------------------------------------ input

    private void click(int x, int y, boolean right, boolean shift) {
        if (screen == Screen.TITLE) {
            for (int i = 0; i < mapCards.length; i++) if (mapCards[i].contains(x, y)) newGame(maps.get(i));
            return;
        }
        if (world.gameOver) {
            screen = Screen.TITLE;
            return;
        }
        if (world.pausedForVictory()) {
            if (continueRect.contains(x, y)) world.victoryAcknowledged = true;
            else if (titleRect.contains(x, y)) screen = Screen.TITLE;
            return;
        }
        if (right) {
            placing = null;
            selected = null;
            return;
        }
        if (x < PW) {
            if (placing != null) {
                Tower t = world.place(placing, x, y);
                if (t != null && (!shift || !world.canAfford(placing.cost))) {
                    placing = null;
                    selected = t;
                }
                return;
            }
            selected = towerAt(x, y);
            return;
        }
        for (int i = 0; i < shopRects.length; i++) {
            if (shopRects[i].contains(x, y)) {
                pickShop(i);
                return;
            }
        }
        if (selected != null) {
            for (int p = 0; p < 2; p++) if (upgradeRects[p].contains(x, y)) world.upgrade(selected, p);
            if (selected.type.attack != TowerType.Attack.FARM && selected.type.attack != TowerType.Attack.PULSE
                    && selected.type.attack != TowerType.Attack.RADIAL && targetRect.contains(x, y)) {
                selected.mode = selected.mode.next();
            }
            if (sellRect.contains(x, y)) {
                world.sell(selected);
                selected = null;
            }
        }
        if (startRect.contains(x, y)) startOrFast();
        if (autoRect.contains(x, y)) world.autoStart = !world.autoStart;
    }

    private void pickShop(int i) {
        TowerType t = TowerType.values()[i];
        if (world.canAfford(t.cost)) {
            placing = placing == t ? null : t;
            selected = null;
        }
    }

    private void startOrFast() {
        if (!world.roundActive) world.startRound();
        else fast = !fast;
    }

    private Tower towerAt(int x, int y) {
        for (int i = world.towers.size() - 1; i >= 0; i--) {
            Tower t = world.towers.get(i);
            if (Math.hypot(t.x - x, t.y - y) < 24) return t;
        }
        return null;
    }

    private void key(KeyEvent e) {
        if (screen == Screen.TITLE) {
            if (e.getKeyCode() == KeyEvent.VK_1) newGame(maps.get(0));
            if (e.getKeyCode() == KeyEvent.VK_2) newGame(maps.get(1));
            return;
        }
        if (world.gameOver || world.pausedForVictory()) return;
        int code = e.getKeyCode();
        if (code >= KeyEvent.VK_1 && code < KeyEvent.VK_1 + shopRects.length) pickShop(code - KeyEvent.VK_1);
        switch (code) {
            case KeyEvent.VK_SPACE -> startOrFast();
            case KeyEvent.VK_F -> fast = !fast;
            case KeyEvent.VK_A -> world.autoStart = !world.autoStart;
            case KeyEvent.VK_ESCAPE -> {
                placing = null;
                selected = null;
            }
            case KeyEvent.VK_Z -> { if (selected != null) world.upgrade(selected, 0); }
            case KeyEvent.VK_X -> { if (selected != null) world.upgrade(selected, 1); }
            case KeyEvent.VK_TAB -> { if (selected != null) selected.mode = selected.mode.next(); }
            case KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE -> {
                if (selected != null) {
                    world.sell(selected);
                    selected = null;
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        Art.smooth(g);
        if (screen == Screen.TITLE) drawTitle(g);
        else {
            drawPlay(g);
            drawSidebar(g);
            drawOverlays(g);
        }
        g.dispose();
    }

    private void drawTitle(Graphics2D g) {
        g.drawImage(maps.get(0).background(), 0, 0, null);
        g.drawImage(maps.get(1).background(), PW, 0, PW + SW, PH, PW - SW, 0, PW, PH, null);
        g.setColor(new Color(244, 235, 211, 215));
        g.fillRect(0, 0, W, PH);
        // a parade of puffs drifting across the top
        long t = System.currentTimeMillis();
        PuffType[] parade = {PuffType.ROSE, PuffType.SKY, PuffType.MOSS, PuffType.BUTTER, PuffType.BLUSH,
                PuffType.SOOT, PuffType.FROST, PuffType.PEWTER, PuffType.MARBLE, PuffType.PRISM, PuffType.CLAY};
        for (int i = 0; i < parade.length; i++) {
            BufferedImage img = Art.puff(parade[i], 0);
            double x = ((t / 25.0 + i * 95) % (W + 100)) - 50;
            double y = 40 + Math.sin(t / 600.0 + i) * 6;
            g.drawImage(img, (int) (x - img.getWidth() / 2.0), (int) (y - img.getHeight() / 2.0), null);
        }
        centered(g, "Puffball Patrol", TITLE_FONT, Art.INK, W / 2, 130);
        centered(g, "In which the meadow creatures politely decline a visit from the clouds.", SUB_FONT,
                new Color(0x5A4632), W / 2, 168);
        centered(g, "Choose a place to defend", H_FONT, new Color(0x5A4632), W / 2, 214);
        for (int i = 0; i < mapCards.length; i++) {
            Rectangle r = mapCards[i];
            GameMap m = maps.get(i);
            boolean hover = r.contains(mx, my);
            g.setColor(new Color(0, 0, 0, 50));
            g.fill(new RoundRectangle2D.Double(r.x + 4, r.y + 6, r.width, r.height, 18, 18));
            Shape card = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 18, 18);
            Shape old = g.getClip();
            g.clip(card);
            g.drawImage(m.background(), r.x, r.y, r.width, r.height - 64, null);
            g.setColor(Art.PARCHMENT);
            g.fillRect(r.x, r.y + r.height - 64, r.width, 64);
            g.setClip(old);
            g.setStroke(new BasicStroke(hover ? 4f : 2.5f));
            g.setColor(hover ? new Color(0xD9A21E) : WOOD);
            g.draw(card);
            g.setFont(H_FONT);
            g.setColor(Art.INK);
            g.drawString((i + 1) + ".  " + m.name, r.x + 14, r.y + r.height - 38);
            g.setFont(S_FONT);
            g.setColor(new Color(0x5A4632));
            g.drawString(m.blurb, r.x + 14, r.y + r.height - 16);
        }
        centered(g, "Place critters from the shop, then press Space to let the puffs in. Survive 40 rounds.",
                S_FONT, new Color(0x5A4632), W / 2, 580);
        centered(g, "Space start / fast-forward    1-7 shop    Z / X upgrade    Tab target    Del sell    A auto-start",
                S_FONT, new Color(0x7A6650), W / 2, 600);
    }

    private void drawPlay(Graphics2D g) {
        Shape oldClip = g.getClip();
        g.clipRect(0, 0, PW, PH);
        g.drawImage(world.map.background(), 0, 0, null);

        if (selected != null) drawRange(g, selected.x, selected.y, selected.range, true);

        // puffs: biggest first so little ones stay visible on top
        List<Puff> sorted = new ArrayList<>(world.puffs);
        sorted.sort(Comparator.comparingDouble(Puff::radius).reversed());
        for (Puff p : sorted) drawPuff(g, p);

        for (Tower t : world.towers) drawTower(g, t);

        for (Projectile p : world.projectiles) {
            BufferedImage img = Art.projectile(p.kind, p.owner.fire);
            AffineTransform old = g.getTransform();
            g.translate(p.x, p.y);
            if (p.kind == Projectile.Kind.ACORN) g.rotate(p.life * 0.25);
            else g.rotate(p.angle);
            g.drawImage(img, -img.getWidth() / 2, -img.getHeight() / 2, null);
            g.setTransform(old);
        }
        for (Particle p : world.particles) p.draw(g);

        if (placing != null && mx < PW && mx >= 0) {
            boolean ok = world.map.canPlace(mx, my, world.towers) && world.canAfford(placing.cost);
            Tower ghost = new Tower(placing, mx, my);
            drawRange(g, mx, my, ghost.range, ok);
            Composite c = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            BufferedImage img = Art.tower(placing);
            g.drawImage(img, mx - 32, my - 40, null);
            g.setComposite(c);
            if (!ok) {
                g.setColor(BAD);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(mx - 8, my - 8, mx + 8, my + 8);
                g.drawLine(mx + 8, my - 8, mx - 8, my + 8);
            }
        }

        if (world.bannerTicks > 0 && world.banner != null) {
            float a = Math.min(1f, world.bannerTicks / 30f);
            g.setFont(H_FONT);
            int w = g.getFontMetrics().stringWidth(world.banner) + 40;
            Shape box = new RoundRectangle2D.Double(PW / 2.0 - w / 2.0, 14, w, 34, 16, 16);
            g.setColor(Art.alpha(Art.PARCHMENT, (int) (230 * a)));
            g.fill(box);
            g.setColor(Art.alpha(WOOD, (int) (255 * a)));
            g.setStroke(new BasicStroke(2f));
            g.draw(box);
            g.setColor(Art.alpha(Art.INK, (int) (255 * a)));
            centered(g, world.banner, H_FONT, Art.alpha(Art.INK, (int) (255 * a)), PW / 2, 37);
        }
        if (fast) {
            g.setFont(H_FONT);
            g.setColor(new Color(255, 255, 255, 200));
            g.drawString("x3", PW - 34, PH - 12);
        }
        g.setClip(oldClip);
    }

    private void drawRange(Graphics2D g, double x, double y, double range, boolean ok) {
        double r = Math.min(range, 2000);
        if (r > 1000) return;
        Ellipse2D e = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
        g.setColor(ok ? new Color(255, 255, 255, 55) : new Color(220, 60, 50, 60));
        g.fill(e);
        g.setColor(ok ? new Color(255, 255, 255, 170) : new Color(200, 50, 40, 170));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6, 6}, 0));
        g.draw(e);
    }

    private void drawPuff(Graphics2D g, Puff p) {
        BufferedImage img = Art.puff(p.type, p.damageStage());
        double bob = p.frozen > 0 ? 0 : Math.sin(p.age * 0.12 + p.id) * 1.5;
        int dx = (int) Math.round(p.x - img.getWidth() / 2.0), dy = (int) Math.round(p.y + bob - img.getHeight() / 2.0);
        g.setColor(new Color(20, 30, 10, 45));
        double r = p.radius();
        g.fill(new Ellipse2D.Double(p.x - r * 0.8, p.y + r * 0.75, r * 1.6, r * 0.45));
        g.drawImage(img, dx, dy, null);
        if (p.frozen > 0) g.drawImage(Art.ice(p.type), dx, dy, null);
        if (p.type.isBoss()) {
            double w = r * 1.6, f = p.hp / (double) p.type.hp;
            double bx = p.x - w / 2, by = p.y - r * 1.15;
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(new RoundRectangle2D.Double(bx - 1, by - 1, w + 2, 7, 6, 6));
            g.setColor(new Color(0xE5484D));
            g.fill(new RoundRectangle2D.Double(bx, by, w * f, 5, 5, 5));
        }
    }

    private void drawTower(Graphics2D g, Tower t) {
        BufferedImage img = Art.tower(t.type);
        g.setColor(new Color(20, 30, 10, 60));
        g.fill(new Ellipse2D.Double(t.x - 20, t.y + 10, 40, 12));
        double squash = t.anim > 0 ? 1 + Math.sin(t.anim / 8.0 * Math.PI) * 0.08 : 1;
        AffineTransform old = g.getTransform();
        g.translate(t.x, t.y + 22);
        g.scale(t.facing * (2 - squash), squash);
        g.drawImage(img, -32, -62, null);
        g.setTransform(old);
        if (t == selected) {
            g.setColor(new Color(255, 236, 140, 200));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(t.x - 22, t.y + 8, 44, 16));
        }
        // upgrade pips
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i < t.tiers[p]; i++) {
                double px = t.x + (p == 0 ? -8 - i * 7 : 8 + i * 7), py = t.y + 26;
                Art.ink(g, new Ellipse2D.Double(px - 3, py - 3, 6, 6), p == 0 ? new Color(0xF0766A) : new Color(0x6AAEF0), 1f);
            }
        }
    }

    private void drawSidebar(Graphics2D g) {
        g.setPaint(Art.vgrad(0, new Color(0xF7EFD9), PH, new Color(0xE9DCBC)));
        g.fillRect(PW, 0, SW, PH);
        g.setColor(WOOD);
        g.fillRect(PW, 0, 5, PH);
        g.setColor(new Color(0x6B4F35));
        g.fillRect(PW + 5, 0, 1, PH);

        // stats
        Art.drawHeart(g, PW + 24, 22, 9);
        Art.drawCoin(g, PW + 100, 22, 9);
        g.setFont(STAT_FONT);
        g.setColor(Art.INK);
        g.drawString(String.valueOf(world.lives), PW + 38, 28);
        g.drawString(String.valueOf(world.cash), PW + 114, 28);
        g.setFont(B_FONT);
        String rnd = "Round " + world.displayRound() + (world.displayRound() <= Waves.FINAL_ROUND ? " / " + Waves.FINAL_ROUND : " (freeplay)");
        g.drawString(rnd, PW + 18, 50);
        g.setColor(new Color(0x7A6650));
        g.drawString("pops " + world.totalPops, PW + 160, 50);

        // shop
        TowerType[] types = TowerType.values();
        for (int i = 0; i < types.length; i++) {
            TowerType t = types[i];
            Rectangle r = shopRects[i];
            boolean afford = world.canAfford(t.cost);
            boolean active = placing == t;
            Shape box = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 12, 12);
            g.setColor(active ? new Color(0xFFF1B8) : i == hoverShop ? new Color(0xFBF6E8) : new Color(0xF3E8CC));
            g.fill(box);
            g.setColor(active ? new Color(0xD9A21E) : WOOD);
            g.setStroke(new BasicStroke(active ? 2.5f : 1.5f));
            g.draw(box);
            Composite c = g.getComposite();
            if (!afford) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            g.drawImage(Art.tower(t), r.x + 2, r.y - 4, 50, 50, null);
            g.setComposite(c);
            g.setFont(S_FONT);
            g.setColor(new Color(0x7A6650));
            g.drawString(String.valueOf(i + 1), r.x + r.width - 12, r.y + 14);
            Art.drawCoin(g, r.x + 60, r.y + 30, 5.5);
            g.setFont(B_FONT);
            g.setColor(afford ? Art.INK : BAD);
            g.drawString(String.valueOf(t.cost), r.x + 68, r.y + 35);
            g.setFont(S_FONT);
            g.setColor(Art.INK);
            String[] words = t.label.split(" ");
            g.drawString(words[0], r.x + 8, r.y + 54);
            if (words.length > 1) g.drawString(words[1], r.x + 8, r.y + 66);
        }

        // info panel
        g.setColor(new Color(0xD8C8A4));
        g.fillRect(PW + 12, 366, 216, 1);
        if (selected != null) drawTowerPanel(g, selected);
        else {
            TowerType info = hoverShop >= 0 ? types[hoverShop] : placing;
            if (info != null) {
                g.setFont(H_FONT);
                g.setColor(Art.INK);
                g.drawString(info.label, PW + 14, 392);
                g.setFont(S_FONT);
                wrap(g, info.desc, PW + 14, 414, 210, 15);
                g.setColor(new Color(0x7A6650));
                wrap(g, placing != null ? "Click the meadow to place. Shift-click to place several. Right-click to cancel."
                        : "Click to pick up.", PW + 14, 490, 210, 15);
            } else {
                g.setFont(S_FONT);
                g.setColor(new Color(0x7A6650));
                wrap(g, "Pick a critter from the shop above, then click somewhere on the grass (not the path) to set it down. "
                        + "Click a placed critter to upgrade it or to sell it.", PW + 14, 390, 210, 15);
                wrap(g, "Each puff you pop drops a coin. Bigger puffs have smaller puffs inside them.",
                        PW + 14, 480, 210, 15);
            }
        }

        // start / fast and auto
        boolean active = world.roundActive;
        Shape sb = new RoundRectangle2D.Double(startRect.x, startRect.y, startRect.width, startRect.height, 14, 14);
        g.setPaint(Art.vgrad(startRect.y, active ? (fast ? new Color(0xF7C86A) : new Color(0xE9D9B0)) : new Color(0x8FCB6A),
                startRect.y + startRect.height, active ? (fast ? new Color(0xD9A21E) : new Color(0xCDB98A)) : GOOD));
        g.fill(sb);
        Art.ink(g, sb, null, 2f);
        String label = active ? (fast ? "Fast  >>>" : "Speed  >") : "Start Round " + world.displayRound();
        centered(g, label, H_FONT, Art.INK, startRect.x + startRect.width / 2, startRect.y + 34);
        Shape ab = new RoundRectangle2D.Double(autoRect.x, autoRect.y, autoRect.width, autoRect.height, 14, 14);
        g.setColor(world.autoStart ? new Color(0xBFE0A8) : new Color(0xEDE2C6));
        g.fill(ab);
        Art.ink(g, ab, null, 2f);
        centered(g, "Auto", B_FONT, Art.INK, autoRect.x + autoRect.width / 2, autoRect.y + 26);
        centered(g, world.autoStart ? "on" : "off", S_FONT, new Color(0x5A4632), autoRect.x + autoRect.width / 2, autoRect.y + 42);
    }

    private void drawTowerPanel(Graphics2D g, Tower t) {
        g.setFont(H_FONT);
        g.setColor(Art.INK);
        g.drawString(t.type.label, PW + 14, 390);
        g.setFont(S_FONT);
        g.setColor(new Color(0x7A6650));
        String sub = t.type.attack == TowerType.Attack.FARM ? "+" + t.income + " coins / round" : t.pops + " pops";
        g.drawString(sub, PW + 226 - g.getFontMetrics().stringWidth(sub), 390);
        for (int p = 0; p < 2; p++) {
            Rectangle r = upgradeRects[p];
            Upgrade u = t.nextUpgrade(p);
            boolean locked = u != null && t.pathLocked(p);
            boolean afford = u != null && !locked && world.canAfford(u.cost());
            Shape box = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 12, 12);
            Color tint = p == 0 ? new Color(0xFBE3DC) : new Color(0xDDEBFA);
            g.setColor(u == null || locked ? new Color(0xE6DCC4) : r.contains(mx, my) && afford ? tint.brighter() : tint);
            g.fill(box);
            g.setColor(afford ? (p == 0 ? new Color(0xC0453B) : new Color(0x3E6EB0)) : WOOD);
            g.setStroke(new BasicStroke(afford ? 2f : 1.2f));
            g.draw(box);
            for (int i = 0; i < 3; i++) {
                Ellipse2D pip = new Ellipse2D.Double(r.x + r.width - 40 + i * 11, r.y + 7, 8, 8);
                Art.ink(g, pip, i < t.tiers[p] ? (p == 0 ? new Color(0xF0766A) : new Color(0x6AAEF0)) : new Color(0xF7F1E2), 1f);
            }
            g.setFont(B_FONT);
            g.setColor(Art.INK);
            if (u == null) {
                g.drawString("Path complete", r.x + 10, r.y + 18);
            } else if (locked) {
                g.drawString(u.name(), r.x + 10, r.y + 18);
                g.setFont(S_FONT);
                g.setColor(new Color(0x7A6650));
                g.drawString("Locked: the other path went all the way.", r.x + 10, r.y + 36);
            } else {
                g.drawString(u.name(), r.x + 10, r.y + 18);
                g.setFont(S_FONT);
                g.setColor(new Color(0x5A4632));
                wrap(g, u.desc(), r.x + 10, r.y + 33, 150, 13);
                Art.drawCoin(g, r.x + r.width - 52, r.y + r.height - 13, 5);
                g.setFont(B_FONT);
                g.setColor(afford ? Art.INK : BAD);
                g.drawString(String.valueOf(u.cost()), r.x + r.width - 44, r.y + r.height - 9);
            }
            g.setFont(S_FONT);
            g.setColor(new Color(0x9A8668));
            g.drawString(p == 0 ? "Z" : "X", r.x + r.width - 12, r.y + r.height - 9 - 20);
        }
        boolean aims = t.type.attack != TowerType.Attack.FARM && t.type.attack != TowerType.Attack.PULSE
                && t.type.attack != TowerType.Attack.RADIAL;
        Shape tb = new RoundRectangle2D.Double(targetRect.x, targetRect.y, targetRect.width, targetRect.height, 10, 10);
        g.setColor(aims ? new Color(0xF3E8CC) : new Color(0xE6DCC4));
        g.fill(tb);
        Art.ink(g, tb, null, 1.2f);
        centered(g, aims ? "Target: " + t.mode.label : "No aiming", B_FONT, aims ? Art.INK : new Color(0x9A8668),
                targetRect.x + targetRect.width / 2, targetRect.y + 20);
        Shape sb = new RoundRectangle2D.Double(sellRect.x, sellRect.y, sellRect.width, sellRect.height, 10, 10);
        g.setColor(sellRect.contains(mx, my) ? new Color(0xF6D2C8) : new Color(0xF3E8CC));
        g.fill(sb);
        Art.ink(g, sb, null, 1.2f);
        centered(g, "Sell  " + t.sellValue(), B_FONT, Art.INK, sellRect.x + sellRect.width / 2, sellRect.y + 20);
    }

    private void drawOverlays(Graphics2D g) {
        if (world.gameOver) {
            dim(g);
            centered(g, "The puffs drifted on through.", new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 36), Art.PARCHMENT, PW / 2, 280);
            centered(g, "You reached round " + world.round + " and popped " + world.totalPops + " layers of cloud.",
                    SUB_FONT, Art.PARCHMENT, PW / 2, 320);
            centered(g, "Click anywhere to return to the title page.", S_FONT, new Color(0xE9DCBC), PW / 2, 360);
        } else if (world.pausedForVictory()) {
            dim(g);
            centered(g, "The meadow is quiet again.", new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 36), Art.PARCHMENT, PW / 2, 280);
            centered(g, "All " + Waves.FINAL_ROUND + " rounds weathered with " + world.lives + " lives to spare.",
                    SUB_FONT, Art.PARCHMENT, PW / 2, 320);
            button(g, continueRect, "Keep going (freeplay)");
            button(g, titleRect, "Back to the title page");
        }
    }

    private void dim(Graphics2D g) {
        g.setColor(new Color(30, 22, 12, 160));
        g.fillRect(0, 0, PW, PH);
    }

    private void button(Graphics2D g, Rectangle r, String text) {
        Shape b = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 14, 14);
        g.setColor(r.contains(mx, my) ? new Color(0xFFF1B8) : Art.PARCHMENT);
        g.fill(b);
        Art.ink(g, b, null, 2f);
        centered(g, text, H_FONT, Art.INK, r.x + r.width / 2, r.y + 28);
    }

    private static void centered(Graphics2D g, String s, Font f, Color c, int cx, int y) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
    }

    private static void wrap(Graphics2D g, String text, int x, int y, int width, int lh) {
        FontMetrics fm = g.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String w : text.split(" ")) {
            String trial = line.isEmpty() ? w : line + " " + w;
            if (fm.stringWidth(trial) > width && !line.isEmpty()) {
                g.drawString(line.toString(), x, y);
                y += lh;
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (!line.isEmpty()) g.drawString(line.toString(), x, y);
    }
}
