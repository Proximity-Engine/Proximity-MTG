package dev.hephaestus.proximity.mtg.preparation;

import dev.hephaestus.proximity.api.DataSet;
import dev.hephaestus.proximity.api.TaskScheduler;
import dev.hephaestus.proximity.api.Values;
import dev.hephaestus.proximity.api.json.JsonArray;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.api.tasks.DataFinalization;
import dev.hephaestus.proximity.mtg.ArtSource;
import dev.hephaestus.proximity.mtg.MTGValues;
import dev.hephaestus.proximity.util.Pair;

import java.util.ArrayList;
import java.util.List;

public final class Miscellaneous {
    public static final String FILE_CHARS = "[^a-zA-Z0-9.,'& ()-]";

    private static void parseFace(JsonObject cardFace, String face) {
        JsonArray path = new JsonArray();

        path.add(face + "s");

        StringBuilder fileName = new StringBuilder();

        if (MTGValues.SAVE_FILE_WITH_CARD_NUMBER.get(cardFace)) {
            fileName.append(Values.ITEM_NUMBER.get(cardFace));
            fileName.append(face.equals("front") ? "a" : "b");
            fileName.append(" ");
        }

        fileName.append(cardFace.getAsString("name").replaceAll(FILE_CHARS, " "));
        path.add(fileName.toString());
        Values.PATH.set(cardFace, path);
        MTGValues.FRONT_FACE.set(cardFace, face.equals("front"));
        MTGValues.IS_ORIGINAL_TWO_SIDED_CARD.set(cardFace, false);
    }

    private static Pair<JsonObject, JsonObject> parseTwoSidedCard(List<Runnable> tasks, JsonObject card, TaskScheduler scheduler, DataSet cards, JsonObject overrides) {
        JsonArray faces = card.getAsJsonArray("card_faces");
        JsonObject front = card.deepCopy().copyAll(faces.get(0).getAsJsonObject());
        JsonObject back = card.deepCopy().copyAll(faces.get(1).getAsJsonObject());

        parseFace(front, "front");
        parseFace(back, "back");

        scheduler.submit(DataFinalization.DEFINITION, () -> {
            MTGValues.FLIPPED.set(back, front.deepCopy());
            MTGValues.FLIPPED.set(front, back.deepCopy());
        });

        return new Pair<>(front, back);
    }

    public static void split(TaskScheduler scheduler, DataSet cards, JsonObject overrides) {
        List<Runnable> tasks = new ArrayList<>();

        for (JsonObject card : cards) {
            if (card.has("card_faces")) {
                Pair<JsonObject, JsonObject> faces = parseTwoSidedCard(tasks, card, scheduler, cards, overrides);

                if (MTGValues.REMOVE_ORIGINAL_CARD.get(card)) {
                    tasks.add(() -> cards.remove(card));
                } else {
                    MTGValues.IS_ORIGINAL_TWO_SIDED_CARD.set(card, true);
                    MTGValues.ART_SOURCE.set(card, ArtSource.NONE);

                    JsonArray path = new JsonArray();

                    path.add("substitutions");

                    StringBuilder fileName = new StringBuilder();

                    if (MTGValues.SAVE_FILE_WITH_CARD_NUMBER.get(card)) {
                        fileName.append(Values.ITEM_NUMBER.get(card)).append("s").append(" ");
                    }


                    fileName.append(card.getAsString("name").replace("//", "&").replaceAll(FILE_CHARS, " "));
                    path.add(fileName.toString());
                    Values.PATH.set(card, path);

                    JsonArray facesArray = card.getAsJsonArray("card_faces");

                    facesArray.clear();

                    facesArray.add(0, faces.left());
                    facesArray.add(1, faces.right());
                }

                MTGValues.DOUBLE_SIDED.set(card, true);
                tasks.add(() -> cards.add(faces.left(), faces.right()));
            } else {
                JsonArray frontPath = new JsonArray();

                frontPath.add("fronts");
                frontPath.add(Values.ITEM_NUMBER.get(card) + " " + card.getAsString("name").replaceAll(FILE_CHARS, " "));
                MTGValues.IS_ORIGINAL_TWO_SIDED_CARD.set(card, false);

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

            if (types.contains("land") && card.has("oracle_text")) {
                for (String line : card.getAsString("oracle_text").split("\n")) {
                    if (line.contains("Add {")) {
                        MTGValues.MANA_ABILITY.set(card, line);
                    }
                }
            }

            card.copyAll(overrides);
        }
    }
}