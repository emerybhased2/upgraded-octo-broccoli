package puffpatrol;

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
    final double x, y;
    final int[] tiers = new int[2];
    int spent;
    Target mode = Target.FIRST;

    // stats (set by TowerType.init and upgrades)
    double range, cooldown, projSpeed, spread, splash;
    int damage, pierce, shots = 1, maxHits, freezeTicks, pulseDamage, income;
    DamageType dmgType = DamageType.NORMAL;
    boolean cluster, brittle, permafrost, fire;

    int timer;
    int anim;
    int facing = 1;
    int pops;

    Tower(TowerType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.spent = type.cost;
        type.init.accept(this);
    }

    /** Next upgrade on a path, or null if maxed. */
    Upgrade nextUpgrade(int path) {
        Upgrade[] ups = type.paths[path];
        return tiers[path] < ups.length ? ups[tiers[path]] : null;
    }

    /** One path may reach tier 3; the other then stops at tier 2. */
    boolean pathLocked(int path) {
        int next = tiers[path] + 1;
        int other = tiers[1 - path];
        return next >= 3 && other >= 3 || other >= 3 && next > 2;
    }

    void applyUpgrade(int path) {
        Upgrade u = nextUpgrade(path);
        u.effect().accept(this);
        tiers[path]++;
        spent += u.cost();
        anim = 14;
    }

    int sellValue() {
        return (int) Math.round(spent * 0.7);
    }

    void update(World w) {
        if (anim > 0) anim--;
        if (timer > 0) {
            timer--;
            return;
        }
        boolean fired = switch (type.attack) {
            case DART -> shoot(w, Projectile.Kind.QUILL);
            case BOMB -> shoot(w, Projectile.Kind.ACORN);
            case BOLT -> shoot(w, Projectile.Kind.BOLT);
            case RADIAL -> buzz(w);
            case HITSCAN -> swoop(w);
            case PULSE -> pulse(w);
            case FARM -> false;
        };
        if (fired) {
            timer = (int) Math.max(3, Math.round(cooldown));
            anim = 8;
        }
    }

    private boolean shoot(World w, Projectile.Kind kind) {
        Puff t = w.findTarget(this);
        if (t == null) return false;
        double[] aim = w.lead(this, t, projSpeed);
        double ang = Math.atan2(aim[1] - (y - 8), aim[0] - x);
        facing = Math.cos(ang) < 0 ? -1 : 1;
        for (int i = 0; i < shots; i++) {
            double a = ang + (i - (shots - 1) / 2.0) * spread;
            w.projectiles.add(new Projectile(kind, this, a));
        }
        return true;
    }

    private boolean buzz(World w) {
        if (w.findTarget(this) == null) return false;
        double offset = w.rnd.nextDouble() * Math.PI;
        for (int i = 0; i < shots; i++) {
            w.projectiles.add(new Projectile(Projectile.Kind.STINGER, this, offset + i * Math.PI * 2 / shots));
        }
        return true;
    }

    private boolean swoop(World w) {
        Puff t = w.findTarget(this);
        if (t == null) return false;
        facing = t.x < x ? -1 : 1;
        w.particles.add(Particle.streak(x, y - 14, t.x, t.y));
        w.particles.add(Particle.ring(t.x, t.y, t.radius() + 6, new java.awt.Color(0xFFF3B0)));
        w.damage(t, damage, dmgType, this);
        return true;
    }

    private boolean pulse(World w) {
        boolean any = false;
        for (Puff p : w.puffs) {
            if (p.dead) continue;
            double dx = p.x - x, dy = p.y - y, rr = range + p.radius();
            if (dx * dx + dy * dy > rr * rr) continue;
            any = true;
            if (pulseDamage > 0) w.damage(p, pulseDamage, DamageType.COLD, this);
            if (p.dead || p.type.freezeImmune()) continue;
            if (p.type.isBoss()) {
                p.slowTicks = Math.max(p.slowTicks, freezeTicks);
                p.slowFactor = 0.5;
            } else {
                p.frozen = Math.max(p.frozen, freezeTicks);
                p.brittle |= brittle;
                if (permafrost) {
                    p.slowTicks = Math.max(p.slowTicks, 150);
                    p.slowFactor = 0.5;
                }
            }
        }
        if (any) w.particles.add(Particle.ring(x, y, range, new java.awt.Color(0xBFE6FF)));
        return any;
    }
}
