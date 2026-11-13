package puffpatrol;

import java.awt.Color;

/** How hard the weather blows. Each one earns its own medal on every map. */
enum Difficulty {
    BREEZY("Breezy", "200 lives, critters 15% cheaper, slower puffs. 40 rounds.", 200, 0.85, 0.9, 1, 40, new Color(0xC08A4A)),
    BLUSTERY("Blustery", "150 lives, normal prices. 50 rounds.", 150, 1.0, 1.0, 1, 50, new Color(0xB8C2CC)),
    STORMY("Stormy", "100 lives, pricier critters, faster puffs. Starts at round 3, 60 rounds.", 100, 1.08, 1.1, 3, 60, new Color(0xF2C14E)),
    TEMPEST("Tempest", "A single life and 20% dearer critters. Starts at round 6, 70 rounds.", 1, 1.2, 1.1, 6, 70, new Color(0xD8435A));

    final String label;
    final String desc;
    final int lives;
    final double costMul;
    final double speedMul;
    final int startRound;
    final int finalRound;
    final Color medal;

    Difficulty(String label, String desc, int lives, double costMul, double speedMul, int startRound, int finalRound, Color medal) {
        this.label = label;
        this.desc = desc;
        this.lives = lives;
        this.costMul = costMul;
        this.speedMul = speedMul;
        this.startRound = startRound;
        this.finalRound = finalRound;
        this.medal = medal;
    }

    int cost(int base) {
        return (int) (Math.round(base * costMul / 5.0) * 5);
    }
}
