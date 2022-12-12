package dev.hephaestus.proximity.mtg.cards;

import dev.hephaestus.proximity.app.api.logging.Log;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.mtg.data.Colored;
import dev.hephaestus.proximity.mtg.data.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface MagicCard extends Colored {
    static List<BaseMagicCard> parse(JsonObject json, Log log) {
        switch (json.getString("layout")) {
            case "normal" -> {

                if (json.getString("type_line").contains("Planeswalker")) {
                    return Collections.singletonList(new Planeswalker(json));
                } else {
                    return Collections.singletonList(new SingleFacedCard(json));
                }
            }
            case "transform", "modal_dfc", "split", "adventure", "flip" -> {
                MultifacedCard card = new MultifacedCard(json);

                List<BaseMagicCard> result = new ArrayList<>(card.numberOfFaces() + 1);

                result.add(card);

                for (CardFace face : card) {
                    result.add(face);
                }

                return result;
            }
            default -> {
                log.print(String.format("WARNING: Unexpected layout '%s' for card '%s'", json.getString("layout"), json.getString("name")));

                return Collections.singletonList(new SingleFacedCard(json));
            }
        }
    }

    JsonObject json();

    String getName();
    String getTypeLine();
    int getManaValue();

    boolean is(String type);

    // Builtin checks for specific types for convenience
    boolean isLand();
    boolean isCreature();
    boolean isArtifact();
    boolean isEnchantment();
    boolean isPlaneswalker();
    boolean isInstant();
    boolean isSorcery();

    Colors colors();

    String getPower();
    String getToughness();

    boolean hasFrameEffect(String effect);
}
