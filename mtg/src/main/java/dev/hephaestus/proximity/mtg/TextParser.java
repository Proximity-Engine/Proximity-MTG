package dev.hephaestus.proximity.mtg;

import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.text.Word;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TextParser {
    private final MTGTemplate template;
    private final String newLine;
    private final boolean italicByDefault;
    private final String contextName;
    private final Transform[] transforms;

    public TextParser(MTGTemplate template, String newLine, boolean italicByDefault, String contextName, Transform... transforms) {
        this.template = template;
        this.newLine = newLine;
        this.italicByDefault = italicByDefault;
        this.contextName = contextName;
        this.transforms = transforms;
    }

    public ObservableList<Word> apply(String input, Card card, TextStyle baseStyle) {
        for (Transform transform : this.transforms) {
            input = transform.transform(card, input);
        }

        Job job = new Job(input, baseStyle);
        int i, n = input.codePointCount(0, input.length());

        characters: for (i = 0; i < n; ++i) {
            int c = input.codePointAt(i);

            symbols: for (var entry : this.template.symbols.symbolsByContext.getOrDefault(this.contextName, Collections.emptyMap()).entrySet()) {
                for (int j = 0; j < entry.getKey().length() && j + i < n; ++j) {
                    if (entry.getKey().charAt(j) != input.codePointAt(j + i)) {
                        continue symbols;
                    }
                }

                job.completeWord();
                entry.getValue().forEach(component -> {
                    job.currentGroup.add(new TextComponent(component.style.derive(job.baseStyle), component.text));
                });
                i += entry.getKey().length();
                continue characters;
            }

            symbols: for (var entry : this.template.symbols.defaultSymbols.entrySet()) {
                for (int j = 0; j < entry.getKey().length() && j + i < n; ++j) {
                    if (entry.getKey().charAt(j) != input.codePointAt(j + i)) {
                        continue symbols;
                    }
                }

                job.completeWord();
                entry.getValue().forEach(component -> {
                    job.currentGroup.add(new TextComponent(component.style.derive(job.baseStyle), component.text));
                });
                i += entry.getKey().length() - 1;
                continue characters;
            }

            switch (c) {
                case '(' -> {
                    if (card.getOption(MTGOptions.SHOW_REMINDER_TEXT).getValue()) {
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
                        List<TextComponent> list = new ArrayList<>();

                        list.add(new TextComponent(
                                job.baseStyle,
                                input.codePointAt(i + 1) == 0x2022 /* Bullet points */ ? "\n" : this.newLine,
                                job.italic
                        ));

                        job.result.add(list);
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

                                    if (component.text.equals(this.newLine)) {
                                        break loop;
                                    } else {
                                        list.set(k, new TextComponent(component.style, component.text, !component.italic));
                                    }
                                }
                            }
                        }
                    }

                    job.append(c);
                }
                default -> {
                    job.append(c);

                    if (Character.isIdeographic(c) || c == 0x3002 || c == 0x3001) {
                        job.completeWord();
                    }
                }
            }
        }

        job.completeWord();

        ObservableList<Word> words = FXCollections.observableArrayList();

        for (var list : job.result) {
            words.add(new Word(list));
        }

        return words;
    }

    private final class Job {
        final String input;
        final TextStyle baseStyle;
        final List<List<TextComponent>> result;
        List<TextComponent> currentGroup = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();
        boolean italic = TextParser.this.italicByDefault;

        private Job(String input, TextStyle baseStyle) {
            this.input = input;
            this.baseStyle = baseStyle;
            this.result = new ArrayList<>();
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
                if (word.length() > 0) {
                    this.currentGroup.add(new TextComponent(
                            this.baseStyle,
                            word,
                            this.italic
                    ));

                    this.currentWord = new StringBuilder();
                }

                this.result.add(this.currentGroup);

                this.currentGroup = new ArrayList<>();
            }

            return word;
        }
    }

    interface Transform {
        String transform(Card card, String input);
    }
}
