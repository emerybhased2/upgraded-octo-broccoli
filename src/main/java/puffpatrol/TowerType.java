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
                            t -> { t.shots = 5; t.spread = 0.18; t.pierce += 4; t.damage += 2; t.dmgType = DamageType.NORMAL; }),
                    up("Thousand Needles", 22000, "Eight quills per throw, +8 pierce, +4 damage.", t -> { t.shots = 8; t.spread = 0.14; t.pierce += 8; t.damage += 4; })),
            path(
                    up("Keen Nose", 100, "Sniffs out puffs 25% further away.", t -> t.range *= 1.25),
                    up("Night Snuffle", 300, "Smells hidden puffs and throws 20% faster.", t -> { t.detects = true; t.cooldown *= 0.8; }),
                    up("Hedgehog Frenzy", 1400, "Throws twice as fast, +1 damage.", t -> { t.cooldown *= 0.5; t.damage += 1; }),
                    ability("Bristleback", 5000, "Ability: Quill Blizzard. Also throws 20% faster.", Ability.QUILL_BLIZZARD,
                            t -> t.cooldown *= 0.8),
                    up("Hedgehog Hurricane", 18000, "Throws 2.5x as fast, +2 damage.", t -> { t.cooldown *= 0.4; t.damage += 2; })),
            path(
                    up("Long Spines", 120, "15% more range; quills fly faster.", t -> { t.range *= 1.15; t.projSpeed *= 1.2; }),
                    up("Barbed Tips", 250, "+1 damage.", t -> t.damage += 1),
                    up("Bristle Ball", 900, "Rolls a spiky ball: +6 pierce, bursts on each hit.", t -> { t.pierce += 6; t.hitSplash = 20; t.maxHits = 6; }),
                    up("Bramble Boulder", 4000, "+3 damage, +10 pierce, pops anything.", t -> { t.damage += 3; t.pierce += 10; t.dmgType = DamageType.NORMAL; }),
                    up("Mountain Roller", 16000, "+8 damage, +20 pierce, much bigger bursts.", t -> { t.damage += 8; t.pierce += 20; t.hitSplash = 40; t.maxHits = 15; }))),
    BEEHIVE("Bumble Hive", 300, Attack.RADIAL, Placement.LAND,
            "Sends stingers buzzing out in every direction when puffs wander close.",
            t -> { t.range = 85; t.cooldown = 70; t.damage = 1; t.pierce = 1; t.shots = 8; t.dmgType = DamageType.SHARP; t.projSpeed = 7; },
            path(
                    up("More Bees", 150, "10 stingers per buzz.", t -> t.shots = 10),
                    up("Swarm", 320, "16 stingers per buzz.", t -> t.shots = 16),
                    up("Honey Inferno", 2200, "Stingers turn to fire: pop anything, 3 pierce, +1 damage.",
                            t -> { t.dmgType = DamageType.FIRE; t.pierce = 3; t.damage += 1; t.fire = true; }),
                    up("Royal Jelly Blaze", 7000, "24 blazing stingers, 5 pierce, +2 damage.",
                            t -> { t.shots = 24; t.pierce = 5; t.damage += 2; }),
                    up("Sunfire Swarm", 20000, "32 blazing stingers, 8 pierce, +4 damage.", t -> { t.shots = 32; t.pierce = 8; t.damage += 4; })),
            path(
                    up("Busy Bees", 140, "Buzzes 25% faster.", t -> t.cooldown *= 0.75),
                    up("Far Flyers", 120, "Stingers fly 25% further.", t -> t.range *= 1.25),
                    up("Queen's Fury", 1300, "Buzzes twice as fast, +1 damage.", t -> { t.cooldown *= 0.5; t.damage += 1; }),
                    ability("Hive Mind", 4500, "Ability: Swarm Surge. Also +1 pierce.", Ability.SWARM_SURGE, t -> t.pierce += 1),
                    up("Empress Hive", 16000, "Buzzes 2.5x as fast, +2 damage.", t -> { t.cooldown *= 0.4; t.damage += 2; })),
            path(
                    up("Honey Drip", 150, "Stung puffs are slowed by honey for a moment.", t -> { t.slowOnHitTicks = 90; t.slowOnHitFactor = 0.7; }),
                    up("Thick Honey", 300, "Honey slows more and lasts longer.", t -> { t.slowOnHitTicks = 150; t.slowOnHitFactor = 0.5; }),
                    up("Royal Pollen", 1200, "20% more range, +2 pierce.", t -> { t.range *= 1.2; t.pierce += 2; }),
                    up("Golden Comb", 3500, "Sells honey: +80 coins per round. +1 damage.", t -> { t.income += 80; t.damage += 1; }),
                    up("Hive Metropolis", 14000, "16 more stingers, buzzes faster, honey slows big clouds too.", t -> { t.shots += 16; t.cooldown *= 0.6; t.slowBosses = true; }))),
    BADGER("Acorn Badger", 550, Attack.BOMB, Placement.LAND,
            "Lobs exploding acorns. Soot and Marble puffs shrug off explosions.",
            t -> { t.range = 135; t.cooldown = 72; t.damage = 1; t.splash = 42; t.maxHits = 22; t.dmgType = DamageType.EXPLOSIVE; t.projSpeed = 7; },
            path(
                    up("Heavy Acorns", 300, "Larger blast that pops more puffs.", t -> { t.splash = 56; t.maxHits = 32; }),
                    up("Mighty Acorns", 650, "Even larger blast, 2 damage.", t -> { t.splash = 66; t.damage = 2; }),
                    up("Oak Bomb", 3200, "An enormous acorn. 6 damage, huge blast.", t -> { t.splash = 82; t.damage = 6; t.maxHits = 60; }),
                    up("Thunder Acorn", 9000, "15 damage per blast with a colossal radius.", t -> { t.splash = 100; t.damage = 15; t.maxHits = 90; }),
                    up("Earthshaker Acorn", 28000, "40 damage per blast with a gigantic radius.", t -> { t.damage = 40; t.splash = 130; t.maxHits = 150; })),
            path(
                    up("Long Throw", 220, "Throws 25% further.", t -> t.range *= 1.25),
                    up("Rapid Lobbing", 380, "Throws 30% faster.", t -> t.cooldown *= 0.7),
                    up("Cluster Nuts", 2100, "Each blast scatters six smaller blasts.", t -> t.cluster = true),
                    ability("Burrow Artillery", 6000, "Ability: Acorn Avalanche. Also +1 damage.", Ability.ACORN_AVALANCHE,
                            t -> t.damage += 1),
                    up("Burrow Battery", 18000, "Throws 2.5x as fast with cluster blasts, +3 damage.", t -> { t.cooldown *= 0.4; t.cluster = true; t.damage += 3; })),
            path(
                    up("Sharp Shells", 200, "+2 damage against big clouds.", t -> t.bossBonus = 2),
                    up("Cloud Cracker", 500, "+6 damage against big clouds, 10% more range.", t -> { t.bossBonus = 6; t.range *= 1.1; }),
                    up("Storm Breaker", 2500, "+20 damage against big clouds.", t -> t.bossBonus = 20),
                    up("Sky Splitter", 7500, "+60 damage against big clouds, throws faster.", t -> { t.bossBonus = 60; t.cooldown *= 0.8; }),
                    up("Thunderclap Acorn", 25000, "+200 damage against big clouds and a wider blast.", t -> { t.bossBonus = 200; t.splash += 20; }))),
    OWL("Night Owl", 350, Attack.HITSCAN, Placement.LAND,
            "Sees every puff on the map and swoops at the strongest-looking one. Pops anything.",
            t -> { t.range = 9999; t.cooldown = 80; t.damage = 2; t.dmgType = DamageType.NORMAL; t.mode = Tower.Target.STRONG; },
            path(
                    up("Talon Strike", 350, "4 damage per swoop.", t -> t.damage = 4),
                    up("Thunder Hoot", 1600, "8 damage per swoop.", t -> t.damage = 8),
                    up("Moonfall", 4000, "20 damage per swoop. Clouds weep.", t -> t.damage = 20),
                    up("Eclipse", 11000, "50 damage per swoop.", t -> t.damage = 50),
                    up("Starfall", 30000, "150 damage per swoop.", t -> t.damage = 150)),
            path(
                    up("Night Eyes", 300, "Spots hidden puffs and swoops 30% faster.", t -> { t.detects = true; t.cooldown *= 0.7; }),
                    up("Swift Wings", 650, "Swoops 40% faster again.", t -> t.cooldown *= 0.6),
                    up("Parliament of Owls", 3600, "Swoops 2.5x faster. Many owls, one opinion.", t -> t.cooldown *= 0.4),
                    ability("Midnight Muster", 8000, "Ability: Owl Squadron. Also +2 damage.", Ability.OWL_SQUADRON, t -> t.damage += 2),
                    up("Great Parliament", 20000, "Swoops 2.5x as fast, +5 damage.", t -> { t.cooldown *= 0.4; t.damage += 5; })),
            path(
                    up("Owl Pellets", 200, "Each swoop bursts, hitting 5 puffs nearby.", t -> { t.hitSplash = 25; t.maxHits = 5; }),
                    up("Pellet Burst", 450, "Bigger bursts that hit 10 puffs.", t -> { t.hitSplash = 35; t.maxHits = 10; }),
                    up("Full Moon", 1500, "Sees hidden puffs; each layer it pops drops an extra coin.", t -> { t.detects = true; t.coinBonus = 1; }),
                    up("Night Watch", 5000, "Critters near the owl see hidden puffs and 10% further.", t -> { t.auraRange = 200; t.giveDetect = true; t.giveRange = 1.1; }),
                    up("Owl Oracle", 18000, "Its watch reaches wider; nearby critters attack 15% faster with +1 damage.", t -> { t.auraRange = 260; t.giveSpeed = 0.85; t.giveDamage = 1; }))),
    MOTH("Frost Moth", 400, Attack.PULSE, Placement.LAND,
            "Flutters icy wingbeats that freeze nearby puffs in place. Big clouds are only slowed.",
            t -> { t.range = 72; t.cooldown = 95; t.freezeTicks = 60; },
            path(
                    up("Long Chill", 220, "Freezes last much longer.", t -> t.freezeTicks = 100),
                    up("Brittle Ice", 420, "Frozen puffs take +1 damage from every hit.", t -> t.brittle = true),
                    up("Hoarfrost Wings", 2600, "Each wingbeat deals 2 cold damage and beats faster.",
                            t -> { t.pulseDamage = 2; t.cooldown *= 0.8; }),
                    up("Glacier Heart", 7500, "6 cold damage per beat, longer freezes, big clouds slowed to a crawl.",
                            t -> { t.pulseDamage = 6; t.freezeTicks = 140; t.heavySlow = true; }),
                    up("Eternal Winter", 22000, "15 cold damage per beat and very long freezes.", t -> { t.pulseDamage = 15; t.freezeTicks = 180; })),
            path(
                    up("Wider Wings", 180, "Chills 30% further out.", t -> t.range *= 1.3),
                    up("Snowfall", 320, "Beats wings 30% faster.", t -> t.cooldown *= 0.7),
                    up("Lingering Chill", 950, "Thawed puffs stay half-speed for a while.", t -> t.permafrost = true),
                    ability("Winter Queen", 5000, "Ability: Deep Winter. Also 20% wider wings.", Ability.DEEP_WINTER,
                            t -> t.range *= 1.2),
                    up("Aurora Wings", 15000, "30% wider, beats much faster.", t -> { t.range *= 1.3; t.cooldown *= 0.6; })),
            path(
                    up("Ice Flakes", 180, "Wingbeats deal 1 cold damage.", t -> t.pulseDamage = Math.max(t.pulseDamage, 1)),
                    up("Snow Shards", 400, "+1 cold damage; thawed puffs stay slow.", t -> { t.pulseDamage += 1; t.permafrost = true; }),
                    up("Hailstorm", 1600, "+2 cold damage.", t -> t.pulseDamage += 2),
                    up("Blizzard Moth", 5000, "25% wider, +3 cold damage.", t -> { t.range *= 1.25; t.pulseDamage += 3; }),
                    up("Frost Titan", 17000, "+10 cold damage, big clouds crawl.", t -> { t.pulseDamage += 10; t.heavySlow = true; }))),
    TOAD("Wizard Toad", 500, Attack.BOLT, Placement.LAND,
            "Croaks glittering bolts that pop anything and pass through several puffs.",
            t -> { t.range = 125; t.cooldown = 55; t.damage = 1; t.pierce = 4; t.dmgType = DamageType.MAGIC; t.projSpeed = 9; },
            path(
                    up("Fire Tongue", 450, "+3 pierce, +1 damage.", t -> { t.pierce += 3; t.damage += 1; }),
                    up("Chain Croak", 1300, "Bolts skip through 8 more puffs.", t -> t.pierce += 8),
                    up("Archtoad", 4200, "+3 damage and casts 40% faster.", t -> { t.damage += 3; t.cooldown *= 0.6; }),
                    up("Grand Archtoad", 12000, "+6 damage and +10 pierce.", t -> { t.damage += 6; t.pierce += 10; }),
                    up("Toad Sage", 28000, "+15 damage and +20 pierce.", t -> { t.damage += 15; t.pierce += 20; })),
            path(
                    up("Mystic Reach", 250, "20% more range, and sees hidden puffs.", t -> { t.range *= 1.2; t.detects = true; }),
                    up("Hasty Hex", 480, "Casts 30% faster.", t -> t.cooldown *= 0.7),
                    up("Big Bolts", 1900, "+2 damage per bolt.", t -> t.damage += 2),
                    ability("Bog Oracle", 6500, "Ability: Great Croak. Also casts 20% faster.", Ability.GREAT_CROAK,
                            t -> t.cooldown *= 0.8),
                    up("Storm Toad", 18000, "Casts 2.5x as fast, +4 damage.", t -> { t.cooldown *= 0.4; t.damage += 4; })),
            path(
                    up("Ember Spit", 250, "Bolts burst into small flames.", t -> { t.hitSplash = 18; t.maxHits = 4; }),
                    up("Fire Breath", 600, "Bigger flames, +1 damage.", t -> { t.hitSplash = 28; t.maxHits = 8; t.damage += 1; }),
                    up("Dragon Croak", 2400, "Three bolts at once.", t -> { t.shots = 3; t.spread = 0.2; }),
                    up("Ember Toad", 7000, "+4 damage, huge flames.", t -> { t.damage += 4; t.hitSplash = 40; t.maxHits = 16; }),
                    up("Cinder Sage", 22000, "Five bolts at once, +8 damage.", t -> { t.shots = 5; t.spread = 0.16; t.damage += 8; }))),
    MUSHROOM("Coin Mushrooms", 900, Attack.FARM, Placement.LAND,
            "Doesn't pop anything. Grows a little pile of coins at the end of every round.",
            t -> t.income = 70,
            path(
                    up("More Spores", 400, "+40 coins per round.", t -> t.income += 40),
                    up("Glowcaps", 1200, "+90 coins per round.", t -> t.income += 90),
                    up("Fairy Ring", 4200, "+260 coins per round.", t -> t.income += 260),
                    up("Golden Mycelium", 12000, "+700 coins per round.", t -> t.income += 700),
                    up("Mycelium Kingdom", 30000, "+1800 coins per round.", t -> t.income += 1800)),
            path(
                    up("Rich Soil", 350, "+25 coins per round.", t -> t.income += 25),
                    up("Truffle Hunt", 900, "+60 coins per round.", t -> t.income += 60),
                    up("Mushroom Vault", 3000, "+170 coins per round.", t -> t.income += 170),
                    ability("Moonlit Grove", 7000, "Ability: Harvest Moon. Also +100 coins per round.", Ability.HARVEST_MOON,
                            t -> t.income += 100),
                    up("Harvest Festival", 20000, "+700 coins per round.", t -> t.income += 700)),
            path(
                    up("Puffball Spores", 300, "Puffs out spores that pop nearby puffs.", t -> { t.sparks = true; t.range = 90; t.damage = 1; t.cooldown = 40; t.dmgType = DamageType.NORMAL; }),
                    up("Sleepy Spores", 600, "Spored puffs slow down.", t -> { t.slowOnHitTicks = 60; t.slowOnHitFactor = 0.5; }),
                    up("Coin Spores", 1500, "Pops drop an extra coin; +60 coins per round.", t -> { t.coinBonus = 1; t.income += 60; }),
                    up("Giant Puffball", 5000, "Spores hit for 6, twice as often.", t -> { t.damage = 6; t.cooldown = 20; }),
                    up("Spore Cloud", 16000, "Spores hit for 20 over a wider area.", t -> { t.damage = 20; t.range = 140; t.cooldown = 10; }))),
    SNAIL("Slime Snail", 275, Attack.SLIME, Placement.LAND,
            "Spits sticky slime that slows puffs down. Big clouds slip free until the slime gets stronger.",
            t -> { t.range = 110; t.cooldown = 40; t.pierce = 1; t.projSpeed = 7; t.slimeFactor = 0.5; t.slimeTicks = 360; },
            path(
                    up("Thicker Slime", 120, "Slime is stickier and lasts longer.", t -> { t.slimeFactor = 0.4; t.slimeTicks = 480; }),
                    up("Acid Slime", 300, "Slimed puffs lose a layer every second.", t -> { t.slimeDot = 1; t.slimeDotEvery = 60; }),
                    up("Melting Slime", 1800, "Burns three times as fast and sticks to big clouds too.",
                            t -> { t.slimeDotEvery = 20; t.slimeBosses = true; }),
                    up("Dissolving Ooze", 4500, "Burns for 3 damage at a time.", t -> t.slimeDot = 3),
                    up("Primordial Ooze", 15000, "Slime burns for 10 at a time.", t -> t.slimeDot = 10)),
            path(
                    up("Splat", 250, "Each blob also slimes 3 puffs nearby.", t -> { t.slimeSplash = 3; t.slimeSplashR = 40; }),
                    up("Big Splat", 450, "Splashes 6 puffs further away.", t -> { t.slimeSplash = 6; t.slimeSplashR = 60; }),
                    up("Slime Hose", 1200, "Spits 2.5x as often.", t -> t.cooldown *= 0.4),
                    ability("Swamp Sovereign", 3200, "Ability: Slime Flood. Also +20% range.", Ability.SLIME_FLOOD,
                            t -> t.range *= 1.2),
                    up("Slime Cannon", 12000, "Spits 2.5x as often and splashes 12 puffs.", t -> { t.cooldown *= 0.4; t.slimeSplash = 12; t.slimeSplashR = 80; })),
            path(
                    up("Sticky Trail", 150, "Slime lasts 50% longer.", t -> t.slimeTicks = (int) (t.slimeTicks * 1.5)),
                    up("Long Spit", 250, "30% more range; blobs fly faster.", t -> { t.range *= 1.3; t.projSpeed *= 1.3; }),
                    up("Double Gob", 900, "Spits two blobs at once.", t -> { t.shots = 2; t.spread = 0.2; }),
                    up("Shell Shock", 3000, "Blobs stun puffs for half a second.", t -> t.stunOnHit = 30),
                    up("Snail Sovereign", 12000, "Four blobs, stickier slime that holds big clouds.", t -> { t.shots = 4; t.slimeBosses = true; t.slimeFactor = 0.3; }))),
    THISTLE("Thistle Patch", 650, Attack.BURR, Placement.LAND,
            "Drops spiky burrs onto the nearby path. Each burr catches several puffs before it wears out.",
            t -> { t.range = 70; t.cooldown = 90; t.damage = 1; t.pierce = 5; t.dmgType = DamageType.SHARP; t.projSpeed = 4; t.burrLife = 1800; },
            path(
                    up("More Burrs", 300, "Burrs catch 9 puffs.", t -> t.pierce = 9),
                    up("Fire Burrs", 700, "Burning burrs pop anything for 2 damage.", t -> { t.dmgType = DamageType.FIRE; t.damage = 2; }),
                    up("Burr Bombs", 2200, "Spent burrs burst in a small explosion.", t -> t.burrBombs = true),
                    up("Bramble Wall", 6000, "Burrs catch 40 puffs for 4 damage each.", t -> { t.pierce = 40; t.damage = 4; }),
                    up("Briar Fortress", 20000, "Burrs catch 120 puffs for 8 damage.", t -> { t.pierce = 120; t.damage = 8; })),
            path(
                    up("Faster Growth", 400, "Drops burrs 30% faster.", t -> t.cooldown *= 0.7),
                    up("Even Faster Growth", 700, "And 30% faster again.", t -> t.cooldown *= 0.7),
                    up("Long Thorns", 1500, "Reaches 50% further; burrs last twice as long.",
                            t -> { t.range *= 1.5; t.burrLife *= 2; }),
                    ability("Briar Queen", 4000, "Ability: Thorn Storm. Also +2 pierce.", Ability.THORN_STORM, t -> t.pierce += 2),
                    up("Thorn Forest", 16000, "Drops burrs 2.5x as fast, 20% further.", t -> { t.cooldown *= 0.4; t.range *= 1.2; })),
            path(
                    up("Seed Pods", 250, "Drops two burrs at a time.", t -> t.shots = 2),
                    up("Wind Seeds", 500, "Reaches 30% further.", t -> t.range *= 1.3),
                    up("Sticky Burrs", 1400, "Snagged puffs slow down.", t -> { t.slowOnHitTicks = 60; t.slowOnHitFactor = 0.5; }),
                    up("Thistledown", 4000, "Four burrs at a time.", t -> t.shots = 4),
                    up("Meadow Overgrowth", 15000, "Eight burrs at a time, +2 damage.", t -> { t.shots = 8; t.damage += 2; }))),
    LANTERN("Firefly Lantern", 900, Attack.SUPPORT, Placement.LAND,
            "Doesn't attack. Critters in its glow see 12% further, and upgrades make them stronger.",
            t -> { t.range = 120; t.auraRange = 120; t.giveRange = 1.12; t.cooldown = 20; t.damage = 2; t.dmgType = DamageType.MAGIC; },
            path(
                    up("Warm Glow", 500, "Critters in the glow attack 12% faster.", t -> t.giveSpeed = 0.88),
                    up("Golden Glow", 1200, "Their attacks pierce one more puff.", t -> t.givePierce = 1),
                    up("Sunlit Grove", 4000, "Their attacks deal +1 damage.", t -> t.giveDamage = 1),
                    ability("Midsummer Lantern", 8000, "Ability: Bonfire Night. Glow attack speed bonus rises to 20%.",
                            Ability.BONFIRE_NIGHT, t -> t.giveSpeed = 0.8),
                    up("Solstice Lantern", 25000, "Critters in the glow attack 30% faster with +2 damage.", t -> { t.giveSpeed = 0.7; t.giveDamage = 2; })),
            path(
                    up("Firefly Eyes", 300, "Critters in the glow can see hidden puffs.", t -> t.giveDetect = true),
                    up("Wider Glow", 400, "The glow reaches 40% further.", t -> t.range *= 1.4),
                    up("Spark Swarm", 1800, "Fireflies zap puffs in the glow for 2 damage.", t -> t.sparks = true),
                    up("Unmasking Light", 3500, "Hidden puffs in the glow are revealed for good; sparks hit for 5.",
                            t -> { t.reveal = true; t.damage = 5; }),
                    up("Dawnlight", 15000, "30% wider glow; sparks hit for 15, twice as often.", t -> { t.range *= 1.3; t.damage = 15; t.cooldown = 10; t.sparks = true; })),
            path(
                    up("Moth Lure", 300, "Visitors pay to see it: +30 coins per round.", t -> t.income += 30),
                    up("Glow Market", 700, "+60 coins per round.", t -> t.income += 60),
                    up("Festival Lights", 2000, "Critters in the glow see 25% further.", t -> t.giveRange = Math.max(t.giveRange, 1.25)),
                    up("Lantern Parade", 6000, "+200 coins per round.", t -> t.income += 200),
                    up("Night of a Thousand Lights", 18000, "Glow range bonus 35%, reveals hidden puffs, +300 coins.", t -> { t.giveRange = 1.35; t.giveDetect = true; t.income += 300; }))),
    DRAGONFLY("Dragonfly Gunner", 850, Attack.RAPID, Placement.LAND,
            "Darts back and forth firing a stream of tiny barbs. Not very accurate, but very busy.",
            t -> { t.range = 160; t.cooldown = 8; t.damage = 1; t.pierce = 1; t.dmgType = DamageType.SHARP; t.projSpeed = 13; t.inaccuracy = 0.12; },
            path(
                    up("Quick Wings", 300, "Fires 25% faster.", t -> t.cooldown *= 0.75),
                    up("Laser Eyes", 1100, "Fires beams of light that pierce 6 puffs and pop anything.",
                            t -> { t.laser = true; t.pierce = 6; t.dmgType = DamageType.MAGIC; }),
                    up("Sunbeam", 5500, "4 damage, 12 pierce.", t -> { t.damage += 3; t.pierce = 12; }),
                    up("Solar Flare", 12000, "10 damage, 20 pierce.", t -> { t.damage += 6; t.pierce = 20; }),
                    up("Supernova Eyes", 35000, "+25 damage, 40 pierce.", t -> { t.damage += 25; t.pierce = 40; })),
            path(
                    up("Hard-Shell Barbs", 500, "+1 damage, and barbs pop anything.", t -> { t.damage += 1; if (!t.laser) t.dmgType = DamageType.NORMAL; }),
                    up("Seed Missiles", 1200, "Each hit bursts in a small explosion.", t -> { t.hitSplash = 25; t.maxHits = 6; }),
                    up("Thunder Pods", 3800, "Bigger bursts, +2 damage.", t -> { t.hitSplash = 40; t.damage += 2; t.maxHits = 12; }),
                    ability("Hornet Ace", 8000, "Ability: Hornet Hurricane. Also fires 20% faster.", Ability.HORNET_HURRICANE,
                            t -> t.cooldown *= 0.8),
                    up("Hornet Armada", 20000, "Fires twice as fast with huge bursts, +4 damage.", t -> { t.cooldown *= 0.5; t.hitSplash = 55; t.damage += 4; t.maxHits = Math.max(t.maxHits, 16); })),
            path(
                    up("Compound Eyes", 200, "Much more accurate, 15% more range.", t -> { t.inaccuracy *= 0.5; t.range *= 1.15; }),
                    up("Long Wings", 400, "30% more range; faster barbs.", t -> { t.range *= 1.3; t.projSpeed *= 1.3; }),
                    up("Paired Barbs", 1600, "Fires two barbs at a time.", t -> { t.shots = 2; t.spread = 0.06; }),
                    up("Hunting Swarm", 5000, "Three at a time, and spots hidden puffs.", t -> { t.shots = 3; t.detects = true; }),
                    up("Sky Lancer", 18000, "Five at a time, +5 damage.", t -> { t.shots = 5; t.damage += 5; }))),
    OTTER("River Otter", 500, Attack.PEBBLE, Placement.WATER,
            "Lives in the water and skims pebbles that pop anything. Sees hidden puffs from below. Water only.",
            t -> { t.range = 130; t.cooldown = 45; t.damage = 1; t.pierce = 3; t.dmgType = DamageType.NORMAL; t.projSpeed = 9; t.detects = true; },
            path(
                    up("Skipping Stones", 250, "+3 pierce.", t -> t.pierce += 3),
                    up("Shell Toss", 600, "Throws two shells for +1 damage.", t -> { t.damage += 1; t.shots = 2; t.spread = 0.15; }),
                    up("Clam Cannon", 2200, "Shells burst on impact. +1 damage.", t -> { t.hitSplash = 30; t.maxHits = 10; t.damage += 1; }),
                    ability("Riverkeeper", 6000, "Ability: Tidal Wave. Also +2 pierce.", Ability.TIDAL_WAVE, t -> t.pierce += 2),
                    up("River Spirit", 22000, "+10 damage, +10 pierce, big splashes.", t -> { t.damage += 10; t.pierce += 10; t.hitSplash = 50; t.maxHits = Math.max(t.maxHits, 20); })),
            path(
                    up("Quick Paws", 300, "Throws 30% faster.", t -> t.cooldown *= 0.7),
                    up("Two Otters", 900, "A friend joins: one more throw, 15% faster.", t -> { t.shots += 1; t.spread = Math.max(t.spread, 0.15); t.cooldown *= 0.85; }),
                    up("Whirlpool", 2500, "Every 4 seconds, drags nearby puffs back along the path.", t -> t.whirlpool = true),
                    up("Otter Holt", 7000, "A whole family: throws twice as fast, +2 damage.", t -> { t.cooldown *= 0.5; t.damage += 2; }),
                    up("Otter Kingdom", 18000, "Two more throws, twice as fast.", t -> { t.shots += 2; t.cooldown *= 0.5; t.spread = Math.max(t.spread, 0.15); })),
            path(
                    up("Fishing", 250, "+25 coins per round.", t -> t.income += 25),
                    up("Pearl Diving", 700, "+60 coins per round.", t -> t.income += 60),
                    up("Treasure Diver", 2000, "Pops drop an extra coin; +80 coins per round.", t -> { t.coinBonus = 1; t.income += 80; }),
                    up("Sunken Hoard", 6000, "+250 coins per round.", t -> t.income += 250),
                    up("Riverbank Market", 16000, "+600 coins per round and one more throw.", t -> { t.income += 600; t.shots += 1; t.spread = Math.max(t.spread, 0.15); }))),
    STORK("Stork Patrol", 800, Attack.FLYER, Placement.LAND,
            "Circles high over its nest, scattering feathers in every direction. Place the nest anywhere.",
            t -> { t.range = 110; t.cooldown = 28; t.damage = 1; t.pierce = 3; t.shots = 6; t.dmgType = DamageType.SHARP; t.projSpeed = 8; t.orbitR = 140; t.orbitSpeed = 0.018; },
            path(
                    up("More Feathers", 400, "8 feathers per volley.", t -> t.shots = 8),
                    up("Thunder Feathers", 1500, "Charged feathers pop anything and pierce 6.", t -> { t.dmgType = DamageType.MAGIC; t.pierce = 6; }),
                    up("Egg Bombers", 3000, "Drops exploding eggs on puffs below.", t -> t.eggs = true),
                    up("Sky Sovereign", 9000, "16 feathers per volley, 3 damage each.", t -> { t.shots = 16; t.damage = 3; }),
                    up("Storm Stork", 26000, "24 feathers per volley, 6 damage, 10 pierce.", t -> { t.shots = 24; t.damage = 6; t.pierce = 10; })),
            path(
                    up("Keen Sight", 300, "Spots hidden puffs from the sky.", t -> t.detects = true),
                    up("Wide Loops", 500, "Flies a much wider circle.", t -> t.orbitR = 220),
                    up("Gale Wings", 2000, "Volleys 40% faster and flies faster.", t -> { t.cooldown *= 0.6; t.orbitSpeed *= 1.5; }),
                    ability("Flock Leader", 7000, "Ability: Pinecone Barrage. Also +1 damage.", Ability.PINECONE_BARRAGE, t -> t.damage += 1),
                    up("Skyfleet", 20000, "Volleys 2.5x as fast, flies faster, +2 damage.", t -> { t.cooldown *= 0.4; t.orbitSpeed *= 1.5; t.damage += 2; })),
            path(
                    up("Steady Glide", 200, "Feathers fly faster and 20% further.", t -> { t.projSpeed *= 1.3; t.range *= 1.2; }),
                    up("Tailwind", 500, "Flies 30% faster.", t -> t.orbitSpeed *= 1.3),
                    up("Dive Bomb", 2000, "Drops exploding eggs on puffs below.", t -> t.eggs = true),
                    up("Heron Wing", 6000, "Eggs hit for 12.", t -> t.eggDamage = 12),
                    up("Thunderbird", 20000, "Eggs hit for 40.", t -> t.eggDamage = 40))),
    MOLE("Mole Burrower", 450, Attack.BURROW, Placement.LAND,
            "Pops up under puffs that pass close by, bumping them to a stop. Feels hidden puffs through the ground.",
            t -> { t.range = 55; t.cooldown = 70; t.damage = 1; t.maxHits = 8; t.dmgType = DamageType.NORMAL; t.stun = 20; t.detects = true; },
            path(
                    up("Wider Burrows", 200, "Reaches 75 instead of 55.", t -> t.range = 75),
                    up("Rock Toss", 500, "Hits 16 puffs for 2 damage.", t -> { t.maxHits = 16; t.damage = 2; }),
                    up("Earthquake", 2500, "Hits 40 puffs for 5 damage over a wide area.", t -> { t.maxHits = 40; t.damage = 5; t.range = 95; }),
                    ability("Tunnel Network", 7000, "Ability: Cave-In. Also +2 damage.", Ability.CAVE_IN, t -> t.damage += 2),
                    up("Worldquake", 25000, "20 damage to 120 puffs over a huge area.", t -> { t.damage = 20; t.range = 130; t.maxHits = 120; })),
            path(
                    up("Fast Digging", 300, "Digs 30% faster.", t -> t.cooldown *= 0.7),
                    up("Gem Finder", 700, "Every puff layer it pops drops an extra coin.", t -> t.coinBonus = 1),
                    up("Crystal Claws", 1800, "+2 damage and longer stuns.", t -> { t.damage += 2; t.stun = 40; }),
                    up("Deep Delve", 5000, "Digs twice as fast and even stuns big clouds.", t -> { t.cooldown *= 0.5; t.bossStun = true; }),
                    up("Mole Emperor", 15000, "Digs 2.5x as fast; very long stuns.", t -> { t.cooldown *= 0.4; t.stun = 70; })),
            path(
                    up("Root Digger", 200, "+20 coins per round.", t -> t.income += 20),
                    up("Gem Mine", 700, "+70 coins per round, and pops drop an extra coin.", t -> { t.income += 70; t.coinBonus = 1; }),
                    up("Crystal Cavern", 2200, "30% more reach, +2 damage.", t -> { t.range *= 1.3; t.damage += 2; }),
                    up("Mother Lode", 6000, "+250 coins per round.", t -> t.income += 250),
                    up("Underground Kingdom", 18000, "+500 coins per round, +5 damage.", t -> { t.income += 500; t.damage += 5; })));

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
              Upgrade[] a, Upgrade[] b, Upgrade[] c) {
        this.label = label;
        this.cost = cost;
        this.attack = attack;
        this.placement = placement;
        this.desc = desc;
        this.init = init;
        this.paths = new Upgrade[][]{a, b, c};
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
