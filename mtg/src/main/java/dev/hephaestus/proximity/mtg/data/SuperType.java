package dev.hephaestus.proximity.mtg.data;

import com.google.common.collect.ImmutableSet;

public enum SuperType {
    BASIC, ELITE, HOST, LEGENDARY, ONGOING, SNOW, WORLD;

    static ImmutableSet<SuperType> parse(String typeLine) {
        ImmutableSet.Builder<SuperType> builder = ImmutableSet.builder();

        for (String string : typeLine.split("\u2014")[0].split(" ")) {
            for (SuperType type : SuperType.values()) {
                if (type.name().equalsIgnoreCase(string)) {
                    builder.add(type);
                }
            }
        }

        return builder.build();
    }
}
