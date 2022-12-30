package dev.hephaestus.proximity.mtg.data;

import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.json.api.JsonString;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Colors implements Iterable<Color>, Colored, Observable {
    private static final Set<Color> MANA_COLORS = EnumSet.allOf(Color.class);
    private static final Map<String, Color> LAND_TYPES = Map.of(
            "Plains", Color.WHITE,
            "Island", Color.BLUE,
            "Swamp", Color.BLACK,
            "Mountain", Color.RED,
            "Forest", Color.GREEN
    );

    private final List<InvalidationListener> listeners = new ArrayList<>();

    private boolean hybrid;

    private List<Color> colors;

    public Colors(List<Color> colors, boolean hybrid) {
        colors.sort(Color::compare);
        this.colors = colors;
        this.hybrid = hybrid;
    }

    public Colors(Color... colors) {
        this(Arrays.asList(colors), false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Colors colors1 = (Colors) o;
        return colors.equals(colors1.colors);
    }

    @NotNull
    @Override
    public Iterator<Color> iterator() {
        return this.colors.iterator();
    }

    @Override
    public boolean isColorless() {
        return this.colors.isEmpty();
    }

    @Override
    public boolean isWhite() {
        return this.colors.contains(Color.WHITE);
    }

    @Override
    public boolean isBlue() {
        return this.colors.contains(Color.BLUE);
    }

    @Override
    public boolean isBlack() {
        return this.colors.contains(Color.BLACK);
    }

    @Override
    public boolean isRed() {
        return this.colors.contains(Color.RED);
    }

    @Override
    public boolean isGreen() {
        return this.colors.contains(Color.GREEN);
    }

    public int count() {
        return this.colors.size();
    }

    @Override
    public boolean isHybrid() {
        return this.hybrid;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (Color color : this.colors) {
            builder.append(color.initial);
        }

        return builder.toString();
    }

    private static boolean isRare(String rarity) {
        return rarity.equals("rare") || rarity.equals("mythic");
    }

    public static Colors parse(JsonObject card) {
        JsonArray printedColors = card.get("colors");
        JsonString typeLine = card.get("type_line");
        JsonArray producedMana = card.get("produced_mana");
        JsonString oracleText = card.get("oracle_text");
        JsonString manaCost = card.get("mana_cost");
        JsonString rarity = card.get("rarity");

        Colors colors = new Colors();

        colors.parse(printedColors, typeLine, producedMana, oracleText, manaCost, rarity);

        printedColors.addListener(observable -> colors.parse(printedColors, typeLine, producedMana, oracleText, manaCost, rarity));
        typeLine.addListener(observable -> colors.parse(printedColors, typeLine, producedMana, oracleText, manaCost, rarity));
        producedMana.addListener(observable -> colors.parse(printedColors, typeLine, producedMana, oracleText, manaCost, rarity));
        oracleText.addListener(observable -> colors.parse(printedColors, typeLine, producedMana, oracleText, manaCost, rarity));
        manaCost.addListener(observable -> colors.parse(printedColors, typeLine, producedMana, oracleText, manaCost, rarity));
        rarity.addListener(observable -> colors.parse(printedColors, typeLine, producedMana, oracleText, manaCost, rarity));

        return colors;
    }

    private void parse(JsonArray printedColors, JsonString typeLine, JsonArray producedMana, JsonString oracleText, JsonString manaCost, JsonString rarity) {
        List<Color> colors = new ArrayList<>();
        Color[] cs = Color.values();

        if (printedColors != null) {
            for (JsonElement element : printedColors) {
                if (element instanceof JsonString s) {
                    String string = s.get();

                    for (Color color : cs) {
                        if (color.initial.equalsIgnoreCase(string)) {
                            colors.add(color);
                        }
                    }
                }
            }
        }

        if (typeLine.get().toLowerCase(Locale.ROOT).contains("land")) {
            if (producedMana != null) {
                for (var element : producedMana) {
                    String string = ((JsonString) element).get();

                    if (MANA_COLORS.stream().map(Color::name).anyMatch(c -> c.equals(string))) {
                        for (Color color : cs) {
                            if (color.initial.equalsIgnoreCase(string)) {
                                colors.add(color);
                            }
                        }
                    }
                }
            }

            if (oracleText != null) {
                String oracle = oracleText.get();

                for (var entry : LAND_TYPES.entrySet()) {
                    if (oracle.contains(entry.getKey())) {
                        colors.add(entry.getValue());
                    }
                }

                if (oracle.contains("of any color") || (oracle.contains("Search your library for a basic land card, put it onto the battlefield") && isRare(rarity.get()))) {
                    colors.addAll(LAND_TYPES.values());
                }

                for (Color color : MANA_COLORS) {
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

        colors.sort(Color::compare);

        this.hybrid = false;

        if (colors.size() == 2) {
            hybrid = true;

            if (manaCost != null && !manaCost.get().isEmpty()) {
                String mc = manaCost.get();

                if (mc.contains("{W}") || mc.contains("{U}") || mc.contains("{B}") || mc.contains("{R}") || mc.contains("{G}")) {
                    hybrid = false;
                }
            }
        }

        this.colors = colors;

        for (InvalidationListener listener : this.listeners) {
            listener.invalidated(this);
        }
    }

    @Override
    public void addListener(InvalidationListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        this.listeners.remove(listener);
    }
}
