package dev.hephaestus.proximity.mtg;

import dev.hephaestus.proximity.app.api.logging.Log;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.text.Word;
import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.json.api.JsonString;
import dev.hephaestus.proximity.mtg.cards.CardFace;
import dev.hephaestus.proximity.mtg.cards.MultifacedCard;
import dev.hephaestus.proximity.mtg.cards.Planeswalker;
import dev.hephaestus.proximity.mtg.cards.SingleFacedCard;
import dev.hephaestus.proximity.mtg.data.Colors2;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Card extends RenderData {
    static final String[] KEYWORD_ABILITIES = new String[] {
            "deathtouch",
            "defender",
            "double strike",
            "first strike",
            "flying",
            "haste",
            "hexproof",
            "indestructible",
            "lifelink",
            "reach",
            "shroud",
            "trample",
            "vigilance",
            "menace"
    };

    private final Colors2 colors;

    private final Map<String, SimpleBooleanProperty> typeProperties = new HashMap<>();
    private final Map<String, SimpleBooleanProperty> frameEffectProperties = new HashMap<>();

    public Card(JsonObject json) {
        super(json);

        this.colors = new Colors2(json);

        JsonString oracleText = json.get("oracle_text");
        JsonString flavorText = json.get("flavor_text");
    }

    public Colors2 colors() {
        return colors;
    }

    public ReadOnlyBooleanProperty isLegendary() {
        return this.isType("legendary");
    }

    public ReadOnlyBooleanProperty isLand() {
        return this.isType("land");
    }

    public ReadOnlyBooleanProperty isCreature() {
        return this.isType("creature");
    }

    public ReadOnlyBooleanProperty isArtifact() {
        return this.isType("artifact");
    }

    public ReadOnlyBooleanProperty isEnchantment() {
        return this.isType("enchantment");
    }

    public ReadOnlyBooleanProperty isPlaneswalker() {
        return this.isType("planeswalker");
    }

    public ReadOnlyBooleanProperty isInstant() {
        return this.isType("instant");
    }

    public ReadOnlyBooleanProperty isSorcery() {
        return this.isType("sorcery");
    }

    public ReadOnlyBooleanProperty isType(String type) {
        return this.typeProperties.computeIfAbsent(type, t -> {
            SimpleBooleanProperty property = new SimpleBooleanProperty(null, type);
            JsonString typeLine = this.json.get("type_line");

            property.bind(Bindings.createBooleanBinding(() -> typeLine.get().toLowerCase(Locale.ROOT).contains(t), typeLine));

            return property;
        });
    }

    public final ObservableBooleanValue hasFrameEffect(String effect) {
        return this.frameEffectProperties.computeIfAbsent(effect, e -> {
            JsonArray frameEffects = this.json.get("frame_effects");
            SimpleBooleanProperty property = new SimpleBooleanProperty(null, effect);

            if (frameEffects != null) {
                property.bind(Bindings.createBooleanBinding(() -> frameEffects.contains(e), frameEffects));
            }

            return property;
        });
    }

    public static List<Card> parse(JsonObject json, Log log) {
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

                List<Card> result = new ArrayList<>(card.numberOfFaces() + 1);

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
}
