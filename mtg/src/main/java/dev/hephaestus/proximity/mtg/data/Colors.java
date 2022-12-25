package dev.hephaestus.proximity.mtg.data;

import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.json.api.JsonString;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class Colors implements Iterable<Color>, Colored {
    private static final Set<Color> MANA_COLORS = EnumSet.allOf(Color.class);
    private static final Map<String, Color> LAND_TYPES = Map.of(
            "Plains", Color.WHITE,
            "Island", Color.BLUE,
            "Swamp", Color.BLACK,
            "Mountain", Color.RED,
            "Forest", Color.GREEN
    );

    private final List<Color> colors;
    private final boolean hybrid;

    public Colors(List<Color> colors, boolean hybrid) {
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
        List<Color> colors = new ArrayList<>();
        Color[] cs = Color.values();

        if (card.has("colors")) {
            for (JsonElement element : card.getArray("colors")) {
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

        if (card.getString("type_line").toLowerCase(Locale.ROOT).contains("land")) {
            if (card.has("produced_mana")) {
                for (var element : card.getArray("produced_mana")) {
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

            if (card.has("oracle_text")) {
                String oracle = card.getString("oracle_text");

                for (var entry : LAND_TYPES.entrySet()) {
                    if (oracle.contains(entry.getKey())) {
                        colors.add(entry.getValue());
                    }
                }

                if (oracle.contains("of any color") || (oracle.contains("Search your library for a basic land card, put it onto the battlefield") && isRare(card.getString("rarity")))) {
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

        boolean hybrid = false;

        if (colors.size() == 2) {
            hybrid = true;

            if (card.has("mana_cost") && !card.getString("mana_cost").isEmpty()) {
                String manaCost = card.getString("mana_cost");

                if (manaCost.contains("{W}") || manaCost.contains("{U}") || manaCost.contains("{B}") || manaCost.contains("{R}") || manaCost.contains("{G}")) {
                    hybrid = false;
                }
            }
        }


        return new Colors(new ArrayList<>(colors), hybrid);
    }
}
