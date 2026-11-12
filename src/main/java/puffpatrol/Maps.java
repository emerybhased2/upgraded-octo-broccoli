package puffpatrol;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

final class Maps {
    private Maps() {
    }

    static List<GameMap> all() {
        return List.of(meadow(), isle());
    }

    static GameMap meadow() {
        GameMap m = new GameMap("Willow Meadow",
                "A winding cart track past a sleepy pond. Gentle, mostly.",
                GameMap.Style.MEADOW, 1907, 36,
                -40, 120, 180, 120, 180, 300, 80, 300, 80, 520, 340, 520, 340, 200,
                520, 200, 520, 90, 700, 90, 700, 380, 470, 380, 470, 560, 840, 560);
        m.water.add(new Ellipse2D.Double(560, 205, 110, 85));
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

    static GameMap isle() {
        GameMap m = new GameMap("Lighthouse Isle",
                "A long spiral up the island. Lots of room by the shore.",
                GameMap.Style.ISLE, 1945, 36,
                -40, 560, 160, 560, 160, 400, 320, 400, 320, 560, 480, 560, 480, 80,
                240, 80, 240, 250, 80, 250, 80, -40);
        m.water.add(new Rectangle2D.Double(748, 0, 60, GameMap.H));
        m.lighthouse = new double[]{712, 110};
        m.trees.add(new double[]{620, 300, 24});
        m.trees.add(new double[]{380, 250, 22});
        m.trees.add(new double[]{170, 160, 18});
        m.rocks.add(new double[]{650, 560, 14});
        m.rocks.add(new double[]{720, 420, 12});
        m.rocks.add(new double[]{30, 400, 12});
        return m;
    }
}
