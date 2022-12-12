package dev.hephaestus.proximity.mtg.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Color {
    WHITE("W"), BLUE("U"), BLACK("B"), RED("R"), GREEN("G");

    private static final List<List<Colors>> COMBINATIONS = Arrays.asList(
            List.of(new Colors(WHITE), new Colors(BLUE), new Colors(BLACK), new Colors(RED), new Colors(GREEN)),
            List.of(new Colors(WHITE, BLUE),
                    new Colors(WHITE, BLACK),
                    new Colors(WHITE, RED),
                    new Colors(WHITE, GREEN),
                    new Colors(BLUE, BLACK),
                    new Colors(BLUE, RED),
                    new Colors(BLUE, GREEN),
                    new Colors(BLACK, RED),
                    new Colors(BLACK, GREEN),
                    new Colors(RED, GREEN)
            ),
            List.of(new Colors(WHITE, BLUE, BLACK),
                    new Colors(WHITE, BLUE, RED),
                    new Colors(WHITE, BLUE, GREEN),
                    new Colors(WHITE, BLACK, RED),
                    new Colors(WHITE, BLACK, GREEN),
                    new Colors(WHITE, RED, GREEN),
                    new Colors(BLUE, BLACK, RED),
                    new Colors(BLUE, BLACK, GREEN),
                    new Colors(BLUE, RED, GREEN),
                    new Colors(BLACK, RED, GREEN)
            ),
            List.of(new Colors(WHITE, BLUE, BLACK, RED),
                    new Colors(WHITE, BLUE, RED, GREEN),
                    new Colors(WHITE, BLACK, RED, GREEN),
                    new Colors(WHITE, BLUE, BLACK, GREEN),
                    new Colors(BLUE, BLACK, RED, GREEN)
            ),
            List.of(new Colors(WHITE, BLUE, BLACK, RED, GREEN))
    );

    public final String initial;

    Color(String initial) {
        this.initial = initial;
    }

    public static int compare(Color color1, Color color2) {
        return switch (color1) {
            case WHITE -> switch(color2) {
                case BLUE, BLACK -> -1;
                case RED, GREEN -> 1;
                default -> 0;
            };
            case BLUE -> switch (color2) {
                case BLACK, RED -> -1;
                case WHITE, GREEN -> 1;
                default -> 0;
            };
            case BLACK -> switch (color2) {
                case RED, GREEN -> -1;
                case WHITE, BLUE -> 1;
                default -> 0;
            };
            case GREEN -> switch (color2) {
                case WHITE, BLUE -> -1;
                case BLACK, RED -> 1;
                default -> 0;
            };
            case RED -> switch (color2) {
                case GREEN, WHITE -> -1;
                case BLACK, BLUE -> 1;
                default -> 0;
            };
        };
    }

    private static void helper(List<int[]> combinations, int[] data, int start, int end, int index) {
        if (index == data.length) {
            int[] combination = data.clone();
            combinations.add(combination);
        } else if (start <= end) {
            data[index] = start;
            helper(combinations, data, start + 1, end, index + 1);
            helper(combinations, data, start + 1, end, index);
        }
    }

    public static Iterable<Colors> combinations(int size) {
        return size < 1 || size > 5 ? Collections.emptyList() : COMBINATIONS.get(size - 1);
    }
}
