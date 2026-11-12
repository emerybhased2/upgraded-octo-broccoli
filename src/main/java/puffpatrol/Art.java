package puffpatrol;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Every picture in the game is painted here with Java2D: no image files needed.
 * The style is soft watercolour fills with a thin brown-black ink outline.
 */
final class Art {
    static final Color INK = new Color(0x2B2420);
    static final Color PARCHMENT = new Color(0xF4EBD3);

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private Art() {
    }

    private static BufferedImage cached(String key, Supplier<BufferedImage> s) {
        BufferedImage b = CACHE.get(key);
        if (b == null) {
            b = s.get();
            CACHE.put(key, b);
        }
        return b;
    }

    static BufferedImage newImage(int w, int h) {
        return new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D gfx(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        smooth(g);
        return g;
    }

    static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    // ------------------------------------------------------------------ helpers

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Ellipse2D.Double ell(double x, double y, double w, double h) {
        return new Ellipse2D.Double(x, y, w, h);
    }

    static Ellipse2D.Double circ(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
    }

    static Paint vgrad(double y0, Color c0, double y1, Color c1) {
        return new GradientPaint(0, (float) y0, c0, 0, (float) y1, c1);
    }

    static Paint radial(double cx, double cy, double r, Color in, Color out) {
        return new RadialGradientPaint(new Point2D.Double(cx, cy), (float) r, new float[]{0f, 1f}, new Color[]{in, out});
    }

    static void ink(Graphics2D g, Shape s, Paint fill) {
        ink(g, s, fill, 1.6f);
    }

    static void ink(Graphics2D g, Shape s, Paint fill, float w) {
        if (fill != null) {
            g.setPaint(fill);
            g.fill(s);
        }
        g.setColor(INK);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    static void line(Graphics2D g, Color c, float w, double x1, double y1, double x2, double y2) {
        g.setColor(c);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    /** A dot eye with a tiny white glint. */
    static void eye(Graphics2D g, double cx, double cy, double w, double h) {
        g.setColor(INK);
        g.fill(ell(cx - w / 2, cy - h / 2, w, h));
        g.setColor(Color.WHITE);
        double gr = Math.max(0.7, w * 0.22);
        g.fill(circ(cx - w * 0.18, cy - h * 0.2, gr));
    }

    static void cheek(Graphics2D g, double cx, double cy, double r) {
        g.setColor(new Color(240, 120, 130, 110));
        g.fill(ell(cx - r, cy - r * 0.7, r * 2, r * 1.4));
    }

    static Shape star(double cx, double cy, double rOut, double rIn, int points, double rot) {
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < points * 2; i++) {
            double r = i % 2 == 0 ? rOut : rIn;
            double a = rot - Math.PI / 2 + i * Math.PI / points;
            if (i == 0) p.moveTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
            else p.lineTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
        p.closePath();
        return p;
    }

    // ------------------------------------------------------------------ towers

    static final int TOWER_SIZE = 64;

    static BufferedImage tower(TowerType t) {
        return cached("tower:" + t, () -> towerImage(t, 1));
    }

    static BufferedImage towerImage(TowerType t, double scale) {
        int s = (int) Math.ceil(TOWER_SIZE * scale);
        BufferedImage img = newImage(s, s);
        Graphics2D g = gfx(img);
        g.scale(scale, scale);
        drawTower(g, t);
        g.dispose();
        return img;
    }

    static void drawTower(Graphics2D g, TowerType t) {
        switch (t) {
            case HEDGEHOG -> hedgehog(g);
            case BEEHIVE -> beehive(g);
            case BADGER -> badger(g);
            case OWL -> owl(g);
            case MOTH -> moth(g);
            case TOAD -> toad(g);
            case MUSHROOM -> mushrooms(g);
        }
    }

    private static void hedgehog(Graphics2D g) {
        Color fur = new Color(0x7A5634);
        ink(g, ell(16, 49, 11, 7), fur);
        ink(g, ell(34, 49, 11, 7), fur);
        double cx = 27, cy = 38;
        Path2D.Double spikes = new Path2D.Double();
        int n = 13;
        for (int i = 0; i <= n; i++) {
            double a = Math.PI * 0.92 + i * (Math.PI * 1.12 / n);
            double r1 = 16, r2 = i % 2 == 0 ? 27 : 24;
            double b1 = a - 0.14, b2 = a + 0.14;
            if (i == 0) spikes.moveTo(cx + Math.cos(b1) * r1, cy + Math.sin(b1) * r1);
            else spikes.lineTo(cx + Math.cos(b1) * r1, cy + Math.sin(b1) * r1);
            spikes.lineTo(cx + Math.cos(a) * r2, cy + Math.sin(a) * r2);
            spikes.lineTo(cx + Math.cos(b2) * r1, cy + Math.sin(b2) * r1);
        }
        spikes.lineTo(cx + 16, cy + 12);
        spikes.lineTo(cx - 18, cy + 12);
        spikes.closePath();
        ink(g, spikes, vgrad(10, new Color(0x6B4A30), 50, new Color(0x3E2A1A)));
        // pale spike tips
        for (int i = 0; i <= n; i += 2) {
            double a = Math.PI * 0.92 + i * (Math.PI * 1.12 / n);
            line(g, new Color(0xD9C29A), 1.4f, cx + Math.cos(a) * 21, cy + Math.sin(a) * 21,
                    cx + Math.cos(a) * 25.5, cy + Math.sin(a) * 25.5);
        }
        ink(g, ell(10, 28, 34, 24), vgrad(28, new Color(0x8C6A45), 52, new Color(0x6E5034)));
        Color face = new Color(0xEBD0A8);
        Area head = new Area(ell(32, 26, 22, 22));
        head.add(new Area(ell(44, 34, 15, 11)));
        ink(g, head, vgrad(26, new Color(0xF4E0BF), 48, face));
        ink(g, ell(35, 21, 8, 9), face);
        g.setColor(new Color(0xE9A6A0));
        g.fill(ell(37, 23.5, 4, 5));
        g.setColor(INK);
        g.fill(circ(58, 38.5, 3.2));
        g.setColor(Color.WHITE);
        g.fill(circ(57.2, 37.5, 0.9));
        eye(g, 45, 33, 3.6, 4.6);
        cheek(g, 47, 41, 3.2);
        line(g, INK, 1.2f, 50, 43, 54, 43.5);
    }

    private static void beehive(Graphics2D g) {
        Color honey = new Color(0xF2B53A), deep = new Color(0xC9851C);
        ink(g, ell(10, 50, 44, 9), new Color(0x8A5A22));
        for (int i = 0; i < 5; i++) {
            double w = 46 - i * 7.5, h = 13, y = 50 - i * 8.6;
            ink(g, ell(32 - w / 2, y - h / 2, w, h), vgrad(y - h / 2, mix(honey, Color.WHITE, 0.25), y + h / 2, deep));
        }
        ink(g, ell(26, 9, 12, 7), vgrad(9, honey, 16, deep));
        ink(g, ell(26, 40, 12, 10), new Color(0x3A2410));
        g.setColor(new Color(255, 245, 210, 120));
        g.fill(ell(17, 27, 9, 4));
        bee(g, 50, 17, 1.0, 0.3);
        bee(g, 13, 25, 0.8, -0.4);
        bee(g, 52, 36, 0.7, 0.1);
    }

    static void bee(Graphics2D g, double cx, double cy, double s, double rot) {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.rotate(rot);
        g.scale(s, s);
        g.setColor(new Color(255, 255, 255, 190));
        g.fill(ell(-4, -9, 6, 7));
        g.fill(ell(0, -8, 6, 6));
        g.setColor(INK);
        g.setStroke(new BasicStroke(0.8f));
        g.draw(ell(-4, -9, 6, 7));
        g.draw(ell(0, -8, 6, 6));
        Shape body = ell(-6, -3.5, 12, 7);
        g.setColor(new Color(0xFFD23F));
        g.fill(body);
        Shape oldClip = g.getClip();
        g.clip(body);
        g.setColor(INK);
        g.fill(new Rectangle2D.Double(-2.5, -4, 2, 8));
        g.fill(new Rectangle2D.Double(1.5, -4, 2, 8));
        g.setClip(oldClip);
        ink(g, body, null, 1.1f);
        g.setColor(INK);
        g.fill(circ(4.2, -0.6, 0.9));
        g.setTransform(old);
    }

    private static void badger(Graphics2D g) {
        Color grey = new Color(0x737784);
        ink(g, ell(12, 50, 10, 8), new Color(0x2F2F36));
        ink(g, ell(30, 51, 10, 8), new Color(0x2F2F36));
        ink(g, ell(6, 29, 44, 28), vgrad(29, mix(grey, Color.WHITE, 0.2), 57, mix(grey, INK, 0.25)));
        g.setColor(new Color(255, 255, 255, 60));
        g.fill(ell(12, 33, 26, 8));
        ink(g, circ(37, 22, 4.5), new Color(0x2F2F36));
        ink(g, circ(50, 20, 4.5), new Color(0x2F2F36));
        Shape head = ell(31, 15, 26, 24);
        g.setColor(new Color(0xF4F1EA));
        g.fill(head);
        Shape oldClip = g.getClip();
        g.clip(head);
        g.setColor(new Color(0x2C2C30));
        AffineTransform old = g.getTransform();
        g.rotate(-0.35, 44, 27);
        g.fill(new RoundRectangle2D.Double(32, 18, 8, 22, 6, 6));
        g.fill(new RoundRectangle2D.Double(46, 16, 8, 22, 6, 6));
        g.setTransform(old);
        g.setClip(oldClip);
        ink(g, head, null);
        g.setColor(Color.WHITE);
        g.fill(circ(41, 27, 1.9));
        g.fill(circ(51, 24, 1.9));
        g.setColor(INK);
        g.fill(circ(41.3, 27.3, 1.1));
        g.fill(circ(51.3, 24.3, 1.1));
        ink(g, ell(53, 30, 7, 5), INK);
        cheek(g, 47, 34, 2.8);
        acorn(g, 50, 46, 1.0);
        ink(g, ell(42, 45, 8, 6), new Color(0x2F2F36));
    }

    static void acorn(Graphics2D g, double cx, double cy, double s) {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.scale(s, s);
        ink(g, ell(-5.5, -3, 11, 11), vgrad(-3, new Color(0xD08A42), 8, new Color(0x9A5A24)), 1.3f);
        Shape cap = new Arc2D.Double(-6.5, -7, 13, 10, 0, 180, Arc2D.CHORD);
        ink(g, cap, new Color(0x6B4A2A), 1.3f);
        g.setColor(new Color(0x8C6A45));
        g.setStroke(new BasicStroke(0.7f));
        for (int i = -2; i <= 2; i++) g.draw(new Line2D.Double(i * 2.4 - 1, -5.5, i * 2.4 + 1, -2.4));
        line(g, INK, 1.4f, 0, -7, 1.5, -9.5);
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(ell(-3.5, 0, 3, 4));
        g.setTransform(old);
    }

    private static void owl(Graphics2D g) {
        Color brown = new Color(0x8B6542), dark = new Color(0x5C4129);
        Path2D.Double tufts = new Path2D.Double();
        tufts.moveTo(17, 18);
        tufts.lineTo(19, 3);
        tufts.lineTo(28, 13);
        tufts.moveTo(36, 13);
        tufts.lineTo(45, 3);
        tufts.lineTo(47, 18);
        ink(g, tufts, dark);
        ink(g, ell(13, 8, 38, 50), vgrad(8, mix(brown, Color.WHITE, 0.15), 58, dark));
        ink(g, ell(9, 27, 13, 26), dark);
        ink(g, ell(42, 27, 13, 26), dark);
        ink(g, ell(20, 29, 24, 26), vgrad(29, new Color(0xF0DDB5), 55, new Color(0xD3B486)));
        g.setColor(new Color(0x9C7A50));
        g.setStroke(new BasicStroke(1f));
        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 3 - (row == 2 ? 1 : 0); i++) {
                double x = 24 + i * 6 + (row % 2) * 3, y = 36 + row * 6;
                g.draw(new Arc2D.Double(x, y, 5, 4, 200, 140, Arc2D.OPEN));
            }
        }
        Color ring = new Color(0xF2A23A);
        ink(g, circ(24.5, 22, 7.5), new Color(0xF7F1E4));
        ink(g, circ(39.5, 22, 7.5), new Color(0xF7F1E4));
        g.setColor(ring);
        g.fill(circ(25.5, 22.5, 4.6));
        g.fill(circ(40.5, 22.5, 4.6));
        eye(g, 25.8, 22.8, 4.6, 4.6);
        eye(g, 40.8, 22.8, 4.6, 4.6);
        Path2D.Double beak = new Path2D.Double();
        beak.moveTo(29.5, 28);
        beak.lineTo(35.5, 28);
        beak.lineTo(32.5, 35);
        beak.closePath();
        ink(g, beak, ring, 1.2f);
        ink(g, ell(22, 54, 8, 5), ring, 1.2f);
        ink(g, ell(35, 54, 8, 5), ring, 1.2f);
    }

    private static void moth(Graphics2D g) {
        Paint wing = vgrad(6, new Color(0xE6F3FF), 58, new Color(0x88B3E6));
        AffineTransform old = g.getTransform();
        for (int side = -1; side <= 1; side += 2) {
            g.setTransform(old);
            g.translate(32, 32);
            g.scale(side, 1);
            g.translate(-32, -32);
            g.rotate(-0.45, 22, 22);
            ink(g, ell(6, 10, 26, 22), wing);
            g.setColor(Color.WHITE);
            g.fill(circ(16, 20, 4.5));
            g.setColor(new Color(0x5C8FD6));
            g.fill(circ(16, 20, 2.2));
            g.setTransform(old);
            g.translate(32, 32);
            g.scale(side, 1);
            g.translate(-32, -32);
            g.rotate(0.35, 24, 42);
            ink(g, ell(12, 34, 20, 17), wing);
            g.setColor(new Color(255, 255, 255, 170));
            g.fill(circ(20, 42, 3));
        }
        g.setTransform(old);
        Path2D.Double ant = new Path2D.Double();
        ant.moveTo(30, 13);
        ant.quadTo(26, 4, 19, 3);
        ant.moveTo(34, 13);
        ant.quadTo(38, 4, 45, 3);
        g.setColor(INK);
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(ant);
        for (int i = 0; i < 4; i++) {
            line(g, INK, 0.8f, 26 - i * 2, 7 - i * 0.8, 24 - i * 2, 5 - i * 1.2);
            line(g, INK, 0.8f, 38 + i * 2, 7 - i * 0.8, 40 + i * 2, 5 - i * 1.2);
        }
        ink(g, ell(27.5, 18, 9, 30), vgrad(18, Color.WHITE, 48, new Color(0xD8E2F2)));
        g.setColor(new Color(0xB8C8E0));
        g.setStroke(new BasicStroke(0.9f));
        for (int i = 0; i < 4; i++) g.draw(new Arc2D.Double(28.5, 26 + i * 5, 7, 4, 200, 140, Arc2D.OPEN));
        ink(g, circ(32, 15, 6), new Color(0xF8FAFF));
        eye(g, 29.6, 14.6, 2.6, 3.2);
        eye(g, 34.4, 14.6, 2.6, 3.2);
        cheek(g, 32, 18.5, 2);
        g.setColor(new Color(255, 255, 255, 230));
        g.fill(star(53, 50, 4, 1.4, 6, 0));
        g.fill(star(9, 52, 3, 1, 6, 0.3));
    }

    private static void toad(Graphics2D g) {
        Color green = new Color(0x62A04C), dark = new Color(0x3D7431);
        ink(g, ell(6, 50, 14, 8), dark);
        ink(g, ell(44, 50, 14, 8), dark);
        ink(g, ell(6, 26, 52, 32), vgrad(26, mix(green, Color.WHITE, 0.15), 58, dark));
        ink(g, ell(15, 40, 34, 16), vgrad(40, new Color(0xE2EBB4), 56, new Color(0xC2D38A)));
        g.setColor(new Color(0x497F38));
        g.fill(circ(13, 37, 2.2));
        g.fill(circ(51, 36, 2.6));
        g.fill(circ(48, 30, 1.6));
        for (int side = 0; side < 2; side++) {
            double ex = side == 0 ? 15 : 49;
            ink(g, circ(ex, 27, 7.5), mix(green, Color.WHITE, 0.1));
            g.setColor(new Color(0xF2C94C));
            g.fill(circ(ex, 27, 4.6));
            g.setColor(INK);
            g.fill(ell(ex - 3.6, 25.8, 7.2, 2.4));
            g.setColor(Color.WHITE);
            g.fill(circ(ex - 1.8, 25.2, 1));
        }
        g.setColor(INK);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(18, 30, 28, 12, 200, 140, Arc2D.OPEN));
        cheek(g, 17, 38, 3);
        cheek(g, 47, 38, 3);
        AffineTransform old = g.getTransform();
        g.rotate(0.14, 32, 20);
        Color purple = new Color(0x5B3FA6);
        Path2D.Double hat = new Path2D.Double();
        hat.moveTo(21, 21);
        hat.quadTo(30, 10, 34, 1);
        hat.quadTo(38, 12, 44, 21);
        hat.closePath();
        ink(g, hat, vgrad(1, mix(purple, Color.WHITE, 0.2), 21, purple));
        ink(g, ell(16, 17.5, 33, 7), mix(purple, INK, 0.2));
        g.setColor(new Color(0xFFE066));
        g.fill(star(31, 13, 2.8, 1.1, 5, 0));
        g.fill(star(37, 8, 1.8, 0.7, 5, 0.4));
        g.fill(star(27, 18, 1.4, 0.6, 5, 0.2));
        g.setTransform(old);
    }

    private static void mushrooms(Graphics2D g) {
        ink(g, ell(6, 49, 52, 11), vgrad(49, new Color(0x7DAA4C), 60, new Color(0x557D33)));
        mushroom(g, 41, 54, 15, 22, new Color(0xE0AD3A));
        mushroom(g, 21, 54, 19, 26, new Color(0xD7443E));
        mushroom(g, 33, 56, 11, 16, new Color(0xE7684A));
        g.setColor(new Color(0x4F7A2E));
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 7; i++) {
            double x = 10 + i * 7;
            g.draw(new QuadCurve2D.Double(x, 56, x + 1, 52, x + 3, 50));
        }
        drawCoin(g, 52, 16, 6);
        g.setColor(new Color(255, 250, 210, 230));
        g.fill(star(56.5, 10, 3.2, 1, 4, 0));
    }

    private static void mushroom(Graphics2D g, double cx, double base, double h, double capW, Color cap) {
        double sw = capW * 0.36;
        ink(g, new RoundRectangle2D.Double(cx - sw / 2, base - h, sw, h, sw * 0.8, sw * 0.8),
                vgrad(base - h, new Color(0xFBF3DE), base, new Color(0xE3D3AE)));
        Shape c = new Arc2D.Double(cx - capW / 2, base - h - capW * 0.42, capW, capW * 0.84, 0, 180, Arc2D.CHORD);
        ink(g, c, vgrad(base - h - capW * 0.42, mix(cap, Color.WHITE, 0.25), base - h, mix(cap, INK, 0.15)));
        g.setColor(new Color(0xFFF8EC));
        g.fill(circ(cx - capW * 0.2, base - h - capW * 0.22, capW * 0.08));
        g.fill(circ(cx + capW * 0.15, base - h - capW * 0.3, capW * 0.1));
        g.fill(circ(cx + capW * 0.3, base - h - capW * 0.1, capW * 0.06));
        g.fill(circ(cx - capW * 0.36, base - h - capW * 0.06, capW * 0.05));
    }

    static void drawCoin(Graphics2D g, double cx, double cy, double r) {
        ink(g, circ(cx, cy, r), radial(cx - r * 0.3, cy - r * 0.3, r * 1.4, new Color(0xFFF1A8), new Color(0xD9A21E)), 1.2f);
        g.setColor(new Color(0xB07D10));
        g.setStroke(new BasicStroke(1f));
        g.draw(circ(cx, cy, r * 0.62));
        g.setColor(new Color(255, 255, 255, 170));
        g.fill(ell(cx - r * 0.5, cy - r * 0.6, r * 0.45, r * 0.35));
    }

    static void drawHeart(Graphics2D g, double cx, double cy, double s) {
        Path2D.Double h = new Path2D.Double();
        h.moveTo(cx, cy + s * 0.8);
        h.curveTo(cx - s * 1.3, cy - s * 0.1, cx - s * 0.6, cy - s * 1.1, cx, cy - s * 0.35);
        h.curveTo(cx + s * 0.6, cy - s * 1.1, cx + s * 1.3, cy - s * 0.1, cx, cy + s * 0.8);
        h.closePath();
        ink(g, h, radial(cx - s * 0.3, cy - s * 0.4, s * 1.4, new Color(0xFF9AA5), new Color(0xD8343F)), 1.2f);
    }

    // ------------------------------------------------------------------ puffs

    static BufferedImage puff(PuffType t, int stage) {
        return cached("puff:" + t + ":" + stage, () -> puffImage(t, stage, 1));
    }

    static int puffImageSize(PuffType t, double scale) {
        return (int) Math.ceil((t.radius * 3.0 + 8) * scale);
    }

    static BufferedImage puffImage(PuffType t, int stage, double scale) {
        int s = puffImageSize(t, scale);
        BufferedImage img = newImage(s, s);
        Graphics2D g = gfx(img);
        g.scale(scale, scale);
        double c = s / scale / 2;
        drawPuff(g, t, stage, c, c);
        g.dispose();
        return img;
    }

    static Shape cloud(double cx, double cy, double r) {
        Area a = new Area(ell(cx - r, cy - r * 0.82, r * 2, r * 1.64));
        int bumps = 7;
        for (int i = 0; i < bumps; i++) {
            double ang = -Math.PI / 2 + i * Math.PI * 2 / bumps + 0.25;
            double br = r * (i % 2 == 0 ? 0.52 : 0.45);
            a.add(new Area(circ(cx + Math.cos(ang) * r * 0.74, cy + Math.sin(ang) * r * 0.6, br)));
        }
        return a;
    }

    static void drawPuff(Graphics2D g, PuffType t, int stage, double cx, double cy) {
        double r = t.radius;
        Shape body = cloud(cx, cy, r);
        float sw = (float) Math.min(2.6, Math.max(1.4, r / 9));
        Color c = t.color;

        if (t == PuffType.NIMBUS) {
            g.setColor(new Color(0x5B8FD6));
            for (int i = 0; i < 4; i++) {
                double x = cx - r * 0.6 + i * r * 0.4, y = cy + r * 1.05 + (i % 2) * r * 0.15;
                Path2D.Double drop = new Path2D.Double();
                drop.moveTo(x, y - 4);
                drop.quadTo(x + 3, y + 1, x, y + 3);
                drop.quadTo(x - 3, y + 1, x, y - 4);
                g.fill(drop);
            }
        }

        Paint fill = switch (t) {
            case PEWTER -> new LinearGradientPaint(new Point2D.Double(cx - r, cy - r), new Point2D.Double(cx + r, cy + r),
                    new float[]{0f, 0.35f, 0.5f, 0.7f, 1f},
                    new Color[]{new Color(0xDDE3EA), new Color(0x9AA4B1), new Color(0xF0F3F7), new Color(0x7C8694), new Color(0x5E6774)});
            case PRISM -> new LinearGradientPaint(new Point2D.Double(cx - r, cy - r), new Point2D.Double(cx + r * 0.4, cy + r * 0.4),
                    new float[]{0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f},
                    new Color[]{new Color(0xF0525A), new Color(0xF79A3E), new Color(0xF7DE4A), new Color(0x5CC86A), new Color(0x4C8EE8), new Color(0x9A5CE0)},
                    MultipleGradientPaint.CycleMethod.REFLECT);
            case FROST -> radial(cx - r * 0.3, cy - r * 0.4, r * 1.6, Color.WHITE, new Color(0xCFE6FA));
            case MARBLE -> radial(cx - r * 0.3, cy - r * 0.4, r * 1.6, Color.WHITE, new Color(0xD6D6DC));
            default -> new RadialGradientPaint(new Point2D.Double(cx - r * 0.35, cy - r * 0.45), (float) (r * 1.7),
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{mix(c, Color.WHITE, 0.5), c, mix(c, INK, 0.35)});
        };
        g.setPaint(fill);
        g.fill(body);

        Shape oldClip = g.getClip();
        g.clip(body);
        switch (t) {
            case MARBLE -> {
                g.setColor(new Color(0x26232B));
                g.setStroke(new BasicStroke((float) (r * 0.26), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int k = 0; k < 3; k++) {
                    double y0 = cy - r * 0.85 + k * r * 0.8;
                    g.draw(new CubicCurve2D.Double(cx - r * 1.4, y0, cx - r * 0.4, y0 - r * 0.6,
                            cx + r * 0.4, y0 + r * 0.6, cx + r * 1.4, y0));
                }
            }
            case CLAY -> {
                g.setColor(new Color(110, 50, 20, 70));
                g.setStroke(new BasicStroke(sw * 0.8f));
                for (int k = 0; k < 3; k++) {
                    double y0 = cy - r * 0.4 + k * r * 0.45;
                    g.draw(new QuadCurve2D.Double(cx - r * 1.2, y0, cx, y0 + r * 0.25, cx + r * 1.2, y0));
                }
            }
            case PEWTER -> {
                g.setColor(new Color(0x4E5764));
                for (int k = 0; k < 5; k++) {
                    double a = k * Math.PI * 2 / 5 + 0.3;
                    g.fill(circ(cx + Math.cos(a) * r * 0.78, cy + Math.sin(a) * r * 0.62, r * 0.07));
                }
            }
            case STORMHEAD -> {
                g.setPaint(vgrad(cy, new Color(20, 10, 40, 0), cy + r, new Color(20, 10, 40, 150)));
                g.fill(new Rectangle2D.Double(cx - r * 2, cy, r * 4, r * 2));
            }
            default -> {
            }
        }
        if (stage > 0) {
            Random rnd = new Random(t.ordinal() * 31L + 7);
            g.setColor(mix(c, INK, 0.7));
            g.setStroke(new BasicStroke(sw * 0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < stage * 2; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                Path2D.Double crack = new Path2D.Double();
                double x = cx + Math.cos(a) * r * 0.2, y = cy + Math.sin(a) * r * 0.2;
                crack.moveTo(x, y);
                for (int k = 0; k < 4; k++) {
                    a += rnd.nextDouble() * 0.9 - 0.45;
                    x += Math.cos(a) * r * 0.25;
                    y += Math.sin(a) * r * 0.25;
                    crack.lineTo(x, y);
                }
                g.draw(crack);
            }
        }
        g.setPaint(radial(cx - r * 0.35, cy - r * 0.5, r * 0.55, new Color(255, 255, 255, 170), new Color(255, 255, 255, 0)));
        g.fill(circ(cx - r * 0.35, cy - r * 0.5, r * 0.55));
        g.setClip(oldClip);

        g.setColor(t == PuffType.FROST ? new Color(0x6FA4DA) : INK);
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(body);

        // face
        boolean darkFace = t == PuffType.SOOT || t == PuffType.STORMHEAD;
        Color faceC = darkFace ? new Color(0xF4F0FF) : INK;
        double ex = r * 0.3, ey = cy + r * 0.02, ew = r * 0.16, eh = r * 0.22;
        if (t == PuffType.MARBLE) {
            g.setColor(Color.WHITE);
            g.fill(circ(cx - ex, ey, ew * 0.95));
            g.fill(circ(cx + ex, ey, ew * 0.95));
        }
        g.setColor(faceC);
        g.fill(ell(cx - ex - ew / 2, ey - eh / 2, ew, eh));
        g.fill(ell(cx + ex - ew / 2, ey - eh / 2, ew, eh));
        if (!darkFace) {
            g.setColor(Color.WHITE);
            g.fill(circ(cx - ex - ew * 0.15, ey - eh * 0.2, Math.max(0.6, ew * 0.22)));
            g.fill(circ(cx + ex - ew * 0.15, ey - eh * 0.2, Math.max(0.6, ew * 0.22)));
        }
        g.setColor(faceC);
        g.setStroke(new BasicStroke(Math.max(1f, sw * 0.8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (t.isBoss()) {
            g.draw(new Line2D.Double(cx - ex - ew, ey - eh * 1.2, cx - ex + ew * 0.8, ey - eh * 0.7));
            g.draw(new Line2D.Double(cx + ex + ew, ey - eh * 1.2, cx + ex - ew * 0.8, ey - eh * 0.7));
            g.draw(new Arc2D.Double(cx - r * 0.16, cy + r * 0.28, r * 0.32, r * 0.18, 20, 140, Arc2D.OPEN));
        } else {
            g.draw(new Arc2D.Double(cx - r * 0.13, cy + r * 0.1, r * 0.26, r * 0.2, 200, 140, Arc2D.OPEN));
        }
        if (t != PuffType.SOOT && t != PuffType.PEWTER && t != PuffType.STORMHEAD) {
            cheek(g, cx - r * 0.5, cy + r * 0.22, r * 0.13);
            cheek(g, cx + r * 0.5, cy + r * 0.22, r * 0.13);
        }
        if (t == PuffType.FROST) {
            g.setColor(new Color(0x9CC8F0));
            g.fill(star(cx + r * 0.75, cy - r * 0.55, r * 0.28, r * 0.08, 6, 0));
            g.fill(star(cx - r * 0.8, cy + r * 0.45, r * 0.2, r * 0.06, 6, 0.3));
        }
        if (t == PuffType.STORMHEAD) {
            Path2D.Double bolt = new Path2D.Double();
            double bx = cx + r * 0.45, by = cy + r * 0.55;
            bolt.moveTo(bx, by);
            bolt.lineTo(bx + r * 0.28, by);
            bolt.lineTo(bx + r * 0.12, by + r * 0.3);
            bolt.lineTo(bx + r * 0.3, by + r * 0.3);
            bolt.lineTo(bx - r * 0.05, by + r * 0.8);
            bolt.lineTo(bx + r * 0.05, by + r * 0.42);
            bolt.lineTo(bx - r * 0.12, by + r * 0.42);
            bolt.closePath();
            ink(g, bolt, new Color(0xFFE14D), sw * 0.8f);
        }
    }

    static BufferedImage ice(PuffType t) {
        return cached("ice:" + t, () -> {
            int s = puffImageSize(t, 1);
            BufferedImage img = newImage(s, s);
            Graphics2D g = gfx(img);
            double c = s / 2.0, r = t.radius * 1.12;
            Shape sh = cloud(c, c, r);
            g.setColor(new Color(190, 228, 255, 120));
            g.fill(sh);
            g.setColor(new Color(235, 248, 255, 230));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(sh);
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(c - r * 0.6, c - r * 0.2, c - r * 0.25, c - r * 0.55));
            g.draw(new Line2D.Double(c - r * 0.45, c + 0.05 * r, c - r * 0.1, c - r * 0.3));
            g.dispose();
            return img;
        });
    }

    // ------------------------------------------------------------------ projectiles

    static BufferedImage projectile(Projectile.Kind k, boolean fire) {
        return cached("proj:" + k + fire, () -> projectileImage(k, fire, 1));
    }

    static BufferedImage projectileImage(Projectile.Kind k, boolean fire, double scale) {
        int s = (int) Math.ceil(20 * scale);
        BufferedImage img = newImage(s, s);
        Graphics2D g = gfx(img);
        g.scale(scale, scale);
        switch (k) {
            case QUILL -> {
                line(g, new Color(0x4A3220), 2.6f, 2, 10, 14, 10);
                line(g, new Color(0xE8D7B4), 1.4f, 3, 10, 13, 10);
                Path2D.Double tip = new Path2D.Double();
                tip.moveTo(13, 8);
                tip.lineTo(19, 10);
                tip.lineTo(13, 12);
                tip.closePath();
                ink(g, tip, new Color(0x3E2A1A), 1f);
            }
            case STINGER -> {
                if (fire) {
                    g.setPaint(radial(10, 10, 9, new Color(255, 220, 90, 230), new Color(255, 90, 30, 0)));
                    g.fill(circ(10, 10, 9));
                    ink(g, ell(5, 7.5, 10, 5), new Color(0xFF7A2A), 1f);
                } else {
                    Path2D.Double st = new Path2D.Double();
                    st.moveTo(4, 7.5);
                    st.lineTo(13, 7.5);
                    st.lineTo(18, 10);
                    st.lineTo(13, 12.5);
                    st.lineTo(4, 12.5);
                    st.closePath();
                    ink(g, st, new Color(0xFFD23F), 1f);
                    g.setColor(INK);
                    g.fill(new Rectangle2D.Double(7, 7.8, 2, 4.4));
                    g.fill(new Rectangle2D.Double(11, 7.8, 1.6, 4.4));
                }
            }
            case ACORN -> acorn(g, 10, 11, 0.9);
            case BOLT -> {
                g.setPaint(radial(10, 10, 10, new Color(220, 170, 255, 220), new Color(150, 80, 255, 0)));
                g.fill(circ(10, 10, 10));
                ink(g, star(10, 10, 6.5, 2.6, 4, 0), new Color(0xF4E8FF), 1f);
                g.setColor(new Color(0xFFE066));
                g.fill(circ(10, 10, 1.6));
            }
        }
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------------ maps

    static BufferedImage background(GameMap m) {
        int W = GameMap.W, H = GameMap.H;
        BufferedImage img = newImage(W, H);
        Graphics2D g = gfx(img);
        Random rnd = new Random(m.seed);
        boolean isle = m.style == GameMap.Style.ISLE;

        Color grassA = isle ? new Color(0x9CC66A) : new Color(0x8EC063);
        Color grassB = isle ? new Color(0x6FA34B) : new Color(0x5E9A45);
        g.setPaint(vgrad(0, grassA, H, grassB));
        g.fillRect(0, 0, W, H);
        for (int i = 0; i < 45; i++) {
            double x = rnd.nextDouble() * W, y = rnd.nextDouble() * H, r = 40 + rnd.nextDouble() * 90;
            Color c = rnd.nextBoolean() ? new Color(255, 255, 210, 40) : new Color(30, 70, 20, 32);
            g.setPaint(radial(x, y, r, c, alpha(c, 0)));
            g.fill(circ(x, y, r));
        }
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 9000; i++) {
            double x = rnd.nextDouble() * W, y = rnd.nextDouble() * H;
            double t = rnd.nextDouble();
            g.setColor(alpha(mix(mix(grassA, Color.WHITE, 0.25), mix(grassB, INK, 0.3), t), 140));
            g.draw(new Line2D.Double(x, y, x + rnd.nextDouble() * 2 - 1, y - 2 - rnd.nextDouble() * 3));
        }

        if (isle) {
            Path2D.Double shore = new Path2D.Double();
            shore.moveTo(W, 0);
            shore.lineTo(705, 0);
            for (int y = 0; y <= H; y += 20) shore.lineTo(700 + Math.sin(y * 0.05) * 8 + rnd.nextDouble() * 4, y);
            shore.lineTo(W, H);
            shore.closePath();
            g.setPaint(new GradientPaint(690, 0, new Color(0xE8D9A6), 760, 0, new Color(0xF4E7BE)));
            g.fill(shore);
            g.setColor(alpha(new Color(0xB89F62), 120));
            for (int i = 0; i < 500; i++) {
                double x = 710 + rnd.nextDouble() * 40, y = rnd.nextDouble() * H;
                g.fill(circ(x, y, 0.8));
            }
        }
        for (java.awt.Shape w : m.water) drawWater(g, w, rnd, isle);

        // path
        Path2D.Double path = m.pathShape();
        float pw = (float) m.pathWidth;
        g.setStroke(new BasicStroke(pw + 12, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(40, 60, 20, 60));
        g.draw(path);
        g.setStroke(new BasicStroke(pw + 6, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x7A5A36));
        g.draw(path);
        g.setStroke(new BasicStroke(pw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(isle ? new Color(0xD9BE88) : new Color(0xCBA26E));
        g.draw(path);
        g.setStroke(new BasicStroke(pw * 0.45f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 240, 200, 45));
        g.draw(path);
        for (double d = 0; d < m.length; d += 3.5) {
            Point2D.Double p = m.pointAt(d);
            double ox = (rnd.nextDouble() - 0.5) * pw * 0.9, oy = (rnd.nextDouble() - 0.5) * pw * 0.9;
            double r = 0.6 + rnd.nextDouble() * 1.6;
            g.setColor(rnd.nextInt(3) == 0 ? new Color(0x8C6A45) : new Color(0xE6CDA0));
            g.fill(circ(p.x + ox, p.y + oy, r));
        }

        // flowers and little stones
        Color[] petals = {new Color(0xFFFFFF), new Color(0xFFE066), new Color(0xF7A8C8), new Color(0xB9A7F2), new Color(0xFF8F6B)};
        for (int i = 0; i < 220; i++) {
            double x = rnd.nextDouble() * W, y = rnd.nextDouble() * H;
            if (m.distToPath(x, y) < m.pathWidth / 2 + 8 || inWater(m, x, y) || (isle && x > 695)) continue;
            flower(g, x, y, petals[rnd.nextInt(petals.length)], 1.6 + rnd.nextDouble() * 1.2);
        }
        for (double[] r : m.rocks) rock(g, r[0], r[1], r[2], rnd);
        for (double[] t : m.trees) tree(g, t[0], t[1], t[2], rnd, isle);
        if (m.lighthouse != null) lighthouse(g, m.lighthouse[0], m.lighthouse[1]);

        // soft vignette to frame the page
        g.setPaint(new RadialGradientPaint(new Point2D.Double(W / 2.0, H / 2.0), (float) (W * 0.75),
                new float[]{0.6f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(40, 30, 10, 70)}));
        g.fillRect(0, 0, W, H);
        g.dispose();
        return img;
    }

    private static boolean inWater(GameMap m, double x, double y) {
        for (java.awt.Shape s : m.water) if (s.contains(x, y)) return true;
        return false;
    }

    private static void drawWater(Graphics2D g, java.awt.Shape w, Random rnd, boolean sea) {
        Rectangle2D b = w.getBounds2D();
        if (sea) {
            g.setPaint(new GradientPaint((float) b.getX(), 0, new Color(0x7CC4D8), (float) b.getMaxX(), 0, new Color(0x3F86B8)));
            g.fill(w);
            g.setColor(new Color(255, 255, 255, 120));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 40; i++) {
                double x = b.getX() + 6 + rnd.nextDouble() * (b.getWidth() - 12), y = rnd.nextDouble() * b.getHeight();
                g.draw(new Arc2D.Double(x, y, 8, 4, 0, 180, Arc2D.OPEN));
            }
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Double foam = new Path2D.Double();
            foam.moveTo(b.getX() + 2, 0);
            for (int y = 0; y <= b.getHeight(); y += 10) foam.lineTo(b.getX() + 2 + Math.sin(y * 0.15) * 2.5, y);
            g.draw(foam);
            return;
        }
        g.setColor(new Color(0x6E8F4A));
        g.fill(new Ellipse2D.Double(b.getX() - 5, b.getY() - 4, b.getWidth() + 10, b.getHeight() + 8));
        g.setPaint(radial(b.getCenterX() - b.getWidth() * 0.15, b.getCenterY() - b.getHeight() * 0.2,
                b.getWidth() * 0.7, new Color(0x9ED6E8), new Color(0x4F92B8)));
        g.fill(w);
        ink(g, w, null, 1.6f);
        g.setColor(new Color(255, 255, 255, 140));
        g.setStroke(new BasicStroke(1.3f));
        for (int i = 0; i < 4; i++) {
            double x = b.getX() + 15 + rnd.nextDouble() * (b.getWidth() - 40), y = b.getY() + 15 + rnd.nextDouble() * (b.getHeight() - 30);
            g.draw(new Arc2D.Double(x, y, 16, 6, 10, 160, Arc2D.OPEN));
        }
        for (int i = 0; i < 3; i++) {
            double x = b.getX() + 20 + rnd.nextDouble() * (b.getWidth() - 40), y = b.getY() + 18 + rnd.nextDouble() * (b.getHeight() - 36);
            Shape pad = new Arc2D.Double(x - 7, y - 5, 14, 10, 30, 300, Arc2D.PIE);
            ink(g, pad, new Color(0x5FA046), 1f);
            if (i == 0) {
                g.setColor(new Color(0xF7B8D0));
                g.fill(star(x, y, 3.5, 1.8, 6, 0));
            }
        }
    }

    private static void flower(Graphics2D g, double x, double y, Color c, double r) {
        g.setColor(c);
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5;
            g.fill(circ(x + Math.cos(a) * r, y + Math.sin(a) * r, r * 0.75));
        }
        g.setColor(new Color(0xF2B530));
        g.fill(circ(x, y, r * 0.6));
    }

    private static void rock(Graphics2D g, double x, double y, double r, Random rnd) {
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(ell(x - r, y - r * 0.2, r * 2.2, r * 1.3));
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI * 2 / 8, rr = r * (0.8 + rnd.nextDouble() * 0.3);
            double px = x + Math.cos(a) * rr * 1.15, py = y + Math.sin(a) * rr * 0.85;
            if (i == 0) p.moveTo(px, py);
            else p.lineTo(px, py);
        }
        p.closePath();
        ink(g, p, vgrad(y - r, new Color(0xC9C4BA), y + r, new Color(0x8A847A)), 1.4f);
        g.setColor(new Color(0x7FA65A));
        g.fill(ell(x - r * 0.6, y - r * 0.75, r * 0.7, r * 0.35));
    }

    private static void tree(Graphics2D g, double x, double y, double r, Random rnd, boolean isle) {
        g.setColor(new Color(20, 40, 10, 70));
        g.fill(ell(x - r * 0.9, y + r * 0.2, r * 2.2, r * 1.1));
        Area canopy = new Area(circ(x, y, r * 0.75));
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI * 2 / 6 + rnd.nextDouble() * 0.3;
            canopy.add(new Area(circ(x + Math.cos(a) * r * 0.5, y + Math.sin(a) * r * 0.45, r * 0.5)));
        }
        Color leaf = isle ? new Color(0x4E8E46) : new Color(0x3F7F3C);
        ink(g, canopy, radial(x - r * 0.35, y - r * 0.4, r * 1.4, mix(leaf, new Color(0xD8F0A0), 0.45), mix(leaf, INK, 0.25)), 1.6f);
        g.setColor(new Color(255, 255, 220, 70));
        g.fill(circ(x - r * 0.3, y - r * 0.35, r * 0.3));
        g.setColor(new Color(20, 50, 15, 90));
        g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 5; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = rnd.nextDouble() * r * 0.6;
            g.draw(new Arc2D.Double(x + Math.cos(a) * d - 3, y + Math.sin(a) * d - 2, 6, 4, 200, 140, Arc2D.OPEN));
        }
        if (!isle && rnd.nextBoolean()) {
            g.setColor(new Color(0xE5484D));
            g.fill(circ(x + r * 0.3, y + r * 0.1, 2.2));
            g.fill(circ(x - r * 0.2, y + r * 0.35, 2.2));
        }
    }

    private static void lighthouse(Graphics2D g, double x, double y) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(ell(x - 20, y + 30, 46, 14));
        g.setPaint(radial(x, y - 38, 40, new Color(255, 240, 160, 150), new Color(255, 240, 160, 0)));
        g.fill(circ(x, y - 38, 40));
        Path2D.Double tower = new Path2D.Double();
        tower.moveTo(x - 15, y + 36);
        tower.lineTo(x - 9, y - 26);
        tower.lineTo(x + 9, y - 26);
        tower.lineTo(x + 15, y + 36);
        tower.closePath();
        g.setColor(new Color(0xF7F2E8));
        g.fill(tower);
        Shape old = g.getClip();
        g.clip(tower);
        g.setColor(new Color(0xD8463E));
        for (int i = 0; i < 3; i++) g.fill(new Rectangle2D.Double(x - 20, y - 18 + i * 20, 40, 10));
        g.setClip(old);
        ink(g, tower, null);
        ink(g, new Rectangle2D.Double(x - 11, y - 30, 22, 5), new Color(0x3A3A44));
        ink(g, new RoundRectangle2D.Double(x - 7, y - 44, 14, 14, 3, 3), new Color(0xFFF2A8));
        Path2D.Double roof = new Path2D.Double();
        roof.moveTo(x - 10, y - 44);
        roof.lineTo(x, y - 55);
        roof.lineTo(x + 10, y - 44);
        roof.closePath();
        ink(g, roof, new Color(0xD8463E));
        ink(g, new RoundRectangle2D.Double(x - 4, y + 22, 8, 14, 6, 6), new Color(0x6B4A2A));
    }
}
