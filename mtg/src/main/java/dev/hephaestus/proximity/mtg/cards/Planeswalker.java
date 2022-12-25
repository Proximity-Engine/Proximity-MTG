package dev.hephaestus.proximity.mtg.cards;

import dev.hephaestus.proximity.json.api.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Planeswalker extends BaseMagicCard {
    private static final Pattern ABILITY = Pattern.compile("^(?<cost>([+\u2212][0-9X]+)|(0)): (?<effect>.+)");

    private final List<Ability> abilities;
    private final int abilitiesLength;

    public Planeswalker(JsonObject json) {
        super(json);

        List<Ability> abilities = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int abilitiesLength = 0;

        for (String string : json.getString("oracle_text").split("\n")) {
            Matcher matcher = ABILITY.matcher(string);

            if (matcher.matches()) {
                if (!builder.isEmpty()) {
                    abilitiesLength += builder.length();
                    abilities.add(new Ability(null, builder.toString()));
                    builder = new StringBuilder();
                }

                abilitiesLength += matcher.group("effect").length();
                abilities.add(new Ability(matcher.group("cost"), matcher.group("effect")));
            } else {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }

                builder.append(string);
            }
        }

        if (!builder.isEmpty()) {
            abilitiesLength += builder.length();
            abilities.add(new Ability(null, builder.toString()));
        }

        this.abilities = abilities;
        this.abilitiesLength = abilitiesLength;
    }

    public List<Ability> getAbilities() {
        return this.abilities;
    }

    public final class Ability {
        private final String cost, text;
        private final Type type;

        private Ability(String cost, String text) {
            this.cost = cost;
            this.text = text;
            this.type = cost == null ? Type.STATIC : switch (cost.substring(0, 1)) {
                case "+" -> Type.PLUS;
                case "\u2212" -> Type.MINUS;
                default -> Type.ZERO;
            };
        }

        public boolean hasCost() {
            return this.cost != null;
        }

        public String getCost() {
            return this.cost;
        }

        public String getText() {
            return this.text;
        }

        public Type getType() {
            return this.type;
        }

        public double getArea() {
            float basePortion = 1F / abilities.size();
            float portion = ((float) this.text.length()) / abilitiesLength;

            // Trend towards equal distributions.
            return (portion / 2) + (basePortion / 2);
        }

        public enum Type {
            STATIC, PLUS, ZERO, MINUS;
        }
    }

}
