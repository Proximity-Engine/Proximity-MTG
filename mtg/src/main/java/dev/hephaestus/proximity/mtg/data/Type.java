package dev.hephaestus.proximity.mtg.data;

import com.google.common.collect.ImmutableSet;

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

    static ImmutableSet<Type> parse(String typeLine) {
        ImmutableSet.Builder<Type> builder = ImmutableSet.builder();

        for (String string : typeLine.split("\u2014")[0].split(" ")) {
            for (Type type : Type.values()) {
                if (type.name().equalsIgnoreCase(string)) {
                    builder.add(type);
                }
            }
        }

        return builder.build();
    }
}
