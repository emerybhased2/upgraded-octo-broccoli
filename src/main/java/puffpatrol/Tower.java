package puffpatrol;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

final class Tower {
    static final double RADIUS = 18;

    enum Target {
        FIRST("First"), LAST("Last"), STRONG("Strong"), CLOSE("Close");
        final String label;

        Target(String label) {
            this.label = label;
        }

        Target next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    final TowerType type;
    /** Where the critter (or its nest) sits. */
    final double x, y;
    /** Where it currently is: differs from x/y only for flyers. */
    double px, py;
    double orbitAngle;
    final int[] tiers = new int[3];
    int spent;
    Target mode = Target.FIRST;

    // ---- stats, set by TowerType.init and upgrades
    double range, cooldown, projSpeed, spread, splash, hitSplash, inaccuracy, orbitR, orbitSpeed;
    double slimeFactor = 1, slimeSplashR;
    int damage, pierce, shots = 1, maxHits, freezeTicks, pulseDamage, income;
    int slimeTicks, slimeDot, slimeDotEvery = 60, slimeSplash, burrLife, stun, coinBonus;
    /** Extra damage dealt to big clouds (Nimbus, Stormhead). */
    int bossBonus;
    /** Hits slow the puff (and stun it, if set). */
    int slowOnHitTicks, stunOnHit;
    double slowOnHitFactor = 1;
    boolean slowBosses;
    int eggDamage = 3;
    DamageType dmgType = DamageType.NORMAL;
    boolean cluster, brittle, permafrost, fire, detects, slimeBosses, burrBombs, laser, eggs, whirlpool, bossStun, heavySlow;
    boolean sparks, reveal;

    // ---- what a lantern (or another aura critter) gives to critters within auraRange
    double auraRange;
    double giveRange = 1, giveSpeed = 1;
    int givePierce, giveDamage;
    boolean giveDetect;

    // ---- buffs this critter is currently receiving (recomputed every tick)
    double rRange = 1, rSpeed = 1;
    int rPierce, rDamage;
    boolean rDetect;

    Ability ability;
    int abilityCooldown;
    int surgeTicks;
    double surgeFactor = 1;

    int timer, anim, facing = 1, pops;
    private int eggTimer, whirlTimer;
    private List<double[]> burrSpots;
    private double burrSpotsRange = -1;

    Tower(TowerType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.px = x;
        this.py = y;
        this.spent = type.cost;
        type.init.accept(this);
    }

    // ---------------------------------------------------------------- effective stats

    double effRange() {
        return range * rRange;
    }

    double effCooldown(World w) {
        double c = cooldown * rSpeed;
        if (surgeTicks > 0) c *= surgeFactor;
        if (w.globalSurge > 0) c *= 0.5;
        return c;
    }

    int effDamage() {
        return damage + rDamage;
    }

    int effPierce() {
        return pierce + rPierce;
    }

    boolean seesHidden() {
        return detects || rDetect;
    }

    void resetBuffs() {
        rRange = 1;
        rSpeed = 1;
        rPierce = 0;
        rDamage = 0;
        rDetect = false;
    }

    // ---------------------------------------------------------------- upgrades

    /** Next upgrade on a path, or null if the path is complete. */
    Upgrade nextUpgrade(int path) {
        Upgrade[] ups = type.paths[path];
        return tiers[path] < ups.length ? ups[tiers[path]] : null;
    }

    /**
     * Crosspathing: at most two paths may be upgraded at all, and only one of them past tier 2
     * (so the best you can do is 5-2-0).
     */
    boolean pathLocked(int path) {
        int next = tiers[path] + 1;
        int othersUsed = 0;
        for (int p = 0; p < tiers.length; p++) {
            if (p == path) continue;
            if (tiers[p] > 0) othersUsed++;
            if (next > 2 && tiers[p] > 2) return true;
        }
        return tiers[path] == 0 && othersUsed >= 2;
    }

    /** Crosspath code such as "3-0-2". */
    String crosspath() {
        return tiers[0] + "-" + tiers[1] + "-" + tiers[2];
    }

    void applyUpgrade(int path, int paid) {
        Upgrade u = nextUpgrade(path);
        u.effect().accept(this);
        if (u.ability() != null) {
            ability = u.ability();
            abilityCooldown = 0;
        }
        tiers[path]++;
        spent += paid;
        anim = 14;
    }

    int sellValue() {
        return (int) Math.round(spent * 0.7);
    }

    // ---------------------------------------------------------------- behaviour

    void update(World w) {
        if (anim > 0) anim--;
        if (abilityCooldown > 0) abilityCooldown--;
        if (surgeTicks > 0) surgeTicks--;
        if (type.attack == TowerType.Attack.FLYER) fly();
        if (eggs) eggs(w);
        if (whirlpool) whirl(w);
        if (reveal) reveal(w);
        if (timer > 0) {
            timer--;
            return;
        }
        boolean fired = switch (type.attack) {
            case DART -> shoot(w, Projectile.Kind.QUILL);
            case BOMB -> shoot(w, Projectile.Kind.ACORN);
            case BOLT -> shoot(w, Projectile.Kind.BOLT);
            case SLIME -> shoot(w, Projectile.Kind.SLIME);
            case PEBBLE -> shoot(w, Projectile.Kind.PEBBLE);
            case RAPID -> shoot(w, laser ? Projectile.Kind.LASER : Projectile.Kind.BARB);
            case RADIAL -> buzz(w);
            case HITSCAN -> swoop(w);
            case PULSE -> pulse(w);
            case BURR -> dropBurr(w);
            case FLYER -> volley(w);
            case BURROW -> burrow(w);
            case SUPPORT -> sparks && spark(w);
            case FARM -> sparks && spark(w);
        };
        if (fired) {
            timer = (int) Math.max(2, Math.round(effCooldown(w)));
            if (type.attack != TowerType.Attack.RAPID || anim == 0) anim = 8;
        }
    }

    private boolean shoot(World w, Projectile.Kind kind) {
        Puff t = w.findTarget(this);
        if (t == null) return false;
        double[] aim = w.lead(this, t, projSpeed);
        double ang = Math.atan2(aim[1] - (py - 8), aim[0] - px);
        facing = Math.cos(ang) < 0 ? -1 : 1;
        for (int i = 0; i < shots; i++) {
            double a = ang + (i - (shots - 1) / 2.0) * spread + (w.rnd.nextDouble() - 0.5) * 2 * inaccuracy;
            w.projectiles.add(Projectile.shot(kind, this, a));
        }
        w.sfx(Sound.Sfx.THROW);
        return true;
    }

    private boolean buzz(World w) {
        if (w.findTarget(this) == null) return false;
        double offset = w.rnd.nextDouble() * Math.PI;
        for (int i = 0; i < shots; i++) {
            w.projectiles.add(Projectile.shot(Projectile.Kind.STINGER, this, offset + i * Math.PI * 2 / shots));
        }
        w.sfx(Sound.Sfx.THROW);
        return true;
    }

    private boolean swoop(World w) {
        Puff t = w.findTarget(this);
        if (t == null) return false;
        facing = t.x < x ? -1 : 1;
        w.particles.add(Particle.streak(x, y - 14, t.x, t.y, new Color(255, 246, 200)));
        w.particles.add(Particle.ring(t.x, t.y, t.radius() + 6, new Color(0xFFF3B0)));
        w.damage(t, effDamage(), dmgType, this);
        if (hitSplash > 0) w.blast(t.x, t.y, hitSplash, effDamage(), Math.max(4, maxHits), this, DamageType.EXPLOSIVE, true);
        w.sfx(Sound.Sfx.ZAP);
        return true;
    }

    private boolean pulse(World w) {
        boolean any = false;
        double r0 = effRange();
        for (Puff p : w.puffs) {
            if (p.dead) continue;
            double dx = p.x - x, dy = p.y - y, rr = r0 + p.radius();
            if (dx * dx + dy * dy > rr * rr) continue;
            any = true;
            if (pulseDamage > 0) w.damage(p, pulseDamage + rDamage, DamageType.COLD, this);
            if (p.dead || p.type.freezeImmune()) continue;
            if (p.type.isBoss()) {
                p.slowTicks = Math.max(p.slowTicks, freezeTicks);
                p.slowFactor = heavySlow ? 0.25 : 0.5;
            } else {
                p.frozen = Math.max(p.frozen, freezeTicks);
                p.brittle |= brittle;
                if (permafrost) {
                    p.slowTicks = Math.max(p.slowTicks, 150);
                    p.slowFactor = 0.5;
                }
            }
        }
        if (any) {
            w.particles.add(Particle.ring(x, y, r0, new Color(0xBFE6FF)));
            w.sfx(Sound.Sfx.FREEZE);
        }
        return any;
    }

    private boolean dropBurr(World w) {
        if (!w.roundActive && w.puffs.isEmpty()) return false;
        if (burrSpots == null || burrSpotsRange != effRange()) {
            burrSpotsRange = effRange();
            burrSpots = new ArrayList<>();
            for (int t = 0; t < w.map.tracks.size(); t++) {
                for (double d = 0; d < w.map.length(t); d += 8) {
                    Point2D.Double p = w.map.pointAt(t, d);
                    if (p.x > 0 && p.y > 0 && p.x < GameMap.W && p.y < GameMap.H && Math.hypot(p.x - x, p.y - y) <= burrSpotsRange) {
                        burrSpots.add(new double[]{p.x, p.y});
                    }
                }
            }
        }
        if (burrSpots.isEmpty()) return false;
        for (int i = 0; i < Math.max(1, shots); i++) {
            double[] s = burrSpots.get(w.rnd.nextInt(burrSpots.size()));
            double jx = s[0] + (w.rnd.nextDouble() - 0.5) * w.map.pathWidth * 0.6;
            double jy = s[1] + (w.rnd.nextDouble() - 0.5) * w.map.pathWidth * 0.6;
            w.projectiles.add(Projectile.burr(this, x, y - 8, jx, jy));
        }
        return true;
    }

    private void fly() {
        orbitAngle += orbitSpeed * (surgeTicks > 0 ? 1.5 : 1);
        double nx = x + Math.cos(orbitAngle) * orbitR, ny = y + Math.sin(orbitAngle) * orbitR * 0.75;
        nx = Math.max(24, Math.min(GameMap.W - 24, nx));
        ny = Math.max(24, Math.min(GameMap.H - 24, ny));
        facing = nx < px ? -1 : 1;
        px = nx;
        py = ny;
    }

    private boolean volley(World w) {
        if (w.puffs.isEmpty()) return false;
        double offset = w.rnd.nextDouble() * Math.PI;
        for (int i = 0; i < shots; i++) {
            w.projectiles.add(Projectile.shot(Projectile.Kind.FEATHER, this, offset + i * Math.PI * 2 / shots));
        }
        return true;
    }

    private void eggs(World w) {
        if (++eggTimer < 60) return;
        for (Puff p : w.puffs) {
            if (!p.dead && Math.hypot(p.x - px, p.y - py) < 70) {
                eggTimer = 0;
                w.blast(p.x, p.y, 55, eggDamage + rDamage, 20, this, DamageType.EXPLOSIVE, true);
                return;
            }
        }
    }

    private void whirl(World w) {
        if (++whirlTimer < 240) return;
        whirlTimer = 0;
        boolean any = false;
        for (Puff p : w.puffs) {
            if (!p.dead && !p.type.isBoss() && Math.hypot(p.x - x, p.y - y) <= effRange()) {
                w.pushBack(p, 60);
                any = true;
            }
        }
        if (any) w.particles.add(Particle.ring(x, y, effRange(), new Color(0x7CC4D8)));
    }

    private void reveal(World w) {
        for (Puff p : w.puffs) if (p.hidden && Math.hypot(p.x - x, p.y - y) <= range) p.hidden = false;
    }

    private boolean spark(World w) {
        Puff best = null;
        double bd = Double.MAX_VALUE;
        for (Puff p : w.puffs) {
            if (p.dead || (p.hidden && !giveDetect)) continue;
            double d = Math.hypot(p.x - x, p.y - y);
            if (d <= range && d < bd) {
                bd = d;
                best = p;
            }
        }
        if (best == null) return false;
        w.particles.add(Particle.streak(x, y - 24, best.x, best.y,
                type == TowerType.MUSHROOM ? new Color(0xE8D9A6) : new Color(0xFFE066)));
        w.damage(best, damage + rDamage, dmgType, this);
        w.onHit(best, this);
        return true;
    }

    private boolean burrow(World w) {
        int hits = 0;
        double r0 = effRange();
        for (Puff p : w.puffs) {
            if (p.dead || Math.hypot(p.x - x, p.y - y) > r0 + p.radius() * 0.5) continue;
            boolean boss = p.type.isBoss();
            w.damage(p, effDamage(), dmgType, this);
            if (!p.dead && (!boss || bossStun)) p.stun = Math.max(p.stun, boss ? 15 : stun);
            if (++hits >= maxHits) break;
        }
        if (hits > 0) {
            w.particles.add(Particle.ring(x, y, r0, new Color(0xA8865A)));
            w.sfx(Sound.Sfx.THUMP);
        }
        return hits > 0;
    }
}
