package puffpatrol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Round definitions. Each group is "type count spacingTicks [+delayTicks]"; groups run side by side. */
final class Waves {
    static final int FINAL_ROUND = 40;

    record Spawn(int tick, PuffType type) {
    }

    private static final String[] ROUNDS = {
            "rose 20 35",
            "rose 30 28",
            "rose 20 30, sky 6 40 +300",
            "rose 30 22, sky 15 35 +200",
            "sky 25 30, rose 10 30 +400",
            "rose 15 25, sky 15 30, moss 4 50 +300",
            "rose 20 20, sky 25 25 +100, moss 6 40 +500",
            "sky 20 22, moss 14 30 +200",
            "moss 30 22",
            "sky 100 10",
            "rose 10 20, sky 10 20, moss 12 25 +100, butter 3 60 +500",
            "sky 15 20, moss 10 25, butter 5 45 +300",
            "sky 50 14, moss 23 20 +200",
            "rose 50 10, sky 15 20, moss 10 20 +200, butter 9 30 +400",
            "rose 20 15, moss 15 20, butter 12 25 +200, blush 4 40 +500",
            "moss 40 14, butter 8 25 +400",
            "butter 12 22",
            "moss 80 10",
            "moss 10 20, butter 6 25 +100, blush 10 30 +300, soot 4 40 +600",
            "soot 6 40",
            "butter 40 14, blush 14 22 +200",
            "frost 16 35",
            "soot 7 35, frost 7 35 +150",
            "moss 20 15, pewter 1 1 +400, blush 10 20",
            "butter 25 15, pewter 4 60 +200, soot 10 25 +400",
            "marble 23 30, blush 10 25",
            "rose 100 6, sky 60 8, moss 45 10, butter 45 12",
            "pewter 6 45",
            "butter 40 10, blush 30 12 +200, marble 6 40 +500",
            "pewter 9 30",
            "marble 10 30, frost 12 25, soot 12 25 +200",
            "soot 25 15, frost 25 15, marble 12 30 +300",
            "butter 50 10, prism 5 50 +300",
            "butter 140 5, marble 6 40 +300",
            "soot 35 12, prism 25 25 +300",
            "blush 140 5, prism 10 30 +200",
            "prism 20 20, marble 15 25, pewter 10 30 +300, clay 2 60 +600",
            "pewter 30 25, clay 8 50 +200",
            "prism 40 15, clay 10 40 +300",
            "nimbus 1 1, prism 12 30 +200",
    };

    private Waves() {
    }

    static String spec(int round) {
        if (round <= ROUNDS.length) return ROUNDS[round - 1];
        int k = round - FINAL_ROUND;
        StringBuilder sb = new StringBuilder();
        sb.append("clay ").append(6 + k * 2).append(" 25");
        sb.append(", prism ").append(10 + k * 3).append(" 12 +100");
        sb.append(", nimbus ").append(1 + k / 3).append(" 240 +300");
        if (round % 10 == 0) sb.append(", stormhead ").append(Math.max(1, round / 10 - 4)).append(" 600 +600");
        return sb.toString();
    }

    static List<Spawn> round(int round) {
        List<Spawn> out = new ArrayList<>();
        for (String group : spec(round).split(",")) {
            String[] p = group.trim().split("\\s+");
            PuffType type = PuffType.valueOf(p[0].toUpperCase());
            int count = Integer.parseInt(p[1]);
            int spacing = Integer.parseInt(p[2]);
            int delay = p.length > 3 ? Integer.parseInt(p[3].replace("+", "")) : 0;
            for (int i = 0; i < count; i++) out.add(new Spawn(delay + i * spacing, type));
        }
        out.sort(Comparator.comparingInt(Spawn::tick));
        return out;
    }
}
