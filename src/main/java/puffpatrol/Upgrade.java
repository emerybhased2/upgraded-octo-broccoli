package puffpatrol;

import java.util.function.Consumer;

record Upgrade(String name, int cost, String desc, Consumer<Tower> effect) {
}
