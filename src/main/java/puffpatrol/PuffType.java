package puffpatrol;

import java.awt.Color;

/**
 * The drifting puffs. Popping one layer reveals the puffs it was hiding inside,
 * like a set of nesting dolls made of weather.
 */
enum PuffType {
    ROSE("Rose puff", 1.0, 1, 12, 0xE5484D, ""),
    SKY("Sky puff", 1.4, 1, 12.5, 0x3E8EDE, "ROSE"),
    MOSS("Moss puff", 1.8, 1, 13, 0x4FB35A, "SKY"),
    BUTTER("Butter puff", 3.2, 1, 13.5, 0xF5C842, "MOSS"),
    BLUSH("Blush puff", 3.5, 1, 13.5, 0xF28DC0, "BUTTER"),
    SOOT("Soot puff", 1.8, 1, 10, 0x2E2B36, "BLUSH BLUSH"),
    FROST("Frost puff", 2.0, 1, 10, 0xEEF6FF, "BLUSH BLUSH"),
    PEWTER("Pewter puff", 1.0, 1, 13, 0x8A94A3, "SOOT SOOT"),
    MARBLE("Marble puff", 1.8, 1, 13.5, 0xF2F2F2, "SOOT FROST"),
    PRISM("Prism puff", 2.2, 1, 14, 0xB070F0, "MARBLE MARBLE"),
    CLAY("Clay puff", 2.5, 10, 15, 0xC8693A, "PRISM PRISM"),
    NIMBUS("Nimbus", 1.0, 200, 38, 0x8FA4C4, "CLAY CLAY CLAY CLAY"),
    STORMHEAD("Stormhead", 0.3, 700, 52, 0x4B3A6E, "NIMBUS NIMBUS NIMBUS NIMBUS");

    /** Pixels per tick for a speed-1.0 puff. */
    static final double BASE_SPEED = 0.9;

    final String label;
    final double speed;
    final int hp;
    final double radius;
    final Color color;
    private final String childSpec;
    private PuffType[] children;
    private int rbe = -1;

    PuffType(String label, double speed, int hp, double radius, int rgb, String childSpec) {
        this.label = label;
        this.speed = speed;
        this.hp = hp;
        this.radius = radius;
        this.color = new Color(rgb);
        this.childSpec = childSpec;
    }

    PuffType[] children() {
        if (children == null) {
            String s = childSpec.trim();
            if (s.isEmpty()) {
                children = new PuffType[0];
            } else {
                String[] parts = s.split("\\s+");
                children = new PuffType[parts.length];
                for (int i = 0; i < parts.length; i++) children[i] = valueOf(parts[i]);
            }
        }
        return children;
    }

    /** Total layers inside: how many lives it costs if it slips through. */
    int rbe() {
        if (rbe < 0) {
            int s = hp;
            for (PuffType c : children()) s += c.rbe();
            rbe = s;
        }
        return rbe;
    }

    boolean immuneTo(DamageType t) {
        return switch (this) {
            case SOOT -> t == DamageType.EXPLOSIVE;
            case FROST -> t == DamageType.COLD;
            case PEWTER -> t == DamageType.SHARP;
            case MARBLE -> t == DamageType.EXPLOSIVE || t == DamageType.COLD;
            default -> false;
        };
    }

    boolean freezeImmune() {
        return this == FROST || this == MARBLE;
    }

    boolean isBoss() {
        return this == NIMBUS || this == STORMHEAD;
    }

    /** Puffs with several hit points per layer show cracks as they wear down. */
    boolean showsDamage() {
        return hp > 1;
    }
}
