package puffpatrol;

import java.util.function.Consumer;

/** The meadow creatures who have volunteered to keep the puffs moving along. */
enum TowerType {
    HEDGEHOG("Quill Hedgehog", 200, Attack.DART,
            "Flicks sharp quills at the nearest puff. Cheerful, reliable, slightly prickly.",
            t -> { t.range = 115; t.cooldown = 48; t.damage = 1; t.pierce = 2; t.dmgType = DamageType.SHARP; t.projSpeed = 10; },
            new Upgrade[]{
                    new Upgrade("Sharper Quills", 90, "Quills pass through one more puff.", t -> t.pierce += 1),
                    new Upgrade("Razor Quills", 220, "+2 pierce and quills hit twice as hard.", t -> { t.pierce += 2; t.damage += 1; }),
                    new Upgrade("Quill Storm", 950, "Throws three quills at once, +2 pierce.", t -> { t.shots = 3; t.spread = 0.22; t.pierce += 2; }),
            },
            new Upgrade[]{
                    new Upgrade("Keen Nose", 100, "Sniffs out puffs 25% further away.", t -> t.range *= 1.25),
                    new Upgrade("Quick Paws", 180, "Throws 30% faster.", t -> t.cooldown *= 0.7),
                    new Upgrade("Hedgehog Frenzy", 1400, "Throws twice as fast, +1 damage.", t -> { t.cooldown *= 0.5; t.damage += 1; }),
            }),
    BEEHIVE("Bumble Hive", 300, Attack.RADIAL,
            "Sends stingers buzzing out in every direction when puffs wander close.",
            t -> { t.range = 85; t.cooldown = 70; t.damage = 1; t.pierce = 1; t.shots = 8; t.dmgType = DamageType.SHARP; t.projSpeed = 7; },
            new Upgrade[]{
                    new Upgrade("More Bees", 150, "10 stingers per buzz.", t -> t.shots = 10),
                    new Upgrade("Swarm", 320, "16 stingers per buzz.", t -> t.shots = 16),
                    new Upgrade("Honey Inferno", 2200, "Stingers turn to fire: pops anything, 3 pierce, 2 damage.",
                            t -> { t.dmgType = DamageType.FIRE; t.pierce = 3; t.damage += 1; t.fire = true; }),
            },
            new Upgrade[]{
                    new Upgrade("Busy Bees", 140, "Buzzes 25% faster.", t -> t.cooldown *= 0.75),
                    new Upgrade("Far Flyers", 120, "Stingers fly 25% further.", t -> t.range *= 1.25),
                    new Upgrade("Queen's Fury", 1300, "Buzzes twice as fast, +1 damage.", t -> { t.cooldown *= 0.5; t.damage += 1; }),
            }),
    BADGER("Acorn Badger", 550, Attack.BOMB,
            "Lobs exploding acorns. Soot puffs are unimpressed by explosions.",
            t -> { t.range = 135; t.cooldown = 72; t.damage = 1; t.splash = 42; t.maxHits = 22; t.dmgType = DamageType.EXPLOSIVE; t.projSpeed = 7; },
            new Upgrade[]{
                    new Upgrade("Bigger Acorns", 300, "Larger blast that pops more puffs.", t -> { t.splash = 56; t.maxHits = 32; }),
                    new Upgrade("Mighty Acorns", 650, "Even larger blast, 2 damage.", t -> { t.splash = 66; t.damage = 2; }),
                    new Upgrade("Oak Bomb", 3200, "An enormous acorn. 6 damage, huge blast.", t -> { t.splash = 82; t.damage = 6; t.maxHits = 60; }),
            },
            new Upgrade[]{
                    new Upgrade("Long Throw", 220, "Throws 25% further.", t -> t.range *= 1.25),
                    new Upgrade("Rapid Lobbing", 380, "Throws 30% faster.", t -> t.cooldown *= 0.7),
                    new Upgrade("Cluster Nuts", 2100, "Each blast scatters six smaller blasts.", t -> t.cluster = true),
            }),
    OWL("Night Owl", 350, Attack.HITSCAN,
            "Sees every puff on the map and swoops at the strongest-looking one. Pops anything.",
            t -> { t.range = 9999; t.cooldown = 80; t.damage = 2; t.dmgType = DamageType.NORMAL; t.mode = Tower.Target.STRONG; },
            new Upgrade[]{
                    new Upgrade("Talon Strike", 350, "4 damage per swoop.", t -> t.damage = 4),
                    new Upgrade("Thunder Hoot", 1600, "8 damage per swoop.", t -> t.damage = 8),
                    new Upgrade("Moonfall", 4000, "20 damage per swoop. Clouds weep.", t -> t.damage = 20),
            },
            new Upgrade[]{
                    new Upgrade("Night Eyes", 300, "Swoops 30% faster.", t -> t.cooldown *= 0.7),
                    new Upgrade("Swift Wings", 650, "Swoops 40% faster again.", t -> t.cooldown *= 0.6),
                    new Upgrade("Parliament of Owls", 3600, "Swoops 2.5x faster. Many owls, one opinion.", t -> t.cooldown *= 0.4),
            }),
    MOTH("Frost Moth", 400, Attack.PULSE,
            "Flutters icy wingbeats that freeze nearby puffs in place. Big clouds are only slowed.",
            t -> { t.range = 72; t.cooldown = 95; t.freezeTicks = 60; },
            new Upgrade[]{
                    new Upgrade("Long Chill", 220, "Freezes last much longer.", t -> t.freezeTicks = 100),
                    new Upgrade("Brittle Ice", 420, "Frozen puffs take +1 damage from every hit.", t -> t.brittle = true),
                    new Upgrade("Absolute Zero", 2600, "Each wingbeat deals 2 cold damage and beats faster.",
                            t -> { t.pulseDamage = 2; t.cooldown *= 0.8; }),
            },
            new Upgrade[]{
                    new Upgrade("Wider Wings", 180, "Chills 30% further out.", t -> t.range *= 1.3),
                    new Upgrade("Snowfall", 320, "Beats wings 30% faster.", t -> t.cooldown *= 0.7),
                    new Upgrade("Permafrost", 950, "Thawed puffs stay half-speed for a while.", t -> t.permafrost = true),
            }),
    TOAD("Wizard Toad", 500, Attack.BOLT,
            "Croaks glittering bolts that pop anything and pass through several puffs.",
            t -> { t.range = 125; t.cooldown = 55; t.damage = 1; t.pierce = 4; t.dmgType = DamageType.MAGIC; t.projSpeed = 9; },
            new Upgrade[]{
                    new Upgrade("Fire Tongue", 450, "+3 pierce, +1 damage.", t -> { t.pierce += 3; t.damage += 1; }),
                    new Upgrade("Chain Croak", 1300, "Bolts skip through 8 more puffs.", t -> t.pierce += 8),
                    new Upgrade("Archtoad", 4200, "+3 damage and casts 40% faster.", t -> { t.damage += 3; t.cooldown *= 0.6; }),
            },
            new Upgrade[]{
                    new Upgrade("Mystic Reach", 250, "20% more range.", t -> t.range *= 1.2),
                    new Upgrade("Hasty Hex", 480, "Casts 30% faster.", t -> t.cooldown *= 0.7),
                    new Upgrade("Big Bolts", 1900, "+2 damage per bolt.", t -> t.damage += 2),
            }),
    MUSHROOM("Coin Mushrooms", 900, Attack.FARM,
            "Doesn't pop anything. Grows a little pile of coins at the end of every round.",
            t -> t.income = 70,
            new Upgrade[]{
                    new Upgrade("More Spores", 400, "+40 coins per round.", t -> t.income += 40),
                    new Upgrade("Glowcaps", 1200, "+90 coins per round.", t -> t.income += 90),
                    new Upgrade("Fairy Ring", 4200, "+260 coins per round.", t -> t.income += 260),
            },
            new Upgrade[]{
                    new Upgrade("Rich Soil", 350, "+25 coins per round.", t -> t.income += 25),
                    new Upgrade("Truffle Hunt", 900, "+60 coins per round.", t -> t.income += 60),
                    new Upgrade("Mushroom Bank", 3000, "+170 coins per round.", t -> t.income += 170),
            });

    enum Attack { DART, RADIAL, BOMB, HITSCAN, PULSE, BOLT, FARM }

    final String label;
    final int cost;
    final Attack attack;
    final String desc;
    final Consumer<Tower> init;
    final Upgrade[][] paths;

    TowerType(String label, int cost, Attack attack, String desc, Consumer<Tower> init, Upgrade[] a, Upgrade[] b) {
        this.label = label;
        this.cost = cost;
        this.attack = attack;
        this.desc = desc;
        this.init = init;
        this.paths = new Upgrade[][]{a, b};
    }
}
