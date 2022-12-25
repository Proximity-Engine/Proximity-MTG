package dev.hephaestus.proximity.mtg.cards;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.mtg.data.Colors;

import java.util.Locale;

public class BaseMagicCard extends RenderJob<JsonObject> implements MagicCard {
    @Override
    public boolean isColorless() {
        return colors.isColorless();
    }

    @Override
    public boolean isWhite() {
        return colors.isWhite();
    }

    @Override
    public boolean isBlue() {
        return colors.isBlue();
    }

    @Override
    public boolean isBlack() {
        return colors.isBlack();
    }

    @Override
    public boolean isRed() {
        return colors.isRed();
    }

    @Override
    public boolean isGreen() {
        return colors.isGreen();
    }

    private final Colors colors;

    public BaseMagicCard(JsonObject json) {
        super(json);
        this.colors = Colors.parse(this.json);
    }

    @Override
    public String getName() {
        return this.json.getString("name");
    }

    @Override
    public JsonObject json() {
        return this.json;
    }

    @Override
    public final String getTypeLine() {
        return this.json.getString("type_line");
    }

    @Override
    public final int getManaValue() {
        return this.json.getInt("cmc");
    }

    @Override
    public final boolean is(String type) {
        return this.json.getString("type_line").toLowerCase(Locale.ROOT)
                .contains(type.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean isLand() {
        return this.is("land");
    }

    @Override
    public boolean isCreature() {
        return this.is("creature");
    }

    @Override
    public boolean isArtifact() {
        return this.is("artifact");
    }

    @Override
    public boolean isEnchantment() {
        return this.is("enchantment");
    }

    @Override
    public boolean isPlaneswalker() {
        return this.is("planeswalker");
    }

    @Override
    public boolean isInstant() {
        return this.is("instant");
    }

    @Override
    public boolean isSorcery() {
        return this.is("sorcery");
    }

    @Override
    public final Colors colors() {
        return this.colors;
    }

    @Override
    public final boolean isHybrid() {
        return this.colors.isHybrid();
    }

    @Override
    public final String getPower() {
        return this.json.getString("power");
    }

    @Override
    public final String getToughness() {
        return this.json.getString("toughness");
    }

    @Override
    public final boolean hasFrameEffect(String effect) {
        return this.json.has("frame_effects") && this.json.getArray("frame_effects").contains(effect);
    }

    @Override
    public JsonElement toJson() {
        return this.json;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MagicCard card && this.json.getString("id").equals(card.json().getString("id"));
    }
}
