package puffpatrol;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class Projectile {
    enum Kind { QUILL, STINGER, ACORN, BOLT }

    final Kind kind;
    final Tower owner;
    double x, y, vx, vy;
    final double angle;
    int life;
    int pierce;
    final int damage;
    final DamageType dmgType;
    final double hitR;
    boolean dead;
    private final Set<Puff> hit = Collections.newSetFromMap(new IdentityHashMap<>());

    Projectile(Kind kind, Tower t, double angle) {
        this.kind = kind;
        this.owner = t;
        this.angle = angle;
        this.x = t.x;
        this.y = t.y - 8;
        this.vx = Math.cos(angle) * t.projSpeed;
        this.vy = Math.sin(angle) * t.projSpeed;
        this.life = (int) ((Math.min(t.range, 900) * 1.25 + 30) / t.projSpeed);
        this.pierce = Math.max(1, t.pierce);
        this.damage = t.damage;
        this.dmgType = t.dmgType;
        this.hitR = switch (kind) {
            case QUILL -> 5;
            case STINGER -> 4;
            case ACORN -> 7;
            case BOLT -> 8;
        };
    }

    void update(World w) {
        x += vx;
        y += vy;
        if (--life <= 0 || x < -30 || y < -30 || x > GameMap.W + 30 || y > GameMap.H + 30) {
            dead = true;
            return;
        }
        for (Puff p : w.puffs) {
            if (p.dead || hit.contains(p)) continue;
            double dx = p.x - x, dy = p.y - y, rr = hitR + p.radius();
            if (dx * dx + dy * dy > rr * rr) continue;
            if (kind == Kind.ACORN) {
                w.explode(x, y, owner);
                dead = true;
                return;
            }
            hit.add(p);
            w.damage(p, damage, dmgType, owner);
            if (--pierce <= 0) {
                dead = true;
                return;
            }
        }
    }
}
