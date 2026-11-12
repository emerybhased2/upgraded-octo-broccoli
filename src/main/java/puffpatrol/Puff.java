package puffpatrol;

import java.awt.geom.Point2D;

final class Puff {
    private static long nextId = 1;

    final long id = nextId++;
    PuffType type;
    int hp;
    double dist;
    double x, y;
    boolean dead;
    int frozen;
    boolean brittle;
    int slowTicks;
    double slowFactor = 1;
    int age;

    Puff(PuffType type, double dist) {
        this.type = type;
        this.hp = type.hp;
        this.dist = dist;
    }

    double radius() {
        return type.radius;
    }

    double currentSpeed() {
        if (frozen > 0) return 0;
        double s = type.speed * PuffType.BASE_SPEED;
        if (slowTicks > 0) s *= slowFactor;
        return s;
    }

    void update(GameMap map) {
        age++;
        double s = currentSpeed();
        if (frozen > 0) frozen--;
        else if (slowTicks > 0) slowTicks--;
        dist += s;
        place(map);
    }

    void place(GameMap map) {
        Point2D.Double p = map.pointAt(dist);
        x = p.x;
        y = p.y;
    }

    /** Layers left inside, counting the hit points of the current layer. */
    int rbe() {
        int s = hp;
        for (PuffType c : type.children()) s += c.rbe();
        return s;
    }

    /** 0 = pristine, 3 = nearly popped. */
    int damageStage() {
        if (!type.showsDamage()) return 0;
        double f = hp / (double) type.hp;
        if (f > 0.75) return 0;
        if (f > 0.5) return 1;
        if (f > 0.25) return 2;
        return 3;
    }
}
