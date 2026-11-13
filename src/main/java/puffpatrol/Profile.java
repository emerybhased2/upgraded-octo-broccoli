package puffpatrol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Progress kept between games: critter experience (which unlocks upgrades), medals per map and
 * difficulty, and settings. Stored in ~/.puffpatrol/profile.properties.
 */
final class Profile {
    /** Experience a critter needs before each tier (1 to 5) unlocks, on every path. */
    static final int[] XP_NEEDED = {0, 60, 300, 1200, 3500};

    private final Map<TowerType, Integer> xp = new EnumMap<>(TowerType.class);
    private final Set<String> medals = new HashSet<>();
    private final File file;
    boolean allUnlocked;
    boolean soundOn = true;
    Difficulty lastDifficulty = Difficulty.BLUSTERY;

    private Profile(File file) {
        this.file = file;
    }

    static File dir() {
        return new File(System.getProperty("user.home"), ".puffpatrol");
    }

    /** A profile that never touches the disk (for tests and tools). */
    static Profile memory() {
        return new Profile(null);
    }

    static Profile load() {
        Profile p = new Profile(new File(dir(), "profile.properties"));
        if (!p.file.isFile()) return p;
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(p.file)) {
            props.load(in);
        } catch (IOException e) {
            return p;
        }
        for (TowerType t : TowerType.values()) {
            p.xp.put(t, parse(props.getProperty("xp." + t.name()), 0));
        }
        String m = props.getProperty("medals", "");
        for (String s : m.split(";")) if (!s.isBlank()) p.medals.add(s);
        p.allUnlocked = Boolean.parseBoolean(props.getProperty("allUnlocked", "false"));
        p.soundOn = Boolean.parseBoolean(props.getProperty("sound", "true"));
        try {
            p.lastDifficulty = Difficulty.valueOf(props.getProperty("difficulty", "BLUSTERY"));
        } catch (IllegalArgumentException ignored) {
            // keep default
        }
        return p;
    }

    private static int parse(String s, int def) {
        try {
            return s == null ? def : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    void save() {
        if (file == null) return;
        Properties props = new Properties();
        for (Map.Entry<TowerType, Integer> e : xp.entrySet()) props.setProperty("xp." + e.getKey().name(), String.valueOf(e.getValue()));
        props.setProperty("medals", String.join(";", medals));
        props.setProperty("allUnlocked", String.valueOf(allUnlocked));
        props.setProperty("sound", String.valueOf(soundOn));
        props.setProperty("difficulty", lastDifficulty.name());
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "Puffball Patrol profile");
        } catch (IOException e) {
            System.err.println("Could not save profile: " + e.getMessage());
        }
    }

    int xp(TowerType t) {
        return xp.getOrDefault(t, 0);
    }

    void addXp(TowerType t, int amount) {
        xp.merge(t, amount, Integer::sum);
    }

    boolean unlocked(TowerType t, int path, int tier) {
        return allUnlocked || xp(t) >= XP_NEEDED[tier - 1];
    }

    /** Experience still needed for an upgrade (0 if unlocked). */
    int xpShort(TowerType t, int path, int tier) {
        return allUnlocked ? 0 : Math.max(0, XP_NEEDED[tier - 1] - xp(t));
    }

    boolean hasMedal(String map, Difficulty d) {
        return medals.contains(map + ":" + d.name());
    }

    void addMedal(String map, Difficulty d) {
        medals.add(map + ":" + d.name());
    }
}
