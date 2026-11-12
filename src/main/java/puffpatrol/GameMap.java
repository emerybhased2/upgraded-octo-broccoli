package puffpatrol;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

final class GameMap {
    static final int W = 800, H = 640;

    enum Style { MEADOW, ISLE }

    final String name;
    final String blurb;
    final Style style;
    final long seed;
    final double pathWidth;
    final double[] xs, ys, cum;
    final double length;
    /** Ponds, sea and the like: drawn as water and not buildable. */
    final List<Shape> water = new ArrayList<>();
    /** Trees: {x, y, radius}. Not buildable. */
    final List<double[]> trees = new ArrayList<>();
    /** Rocks: {x, y, radius}. Not buildable. */
    final List<double[]> rocks = new ArrayList<>();
    /** Lighthouse position {x, y}, or null. */
    double[] lighthouse;
    private BufferedImage background;

    GameMap(String name, String blurb, Style style, long seed, double pathWidth, double... coords) {
        this.name = name;
        this.blurb = blurb;
        this.style = style;
        this.seed = seed;
        this.pathWidth = pathWidth;
        int n = coords.length / 2;
        xs = new double[n];
        ys = new double[n];
        cum = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = coords[i * 2];
            ys[i] = coords[i * 2 + 1];
            if (i > 0) cum[i] = cum[i - 1] + Math.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]);
        }
        length = cum[n - 1];
    }

    BufferedImage background() {
        if (background == null) background = Art.background(this);
        return background;
    }

    Point2D.Double pointAt(double d) {
        if (d <= 0) return new Point2D.Double(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) {
            if (d <= cum[i]) {
                double f = (d - cum[i - 1]) / (cum[i] - cum[i - 1]);
                return new Point2D.Double(xs[i - 1] + (xs[i] - xs[i - 1]) * f, ys[i - 1] + (ys[i] - ys[i - 1]) * f);
            }
        }
        return new Point2D.Double(xs[xs.length - 1], ys[ys.length - 1]);
    }

    Path2D.Double pathShape() {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) p.lineTo(xs[i], ys[i]);
        return p;
    }

    double distToPath(double x, double y) {
        double best = Double.MAX_VALUE;
        for (int i = 1; i < xs.length; i++) {
            double ax = xs[i - 1], ay = ys[i - 1], bx = xs[i], by = ys[i];
            double dx = bx - ax, dy = by - ay;
            double t = ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy);
            t = Math.max(0, Math.min(1, t));
            best = Math.min(best, Math.hypot(x - (ax + dx * t), y - (ay + dy * t)));
        }
        return best;
    }

    boolean blocked(double x, double y, double r) {
        if (x < r || y < r || x > W - r || y > H - r) return true;
        if (distToPath(x, y) < pathWidth / 2 + r - 4) return true;
        Rectangle2D box = new Rectangle2D.Double(x - r * 0.7, y - r * 0.7, r * 1.4, r * 1.4);
        for (Shape s : water) if (s.intersects(box)) return true;
        for (double[] t : trees) if (Math.hypot(x - t[0], y - t[1]) < t[2] + r - 4) return true;
        for (double[] t : rocks) if (Math.hypot(x - t[0], y - t[1]) < t[2] + r - 2) return true;
        return lighthouse != null && Math.hypot(x - lighthouse[0], y - lighthouse[1]) < 26 + r;
    }

    boolean canPlace(double x, double y, List<Tower> towers) {
        if (blocked(x, y, Tower.RADIUS)) return false;
        for (Tower t : towers) if (Math.hypot(t.x - x, t.y - y) < Tower.RADIUS * 2) return false;
        return true;
    }
}
