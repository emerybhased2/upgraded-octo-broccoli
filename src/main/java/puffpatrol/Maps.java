package puffpatrol;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

final class Maps {
    private static List<GameMap> all;

    private Maps() {
    }

    /** All maps, easiest first. The list (and each map's painted background) is created once. */
    static synchronized List<GameMap> all() {
        if (all == null) {
            all = List.of(meadow(), cloverHill(), isle(), millpond(), mushroomHollow(), twinBrooks(), windmillCrossing(), thundercliff());
        }
        return all;
    }

    static GameMap byName(String name) {
        for (GameMap m : all()) if (m.name.equals(name)) return m;
        return null;
    }

    private static GameMap meadow() {
        GameMap m = new GameMap("Willow Meadow", "Beginner", "A winding cart track past a sleepy pond.",
                GameMap.Style.MEADOW, 1907, 36)
                .track(-40, 120, 180, 120, 180, 300, 80, 300, 80, 520, 340, 520, 340, 200,
                        520, 200, 520, 90, 700, 90, 700, 380, 470, 380, 470, 560, 840, 560);
        m.water.add(new Ellipse2D.Double(555, 200, 120, 95));
        m.trees.add(new double[]{40, 40, 24});
        m.trees.add(new double[]{300, 36, 26});
        m.trees.add(new double[]{760, 230, 24});
        m.trees.add(new double[]{600, 470, 26});
        m.trees.add(new double[]{230, 420, 22});
        m.trees.add(new double[]{760, 30, 20});
        m.rocks.add(new double[]{420, 290, 12});
        m.rocks.add(new double[]{150, 600, 10});
        return m;
    }

    private static GameMap cloverHill() {
        GameMap m = new GameMap("Clover Hill", "Beginner", "Long, lazy switchbacks. Plenty of room to settle in.",
                GameMap.Style.MEADOW, 2214, 36)
                .track(-40, 80, 700, 80, 700, 240, 100, 240, 100, 400, 700, 400, 700, 560, -40, 560);
        m.trees.add(new double[]{760, 160, 24});
        m.trees.add(new double[]{30, 320, 20});
        m.trees.add(new double[]{400, 160, 22});
        m.trees.add(new double[]{250, 480, 20});
        m.trees.add(new double[]{770, 610, 18});
        m.rocks.add(new double[]{560, 320, 11});
        m.rocks.add(new double[]{760, 470, 12});
        m.water.add(new Ellipse2D.Double(520, 455, 100, 70));
        return m;
    }

    private static GameMap isle() {
        GameMap m = new GameMap("Lighthouse Isle", "Intermediate", "A long spiral up the island, with sea along the shore.",
                GameMap.Style.ISLE, 1945, 36)
                .track(-40, 560, 160, 560, 160, 400, 320, 400, 320, 560, 480, 560, 480, 80,
                        240, 80, 240, 250, 80, 250, 80, -40);
        m.water.add(new Rectangle2D.Double(740, 0, 70, GameMap.H));
        m.lighthouse = new double[]{705, 110};
        m.trees.add(new double[]{620, 300, 24});
        m.trees.add(new double[]{380, 250, 22});
        m.trees.add(new double[]{170, 160, 18});
        m.rocks.add(new double[]{650, 560, 14});
        m.rocks.add(new double[]{700, 420, 12});
        m.rocks.add(new double[]{30, 400, 12});
        return m;
    }

    private static GameMap millpond() {
        GameMap m = new GameMap("Millpond", "Intermediate", "The track circles a broad pond. Otters welcome.",
                GameMap.Style.MEADOW, 3301, 36)
                .track(200, -40, 200, 90, 680, 90, 680, 550, 120, 550, 120, 200, -40, 200);
        m.water.add(new Ellipse2D.Double(240, 170, 360, 290));
        m.trees.add(new double[]{40, 60, 22});
        m.trees.add(new double[]{760, 300, 22});
        m.trees.add(new double[]{40, 600, 20});
        m.trees.add(new double[]{400, 30, 18});
        m.rocks.add(new double[]{760, 600, 12});
        m.rocks.add(new double[]{40, 330, 10});
        return m;
    }

    private static GameMap mushroomHollow() {
        GameMap m = new GameMap("Mushroom Hollow", "Intermediate", "Deep in the woods, the path zigzags between toadstools.",
                GameMap.Style.FOREST, 4127, 34)
                .track(80, -40, 80, 520, 240, 520, 240, 120, 400, 120, 400, 520, 560, 520, 560, 120, 720, 120, 720, 680);
        m.trees.add(new double[]{160, 40, 22});
        m.trees.add(new double[]{480, 40, 20});
        m.trees.add(new double[]{640, 600, 22});
        m.trees.add(new double[]{320, 610, 20});
        m.trees.add(new double[]{770, 400, 18});
        m.rocks.add(new double[]{160, 330, 11});
        m.rocks.add(new double[]{480, 300, 12});
        m.water.add(new Ellipse2D.Double(600, 220, 80, 120));
        return m;
    }

    private static GameMap twinBrooks() {
        GameMap m = new GameMap("Twin Brooks", "Advanced", "Two paths at once. Critters on the strip between them can reach both.",
                GameMap.Style.MEADOW, 5555, 34)
                .track(-40, 200, 400, 200, 400, 80, 700, 80, 700, 280, 840, 280)
                .track(-40, 440, 400, 440, 400, 560, 700, 560, 700, 360, 840, 360);
        m.water.add(brook(40, 300, 300, 40, 0.03, 8));
        m.water.add(brook(470, 610, 180, 30, 0.04, 4));
        m.trees.add(new double[]{550, 320, 22});
        m.trees.add(new double[]{60, 60, 22});
        m.trees.add(new double[]{60, 580, 22});
        m.trees.add(new double[]{770, 160, 18});
        m.rocks.add(new double[]{230, 120, 11});
        m.rocks.add(new double[]{770, 480, 10});
        return m;
    }

    private static GameMap windmillCrossing() {
        GameMap m = new GameMap("Windmill Crossing", "Advanced", "The path doubles back and crosses itself by the old mill.",
                GameMap.Style.MEADOW, 6062, 34)
                .track(-40, 320, 620, 320, 620, 100, 400, 100, 400, 560, 840, 560);
        m.windmill = new double[]{170, 150};
        m.water.add(new Ellipse2D.Double(560, 400, 140, 90));
        m.trees.add(new double[]{760, 200, 22});
        m.trees.add(new double[]{230, 470, 22});
        m.trees.add(new double[]{60, 560, 20});
        m.trees.add(new double[]{760, 40, 18});
        m.rocks.add(new double[]{300, 210, 11});
        return m;
    }

    private static GameMap thundercliff() {
        GameMap m = new GameMap("Thundercliff", "Expert", "A short, steep path along a cracked clifftop. Little room to build.",
                GameMap.Style.CLIFF, 7777, 34)
                .track(-40, 320, 250, 320, 250, 120, 550, 120, 550, 520, 840, 520);
        m.chasms.add(crack(40, 470, 200, 70, 11));
        m.chasms.add(crack(640, 240, 140, 90, 12));
        m.chasms.add(crack(330, 330, 120, 60, 13));
        m.chasms.add(crack(80, 60, 120, 50, 14));
        for (double[] r : new double[][]{{360, 230, 14}, {700, 80, 16}, {660, 420, 14}, {150, 230, 13}, {440, 600, 14},
                {300, 470, 12}, {760, 600, 13}, {420, 30, 12}}) {
            m.rocks.add(r);
        }
        m.water.add(new Ellipse2D.Double(625, 350, 70, 50));
        return m;
    }

    /** A wavy horizontal band of water. */
    private static Path2D.Double brook(double x, double y, double w, double h, double freq, double amp) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, y + Math.sin(x * freq) * amp);
        for (double xx = x; xx <= x + w; xx += 10) p.lineTo(xx, y + Math.sin(xx * freq) * amp);
        for (double xx = x + w; xx >= x; xx -= 10) p.lineTo(xx, y + h + Math.sin(xx * freq + 1) * amp * 0.6);
        p.closePath();
        return p;
    }

    /** A rocky ravine: an irregular rounded cleft. */
    private static Path2D.Double crack(double x, double y, double w, double h, long seed) {
        java.util.Random r = new java.util.Random(seed);
        double cx = x + w / 2, cy = y + h / 2;
        int n = 14;
        double[] px = new double[n], py = new double[n];
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n, k = 0.75 + r.nextDouble() * 0.35;
            px[i] = cx + Math.cos(a) * w / 2 * k;
            py[i] = cy + Math.sin(a) * h / 2 * k;
        }
        Path2D.Double p = new Path2D.Double();
        p.moveTo((px[n - 1] + px[0]) / 2, (py[n - 1] + py[0]) / 2);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            p.quadTo(px[i], py[i], (px[i] + px[j]) / 2, (py[i] + py[j]) / 2);
        }
        p.closePath();
        return p;
    }
}
