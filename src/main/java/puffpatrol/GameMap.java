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

    enum Style { MEADOW, ISLE, FOREST, CLIFF }

    /** One path the puffs follow. A map may have several; spawns alternate between them. */
    static final class Track {
        final double[] xs, ys, cum;
        final double length;

        Track(double... coords) {
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

        Path2D.Double shape() {
            Path2D.Double p = new Path2D.Double();
            p.moveTo(xs[0], ys[0]);
            for (int i = 1; i < xs.length; i++) p.lineTo(xs[i], ys[i]);
            return p;
        }

        double distTo(double x, double y) {
            double best = Double.MAX_VALUE;
            for (int i = 1; i < xs.length; i++) {
                double ax = xs[i - 1], ay = ys[i - 1], dx = xs[i] - ax, dy = ys[i] - ay;
                double t = Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)));
                best = Math.min(best, Math.hypot(x - (ax + dx * t), y - (ay + dy * t)));
            }
            return best;
        }
    }

    final String name;
    final String blurb;
    final String tier;
    final Style style;
    final long seed;
    final double pathWidth;
    final List<Track> tracks = new ArrayList<>();
    /** Ponds, brooks and sea: water critters live here, land critters cannot. */
    final List<Shape> water = new ArrayList<>();
    /** Ravines and pits: nothing can be built. */
    final List<Shape> chasms = new ArrayList<>();
    /** {x, y, radius} */
    final List<double[]> trees = new ArrayList<>();
    final List<double[]> rocks = new ArrayList<>();
    double[] lighthouse;
    double[] windmill;
    /** Coins at the start of a game. Two-path maps get extra, since critters must cover both paths. */
    int startCash = 650;
    private BufferedImage background;

    GameMap(String name, String tier, String blurb, Style style, long seed, double pathWidth) {
        this.name = name;
        this.tier = tier;
        this.blurb = blurb;
        this.style = style;
        this.seed = seed;
        this.pathWidth = pathWidth;
    }

    GameMap track(double... coords) {
        tracks.add(new Track(coords));
        if (tracks.size() > 1) startCash = 1100;
        return this;
    }

    BufferedImage background() {
        if (background == null) background = Art.background(this);
        return background;
    }

    Point2D.Double pointAt(int track, double d) {
        return tracks.get(track).pointAt(d);
    }

    double length(int track) {
        return tracks.get(track).length;
    }

    double distToPath(double x, double y) {
        double best = Double.MAX_VALUE;
        for (Track t : tracks) best = Math.min(best, t.distTo(x, y));
        return best;
    }

    boolean inWater(double x, double y) {
        for (Shape s : water) if (s.contains(x, y)) return true;
        return false;
    }

    private boolean circleInWater(double x, double y, double r) {
        for (Shape s : water) {
            if (s.contains(x, y) && s.contains(x - r, y) && s.contains(x + r, y) && s.contains(x, y - r) && s.contains(x, y + r)) {
                return true;
            }
        }
        return false;
    }

    private boolean touchesAny(List<Shape> shapes, double x, double y, double r) {
        Rectangle2D box = new Rectangle2D.Double(x - r * 0.7, y - r * 0.7, r * 1.4, r * 1.4);
        for (Shape s : shapes) if (s.intersects(box)) return true;
        return false;
    }

    boolean canPlace(TowerType type, double x, double y, List<Tower> towers) {
        double r = Tower.RADIUS;
        if (x < r || y < r || x > W - r || y > H - r) return false;
        if (distToPath(x, y) < pathWidth / 2 + r - 4) return false;
        for (Tower t : towers) if (Math.hypot(t.x - x, t.y - y) < r * 2) return false;
        if (type.placement == TowerType.Placement.WATER) return circleInWater(x, y, r * 0.75);
        if (touchesAny(water, x, y, r) || touchesAny(chasms, x, y, r)) return false;
        for (double[] t : trees) if (Math.hypot(x - t[0], y - t[1]) < t[2] + r - 4) return false;
        for (double[] t : rocks) if (Math.hypot(x - t[0], y - t[1]) < t[2] + r - 2) return false;
        if (lighthouse != null && Math.hypot(x - lighthouse[0], y - lighthouse[1]) < 26 + r) return false;
        return windmill == null || Math.hypot(x - windmill[0], y - windmill[1]) >= 30 + r;
    }
}
