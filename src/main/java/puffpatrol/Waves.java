package puffpatrol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Round definitions. Each group is "type[.flags] count spacingTicks [+delayTicks]" and groups run side by side.
 * Flags: h = hidden, r = regrowing, a = armoured.
 */
final class Waves {
    record Spawn(int tick, PuffType type, boolean hidden, boolean regrow, boolean armoured) {
    }

    private static final String[] ROUNDS = {
            /* 1 */ "rose 20 35",
            "rose 30 28",
            "rose 20 30, sky 6 40 +300",
            "rose 30 22, sky 15 35 +200",
            "sky 25 30, rose 10 30 +400",
            "rose 15 25, sky 15 30, moss 4 50 +300",
            "rose 20 20, sky 25 25 +100, moss 6 40 +500",
            "sky 20 22, moss 14 30 +200",
            "moss 30 22",
            /* 10 */ "sky 100 10",
            "rose 10 20, sky 10 20, moss 12 25 +100, butter 3 60 +500",
            "sky 15 20, moss 10 25, butter 5 45 +300",
            "sky 50 14, moss 23 20 +200",
            "rose 50 10, sky 15 20, moss 10 20 +200, butter 9 30 +400",
            "rose 20 15, moss 15 20, butter 12 25 +200, blush 4 40 +500",
            "moss 40 14, butter 8 25 +400",
            "butter.r 8 30, butter 6 25 +300",
            "moss 80 10",
            "moss 10 20, butter 6 25 +100, blush 10 30 +300, soot 4 40 +600",
            /* 20 */ "soot 6 40",
            "butter 40 14, blush 14 22 +200",
            "frost 16 35",
            "soot 7 35, frost 7 35 +150",
            "moss.h 3 60, pewter 1 1 +400, blush 10 20",
            "butter.r 25 15, pewter 4 60 +200, soot 10 25 +400",
            "marble 23 30, blush 10 25",
            "rose 100 6, sky 60 8, moss 45 10, butter 45 12",
            "pewter 6 45",
            "butter 40 10, blush.r 30 12 +200, marble 6 40 +500",
            /* 30 */ "pewter 9 30",
            "marble 10 30, frost 12 25, soot 12 25 +200",
            "soot 25 15, frost 25 15, marble 12 30 +300",
            "butter.h 20 15, prism 5 50 +300",
            "butter 140 5, marble 6 40 +300",
            "soot 35 12, prism 25 25 +300",
            "blush 140 5, prism 10 30 +200",
            "prism 20 20, marble.h 15 25, pewter 10 30 +300, clay 2 60 +600",
            "pewter 30 25, clay 8 50 +200",
            "prism.r 40 15, clay 10 40 +300",
            /* 40 */ "nimbus 1 1, prism 12 30 +200",
            "prism 40 12, clay 12 35 +200",
            "prism.h 20 20, clay.r 6 50 +300",
            "clay 25 30, pewter.a 10 40 +100",
            "marble.h 30 15, prism.r 20 18 +300",
            "nimbus 2 300, clay 15 30 +200",
            "prism.h 50 10",
            "clay.a 10 40, prism 30 15 +200",
            "pewter.a 20 25, clay 20 25 +300",
            "prism.hr 40 12, clay.h 10 40 +200",
            /* 50 */ "nimbus 3 240, clay.a 12 30 +300",
            "clay.r 30 20",
            "prism.h 60 8, nimbus 2 300 +400",
            "clay.a 25 25, pewter.ha 15 30 +200",
            "nimbus.a 1 1, clay 20 25 +300",
            "prism.hr 80 6, clay.hr 15 30 +300",
            "nimbus 5 180",
            "clay.a 40 18, nimbus 2 300 +300",
            "prism.h 100 5, clay.ha 20 30 +200",
            "nimbus.a 3 200, clay.r 30 20 +200",
            /* 60 */ "stormhead 1 1, clay 20 30 +300",
            "nimbus 6 150, prism.hr 50 8",
            "clay.ha 50 14",
            "nimbus.a 4 180, clay.a 30 20 +300",
            "prism.hr 150 4, nimbus 3 200 +400",
            "stormhead 1 1, nimbus 4 200 +300",
            "clay.hra 60 10, nimbus.a 3 200 +300",
            "nimbus.a 8 120",
            "stormhead 2 600, clay.ha 40 15 +200",
            "nimbus.a 10 100, prism.hr 100 5 +200",
            /* 70 */ "stormhead.a 2 600, nimbus.a 6 150 +300",
    };

    private Waves() {
    }

    static int handWritten() {
        return ROUNDS.length;
    }

    static String spec(int round) {
        if (round <= ROUNDS.length) return ROUNDS[round - 1];
        int k = round - ROUNDS.length;
        StringBuilder sb = new StringBuilder();
        sb.append("clay.ha ").append(40 + k * 4).append(" 12");
        sb.append(", nimbus.a ").append(6 + k).append(" 120 +200");
        sb.append(", stormhead").append(k > 5 ? ".a " : " ").append(1 + k / 4).append(" 500 +600");
        return sb.toString();
    }

    static List<Spawn> round(int round) {
        List<Spawn> out = new ArrayList<>();
        for (String group : spec(round).split(",")) {
            String[] p = group.trim().split("\\s+");
            String[] typeFlags = p[0].split("\\.");
            PuffType type = PuffType.valueOf(typeFlags[0].toUpperCase());
            String flags = typeFlags.length > 1 ? typeFlags[1] : "";
            int count = Integer.parseInt(p[1]);
            int spacing = Integer.parseInt(p[2]);
            int delay = p.length > 3 ? Integer.parseInt(p[3].replace("+", "")) : 0;
            for (int i = 0; i < count; i++) {
                out.add(new Spawn(delay + i * spacing, type, flags.contains("h"), flags.contains("r"), flags.contains("a")));
            }
        }
        out.sort(Comparator.comparingInt(Spawn::tick));
        return out;
    }
}
