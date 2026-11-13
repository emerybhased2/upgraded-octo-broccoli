package puffpatrol;

/** Special moves granted by some tier-4 upgrades. Trigger them from the ability bar; they then recharge. */
enum Ability {
    QUILL_BLIZZARD("Quill Blizzard", "Fires a ring of 48 heavy quills.", 45),
    SWARM_SURGE("Swarm Surge", "The hive buzzes four times as fast for 8 seconds.", 50),
    ACORN_AVALANCHE("Acorn Avalanche", "Drops huge acorns on the 12 strongest puffs.", 60),
    OWL_SQUADRON("Owl Squadron", "Thirty swoops on the strongest puffs at once.", 60),
    DEEP_WINTER("Deep Winter", "Freezes every puff on the map. Big clouds are slowed.", 60),
    GREAT_CROAK("Great Croak", "Blows puffs back along the path and batters big clouds.", 50),
    HARVEST_MOON("Harvest Moon", "An instant harvest worth three rounds of coins.", 60),
    SLIME_FLOOD("Slime Flood", "Slimes every puff on the map, big clouds included.", 45),
    THORN_STORM("Thorn Storm", "Scatters 30 burrs along the whole path.", 45),
    BONFIRE_NIGHT("Bonfire Night", "Every critter attacks twice as fast for 10 seconds.", 60),
    HORNET_HURRICANE("Hornet Hurricane", "The dragonfly fires four times as fast for 10 seconds.", 50),
    TIDAL_WAVE("Tidal Wave", "A wave washes over every puff and pushes them back.", 60),
    PINECONE_BARRAGE("Pinecone Barrage", "Forty exploding pinecones rain on the puffs.", 50),
    CAVE_IN("Cave-In", "The ground drops: puffs slide back and big clouds are stuck.", 60);

    final String label;
    final String desc;
    final int cooldownTicks;

    Ability(String label, String desc, int seconds) {
        this.label = label;
        this.desc = desc;
        this.cooldownTicks = seconds * 60;
    }
}
