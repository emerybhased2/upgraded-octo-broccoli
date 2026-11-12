package puffpatrol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/** One save slot, written between rounds, so a game can be resumed from the title page. */
final class SaveGame {
    private SaveGame() {
    }

    static File file() {
        return new File(Profile.dir(), "savegame.properties");
    }

    static boolean exists() {
        return file().isFile();
    }

    static void delete() {
        file().delete();
    }

    static void save(World w) {
        Properties p = new Properties();
        p.setProperty("map", w.map.name);
        p.setProperty("difficulty", w.difficulty.name());
        p.setProperty("round", String.valueOf(w.round));
        p.setProperty("cash", String.valueOf(w.cash));
        p.setProperty("lives", String.valueOf(w.lives));
        p.setProperty("pops", String.valueOf(w.totalPops));
        p.setProperty("auto", String.valueOf(w.autoStart));
        p.setProperty("victory", String.valueOf(w.victory));
        for (int i = 0; i < w.towers.size(); i++) {
            Tower t = w.towers.get(i);
            p.setProperty("tower." + i, String.join(",", t.type.name(), fmt(t.x), fmt(t.y),
                    String.valueOf(t.tiers[0]), String.valueOf(t.tiers[1]), String.valueOf(t.tiers[2]), t.mode.name(),
                    String.valueOf(t.spent), String.valueOf(t.pops), String.valueOf(t.abilityCooldown)));
        }
        file().getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file())) {
            p.store(out, "Puffball Patrol saved game");
        } catch (IOException e) {
            System.err.println("Could not save game: " + e.getMessage());
        }
    }

    private static String fmt(double d) {
        return String.valueOf(Math.round(d * 10) / 10.0);
    }

    private static Properties read() {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(file())) {
            p.load(in);
            return p;
        } catch (IOException e) {
            return null;
        }
    }

    /** "Millpond · Stormy · round 23", or null if there is no usable save. */
    static String summary() {
        Properties p = exists() ? read() : null;
        if (p == null) return null;
        try {
            Difficulty d = Difficulty.valueOf(p.getProperty("difficulty"));
            return p.getProperty("map") + " · " + d.label + " · round " + (Integer.parseInt(p.getProperty("round")) + 1);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static World load(Profile profile) {
        Properties p = exists() ? read() : null;
        if (p == null) return null;
        try {
            GameMap map = Maps.byName(p.getProperty("map"));
            if (map == null) return null;
            World w = new World(map, Difficulty.valueOf(p.getProperty("difficulty")), profile);
            w.round = Integer.parseInt(p.getProperty("round"));
            w.cash = Integer.parseInt(p.getProperty("cash"));
            w.lives = Integer.parseInt(p.getProperty("lives"));
            w.totalPops = Long.parseLong(p.getProperty("pops", "0"));
            w.autoStart = Boolean.parseBoolean(p.getProperty("auto", "false"));
            w.victory = Boolean.parseBoolean(p.getProperty("victory", "false"));
            w.victoryAcknowledged = w.victory;
            for (int i = 0; p.containsKey("tower." + i); i++) {
                String[] f = p.getProperty("tower." + i).split(",");
                Tower t = new Tower(TowerType.valueOf(f[0]), Double.parseDouble(f[1]), Double.parseDouble(f[2]));
                // Saves from the two-path version have 9 fields; three-path saves have 10.
                int paths = f.length >= 10 ? 3 : 2;
                for (int path = 0; path < paths; path++) {
                    for (int k = 0; k < Integer.parseInt(f[3 + path]); k++) t.applyUpgrade(path, 0);
                }
                int o = 3 + paths;
                t.mode = Tower.Target.valueOf(f[o]);
                t.spent = Integer.parseInt(f[o + 1]);
                t.pops = Integer.parseInt(f[o + 2]);
                t.abilityCooldown = Integer.parseInt(f[o + 3]);
                t.anim = 0;
                w.towers.add(t);
            }
            return w;
        } catch (RuntimeException e) {
            System.err.println("Saved game could not be read: " + e);
            return null;
        }
    }
}
