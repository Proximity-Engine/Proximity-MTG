package dev.hephaestus.proximity.mtg.preparation;

import dev.hephaestus.proximity.api.DataSet;
import dev.hephaestus.proximity.api.TaskScheduler;
import dev.hephaestus.proximity.api.Values;
import dev.hephaestus.proximity.api.json.JsonArray;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.api.tasks.DataFinalization;
import dev.hephaestus.proximity.mtg.MTGValues;

import java.util.ArrayList;
import java.util.List;

public final class Miscellaneous {
    public static final String FILE_CHARS = "[^a-zA-Z0-9.,'& ()]";

    private static void parseFace(JsonObject cardFace, String face) {
        JsonArray path = new JsonArray();

        path.add(face + "s");

        StringBuilder fileName = new StringBuilder();

        if (MTGValues.SAVE_FILE_WITH_CARD_NUMBER.get(cardFace)) {
            fileName.append(Values.ITEM_NUMBER.get(cardFace)).append(" ");
        }

        fileName.append(cardFace.getAsString("name").replaceAll(FILE_CHARS, " "));
        path.add(fileName.toString());
        Values.PATH.set(cardFace, path);
        MTGValues.FRONT_FACE.set(cardFace, face.equals("front"));
    }

    private static void parseTwoSidedCard(List<Runnable> tasks, JsonObject card, TaskScheduler scheduler, DataSet cards, JsonObject overrides) {
        JsonArray faces = card.getAsJsonArray("card_faces");
        JsonObject front = card.deepCopy().copyAll(faces.get(0).getAsJsonObject());
        JsonObject back = card.deepCopy().copyAll(faces.get(1).getAsJsonObject());

        parseFace(front, "front");
        parseFace(back, "back");

        scheduler.submit(DataFinalization.DEFINITION, () -> {
            MTGValues.FLIPPED.set(back, front.deepCopy());
            MTGValues.FLIPPED.set(front, back.deepCopy());
        });

        tasks.add(() -> cards.add(front, back));
    }

    public static void split(TaskScheduler scheduler, DataSet cards, JsonObject overrides) {
        List<Runnable> tasks = new ArrayList<>();

        for (JsonObject card : cards) {
            if (card.has("card_faces")) {
                tasks.add(() -> cards.remove(card));
                MTGValues.DOUBLE_SIDED.set(card, true);
                parseTwoSidedCard(tasks, card, scheduler, cards, overrides);
            } else {
                JsonArray frontPath = new JsonArray();

                frontPath.add("fronts");
                frontPath.add(Values.ITEM_NUMBER.get(card) + " " + card.getAsString("name").replaceAll(FILE_CHARS, " "));

                Values.PATH.set(card, frontPath);
                MTGValues.DOUBLE_SIDED.set(card, false);

                if (MTGValues.USE_CARD_BACK.get(card)) {
                    JsonArray backPath = new JsonArray();

                    backPath.add("backs");
                    backPath.add(Values.ITEM_NUMBER.get(card) + " " + card.getAsString("name").replaceAll(FILE_CHARS, " "));

                    JsonObject back = card.deepCopy();

                    back.addProperty("name", back.getAsString("name") + " (Back)");
                    Values.PATH.set(back, backPath);
                    MTGValues.IS_CARD_BACK.set(back, true);

                    tasks.add(() -> cards.add(back));
                }
            }
        }

        tasks.forEach(Runnable::run);
    }

    public static void apply(TaskScheduler scheduler, DataSet cards, JsonObject overrides) {
        for (JsonObject card : cards) {
            if (card.getAsJsonArray("keywords").contains("mutate")) {
                String[] split = card.getAsString("oracle_text").split("\n", 2);
                card.addProperty("oracle_text", split[1]);
                MTGValues.MUTATE_TEXT.set(card, split[0]);
            }

            if (!MTGValues.SET_SYMBOL.exists(card)) {
                MTGValues.SET_SYMBOL.set(card, card.getAsString("set"));
            }

            if (card.has("power")) {
                card.addProperty("power", card.getAsString("power").replaceAll("\\*", "\u2605"));
            }

            if (card.has("toughness")) {
                card.addProperty("toughness", card.getAsString("toughness").replaceAll("\\*", "\u2605"));
            }

            JsonArray types = MTGValues.TYPES.get(card);

            if (types.contains("basic") && card.has("oracle_text")) {
                String oracle = card.getAsString("oracle_text");

                if (oracle.startsWith("(") && oracle.endsWith(")") && !oracle.contains("\n")) {
                    card.remove("oracle_text");
                }
            }

            card.copyAll(overrides);
        }
    }
}
