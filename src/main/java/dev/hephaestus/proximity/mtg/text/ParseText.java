package dev.hephaestus.proximity.mtg.text;

import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.api.tasks.TextFunction;
import dev.hephaestus.proximity.mtg.MTGValues;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.TextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class ParseText implements TextFunction {
    private final String newLine;
    private final boolean italicByDefault;

    protected ParseText(String newLine, boolean italicByDefault) {
        this.newLine = newLine;
        this.italicByDefault = italicByDefault;
    }

    @Override
    public final List<List<TextComponent>> apply(String input, JsonObject data, Function<String, Style> styles, Style baseStyle) {
        Job job = new Job(input, data, styles, baseStyle);
        int i, n = input.codePointCount(0, input.length());

        for (i = 0; i < n; ++i) {
            int c = input.codePointAt(i);

            switch (c) {
                case '(' -> {
                    if (MTGValues.REMINDER_TEXT.get(data)) {
                        job.italic = true;
                        job.append(c);
                    } else {
                        while (c != ')') {
                            c = input.charAt(++i);
                        }
                    }
                }
                case ')' -> {
                    job.append(c);
                    job.completeWord();
                    job.italic = this.italicByDefault;
                }
                case '*' -> {
                    job.completeWord();
                    job.italic = !job.italic;
                }
                case '\n' -> {
                    String word = job.completeWord();

                    if (word.length() > 0 || job.result.size() > 0) {
                        job.result.add(Collections.singletonList(new TextComponent.Literal(
                                job.italic ? job.italicStyle : job.baseStyle,
                                this.newLine
                        )));
                    }
                }
                case ' ' -> {
                    job.append(c);
                    job.completeWord();
                }
                case '"' -> {
                    if (job.currentWord.length() == 0 || job.currentWord.charAt(job.currentWord.length() - 1) == '(') {
                        job.append("\u201C");
                    } else {
                        job.append("\u201D");
                    }
                }
                case 0x2014 /* em dash */ -> {
                    if (i > 0 && i < n - 1 && job.input.codePointAt(i - 1) == ' ' && job.input.codePointAt(i + 1) == ' ') {
                        boolean italic = true;

                        String s = job.input.substring(0, i);
                        int nl = s.lastIndexOf('\n');
                        int bp = s.lastIndexOf(Character.toString(0x2022));

                        if (nl >= bp) {
                            for (int j = i + 1; j < n; ++j) {
                                if (job.input.charAt(j) == '(') {
                                    italic = false;
                                    break;
                                } else if (job.input.charAt(j) == '\n') {
                                    break;
                                }
                            }
                        } else {
                            String word = job.input.substring(bp + 1, i).trim();

                            if (job.input.substring(0, nl).contains(word)) {
                                italic = false;
                            }
                        }

                        if (italic) {
                            loop: for (int j = job.result.size() - 1; j >= 0; --j) {
                                List<TextComponent> list = job.result.get(j);

                                for (int k = list.size() - 1; k >= 0; --k) {
                                    TextComponent component = list.get(k);

                                    if (component.string().equals(this.newLine)) {
                                        break loop;
                                    } else {
                                        list.set(k, new TextComponent.Literal(component.style().italic(), component.string()));
                                    }
                                }
                            }
                        }
                    }

                    job.append(c);
                }
                default -> job.append(c);
            }
        }

        job.completeWord();

        return job.result;
    }

    private final class Job {
        final String input;
        final JsonObject data;
        final Function<String, Style> styles;
        final Style baseStyle;
        final Style italicStyle;
        final List<List<TextComponent>> result;
        List<TextComponent> currentGroup = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();
        boolean italic = ParseText.this.italicByDefault;

        private Job(String input, JsonObject data, Function<String, Style> styles, Style baseStyle) {
            this.input = input;
            this.data = data;
            this.styles = styles;
            this.baseStyle = baseStyle;
            this.result = new ArrayList<>();
            this.italicStyle = baseStyle.italic();
        }

        void append(String s) {
            this.currentWord.append(s);
        }

        void append(int c) {
            this.append(Character.toString(c));
        }

        String completeWord() {
            String word = this.currentWord.toString();

            if (word.length() > 0 || this.currentGroup.size() > 0) {
                this.currentGroup.add(new TextComponent.Literal(
                        this.italic ? this.italicStyle : this.baseStyle,
                        word
                ));

                this.result.add(this.currentGroup);

                this.currentWord = new StringBuilder();
                this.currentGroup = new ArrayList<>();
            }

            return word;
        }
    }
}
