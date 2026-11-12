package puffpatrol;

import java.util.function.Consumer;

/** The meadow creatures who have volunteered to keep the puffs moving along. */
enum TowerType {
    HEDGEHOG("Quill Hedgehog", 200, Attack.DART, Placement.LAND,
            "Flicks sharp quills at puffs. Cheerful, reliable, slightly prickly.",
            t -> { t.range = 115; t.cooldown = 48; t.damage = 1; t.pierce = 2; t.dmgType = DamageType.SHARP; t.projSpeed = 10; },
            path(
                    up("Sharper Quills", 90, "Quills pass through one more puff.", t -> t.pierce += 1),
                    up("Razor Quills", 220, "+2 pierce and quills hit twice as hard.", t -> { t.pierce += 2; t.damage += 1; }),
                    up("Quill Storm", 950, "Throws three quills at once, +2 pierce.", t -> { t.shots = 3; t.spread = 0.22; t.pierce += 2; }),
                    up("Spine Tempest", 6500, "Five tempered quills per throw that pop anything. +4 pierce, +2 damage.",
                            t -> { t.shots = 5; t.spread = 0.18; t.pierce += 4; t.damage += 2; t.dmgType = DamageType.NORMAL; })),
            path(
                    up("Keen Nose", 100, "Sniffs out puffs 25% further away.", t -> t.range *= 1.25),
                    up("Night Snuffle", 300, "Smells hidden puffs and throws 20% faster.", t -> { t.detects = true; t.cooldown *= 0.8; }),
                    up("Hedgehog Frenzy", 1400, "Throws twice as fast, +1 damage.", t -> { t.cooldown *= 0.5; t.damage += 1; }),
                    ability("Bristleback", 5000, "Ability: Quill Blizzard. Also throws 20% faster.", Ability.QUILL_BLIZZARD,
                            t -> t.cooldown *= 0.8))),
    BEEHIVE("Bumble Hive", 300, Attack.RADIAL, Placement.LAND,
            "Sends stingers buzzing out in every direction when puffs wander close.",
            t -> { t.range = 85; t.cooldown = 70; t.damage = 1; t.pierce = 1; t.shots = 8; t.dmgType = DamageType.SHARP; t.projSpeed = 7; },
            path(
                    up("More Bees", 150, "10 stingers per buzz.", t -> t.shots = 10),
                    up("Swarm", 320, "16 stingers per buzz.", t -> t.shots = 16),
                    up("Honey Inferno", 2200, "Stingers turn to fire: pop anything, 3 pierce, +1 damage.",
                            t -> { t.dmgType = DamageType.FIRE; t.pierce = 3; t.damage += 1; t.fire = true; }),
                    up("Royal Jelly Blaze", 7000, "24 blazing stingers, 5 pierce, +2 damage.",
                            t -> { t.shots = 24; t.pierce = 5; t.damage += 2; })),
            path(
                    up("Busy Bees", 140, "Buzzes 25% faster.", t -> t.cooldown *= 0.75),
                    up("Far Flyers", 120, "Stingers fly 25% further.", t -> t.range *= 1.25),
                    up("Queen's Fury", 1300, "Buzzes twice as fast, +1 damage.", t -> { t.cooldown *= 0.5; t.damage += 1; }),
                    ability("Hive Mind", 4500, "Ability: Swarm Surge. Also +1 pierce.", Ability.SWARM_SURGE, t -> t.pierce += 1))),
    BADGER("Acorn Badger", 550, Attack.BOMB, Placement.LAND,
            "Lobs exploding acorns. Soot and Marble puffs shrug off explosions.",
            t -> { t.range = 135; t.cooldown = 72; t.damage = 1; t.splash = 42; t.maxHits = 22; t.dmgType = DamageType.EXPLOSIVE; t.projSpeed = 7; },
            path(
                    up("Heavy Acorns", 300, "Larger blast that pops more puffs.", t -> { t.splash = 56; t.maxHits = 32; }),
                    up("Mighty Acorns", 650, "Even larger blast, 2 damage.", t -> { t.splash = 66; t.damage = 2; }),
                    up("Oak Bomb", 3200, "An enormous acorn. 6 damage, huge blast.", t -> { t.splash = 82; t.damage = 6; t.maxHits = 60; }),
                    up("Thunder Acorn", 9000, "15 damage per blast with a colossal radius.", t -> { t.splash = 100; t.damage = 15; t.maxHits = 90; })),
            path(
                    up("Long Throw", 220, "Throws 25% further.", t -> t.range *= 1.25),
                    up("Rapid Lobbing", 380, "Throws 30% faster.", t -> t.cooldown *= 0.7),
                    up("Cluster Nuts", 2100, "Each blast scatters six smaller blasts.", t -> t.cluster = true),
                    ability("Burrow Artillery", 6000, "Ability: Acorn Avalanche. Also +1 damage.", Ability.ACORN_AVALANCHE,
                            t -> t.damage += 1))),
    OWL("Night Owl", 350, Attack.HITSCAN, Placement.LAND,
            "Sees every puff on the map and swoops at the strongest-looking one. Pops anything.",
            t -> { t.range = 9999; t.cooldown = 80; t.damage = 2; t.dmgType = DamageType.NORMAL; t.mode = Tower.Target.STRONG; },
            path(
                    up("Talon Strike", 350, "4 damage per swoop.", t -> t.damage = 4),
                    up("Thunder Hoot", 1600, "8 damage per swoop.", t -> t.damage = 8),
                    up("Moonfall", 4000, "20 damage per swoop. Clouds weep.", t -> t.damage = 20),
                    up("Eclipse", 11000, "50 damage per swoop.", t -> t.damage = 50)),
            path(
                    up("Night Eyes", 300, "Spots hidden puffs and swoops 30% faster.", t -> { t.detects = true; t.cooldown *= 0.7; }),
                    up("Swift Wings", 650, "Swoops 40% faster again.", t -> t.cooldown *= 0.6),
                    up("Parliament of Owls", 3600, "Swoops 2.5x faster. Many owls, one opinion.", t -> t.cooldown *= 0.4),
                    ability("Midnight Muster", 8000, "Ability: Owl Squadron. Also +2 damage.", Ability.OWL_SQUADRON, t -> t.damage += 2))),
    MOTH("Frost Moth", 400, Attack.PULSE, Placement.LAND,
            "Flutters icy wingbeats that freeze nearby puffs in place. Big clouds are only slowed.",
            t -> { t.range = 72; t.cooldown = 95; t.freezeTicks = 60; },
            path(
                    up("Long Chill", 220, "Freezes last much longer.", t -> t.freezeTicks = 100),
                    up("Brittle Ice", 420, "Frozen puffs take +1 damage from every hit.", t -> t.brittle = true),
                    up("Hoarfrost Wings", 2600, "Each wingbeat deals 2 cold damage and beats faster.",
                            t -> { t.pulseDamage = 2; t.cooldown *= 0.8; }),
                    up("Glacier Heart", 7500, "6 cold damage per beat, longer freezes, big clouds slowed to a crawl.",
                            t -> { t.pulseDamage = 6; t.freezeTicks = 140; t.heavySlow = true; })),
            path(
                    up("Wider Wings", 180, "Chills 30% further out.", t -> t.range *= 1.3),
                    up("Snowfall", 320, "Beats wings 30% faster.", t -> t.cooldown *= 0.7),
                    up("Lingering Chill", 950, "Thawed puffs stay half-speed for a while.", t -> t.permafrost = true),
                    ability("Winter Queen", 5000, "Ability: Deep Winter. Also 20% wider wings.", Ability.DEEP_WINTER,
                            t -> t.range *= 1.2))),
    TOAD("Wizard Toad", 500, Attack.BOLT, Placement.LAND,
            "Croaks glittering bolts that pop anything and pass through several puffs.",
            t -> { t.range = 125; t.cooldown = 55; t.damage = 1; t.pierce = 4; t.dmgType = DamageType.MAGIC; t.projSpeed = 9; },
            path(
                    up("Fire Tongue", 450, "+3 pierce, +1 damage.", t -> { t.pierce += 3; t.damage += 1; }),
                    up("Chain Croak", 1300, "Bolts skip through 8 more puffs.", t -> t.pierce += 8),
                    up("Archtoad", 4200, "+3 damage and casts 40% faster.", t -> { t.damage += 3; t.cooldown *= 0.6; }),
                    up("Grand Archtoad", 12000, "+6 damage and +10 pierce.", t -> { t.damage += 6; t.pierce += 10; })),
            path(
                    up("Mystic Reach", 250, "20% more range, and sees hidden puffs.", t -> { t.range *= 1.2; t.detects = true; }),
                    up("Hasty Hex", 480, "Casts 30% faster.", t -> t.cooldown *= 0.7),
                    up("Big Bolts", 1900, "+2 damage per bolt.", t -> t.damage += 2),
                    ability("Bog Oracle", 6500, "Ability: Great Croak. Also casts 20% faster.", Ability.GREAT_CROAK,
                            t -> t.cooldown *= 0.8))),
    MUSHROOM("Coin Mushrooms", 900, Attack.FARM, Placement.LAND,
            "Doesn't pop anything. Grows a little pile of coins at the end of every round.",
            t -> t.income = 70,
            path(
                    up("More Spores", 400, "+40 coins per round.", t -> t.income += 40),
                    up("Glowcaps", 1200, "+90 coins per round.", t -> t.income += 90),
                    up("Fairy Ring", 4200, "+260 coins per round.", t -> t.income += 260),
                    up("Golden Mycelium", 12000, "+700 coins per round.", t -> t.income += 700)),
            path(
                    up("Rich Soil", 350, "+25 coins per round.", t -> t.income += 25),
                    up("Truffle Hunt", 900, "+60 coins per round.", t -> t.income += 60),
                    up("Mushroom Vault", 3000, "+170 coins per round.", t -> t.income += 170),
                    ability("Moonlit Grove", 7000, "Ability: Harvest Moon. Also +100 coins per round.", Ability.HARVEST_MOON,
                            t -> t.income += 100))),
    SNAIL("Slime Snail", 275, Attack.SLIME, Placement.LAND,
            "Spits sticky slime that slows puffs down. Big clouds slip free until the slime gets stronger.",
            t -> { t.range = 110; t.cooldown = 40; t.pierce = 1; t.projSpeed = 7; t.slimeFactor = 0.5; t.slimeTicks = 360; },
            path(
                    up("Thicker Slime", 120, "Slime is stickier and lasts longer.", t -> { t.slimeFactor = 0.4; t.slimeTicks = 480; }),
                    up("Acid Slime", 300, "Slimed puffs lose a layer every second.", t -> { t.slimeDot = 1; t.slimeDotEvery = 60; }),
                    up("Melting Slime", 1800, "Burns three times as fast and sticks to big clouds too.",
                            t -> { t.slimeDotEvery = 20; t.slimeBosses = true; }),
                    up("Dissolving Ooze", 4500, "Burns for 3 damage at a time.", t -> t.slimeDot = 3)),
            path(
                    up("Splat", 250, "Each blob also slimes 3 puffs nearby.", t -> { t.slimeSplash = 3; t.slimeSplashR = 40; }),
                    up("Big Splat", 450, "Splashes 6 puffs further away.", t -> { t.slimeSplash = 6; t.slimeSplashR = 60; }),
                    up("Slime Hose", 1200, "Spits 2.5x as often.", t -> t.cooldown *= 0.4),
                    ability("Swamp Sovereign", 3200, "Ability: Slime Flood. Also +20% range.", Ability.SLIME_FLOOD,
                            t -> t.range *= 1.2))),
    THISTLE("Thistle Patch", 650, Attack.BURR, Placement.LAND,
            "Drops spiky burrs onto the nearby path. Each burr catches several puffs before it wears out.",
            t -> { t.range = 70; t.cooldown = 90; t.damage = 1; t.pierce = 5; t.dmgType = DamageType.SHARP; t.projSpeed = 4; t.burrLife = 1800; },
            path(
                    up("More Burrs", 300, "Burrs catch 9 puffs.", t -> t.pierce = 9),
                    up("Fire Burrs", 700, "Burning burrs pop anything for 2 damage.", t -> { t.dmgType = DamageType.FIRE; t.damage = 2; }),
                    up("Burr Bombs", 2200, "Spent burrs burst in a small explosion.", t -> t.burrBombs = true),
                    up("Bramble Wall", 6000, "Burrs catch 40 puffs for 4 damage each.", t -> { t.pierce = 40; t.damage = 4; })),
            path(
                    up("Faster Growth", 400, "Drops burrs 30% faster.", t -> t.cooldown *= 0.7),
                    up("Even Faster Growth", 700, "And 30% faster again.", t -> t.cooldown *= 0.7),
                    up("Long Thorns", 1500, "Reaches 50% further; burrs last twice as long.",
                            t -> { t.range *= 1.5; t.burrLife *= 2; }),
                    ability("Briar Queen", 4000, "Ability: Thorn Storm. Also +2 pierce.", Ability.THORN_STORM, t -> t.pierce += 2))),
    LANTERN("Firefly Lantern", 900, Attack.SUPPORT, Placement.LAND,
            "Doesn't attack. Critters in its glow see 12% further, and upgrades make them stronger.",
            t -> { t.range = 120; t.giveRange = 1.12; t.cooldown = 20; t.damage = 2; t.dmgType = DamageType.MAGIC; },
            path(
                    up("Warm Glow", 500, "Critters in the glow attack 12% faster.", t -> t.giveSpeed = 0.88),
                    up("Golden Glow", 1200, "Their attacks pierce one more puff.", t -> t.givePierce = 1),
                    up("Sunlit Grove", 4000, "Their attacks deal +1 damage.", t -> t.giveDamage = 1),
                    ability("Midsummer Lantern", 8000, "Ability: Bonfire Night. Glow attack speed bonus rises to 20%.",
                            Ability.BONFIRE_NIGHT, t -> t.giveSpeed = 0.8)),
            path(
                    up("Firefly Eyes", 300, "Critters in the glow can see hidden puffs.", t -> t.giveDetect = true),
                    up("Wider Glow", 400, "The glow reaches 40% further.", t -> t.range *= 1.4),
                    up("Spark Swarm", 1800, "Fireflies zap puffs in the glow for 2 damage.", t -> t.sparks = true),
                    up("Unmasking Light", 3500, "Hidden puffs in the glow are revealed for good; sparks hit for 5.",
                            t -> { t.reveal = true; t.damage = 5; }))),
    DRAGONFLY("Dragonfly Gunner", 850, Attack.RAPID, Placement.LAND,
            "Darts back and forth firing a stream of tiny barbs. Not very accurate, but very busy.",
            t -> { t.range = 160; t.cooldown = 8; t.damage = 1; t.pierce = 1; t.dmgType = DamageType.SHARP; t.projSpeed = 13; t.inaccuracy = 0.12; },
            path(
                    up("Quick Wings", 300, "Fires 25% faster.", t -> t.cooldown *= 0.75),
                    up("Laser Eyes", 1100, "Fires beams of light that pierce 6 puffs and pop anything.",
                            t -> { t.laser = true; t.pierce = 6; t.dmgType = DamageType.MAGIC; }),
                    up("Sunbeam", 5500, "4 damage, 12 pierce.", t -> { t.damage += 3; t.pierce = 12; }),
                    up("Solar Flare", 12000, "10 damage, 20 pierce.", t -> { t.damage += 6; t.pierce = 20; })),
            path(
                    up("Hard-Shell Barbs", 500, "+1 damage, and barbs pop anything.", t -> { t.damage += 1; if (!t.laser) t.dmgType = DamageType.NORMAL; }),
                    up("Seed Missiles", 1200, "Each hit bursts in a small explosion.", t -> { t.hitSplash = 25; t.maxHits = 6; }),
                    up("Thunder Pods", 3800, "Bigger bursts, +2 damage.", t -> { t.hitSplash = 40; t.damage += 2; t.maxHits = 12; }),
                    ability("Hornet Ace", 8000, "Ability: Hornet Hurricane. Also fires 20% faster.", Ability.HORNET_HURRICANE,
                            t -> t.cooldown *= 0.8))),
    OTTER("River Otter", 500, Attack.PEBBLE, Placement.WATER,
            "Lives in the water and skims pebbles that pop anything. Sees hidden puffs from below. Water only.",
            t -> { t.range = 130; t.cooldown = 45; t.damage = 1; t.pierce = 3; t.dmgType = DamageType.NORMAL; t.projSpeed = 9; t.detects = true; },
            path(
                    up("Skipping Stones", 250, "+3 pierce.", t -> t.pierce += 3),
                    up("Shell Toss", 600, "Throws two shells for +1 damage.", t -> { t.damage += 1; t.shots = 2; t.spread = 0.15; }),
                    up("Clam Cannon", 2200, "Shells burst on impact. +1 damage.", t -> { t.hitSplash = 30; t.maxHits = 10; t.damage += 1; }),
                    ability("Riverkeeper", 6000, "Ability: Tidal Wave. Also +2 pierce.", Ability.TIDAL_WAVE, t -> t.pierce += 2)),
            path(
                    up("Quick Paws", 300, "Throws 30% faster.", t -> t.cooldown *= 0.7),
                    up("Two Otters", 900, "A friend joins: one more throw, 15% faster.", t -> { t.shots += 1; t.spread = Math.max(t.spread, 0.15); t.cooldown *= 0.85; }),
                    up("Whirlpool", 2500, "Every 4 seconds, drags nearby puffs back along the path.", t -> t.whirlpool = true),
                    up("Otter Holt", 7000, "A whole family: throws twice as fast, +2 damage.", t -> { t.cooldown *= 0.5; t.damage += 2; }))),
    STORK("Stork Patrol", 800, Attack.FLYER, Placement.LAND,
            "Circles high over its nest, scattering feathers in every direction. Place the nest anywhere.",
            t -> { t.range = 110; t.cooldown = 28; t.damage = 1; t.pierce = 3; t.shots = 6; t.dmgType = DamageType.SHARP; t.projSpeed = 8; t.orbitR = 140; t.orbitSpeed = 0.018; },
            path(
                    up("More Feathers", 400, "8 feathers per volley.", t -> t.shots = 8),
                    up("Thunder Feathers", 1500, "Charged feathers pop anything and pierce 6.", t -> { t.dmgType = DamageType.MAGIC; t.pierce = 6; }),
                    up("Egg Bombers", 3000, "Drops exploding eggs on puffs below.", t -> t.eggs = true),
                    up("Sky Sovereign", 9000, "16 feathers per volley, 3 damage each.", t -> { t.shots = 16; t.damage = 3; })),
            path(
                    up("Keen Sight", 300, "Spots hidden puffs from the sky.", t -> t.detects = true),
                    up("Wide Loops", 500, "Flies a much wider circle.", t -> t.orbitR = 220),
                    up("Gale Wings", 2000, "Volleys 40% faster and flies faster.", t -> { t.cooldown *= 0.6; t.orbitSpeed *= 1.5; }),
                    ability("Flock Leader", 7000, "Ability: Pinecone Barrage. Also +1 damage.", Ability.PINECONE_BARRAGE, t -> t.damage += 1))),
    MOLE("Mole Burrower", 450, Attack.BURROW, Placement.LAND,
            "Pops up under puffs that pass close by, bumping them to a stop. Feels hidden puffs through the ground.",
            t -> { t.range = 55; t.cooldown = 70; t.damage = 1; t.maxHits = 8; t.dmgType = DamageType.NORMAL; t.stun = 20; t.detects = true; },
            path(
                    up("Wider Burrows", 200, "Reaches 75 instead of 55.", t -> t.range = 75),
                    up("Rock Toss", 500, "Hits 16 puffs for 2 damage.", t -> { t.maxHits = 16; t.damage = 2; }),
                    up("Earthquake", 2500, "Hits 40 puffs for 5 damage over a wide area.", t -> { t.maxHits = 40; t.damage = 5; t.range = 95; }),
                    ability("Tunnel Network", 7000, "Ability: Cave-In. Also +2 damage.", Ability.CAVE_IN, t -> t.damage += 2)),
            path(
                    up("Fast Digging", 300, "Digs 30% faster.", t -> t.cooldown *= 0.7),
                    up("Gem Finder", 700, "Every puff layer it pops drops an extra coin.", t -> t.coinBonus = 1),
                    up("Crystal Claws", 1800, "+2 damage and longer stuns.", t -> { t.damage += 2; t.stun = 40; }),
                    up("Deep Delve", 5000, "Digs twice as fast and even stuns big clouds.", t -> { t.cooldown *= 0.5; t.bossStun = true; })));

    enum Attack { DART, RADIAL, BOMB, HITSCAN, PULSE, BOLT, FARM, SLIME, BURR, SUPPORT, RAPID, PEBBLE, FLYER, BURROW }

    enum Placement { LAND, WATER }

    final String label;
    final int cost;
    final Attack attack;
    final Placement placement;
    final String desc;
    final Consumer<Tower> init;
    final Upgrade[][] paths;

    TowerType(String label, int cost, Attack attack, Placement placement, String desc, Consumer<Tower> init,
              Upgrade[] a, Upgrade[] b) {
        this.label = label;
        this.cost = cost;
        this.attack = attack;
        this.placement = placement;
        this.desc = desc;
        this.init = init;
        this.paths = new Upgrade[][]{a, b};
    }

    /** Whether picking a target makes sense for this critter (shown as the targeting button). */
    boolean aims() {
        return switch (attack) {
            case DART, BOMB, HITSCAN, BOLT, SLIME, RAPID, PEBBLE -> true;
            default -> false;
        };
    }

    private static Upgrade[] path(Upgrade... ups) {
        return ups;
    }

    private static Upgrade up(String name, int cost, String desc, Consumer<Tower> effect) {
        return new Upgrade(name, cost, desc, effect);
    }

    private static Upgrade ability(String name, int cost, String desc, Ability ability, Consumer<Tower> effect) {
        return new Upgrade(name, cost, desc, effect, ability);
    }
}
