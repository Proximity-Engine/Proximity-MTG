package dev.hephaestus.proximity.mtg.data;

import java.util.Set;
import java.util.TreeSet;

public enum SuperType {
    BASIC, ELITE, HOST, LEGENDARY, ONGOING, SNOW, WORLD;

    static Set<SuperType> parse(String typeLine) {
        Set<SuperType> types = new TreeSet<>();

        for (String string : typeLine.split("\u2014")[0].split(" ")) {
            for (SuperType type : SuperType.values()) {
                if (type.name().equalsIgnoreCase(string)) {
                    types.add(type);
                }
            }
        }

        return types;
    }
}
