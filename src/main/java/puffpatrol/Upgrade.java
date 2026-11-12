package puffpatrol;

import java.util.function.Consumer;

/** One step on an upgrade path. Some tier-4 upgrades also grant an activated ability. */
record Upgrade(String name, int cost, String desc, Consumer<Tower> effect, Ability ability) {
    Upgrade(String name, int cost, String desc, Consumer<Tower> effect) {
        this(name, cost, desc, effect, null);
    }
}
