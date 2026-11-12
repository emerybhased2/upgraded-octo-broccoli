package puffpatrol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class GamePanel extends JPanel {
    static final int PW = GameMap.W, PH = GameMap.H, SW = 240, W = PW + SW;

    private static final Font TITLE_FONT = new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 50);
    private static final Font BIG_FONT = new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 34);
    private static final Font SUB_FONT = new Font(Font.SERIF, Font.ITALIC, 17);
    private static final Font H_FONT = new Font(Font.SERIF, Font.BOLD, 16);
    private static final Font B_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font S_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    private static final Font STAT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private static final Color WOOD = new Color(0x8B6B4A);
    private static final Color MUTED = new Color(0x7A6650);
    private static final Color GOOD = new Color(0x5E9A45);
    private static final Color BAD = new Color(0xC0453B);
    private static final int[] ABILITY_KEYS = {KeyEvent.VK_Q, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_R, KeyEvent.VK_T, KeyEvent.VK_Y, KeyEvent.VK_U};
    private static final String ABILITY_KEY_NAMES = "QWERTYU";

    private enum Screen { TITLE, PLAYING }

    private final Profile profile;
    private final List<GameMap> maps = Maps.all();
    private Screen screen = Screen.TITLE;
    private World world;
    private Difficulty difficulty;
    private TowerType placing;
    private Tower selected;
    private int hoverShop = -1;
    private int speed = 1;
    private boolean paused;
    private int mx = -100, my = -100;
    private long lastNanos;
    private double accumulator;
    private String saveSummary;
    private final Timer timer;

    // title layout
    private final Rectangle[] diffRects = new Rectangle[Difficulty.values().length];
    private final Rectangle[] mapCards = new Rectangle[8];
    private final Rectangle continueRect = new Rectangle(W - 236, 16, 220, 46);
    private final Rectangle soundRect = new Rectangle(30, 598, 130, 28);
    private final Rectangle unlockRect = new Rectangle(170, 598, 210, 28);

    // play layout
    private final Rectangle[] shopRects = new Rectangle[TowerType.values().length];
    private final Rectangle[] upgradeRects = {new Rectangle(PW + 12, 408, 216, 50), new Rectangle(PW + 12, 462, 216, 50)};
    private final Rectangle targetRect = new Rectangle(PW + 12, 516, 106, 26);
    private final Rectangle sellRect = new Rectangle(PW + 122, 516, 106, 26);
    private final Rectangle startRect = new Rectangle(PW + 12, 576, 156, 52);
    private final Rectangle autoRect = new Rectangle(PW + 172, 576, 56, 52);
    private final Rectangle pauseBtn = new Rectangle(PW + 204, 8, 28, 28);
    private final Rectangle victoryGo = new Rectangle(PW / 2 - 150, 360, 300, 44);
    private final Rectangle victoryTitle = new Rectangle(PW / 2 - 150, 414, 300, 44);
    private final Rectangle[] pauseRects = new Rectangle[6];

    GamePanel(Profile profile) {
        this.profile = profile;
        this.difficulty = profile.lastDifficulty;
        Sound.enabled = profile.soundOn;
        setPreferredSize(new Dimension(W, PH));
        setFocusable(true);
        setBackground(Art.PARCHMENT);
        for (int i = 0; i < shopRects.length; i++) {
            shopRects[i] = new Rectangle(PW + 12 + (i % 3) * 74, 60 + (i / 3) * 64, 70, 60);
        }
        int dw = 150, dx0 = (W - (4 * dw + 3 * 12)) / 2;
        for (int i = 0; i < diffRects.length; i++) diffRects[i] = new Rectangle(dx0 + i * (dw + 12), 118, dw, 34);
        for (int i = 0; i < mapCards.length; i++) mapCards[i] = new Rectangle(30 + (i % 4) * 250, 190 + (i / 4) * 202, 230, 190);
        for (int i = 0; i < pauseRects.length; i++) pauseRects[i] = new Rectangle(PW / 2 - 140, 196 + i * 50, 280, 40);
        refreshSave();

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mx = e.getX();
                my = e.getY();
                hoverShop = -1;
                if (screen == Screen.PLAYING) {
                    for (int i = 0; i < shopRects.length; i++) if (shopRects[i].contains(mx, my)) hoverShop = i;
                }
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
        setFocusTraversalKeysEnabled(false);
        timer = new Timer(15, e -> tick());
    }

    void start() {
        lastNanos = System.nanoTime();
        timer.start();
        requestFocusInWindow();
    }

    // ------------------------------------------------------------------ game lifecycle

    World newGame(GameMap map, Difficulty d) {
        return begin(new World(map, d, profile));
    }

    private World begin(World w) {
        world = w;
        world.sound = Sound::play;
        world.onRoundEnd = () -> {
            if (world == w && !w.gameOver) {
                SaveGame.save(w);
                refreshSave();
            }
        };
        screen = Screen.PLAYING;
        placing = null;
        selected = null;
        paused = false;
        speed = 1;
        return w;
    }

    private void toTitle() {
        screen = Screen.TITLE;
        world = null;
        paused = false;
        refreshSave();
    }

    private void refreshSave() {
        saveSummary = SaveGame.summary();
    }

    // test & tool hooks
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

    void setPaused(boolean p) {
        paused = p;
    }

    private void tick() {
        long now = System.nanoTime();
        accumulator += Math.min(0.25, (now - lastNanos) / 1e9);
        lastNanos = now;
        double step = 1.0 / 60;
        while (accumulator >= step) {
            accumulator -= step;
            if (world != null && !paused && !world.gameOver && !world.pausedForVictory()) {
                for (int i = 0; i < speed; i++) world.update();
                if (world.gameOver) {
                    SaveGame.delete();
                    refreshSave();
                }
            }
        }
        if (selected != null && world != null && !world.towers.contains(selected)) selected = null;
        repaint();
    }

    // ------------------------------------------------------------------ input

    private void click(int x, int y, boolean right, boolean shift) {
        if (screen == Screen.TITLE) {
            clickTitle(x, y);
            return;
        }
        if (world.gameOver) {
            toTitle();
            return;
        }
        if (paused) {
            clickPause(x, y);
            return;
        }
        if (world.pausedForVictory()) {
            if (victoryGo.contains(x, y)) world.victoryAcknowledged = true;
            else if (victoryTitle.contains(x, y)) {
                SaveGame.save(world);
                toTitle();
            }
            return;
        }
        if (right) {
            placing = null;
            selected = null;
            return;
        }
        if (pauseBtn.contains(x, y)) {
            paused = true;
            return;
        }
        if (x < PW) {
            List<Ability> abs = world.abilities();
            for (int i = 0; i < abs.size(); i++) {
                if (abilityRect(i).contains(x, y)) {
                    world.activate(abs.get(i));
                    return;
                }
            }
            if (placing != null) {
                Tower t = world.place(placing, x, y);
                if (t != null && (!shift || !world.canAfford(world.cost(placing)))) {
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
            if (selected.type.aims() && targetRect.contains(x, y)) selected.mode = selected.mode.next();
            if (sellRect.contains(x, y)) {
                world.sell(selected);
                selected = null;
            }
        }
        if (startRect.contains(x, y)) startOrSpeed();
        if (autoRect.contains(x, y)) world.autoStart = !world.autoStart;
    }

    private void clickTitle(int x, int y) {
        if (saveSummary != null && continueRect.contains(x, y)) {
            World w = SaveGame.load(profile);
            if (w != null) begin(w);
            else refreshSave();
            return;
        }
        for (int i = 0; i < diffRects.length; i++) {
            if (diffRects[i].contains(x, y)) {
                difficulty = Difficulty.values()[i];
                profile.lastDifficulty = difficulty;
                profile.save();
            }
        }
        for (int i = 0; i < mapCards.length && i < maps.size(); i++) {
            if (mapCards[i].contains(x, y)) newGame(maps.get(i), difficulty);
        }
        if (soundRect.contains(x, y)) toggleSound();
        if (unlockRect.contains(x, y)) toggleUnlocks();
    }

    private void clickPause(int x, int y) {
        int hit = -1;
        for (int i = 0; i < pauseRects.length; i++) if (pauseRects[i].contains(x, y)) hit = i;
        switch (hit) {
            case 0 -> paused = false;
            case 1 -> speed = speed % 3 + 1;
            case 2 -> toggleSound();
            case 3 -> toggleUnlocks();
            case 4 -> newGame(world.map, world.difficulty);
            case 5 -> {
                if (!world.roundActive) SaveGame.save(world);
                toTitle();
            }
            default -> {
            }
        }
    }

    private void toggleSound() {
        profile.soundOn = !profile.soundOn;
        Sound.enabled = profile.soundOn;
        profile.save();
    }

    private void toggleUnlocks() {
        profile.allUnlocked = !profile.allUnlocked;
        profile.save();
    }

    private void pickShop(int i) {
        TowerType t = TowerType.values()[i];
        if (world.canAfford(world.cost(t))) {
            placing = placing == t ? null : t;
            selected = null;
        }
    }

    private void startOrSpeed() {
        if (!world.roundActive) world.startRound();
        else speed = speed % 3 + 1;
    }

    private Tower towerAt(int x, int y) {
        for (int i = world.towers.size() - 1; i >= 0; i--) {
            Tower t = world.towers.get(i);
            if (Math.hypot(t.x - x, t.y - y) < 24) return t;
        }
        return null;
    }

    private void key(KeyEvent e) {
        int code = e.getKeyCode();
        if (screen == Screen.TITLE) {
            if (code >= KeyEvent.VK_1 && code < KeyEvent.VK_1 + maps.size()) newGame(maps.get(code - KeyEvent.VK_1), difficulty);
            return;
        }
        if (world.gameOver || world.pausedForVictory()) return;
        if (code == KeyEvent.VK_P || (code == KeyEvent.VK_ESCAPE && placing == null && selected == null)) {
            paused = !paused;
            return;
        }
        if (code == KeyEvent.VK_M) toggleSound();
        if (paused) return;
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) pickShop(code - KeyEvent.VK_1);
        if (code == KeyEvent.VK_0) pickShop(9);
        List<Ability> abs = world.abilities();
        for (int i = 0; i < ABILITY_KEYS.length && i < abs.size(); i++) if (code == ABILITY_KEYS[i]) world.activate(abs.get(i));
        switch (code) {
            case KeyEvent.VK_SPACE -> startOrSpeed();
            case KeyEvent.VK_F -> speed = speed % 3 + 1;
            case KeyEvent.VK_A -> world.autoStart = !world.autoStart;
            case KeyEvent.VK_ESCAPE -> {
                placing = null;
                selected = null;
            }
            case KeyEvent.VK_Z -> { if (selected != null) world.upgrade(selected, 0); }
            case KeyEvent.VK_X -> { if (selected != null) world.upgrade(selected, 1); }
            case KeyEvent.VK_TAB -> { if (selected != null && selected.type.aims()) selected.mode = selected.mode.next(); }
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

    private Rectangle abilityRect(int i) {
        return new Rectangle(10 + i * 54, PH - 58, 48, 48);
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
        g.drawImage(maps.get(2).background(), PW, 0, PW + SW, PH, PW - SW, 0, PW, PH, null);
        g.setColor(new Color(244, 235, 211, 222));
        g.fillRect(0, 0, W, PH);
        long t = System.currentTimeMillis();
        PuffType[] parade = {PuffType.ROSE, PuffType.SKY, PuffType.MOSS, PuffType.BUTTER, PuffType.BLUSH,
                PuffType.SOOT, PuffType.FROST, PuffType.PEWTER, PuffType.MARBLE, PuffType.PRISM, PuffType.CLAY};
        for (int i = 0; i < parade.length; i++) {
            BufferedImage img = Art.puff(parade[i], 0);
            double x = ((t / 25.0 + i * 95) % (W + 100)) - 50;
            double y = 22 + Math.sin(t / 600.0 + i) * 5;
            g.drawImage(img, (int) (x - img.getWidth() / 2.0), (int) (y - img.getHeight() / 2.0), null);
        }
        centered(g, "Puffball Patrol", TITLE_FONT, Art.INK, W / 2, 84);
        centered(g, "In which the meadow creatures politely decline a visit from the clouds.", SUB_FONT, new Color(0x5A4632), W / 2, 108);

        Difficulty[] ds = Difficulty.values();
        for (int i = 0; i < ds.length; i++) {
            Rectangle r = diffRects[i];
            boolean on = ds[i] == difficulty;
            Shape b = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 14, 14);
            g.setColor(on ? new Color(0xFFF1B8) : r.contains(mx, my) ? new Color(0xFBF6E8) : new Color(0xEFE3C4));
            g.fill(b);
            g.setColor(on ? new Color(0xD9A21E) : WOOD);
            g.setStroke(new BasicStroke(on ? 2.5f : 1.4f));
            g.draw(b);
            medal(g, r.x + 18, r.y + r.height / 2.0, 7, ds[i], true);
            centered(g, ds[i].label, H_FONT, Art.INK, r.x + r.width / 2 + 8, r.y + 23);
        }
        Difficulty hd = difficulty;
        for (int i = 0; i < ds.length; i++) if (diffRects[i].contains(mx, my)) hd = ds[i];
        centered(g, hd.desc, S_FONT, new Color(0x5A4632), W / 2, 172);

        for (int i = 0; i < mapCards.length && i < maps.size(); i++) drawMapCard(g, mapCards[i], maps.get(i), i);

        if (saveSummary != null) {
            Rectangle r = continueRect;
            Shape b = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 14, 14);
            g.setPaint(Art.vgrad(r.y, new Color(0x9FD47A), r.y + r.height, GOOD));
            g.fill(b);
            Art.ink(g, b, null, 2f);
            centered(g, "Continue", H_FONT, Art.INK, r.x + r.width / 2, r.y + 20);
            centered(g, saveSummary, S_FONT, Art.INK, r.x + r.width / 2, r.y + 37);
        }

        toggle(g, soundRect, "Sound: " + (profile.soundOn ? "on" : "off"), profile.soundOn);
        toggle(g, unlockRect, profile.allUnlocked ? "Upgrades: all unlocked" : "Upgrades: earned with xp", profile.allUnlocked);
        g.setFont(S_FONT);
        g.setColor(MUTED);
        g.drawString("Space start/speed   1-0 shop   Z/X upgrade   Q-U abilities   Tab target   Del sell   P pause   M mute",
                400, 616);
    }

    private void drawMapCard(Graphics2D g, Rectangle r, GameMap m, int i) {
        boolean hover = r.contains(mx, my);
        g.setColor(new Color(0, 0, 0, 45));
        g.fill(new RoundRectangle2D.Double(r.x + 3, r.y + 5, r.width, r.height, 16, 16));
        Shape card = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 16, 16);
        Shape old = g.getClip();
        g.clip(card);
        g.drawImage(m.background(), r.x, r.y, r.width, 146, null);
        g.setColor(Art.PARCHMENT);
        g.fillRect(r.x, r.y + 146, r.width, r.height - 146);
        g.setClip(old);
        g.setStroke(new BasicStroke(hover ? 3.5f : 2f));
        g.setColor(hover ? new Color(0xD9A21E) : WOOD);
        g.draw(card);
        g.setFont(H_FONT);
        g.setColor(Art.INK);
        g.drawString((i + 1) + ". " + m.name, r.x + 10, r.y + 166);
        g.setFont(S_FONT);
        g.setColor(MUTED);
        g.drawString(m.tier + (m.tracks.size() > 1 ? " · 2 paths" : "") + (m.water.isEmpty() ? "" : " · water"), r.x + 10, r.y + 182);
        Difficulty[] ds = Difficulty.values();
        for (int k = 0; k < ds.length; k++) {
            medal(g, r.x + r.width - 58 + k * 14, r.y + 177, 5.5, ds[k], profile.hasMedal(m.name, ds[k]));
        }
        if (hover) {
            g.setColor(new Color(244, 235, 211, 235));
            g.fill(new RoundRectangle2D.Double(r.x + 6, r.y + 116, r.width - 12, 26, 10, 10));
            g.setFont(S_FONT);
            g.setColor(Art.INK);
            g.drawString(m.blurb, r.x + 12, r.y + 133);
        }
    }

    private void medal(Graphics2D g, double cx, double cy, double r, Difficulty d, boolean earned) {
        Ellipse2D e = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        if (earned) Art.ink(g, e, Art.radial(cx - r * 0.3, cy - r * 0.3, r * 1.5, d.medal.brighter(), d.medal), 1f);
        else {
            g.setColor(new Color(0, 0, 0, 25));
            g.fill(e);
            g.setColor(Art.alpha(d.medal, 160));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(e);
        }
    }

    private void toggle(Graphics2D g, Rectangle r, String text, boolean on) {
        Shape b = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 12, 12);
        g.setColor(on ? new Color(0xD7EBC4) : new Color(0xEFE3C4));
        g.fill(b);
        Art.ink(g, b, null, 1.2f);
        centered(g, text, B_FONT, Art.INK, r.x + r.width / 2, r.y + 18);
    }

    private void drawPlay(Graphics2D g) {
        Shape oldClip = g.getClip();
        g.clipRect(0, 0, PW, PH);
        g.drawImage(world.map.background(), 0, 0, null);

        if (selected != null) drawRange(g, selected.x, selected.y,
                selected.type == TowerType.LANTERN ? selected.range
                        : selected.type == TowerType.STORK ? selected.orbitR + selected.effRange() * 0.5 : selected.effRange(), true);

        List<Puff> sorted = new ArrayList<>(world.puffs);
        sorted.sort(Comparator.comparingDouble(Puff::radius).reversed());
        for (Puff p : sorted) drawPuff(g, p);

        for (Projectile p : world.projectiles) if (p.kind == Projectile.Kind.BURR) drawProjectile(g, p);
        for (Tower t : world.towers) if (t.type != TowerType.STORK) drawTower(g, t);
        for (Projectile p : world.projectiles) if (p.kind != Projectile.Kind.BURR) drawProjectile(g, p);
        for (Tower t : world.towers) if (t.type == TowerType.STORK) drawTower(g, t);
        for (Particle p : world.particles) p.draw(g);

        if (placing != null && mx < PW && mx >= 0) {
            boolean ok = world.map.canPlace(placing, mx, my, world.towers) && world.canAfford(world.cost(placing));
            Tower ghost = new Tower(placing, mx, my);
            drawRange(g, mx, my, placing == TowerType.STORK ? ghost.orbitR : ghost.range, ok);
            Composite c = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            g.drawImage(Art.tower(placing), mx - 32, my - 40, null);
            g.setComposite(c);
            if (!ok) {
                g.setColor(BAD);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(mx - 8, my - 8, mx + 8, my + 8);
                g.drawLine(mx + 8, my - 8, mx - 8, my + 8);
                if (placing.placement == TowerType.Placement.WATER && !world.map.inWater(mx, my)) {
                    centered(g, "water only", B_FONT, Color.WHITE, mx, my + 34);
                }
            }
        }

        drawAbilityBar(g);

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
            centered(g, world.banner, H_FONT, Art.alpha(Art.INK, (int) (255 * a)), PW / 2, 37);
        }
        if (speed > 1) {
            g.setFont(H_FONT);
            g.setColor(new Color(255, 255, 255, 210));
            g.drawString("x" + speed, PW - 34, PH - 12);
        }
        if (world.globalSurge > 0) {
            g.setColor(new Color(255, 190, 90, 40));
            g.fillRect(0, 0, PW, PH);
        }
        g.setClip(oldClip);
    }

    private void drawProjectile(Graphics2D g, Projectile p) {
        BufferedImage img = Art.projectile(p.kind, p.owner.fire);
        AffineTransform old = g.getTransform();
        g.translate(p.x, p.y);
        if (p.kind == Projectile.Kind.ACORN) g.rotate(p.life * 0.25);
        else if (p.kind != Projectile.Kind.BURR) g.rotate(p.angle);
        g.drawImage(img, -img.getWidth() / 2, -img.getHeight() / 2, null);
        g.setTransform(old);
    }

    private void drawAbilityBar(Graphics2D g) {
        List<Ability> abs = world.abilities();
        for (int i = 0; i < abs.size(); i++) {
            Ability a = abs.get(i);
            Rectangle r = abilityRect(i);
            int cd = world.abilityCooldown(a);
            Shape b = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 12, 12);
            g.setColor(cd == 0 ? new Color(0xFFF1B8) : new Color(0xD8CCB0));
            g.fill(b);
            TowerType owner = null;
            for (Tower t : world.towers) if (t.ability == a) owner = t.type;
            if (owner != null) g.drawImage(Art.tower(owner), r.x + 2, r.y, 44, 44, null);
            if (cd > 0) {
                double f = cd / (double) a.cooldownTicks;
                Shape old = g.getClip();
                g.clip(b);
                g.setColor(new Color(40, 30, 20, 130));
                g.fill(new Arc2D.Double(r.x - 20, r.y - 20, r.width + 40, r.height + 40, 90, f * 360, Arc2D.PIE));
                g.setClip(old);
                centered(g, String.valueOf((cd + 59) / 60), B_FONT, Color.WHITE, r.x + r.width / 2, r.y + 30);
            }
            g.setColor(cd == 0 ? new Color(0xD9A21E) : WOOD);
            g.setStroke(new BasicStroke(cd == 0 ? 2.5f : 1.5f));
            g.draw(b);
            if (i < ABILITY_KEY_NAMES.length()) {
                g.setFont(B_FONT);
                g.setColor(Art.INK);
                g.drawString(String.valueOf(ABILITY_KEY_NAMES.charAt(i)), r.x + 4, r.y + 13);
            }
            if (r.contains(mx, my)) {
                String tip = a.label + ": " + a.desc;
                g.setFont(S_FONT);
                int w = g.getFontMetrics().stringWidth(tip) + 16;
                int tx = Math.min(r.x, PW - w - 6);
                Shape box = new RoundRectangle2D.Double(tx, r.y - 30, w, 24, 10, 10);
                g.setColor(new Color(244, 235, 211, 240));
                g.fill(box);
                Art.ink(g, box, null, 1f);
                g.setColor(Art.INK);
                g.drawString(tip, tx + 8, r.y - 14);
            }
        }
    }

    private void drawRange(Graphics2D g, double x, double y, double range, boolean ok) {
        if (range > 1000) return;
        Ellipse2D e = new Ellipse2D.Double(x - range, y - range, range * 2, range * 2);
        g.setColor(ok ? new Color(255, 255, 255, 55) : new Color(220, 60, 50, 60));
        g.fill(e);
        g.setColor(ok ? new Color(255, 255, 255, 170) : new Color(200, 50, 40, 170));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6, 6}, 0));
        g.draw(e);
    }

    private void drawPuff(Graphics2D g, Puff p) {
        BufferedImage img = Art.puff(p.type, p.damageStage());
        boolean still = p.frozen > 0 || p.stun > 0;
        double bob = still ? 0 : Math.sin(p.age * 0.12 + p.id) * 1.5;
        int dx = (int) Math.round(p.x - img.getWidth() / 2.0), dy = (int) Math.round(p.y + bob - img.getHeight() / 2.0);
        double r = p.radius();
        g.setColor(new Color(20, 30, 10, 45));
        g.fill(new Ellipse2D.Double(p.x - r * 0.8, p.y + r * 0.75, r * 1.6, r * 0.45));
        Composite oldC = g.getComposite();
        if (p.hidden) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
        g.drawImage(img, dx, dy, null);
        if (p.armoured) g.drawImage(Art.armour(p.type), dx, dy, null);
        g.setComposite(oldC);
        if (p.hidden) {
            g.setColor(new Color(255, 255, 255, 150));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3, 4}, p.age * 0.3f));
            g.draw(new Ellipse2D.Double(p.x - r * 1.1, p.y + bob - r * 1.0, r * 2.2, r * 2.0));
        }
        if (p.slimeTicks > 0) {
            g.setColor(new Color(140, 210, 90, 120));
            g.fill(new Ellipse2D.Double(p.x - r * 0.7, p.y + bob + r * 0.35, r * 1.4, r * 0.5));
            g.fill(new Ellipse2D.Double(p.x - r * 0.15, p.y + bob + r * 0.6, r * 0.3, r * 0.5));
        }
        if (p.regrow) Art.drawSprout(g, p.x + r * 0.35, p.y + bob - r * 0.65);
        if (p.frozen > 0) g.drawImage(Art.ice(p.type), dx, dy, null);
        if (p.stun > 0) {
            g.setColor(new Color(0xFFE066));
            for (int i = 0; i < 3; i++) {
                double a = p.age * 0.2 + i * Math.PI * 2 / 3;
                g.fill(Art.star(p.x + Math.cos(a) * r * 0.8, p.y - r - 2 + Math.sin(a) * 3, 3, 1.2, 5, 0));
            }
        }
        if (p.type.isBoss()) {
            double w = r * 1.6, f = p.hp / (double) p.layerHp(p.type);
            double bx = p.x - w / 2, by = p.y - r * 1.15;
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(new RoundRectangle2D.Double(bx - 1, by - 1, w + 2, 7, 6, 6));
            g.setColor(p.armoured ? new Color(0x9AA4B1) : new Color(0xE5484D));
            g.fill(new RoundRectangle2D.Double(bx, by, w * f, 5, 5, 5));
        }
    }

    private void drawTower(Graphics2D g, Tower t) {
        if (t.type == TowerType.STORK) {
            BufferedImage nest = Art.nest();
            g.drawImage(nest, (int) t.x - 24, (int) t.y - 6, null);
            g.setColor(new Color(20, 30, 10, 40));
            g.fill(new Ellipse2D.Double(t.px - 16, t.py + 26, 32, 8));
        } else {
            g.setColor(new Color(20, 30, 10, 60));
            g.fill(new Ellipse2D.Double(t.x - 20, t.y + 10, 40, 12));
        }
        BufferedImage img = Art.tower(t.type);
        double squash = t.anim > 0 ? 1 + Math.sin(t.anim / 8.0 * Math.PI) * 0.08 : 1;
        AffineTransform old = g.getTransform();
        double bob = t.type == TowerType.STORK ? Math.sin(t.orbitAngle * 6) * 2 : 0;
        g.translate(t.px, t.py + 22 + bob);
        g.scale(t.facing * (2 - squash), squash);
        g.drawImage(img, -32, -62, null);
        g.setTransform(old);
        if (t.type == TowerType.LANTERN && (t.giveSpeed < 1 || t.giveDetect)) {
            g.setPaint(Art.radial(t.x, t.y - 10, 40, new Color(255, 220, 120, 60), new Color(255, 220, 120, 0)));
            g.fill(new Ellipse2D.Double(t.x - 40, t.y - 50, 80, 80));
        }
        if (t == selected) {
            g.setColor(new Color(255, 236, 140, 200));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(t.x - 22, t.y + 8, 44, 16));
        }
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

        Art.drawHeart(g, PW + 22, 20, 8.5);
        Art.drawCoin(g, PW + 96, 20, 8.5);
        g.setFont(STAT_FONT);
        g.setColor(Art.INK);
        g.drawString(String.valueOf(world.lives), PW + 35, 26);
        g.drawString(String.valueOf(world.cash), PW + 109, 26);
        g.setFont(B_FONT);
        int dr = world.displayRound(), fr = world.difficulty.finalRound;
        g.drawString("Round " + dr + (dr <= fr ? " / " + fr : " (freeplay)"), PW + 16, 48);
        g.setFont(S_FONT);
        int lw = g.getFontMetrics().stringWidth(world.difficulty.label);
        medal(g, PW + 222 - lw, 44, 5, world.difficulty, true);
        g.setColor(MUTED);
        g.drawString(world.difficulty.label, PW + 229 - lw, 48);
        Shape pb = new RoundRectangle2D.Double(pauseBtn.x, pauseBtn.y, pauseBtn.width, pauseBtn.height, 8, 8);
        g.setColor(pauseBtn.contains(mx, my) ? new Color(0xFBF6E8) : new Color(0xEFE3C4));
        g.fill(pb);
        Art.ink(g, pb, null, 1.2f);
        g.setColor(Art.INK);
        g.fillRect(pauseBtn.x + 9, pauseBtn.y + 8, 4, 12);
        g.fillRect(pauseBtn.x + 16, pauseBtn.y + 8, 4, 12);

        TowerType[] types = TowerType.values();
        for (int i = 0; i < types.length; i++) {
            TowerType t = types[i];
            Rectangle r = shopRects[i];
            int cost = world.cost(t);
            boolean afford = world.canAfford(cost);
            boolean active = placing == t;
            Shape box = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 10, 10);
            g.setColor(active ? new Color(0xFFF1B8) : i == hoverShop ? new Color(0xFBF6E8) : new Color(0xF3E8CC));
            g.fill(box);
            g.setColor(active ? new Color(0xD9A21E) : WOOD);
            g.setStroke(new BasicStroke(active ? 2.5f : 1.2f));
            g.draw(box);
            Composite c = g.getComposite();
            if (!afford) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            g.drawImage(Art.tower(t), r.x + 13, r.y - 2, 44, 44, null);
            g.setComposite(c);
            if (t.placement == TowerType.Placement.WATER) {
                g.setColor(new Color(0x4F92B8));
                g.fill(new Ellipse2D.Double(r.x + 4, r.y + 4, 7, 7));
            }
            if (i < 10) {
                g.setFont(S_FONT);
                g.setColor(MUTED);
                g.drawString(String.valueOf((i + 1) % 10), r.x + r.width - 10, r.y + 12);
            }
            Art.drawCoin(g, r.x + 16, r.y + 50, 4.5);
            g.setFont(B_FONT);
            g.setColor(afford ? Art.INK : BAD);
            g.drawString(String.valueOf(cost), r.x + 24, r.y + 55);
        }

        g.setColor(new Color(0xD8C8A4));
        g.fillRect(PW + 12, 382, 216, 1);
        if (selected != null) drawTowerPanel(g, selected);
        else {
            TowerType info = hoverShop >= 0 ? types[hoverShop] : placing;
            if (info != null) {
                g.setFont(H_FONT);
                g.setColor(Art.INK);
                g.drawString(info.label, PW + 14, 402);
                g.setFont(S_FONT);
                wrap(g, info.desc, PW + 14, 422, 210, 15);
                g.setColor(MUTED);
                wrap(g, "Critter xp: " + profile.xp(info) + ".  " + (placing != null
                        ? "Click to place; shift-click to place several; right-click to cancel." : "Click to pick up."),
                        PW + 14, 516, 210, 15);
            } else {
                g.setFont(S_FONT);
                g.setColor(MUTED);
                wrap(g, "Pick a critter from the shop, then click the grass (not the path) to set it down. "
                        + "The River Otter lives in water. Click a placed critter to upgrade or sell it.", PW + 14, 402, 210, 15);
                wrap(g, "Hidden puffs (dotted outline) can only be targeted by critters that can spot them. "
                        + "Sprouting puffs regrow; banded ones are armoured.", PW + 14, 484, 210, 15);
            }
        }

        boolean active = world.roundActive;
        Shape sb = new RoundRectangle2D.Double(startRect.x, startRect.y, startRect.width, startRect.height, 14, 14);
        g.setPaint(Art.vgrad(startRect.y, active ? new Color(0xF0DFB0) : new Color(0x8FCB6A),
                startRect.y + startRect.height, active ? new Color(0xD5BE85) : GOOD));
        g.fill(sb);
        Art.ink(g, sb, null, 2f);
        String label = active ? "Speed  " + ">".repeat(speed) : "Start Round " + world.displayRound();
        centered(g, label, H_FONT, Art.INK, startRect.x + startRect.width / 2, startRect.y + 32);
        Shape ab = new RoundRectangle2D.Double(autoRect.x, autoRect.y, autoRect.width, autoRect.height, 14, 14);
        g.setColor(world.autoStart ? new Color(0xBFE0A8) : new Color(0xEDE2C6));
        g.fill(ab);
        Art.ink(g, ab, null, 2f);
        centered(g, "Auto", B_FONT, Art.INK, autoRect.x + autoRect.width / 2, autoRect.y + 24);
        centered(g, world.autoStart ? "on" : "off", S_FONT, new Color(0x5A4632), autoRect.x + autoRect.width / 2, autoRect.y + 40);
    }

    private void drawTowerPanel(Graphics2D g, Tower t) {
        g.setFont(H_FONT);
        g.setColor(Art.INK);
        g.drawString(t.type.label, PW + 14, 400);
        for (int p = 0; p < 2; p++) {
            Rectangle r = upgradeRects[p];
            Upgrade u = t.nextUpgrade(p);
            World.UpgradeState st = world.upgradeState(t, p);
            int cost = u == null ? 0 : world.cost(u);
            boolean afford = st == World.UpgradeState.AVAILABLE && world.canAfford(cost);
            Shape box = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 12, 12);
            Color tint = p == 0 ? new Color(0xFBE3DC) : new Color(0xDDEBFA);
            g.setColor(st != World.UpgradeState.AVAILABLE ? new Color(0xE6DCC4) : r.contains(mx, my) && afford ? tint.brighter() : tint);
            g.fill(box);
            g.setColor(afford ? (p == 0 ? new Color(0xC0453B) : new Color(0x3E6EB0)) : WOOD);
            g.setStroke(new BasicStroke(afford ? 2f : 1.2f));
            g.draw(box);
            for (int i = 0; i < 4; i++) {
                Ellipse2D pip = new Ellipse2D.Double(r.x + r.width - 50 + i * 11, r.y + 6, 8, 8);
                Art.ink(g, pip, i < t.tiers[p] ? (p == 0 ? new Color(0xF0766A) : new Color(0x6AAEF0)) : new Color(0xF7F1E2), 1f);
            }
            g.setFont(B_FONT);
            g.setColor(Art.INK);
            switch (st) {
                case MAXED -> g.drawString("Path complete", r.x + 10, r.y + 17);
                case PATH_LOCKED -> {
                    g.drawString(u.name(), r.x + 10, r.y + 17);
                    g.setFont(S_FONT);
                    g.setColor(MUTED);
                    g.drawString("Locked: the other path went past tier 2.", r.x + 10, r.y + 34);
                }
                case XP_LOCKED -> {
                    g.drawString(u.name(), r.x + 10, r.y + 17);
                    g.setFont(S_FONT);
                    g.setColor(MUTED);
                    g.drawString("Needs " + profile.xpShort(t.type, p, t.tiers[p] + 1) + " more xp to unlock.", r.x + 10, r.y + 34);
                }
                case AVAILABLE -> {
                    g.drawString(u.name() + (u.ability() != null ? " ★" : ""), r.x + 10, r.y + 17);
                    g.setFont(S_FONT);
                    g.setColor(new Color(0x5A4632));
                    wrap(g, u.desc(), r.x + 10, r.y + 31, 150, 12);
                    Art.drawCoin(g, r.x + r.width - 52, r.y + r.height - 11, 5);
                    g.setFont(B_FONT);
                    g.setColor(afford ? Art.INK : BAD);
                    g.drawString(String.valueOf(cost), r.x + r.width - 44, r.y + r.height - 7);
                }
            }
        }
        boolean aims = t.type.aims();
        Shape tb = new RoundRectangle2D.Double(targetRect.x, targetRect.y, targetRect.width, targetRect.height, 10, 10);
        g.setColor(aims ? new Color(0xF3E8CC) : new Color(0xE6DCC4));
        g.fill(tb);
        Art.ink(g, tb, null, 1.2f);
        centered(g, aims ? "Target: " + t.mode.label : "No aiming", B_FONT, aims ? Art.INK : new Color(0x9A8668),
                targetRect.x + targetRect.width / 2, targetRect.y + 17);
        Shape sb = new RoundRectangle2D.Double(sellRect.x, sellRect.y, sellRect.width, sellRect.height, 10, 10);
        g.setColor(sellRect.contains(mx, my) ? new Color(0xF6D2C8) : new Color(0xF3E8CC));
        g.fill(sb);
        Art.ink(g, sb, null, 1.2f);
        centered(g, "Sell  " + t.sellValue(), B_FONT, Art.INK, sellRect.x + sellRect.width / 2, sellRect.y + 17);
        String sub = t.type.attack == TowerType.Attack.FARM ? "+" + t.income + " coins/round"
                : t.type.attack == TowerType.Attack.SUPPORT && !t.sparks ? "support" : t.pops + " pops";
        if (t.seesHidden()) sub += " \u00b7 sees hidden";
        sub += " \u00b7 xp " + profile.xp(t.type);
        g.setFont(S_FONT);
        g.setColor(MUTED);
        g.drawString(sub, PW + 14, 562);
    }

    private void drawOverlays(Graphics2D g) {
        if (world.gameOver) {
            dim(g);
            centered(g, "The puffs drifted on through.", BIG_FONT, Art.PARCHMENT, PW / 2, 280);
            centered(g, "You reached round " + world.round + " and popped " + world.totalPops + " layers of cloud.",
                    SUB_FONT, Art.PARCHMENT, PW / 2, 320);
            centered(g, "Click anywhere to return to the title page.", S_FONT, new Color(0xE9DCBC), PW / 2, 360);
        } else if (world.pausedForVictory()) {
            dim(g);
            centered(g, "The meadow is quiet again.", BIG_FONT, Art.PARCHMENT, PW / 2, 270);
            centered(g, world.difficulty.label + " medal earned on " + world.map.name + ", with " + world.lives
                    + (world.lives == 1 ? " life" : " lives") + " to spare.", SUB_FONT, Art.PARCHMENT, PW / 2, 310);
            medal(g, PW / 2.0, 334, 10, world.difficulty, true);
            button(g, victoryGo, "Keep going (freeplay)");
            button(g, victoryTitle, "Save and return to the title page");
        } else if (paused) {
            dim(g);
            centered(g, "Paused", BIG_FONT, Art.PARCHMENT, PW / 2, 170);
            String[] labels = {"Resume", "Speed: " + speed + "x", "Sound: " + (profile.soundOn ? "on" : "off"),
                    profile.allUnlocked ? "Upgrades: all unlocked" : "Upgrades: earned with xp",
                    "Restart this map", world.roundActive ? "Quit to title (progress since last round lost)" : "Save and quit to title"};
            for (int i = 0; i < pauseRects.length; i++) button(g, pauseRects[i], labels[i]);
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
        Font f = text.length() > 30 ? B_FONT : H_FONT;
        centered(g, text, f, Art.INK, r.x + r.width / 2, r.y + r.height / 2 + 6);
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
