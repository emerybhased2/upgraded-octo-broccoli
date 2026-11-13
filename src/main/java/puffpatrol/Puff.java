package puffpatrol;

import java.awt.geom.Point2D;

final class Puff {
    private static long nextId = 1;

    final long id = nextId++;
    PuffType type;
    int hp;
    final int track;
    double dist;
    double x, y;
    boolean dead;
    int age;

    // traits
    boolean hidden, regrow, armoured;
    PuffType regrowMax;
    int regrowTimer;

    // status effects
    int frozen;
    boolean brittle;
    int stun;
    int slowTicks;
    double slowFactor = 1;
    int slimeTicks;
    double slimeFactor = 1;
    int slimeDot, slimeDotEvery = 60, slimeDotTimer;
    Tower slimeSource;

    Puff(PuffType type, int track, double dist, boolean hidden, boolean regrow, boolean armoured) {
        this.type = type;
        this.track = track;
        this.dist = dist;
        this.hidden = hidden;
        this.regrow = regrow;
        this.armoured = armoured && canArmour(type);
        this.regrowMax = type;
        this.hp = layerHp(type);
    }

    private static boolean canArmour(PuffType t) {
        return t.hp > 1 || t == PuffType.PEWTER;
    }

    int layerHp(PuffType t) {
        if (!armoured) return t.hp;
        return t == PuffType.PEWTER ? 4 : t.hp * 2;
    }

    /** Becomes another layer (after popping or regrowing). Armour stays only on tough layers. */
    void setLayer(PuffType t) {
        type = t;
        armoured = armoured && canArmour(t);
        hp = layerHp(t);
    }

    double radius() {
        return type.radius;
    }

    double currentSpeed(double mul) {
        if (frozen > 0 || stun > 0) return 0;
        double s = type.speed * PuffType.BASE_SPEED * mul;
        if (slowTicks > 0) s *= slowFactor;
        if (slimeTicks > 0) s *= slimeFactor;
        return s;
    }

    void update(GameMap map, double mul) {
        age++;
        double s = currentSpeed(mul);
        if (frozen > 0) frozen--;
        if (stun > 0) stun--;
        if (slowTicks > 0) slowTicks--;
        if (slimeTicks > 0) slimeTicks--;
        dist += s;
        place(map);
    }

    void place(GameMap map) {
        Point2D.Double p = map.pointAt(track, dist);
        x = p.x;
        y = p.y;
    }

    /** Layers left inside, counting the hit points of the current layer. */
    int rbe() {
        int s = hp;
        for (PuffType c : type.children()) s += c.rbe() * (armoured && canArmour(c) ? 2 : 1);
        return s;
    }

    /** 0 = pristine, 3 = nearly popped. */
    int damageStage() {
        if (!type.showsDamage()) return 0;
        double f = hp / (double) layerHp(type);
        if (f > 0.75) return 0;
        if (f > 0.5) return 1;
        if (f > 0.25) return 2;
        return 3;
    }
}
