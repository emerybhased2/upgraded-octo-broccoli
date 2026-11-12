package puffpatrol;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** All the game state and rules; no drawing or input. */
final class World {
    static final int START_CASH = 650, START_LIVES = 150;
    private static final String[] POP_WORDS = {"pop!", "puff!", "fwip!", "plip!", "poof!"};

    final GameMap map;
    final List<Puff> puffs = new ArrayList<>();
    final List<Tower> towers = new ArrayList<>();
    final List<Projectile> projectiles = new ArrayList<>();
    final List<Particle> particles = new ArrayList<>();
    private final List<Puff> pending = new ArrayList<>();
    final Random rnd = new Random();

    int cash = START_CASH;
    int lives = START_LIVES;
    int round;
    long totalPops;
    boolean roundActive, autoStart, gameOver, victory, victoryAcknowledged;
    String banner;
    int bannerTicks;

    private List<Waves.Spawn> spawns = List.of();
    private int spawnIdx, roundTick;

    World(GameMap map) {
        this.map = map;
    }

    int displayRound() {
        return roundActive ? round : round + 1;
    }

    boolean startRound() {
        if (roundActive || gameOver) return false;
        round++;
        spawns = Waves.round(round);
        spawnIdx = 0;
        roundTick = 0;
        roundActive = true;
        return true;
    }

    boolean pausedForVictory() {
        return victory && !victoryAcknowledged;
    }

    void update() {
        if (gameOver) return;
        if (bannerTicks > 0) bannerTicks--;
        if (roundActive) {
            while (spawnIdx < spawns.size() && spawns.get(spawnIdx).tick() <= roundTick) {
                Puff p = new Puff(spawns.get(spawnIdx++).type(), 0);
                p.place(map);
                puffs.add(p);
            }
            roundTick++;
        }
        for (Puff p : puffs) {
            if (p.dead) continue;
            p.update(map);
            if (p.dist >= map.length) {
                p.dead = true;
                lives -= p.rbe();
            }
        }
        if (lives <= 0) {
            lives = 0;
            gameOver = true;
        }
        for (Tower t : towers) t.update(this);
        for (Projectile pr : projectiles) pr.update(this);
        puffs.addAll(pending);
        pending.clear();
        puffs.removeIf(p -> p.dead);
        projectiles.removeIf(p -> p.dead);
        particles.removeIf(p -> !p.update());
        if (roundActive && spawnIdx >= spawns.size() && puffs.isEmpty()) endRound();
    }

    private void endRound() {
        roundActive = false;
        int bonus = 100 + round;
        cash += bonus;
        banner = "Round " + round + " cleared!  +" + bonus;
        bannerTicks = 150;
        for (Tower t : towers) {
            if (t.income > 0) {
                cash += t.income;
                particles.add(Particle.coin(t.x, t.y - 26, t.income));
            }
        }
        if (round >= Waves.FINAL_ROUND && !victory) victory = true;
        if (autoStart && !pausedForVictory()) startRound();
    }

    // ---------------------------------------------------------------- towers

    boolean canAfford(int cost) {
        return cash >= cost;
    }

    Tower place(TowerType type, double x, double y) {
        if (!canAfford(type.cost) || !map.canPlace(x, y, towers)) return null;
        cash -= type.cost;
        Tower t = new Tower(type, x, y);
        towers.add(t);
        particles.add(Particle.ring(x, y, 30, new Color(0xFFF6D8)));
        return t;
    }

    boolean upgrade(Tower t, int path) {
        Upgrade u = t.nextUpgrade(path);
        if (u == null || t.pathLocked(path) || !canAfford(u.cost())) return false;
        cash -= u.cost();
        t.applyUpgrade(path);
        particles.add(Particle.ring(t.x, t.y, 34, path == 0 ? new Color(0xFF9A8A) : new Color(0x8AC4FF)));
        return true;
    }

    void sell(Tower t) {
        if (towers.remove(t)) {
            cash += t.sellValue();
            particles.add(Particle.coin(t.x, t.y - 10, t.sellValue()));
        }
    }

    Puff findTarget(Tower t) {
        Puff best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Puff p : puffs) {
            if (p.dead || p.x < -4 || p.y < -4 || p.x > GameMap.W + 4 || p.y > GameMap.H + 4) continue;
            double dx = p.x - t.x, dy = p.y - t.y, rr = t.range + p.radius();
            double d2 = dx * dx + dy * dy;
            if (d2 > rr * rr) continue;
            double s = switch (t.mode) {
                case FIRST -> p.dist;
                case LAST -> -p.dist;
                case STRONG -> p.rbe() * 10000.0 + p.dist;
                case CLOSE -> -d2;
            };
            if (s > bestScore) {
                bestScore = s;
                best = p;
            }
        }
        return best;
    }

    /** Where to aim so a projectile of the given speed meets the puff. */
    double[] lead(Tower t, Puff p, double speed) {
        double d = Math.hypot(p.x - t.x, p.y - t.y);
        double time = d / Math.max(1, speed);
        Point2D.Double q = map.pointAt(p.dist + p.currentSpeed() * time);
        return new double[]{q.x, q.y};
    }

    // ---------------------------------------------------------------- damage

    void explode(double x, double y, Tower t) {
        blast(x, y, t.splash, t.damage, t.maxHits, t);
        if (t.cluster) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 + rnd.nextDouble() * 0.4;
                blast(x + Math.cos(a) * t.splash * 0.9, y + Math.sin(a) * t.splash * 0.9,
                        t.splash * 0.55, t.damage, t.maxHits / 2, t);
            }
        }
    }

    private void blast(double x, double y, double r, int dmg, int maxHits, Tower src) {
        if (particles.size() < 700) particles.add(Particle.boom(x, y, r));
        int hits = 0;
        for (Puff p : puffs) {
            if (p.dead) continue;
            double rr = r + p.radius() * 0.5;
            if (Math.hypot(p.x - x, p.y - y) > rr) continue;
            damage(p, dmg, DamageType.EXPLOSIVE, src);
            if (++hits >= maxHits) break;
        }
    }

    void damage(Puff p, int dmg, DamageType type, Tower src) {
        if (p.dead) return;
        if (p.brittle && p.frozen > 0) dmg += 1;
        while (dmg > 0 && !p.dead) {
            if (p.type.immuneTo(type)) {
                if (rnd.nextInt(6) == 0 && particles.size() < 300) {
                    particles.add(Particle.text(p.x, p.y - p.radius() - 4, "tink", new Color(0xD8DDE6)));
                }
                return;
            }
            if (dmg < p.hp) {
                p.hp -= dmg;
                return;
            }
            dmg -= p.hp;
            popLayer(p, src);
        }
    }

    private void popLayer(Puff p, Tower src) {
        cash += 1;
        totalPops++;
        if (src != null) src.pops++;
        if (particles.size() < 700) {
            particles.add(Particle.pop(p.x, p.y, p.type.color.brighter(), p.radius(), rnd.nextDouble()));
        }
        if (rnd.nextInt(9) == 0 && particles.size() < 250) {
            particles.add(Particle.text(p.x, p.y - p.radius(), POP_WORDS[rnd.nextInt(POP_WORDS.length)],
                    new Color(0xFFF8E7)));
        }
        PuffType[] ch = p.type.children();
        if (ch.length == 0) {
            p.dead = true;
            return;
        }
        double gap = p.type.isBoss() ? 16 : 7;
        for (int i = 1; i < ch.length; i++) {
            Puff c = new Puff(ch[i], Math.max(0, p.dist - gap * i));
            c.place(map);
            pending.add(c);
        }
        p.type = ch[0];
        p.hp = ch[0].hp;
        p.frozen = 0;
        p.brittle = false;
    }
}
