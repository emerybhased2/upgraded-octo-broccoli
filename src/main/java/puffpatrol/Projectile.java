package puffpatrol;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class Projectile {
    enum Kind { QUILL, STINGER, ACORN, BOLT, SLIME, BURR, PEBBLE, FEATHER, LASER, BARB }

    final Kind kind;
    final Tower owner;
    double x, y, vx, vy;
    final double angle;
    int life;
    int pierce;
    int damage;
    DamageType dmgType;
    final double hitR;
    /** Burrs travel for this many ticks, then sit still on the path. */
    int travel = -1;
    boolean dead;
    private final Set<Puff> hit = Collections.newSetFromMap(new IdentityHashMap<>());

    Projectile(Kind kind, Tower t, double angle, double sx, double sy, double speed, double range) {
        this.kind = kind;
        this.owner = t;
        this.angle = angle;
        this.x = sx;
        this.y = sy;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.life = (int) ((Math.min(range, 900) * 1.25 + 30) / Math.max(0.5, speed));
        this.pierce = Math.max(1, t.effPierce());
        this.damage = t.effDamage();
        this.dmgType = t.dmgType;
        this.hitR = switch (kind) {
            case QUILL, PEBBLE, FEATHER -> 5;
            case STINGER, BARB -> 4;
            case ACORN, SLIME -> 7;
            case BOLT, LASER -> 8;
            case BURR -> 9;
        };
    }

    /** A projectile fired from where the critter currently is, with its own range and speed. */
    static Projectile shot(Kind kind, Tower t, double angle) {
        return new Projectile(kind, t, angle, t.px, t.py - 8, t.projSpeed, t.effRange());
    }

    /** A burr thrown from (sx,sy) to land at (tx,ty) and wait there. */
    static Projectile burr(Tower t, double sx, double sy, double tx, double ty) {
        double d = Math.hypot(tx - sx, ty - sy);
        Projectile p = new Projectile(Kind.BURR, t, Math.atan2(ty - sy, tx - sx), sx, sy, t.projSpeed, 0);
        p.travel = Math.max(1, (int) (d / t.projSpeed));
        p.life = p.travel + t.burrLife;
        return p;
    }

    void update(World w) {
        if (kind == Kind.BURR) {
            if (travel > 0 && --travel == 0) {
                vx = 0;
                vy = 0;
            }
        }
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
            if (kind == Kind.SLIME) {
                w.slime(p, owner);
            } else {
                w.damage(p, damage, dmgType, owner);
                w.onHit(p, owner);
                if (owner.hitSplash > 0) {
                    w.blast(p.x, p.y, owner.hitSplash, damage, Math.max(4, owner.maxHits), owner, DamageType.EXPLOSIVE, true);
                }
            }
            if (--pierce <= 0) {
                if (kind == Kind.BURR && owner.burrBombs) w.blast(x, y, 40, 2, 12, owner, DamageType.EXPLOSIVE, true);
                dead = true;
                return;
            }
        }
    }
}
