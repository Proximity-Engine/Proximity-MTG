package dev.hephaestus.proximity.mtg.preparation;

import dev.hephaestus.proximity.api.DataSet;
import dev.hephaestus.proximity.api.TaskScheduler;
import dev.hephaestus.proximity.api.json.JsonArray;
import dev.hephaestus.proximity.api.json.JsonElement;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.api.json.JsonPrimitive;
import dev.hephaestus.proximity.mtg.MTGValues;

import java.util.*;

public final class ColorParsing {
    private static final Set<String> MANA_COLORS = Set.of("W", "U", "B", "G", "R");
    private static final Map<String, String> LAND_TYPES = Map.of(
            "Plains", "W",
            "Island", "U",
            "Swamp", "B",
            "Mountain", "R",
            "Forest", "G"
    );

    private ColorParsing() {
    }

    private static boolean isRare(String rarity) {
        return rarity.equals("rare") || rarity.equals("mythic");
    }

    private static void processLand(JsonObject card, Set<String> colors) {
        String layout = card.getAsString("layout");

        if (!layout.equals("transform") && !layout.equals("modal_dfc")) {
            for (var element : card.getAsJsonArray("produced_mana")) {
                if (MANA_COLORS.contains(element.getAsString())) {
                    colors.add(element.getAsString());
                }
            }
        }

        if (card.has("oracle_text")) {
            String oracle = card.getAsString("oracle_text");

            for (var entry : LAND_TYPES.entrySet()) {
                if (oracle.contains(entry.getKey())) {
                    colors.add(entry.getValue());
                }
            }

            if (oracle.contains("of any color") || (oracle.contains("Search your library for a basic land card, put it onto the battlefield") && isRare(card.getAsString("rarity")))) {
                colors.addAll(MANA_COLORS);
            }

            for (String color : MANA_COLORS) {
                for (String line : oracle.split("\n")) {
                    if (line.contains("Add ")) {
                        var string = line.substring(line.indexOf("Add "));

                        if (string.contains("{" + color + "}")) {
                            colors.add(color);
                        }
                    }
                }
            }
        }
    }

    private static void processHybrid(JsonObject card, Set<String> colors) {
        boolean hybrid = false;

        if (colors.size() == 2) {
            hybrid = true;

            if (card.has("mana_cost") && !card.getAsString("mana_cost").isEmpty()) {
                String manaCost = card.getAsString("mana_cost");

                if (manaCost.contains("{W}") || manaCost.contains("{U}") || manaCost.contains("{B}") || manaCost.contains("{R}") || manaCost.contains("{G}")) {
                    hybrid = false;
                }
            }

        }

        MTGValues.HYBRID.set(card, hybrid);
    }

    public static void apply(TaskScheduler scheduler, DataSet cards, JsonObject overrides) {
        for (JsonObject card : cards) {
            Set<String> colors = new HashSet<>();
            JsonArray givenColors = card.getAsJsonArray("colors");

            for (JsonElement givenColor : givenColors) {
                colors.add(givenColor.getAsString());
            }

            if (MTGValues.TYPES.get(card).contains("land")) {
                processLand(card, colors);
            }

            processHybrid(card, colors);

            JsonArray colorList = new JsonArray();

            for (String color : colors) {
                colorList.add(new JsonPrimitive(color));
            }

            colorList.sort((c1, c2) -> switch (c1.getAsString()) {
                case "W" -> switch (c2.getAsString()) {
                    case "U", "B" -> -1;
                    case "R", "G" -> 1;
                    default -> 0;
                };
                case "U" -> switch (c2.getAsString()) {
                    case "B", "R" -> -1;
                    case "W", "G" -> 1;
                    default -> 0;
                };
                case "B" -> switch (c2.getAsString()) {
                    case "R", "G" -> -1;
                    case "W", "U" -> 1;
                    default -> 0;
                };
                case "G" -> switch (c2.getAsString()) {
                    case "W", "U" -> -1;
                    case "B", "R" -> 1;
                    default -> 0;
                };
                case "R" -> switch (c2.getAsString()) {
                    case "G", "W" -> -1;
                    case "B", "U" -> 1;
                    default -> 0;
                };
                default -> 0;
            });

            card.add("colors", colorList);
            MTGValues.COLOR_COUNT.set(card, colorList.size());
        }
    }
}
