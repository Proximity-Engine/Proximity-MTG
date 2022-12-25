package dev.hephaestus.proximity.mtg.data;

import java.util.Set;
import java.util.TreeSet;

public enum Type {
    ARTIFACT,
    CONSPIRACY,
    CREATURE,
    DUNGEON,
    ENCHANTMENT,
    INSTANT,
    LAND,
    PHENOMENON,
    PLANE,
    PLANESWALKER,
    SCHEME,
    SORCERY,
    TRIBAL,
    VANGUARD;

    static Set<Type> parse(String typeLine) {
        Set<Type> types = new TreeSet<>();

        for (String string : typeLine.split("\u2014")[0].split(" ")) {
            for (Type type : Type.values()) {
                if (type.name().equalsIgnoreCase(string)) {
                    types.add(type);
                }
            }
        }

        return types;
    }
}
