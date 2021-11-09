package dev.hephaestus.proximity.mtg.preparation;

import dev.hephaestus.proximity.api.DataSet;
import dev.hephaestus.proximity.api.TaskScheduler;
import dev.hephaestus.proximity.api.json.JsonArray;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.mtg.MTGValues;

import java.util.Locale;
import java.util.Set;

public final class TypeParsing {
    private static final Set<String> MAIN_TYPES = Set.of(
            "enchantment",
            "artifact",
            "land",
            "creature",
            "conspiracy",
            "instant",
            "phenomenon",
            "plane",
            "planeswalker",
            "scheme",
            "sorcery",
            "tribal",
            "vanguard"
    );

    private TypeParsing() {
    }

    public static void apply(TaskScheduler scheduler, DataSet cards, JsonObject overrides) {
        for (JsonObject card : cards) {
            JsonArray types = MTGValues.TYPES.get(card);
            String typeLine = card.getAsString("type_line");
            int mainTypeCount = 0;
            StringBuilder mainTypes = new StringBuilder();

            if (typeLine.contains("\u2014")) {
                mainTypes.append(typeLine.split("\u2014")[1]);
            }

            for (String string : typeLine.split("\u2014")) {
                String[] split = string.split(" ");

                for (int i = 0; i < split.length; i++) {
                    String s = split[i];
                    String type = s.toLowerCase(Locale.ROOT);

                    types.add(type);

                    if (MAIN_TYPES.contains(type)) {
                        mainTypeCount++;

                        if (!typeLine.contains("\u2014")) {
                            mainTypes.append(s);

                            if (i < split.length - 1) {
                                mainTypes.append(" ");
                            }
                        }
                    }
                }
            }

            MTGValues.MAIN_TYPES.set(card, mainTypes.toString());
            MTGValues.TYPE_COUNT.set(card, mainTypeCount);
        }
    }
}
