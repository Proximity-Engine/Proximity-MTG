package dev.hephaestus.proximity.mtg;

import dev.hephaestus.proximity.app.api.rendering.Template;
import dev.hephaestus.proximity.app.api.rendering.elements.Group;
import dev.hephaestus.proximity.app.api.rendering.elements.Parent;
import dev.hephaestus.proximity.app.api.rendering.elements.Text;
import dev.hephaestus.proximity.app.api.rendering.elements.Textbox;
import dev.hephaestus.proximity.app.api.rendering.util.Alignment;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.text.Word;
import dev.hephaestus.proximity.json.api.JsonString;
import javafx.beans.InvalidationListener;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class MTGTemplate extends Template<Card> {
    private final TextParser oracleTextParser = new TextParser(this, "\n\n", false, "oracle", MTGTemplate::truncateFlash);
    private final TextParser flavorTextParser = new TextParser(this, "\n", true, "flavor");

    protected final SymbolMap symbols = new SymbolMap();

    public MTGTemplate() {
        this.addSymbols();
    }

    protected Rectangle artBounds() {
        return new Rectangle(239, 402, 1698, 1232);
    }

    protected TextStyle cardNameStyle() {
        return new TextStyle().setFontName("Beleren2016-Bold").setSize(9.6).setColor(Color.BLACK);
    }

    protected int cardNameX() {
        return 257;
    }

    protected int cardNameY() {
        return 345;
    }


    protected int manaCostX() {
        return 1938;
    }

    protected int manaCostY() {
        return 340;
    }

    protected TextStyle manaCostStyle() {
        return new TextStyle().setColor(Color.BLACK).setFontName("NDPMTG").setShadow(new TextStyle.Shadow(Color.BLACK, -4, 6)).setSize(11.0);
    }

    protected Alignment manaCostAlignment() {
        return Alignment.END;
    }

    protected TextStyle typeLineStyle() {
        return new TextStyle().setFontName("Beleren2016-Bold").setSize(8.0).setColor(Color.BLACK);
    }

    protected int typeLineX() {
        return 262;
    }

    protected int typeLineY() {
        return 1761;
    }


    protected Rectangle textboxBounds() {
        return new Rectangle(253, 1842, 1670, 794);
    }

    protected int spaceAboveFlavorBar() {
        return 40;
    }

    protected int spaceBelowFlavorBar() {
        return 40;
    }

    protected TextStyle powerAndToughnessStyle() {
        return new TextStyle().setSize(9.8).setColor(Color.BLACK).setFontName("Beleren2016-Bold");
    }

    protected int powerAndToughnessX() {
        return 1811;
    }

    protected int powerAndToughnessY() {
        return 2669;
    }

    protected Alignment powerAndToughnessAlignment() {
        return Alignment.CENTER;
    }

    protected Rectangle powerAndToughnessBounds() {
        return new Rectangle(1627, 2549, 368, 172);
    }

    protected void addArt(Card card, Group group) {
        group.image("art", image -> {
            var customArtPath = card.getOption(MTGOptions.CUSTOM_ART);
            JsonString artCrop = card.json.get("image_uris", "art_crop");

            if (customArtPath.getValue() == null) {
                image.source(new URL(artCrop.get()));
            } else {
                image.source(customArtPath.getValue().toUri().toURL());
            }

            var bounds = this.artBounds();
            image.cover(bounds.x, bounds.y, bounds.width, bounds.height, Alignment.CENTER, Alignment.CENTER);

            return all(customArtPath, artCrop);
        });
    }

    protected Text addManaCost(Card card, Parent parent) {
        return parent.text("mana_cost", text -> {
            text.pos(this.manaCostX(), this.manaCostY());
            text.style(this.manaCostStyle());
            text.alignment(this.manaCostAlignment());

            JsonString manaCost = card.json.get("mana_cost");

            if (manaCost != null && !manaCost.get().isEmpty()) {
                String[] symbols = manaCost.get().split("((?<=}))((?=\\{))");
                List<Word> words = new ArrayList<>(symbols.length);

                for (String symbol : symbols) {
                    words.add(this.symbols.getSymbol("mana_cost", symbol));
                }

                text.set(words);
            }

            return manaCost;
        });
    }

    protected void addNamePlateText(Card card, Parent parent) {
        var manaCost = this.addManaCost(card, parent);
        JsonString manaCostJson = card.json.get("mana_cost");

        parent.text("name", text -> {
            JsonString name = card.json.get("name");
            var style = this.cardNameStyle();

            text.set(name.get());
            text.pos(this.cardNameX(), this.cardNameY());
            text.style(style);

            while (!manaCost.bounds().isEmpty() && text.bounds().getMaxX() >= manaCost.bounds().getMinX()) {
                style.setSize(style.getSize() - 1);
            }

            return all(name, manaCostJson);
        });
    }

    protected void addTypeLine(Card card, Parent parent) {
        // TODO: Set Symbol

        parent.text("type_line", text -> {
            JsonString typeLine = card.json.get("type_line");

            text.set(typeLine.get());
            text.pos(this.typeLineX(), this.typeLineY());
            text.style(this.typeLineStyle());

            return typeLine;
        });
    }

    protected void addTextbox(Card card, Parent parent) {
        var oracleStyle = new TextStyle().setFontName("PlantinMTProRg").setItalicFontName("PlantinMTProRgIt").setSize(9.0).setColor(Color.BLACK);
        JsonString oracleText = card.json.get("oracle_text");
        var oracleDependencies = all(oracleText, card.isCreature(), card.isType("vehicle"));

        var oracleTextbox = parent.textbox("oracle", textbox -> {
            if (oracleText != null) {
                textbox.set(this.oracleTextParser.apply(oracleText.get(), card, oracleStyle));
            }

            textbox.visibility(oracleText != null);

            var bounds = this.textboxBounds();

            textbox.style(oracleStyle);
            textbox.pos(bounds.x, bounds.y, bounds.width, bounds.height);
            textbox.padding(10, 15, 20, 15);

            if (card.json.has("power") && card.json.has("toughness")) {
                textbox.wrap(this.powerAndToughnessBounds());
            }

            return oracleDependencies;
        });

        parent.group("flavor", flavor -> {
            JsonString flavorText = card.json.get("flavor_text");

            flavor.visibility(flavorText != null);

            var flavorBar = flavor.image("bar", image -> {
                var showFlavorBar = card.getOption(MTGOptions.FLAVOR_BAR);

                image.source("accents/flavor_bar");
                image.visibility(oracleTextbox.isVisible() && flavorText != null && showFlavorBar.getValue());
                image.pos(293, (int) oracleTextbox.bounds().getMaxY() + this.spaceAboveFlavorBar());

                return all(oracleDependencies, showFlavorBar, flavorText, oracleTextbox.boundsProperty());
            });

            var flavorTextbox = flavor.textbox("text", textbox -> {
                if (flavorText != null) {
                    textbox.set(this.flavorTextParser.apply(flavorText.get(), card, oracleStyle));
                }

                textbox.visibility(flavorText != null);
                var bounds = this.textboxBounds();
                var y = (int) (flavorBar.bounds().getMaxY() + this.spaceBelowFlavorBar());
                textbox.style(oracleStyle);
                textbox.pos(bounds.x, y, bounds.width, (int) (bounds.getMaxY() - y));
                textbox.padding(10, 10, 10, 10);

                if (card.json.has("power") && card.json.has("toughness")) {
                    textbox.wrap(this.powerAndToughnessBounds());
                }

                return all(oracleDependencies, flavorText, card.isCreature(), card.isType("vehicle"), oracleTextbox.boundsProperty());
            });

            InvalidationListener adjuster = flavorText != null
                    ? o -> this.adjustTextbox(oracleStyle, oracleTextbox, flavorTextbox)
                    : o -> this.adjustTextbox(oracleStyle, oracleTextbox);

            adjuster.invalidated(oracleTextbox.boundsProperty());
            oracleTextbox.boundsProperty().addListener(adjuster);
            flavorTextbox.boundsProperty().addListener(adjuster);
        });
    }

    private void adjustTextbox(TextStyle style, Textbox oracle) {
        var bounds = this.textboxBounds();
        var oracleBounds = oracle.bounds();

        double height = oracleBounds.getHeight();

        while (height > bounds.height) {
            style.setSize(style.getSize() - 0.1);

            oracleBounds = oracle.bounds();
            height = oracleBounds.getHeight();
        }

        oracleBounds = oracle.bounds();

        double dY = (bounds.getMinY() - oracleBounds.getMinY()) + (bounds.getHeight() - oracleBounds.getHeight()) / 2;

        if (dY >= 1) {
            oracle.pos(bounds.x, (int) (bounds.y + dY), bounds.width, bounds.height);
        }
    }

    private void adjustTextbox(TextStyle style, Textbox oracle, Textbox flavorTextbox) {
        var bounds = this.textboxBounds();
        var oracleBounds = oracle.bounds();
        var flavorBounds = flavorTextbox.bounds();

        double height = flavorBounds.getMaxY() - oracleBounds.getMinY();

        while (height > bounds.height) {
            style.setSize(style.getSize() - 0.1);

            oracleBounds = oracle.bounds();
            flavorBounds = flavorTextbox.bounds();
            height = flavorBounds.getMaxY() - oracleBounds.getMinY();
        }

        oracleBounds = oracle.bounds();
        flavorBounds = flavorTextbox.bounds();

        height = flavorBounds.getMaxY() - oracleBounds.getMinY();
        double dY = (bounds.height - height) / 2;

        if (dY >= 1) {
            oracle.pos(bounds.x, (int) (bounds.y + dY), bounds.width, bounds.height);
        }
    }

    protected void addPowerAndToughness(Card card, Parent parent) {
        parent.text("pt", pt -> {
            JsonString power = card.json.get("power"), toughness = card.json.get("toughness");

            if (power != null && toughness != null) {
                pt.set(power.get() + "/" + toughness.get());
            }

            pt.visibility(power != null && toughness != null);
            pt.pos(this.powerAndToughnessX(), this.powerAndToughnessY());
            pt.style(this.powerAndToughnessStyle());
            pt.alignment(this.powerAndToughnessAlignment());

            return all(power, toughness);
        });
    }

    protected void addCollectorInfo(Card card, Parent parent) {
        parent.group("info", info -> {
            TextStyle style = new TextStyle().setFontName("Gotham-Medium-Regular").setColor(Color.WHITE).setSize(4.1);

            info.text("collector_number", text -> {
                JsonString collectorNumber = card.json.get("collector_number");
                String string = collectorNumber.get();

                if (string.length() < 3) {
                    text.set("000".substring(string.length()) + string);
                } else {
                    text.set(string);
                }

                text.pos(220, 2725);
                text.style(style);

                return collectorNumber;
            });

            info.text("rarity", text -> {
                JsonString rarity = card.json.get("rarity");

                text.set(rarity.get().substring(0, 1).toUpperCase(Locale.ROOT));
                text.pos(493, 2725);
                text.style(style);

                return rarity;
            });

            info.text("set", text -> {
                JsonString set = card.json.get("set"), lang = card.json.get("lang");

                text.set(set.get().toUpperCase(Locale.ROOT) + " \u2022 " + lang.get().toUpperCase(Locale.ROOT));
                text.pos(220, 2775);
                text.style(style);

                return all(set, lang);
            });

            info.text("brush", text -> {
                text.set("a");
                text.style(new TextStyle().setFontName("NDPMTG").setColor(Color.WHITE).setSize(4.9));
                text.pos(493, 2776);

                return all();
            });

            info.text("artist", text -> {
                var option = card.getOption(MTGOptions.ARTIST);

                text.set(option.getValue());
                text.pos(556, 2775);
                text.style(new TextStyle().setFontName("Beleren Small Caps").setColor(Color.WHITE).setSize(4.6));

                return option;
            });
        });
    }

    @Override
    public abstract void build(Card card, Group group);



    protected String getSymbolFont() {
        return "NDPMTG";
    }

    protected Color getGenericColor() {
        return new Color(190, 188, 181);
    }

    protected Color getWhiteColor() {
        return new Color(255, 253, 234);
    }

    protected Color getBlueColor() {
        return new Color(167, 224, 249);
    }

    protected Color getBlackColor() {
        return new Color(190, 188, 181);
    }

    protected Color getRedColor() {
        return new Color(255, 170, 146);
    }

    protected Color getGreenColor() {
        return new Color(166, 222, 187);
    }

    protected void addSymbolWithShadow(String representation, TextComponent... textComponents) {
        this.symbols.addSymbol(representation, textComponents);

        TextComponent[] textComponentsWithShadows = new TextComponent[textComponents.length];

        textComponentsWithShadows[0] = textComponents[0];

        for (int i = 1; i < textComponents.length; ++i) {
            textComponentsWithShadows[i] = new TextComponent(
                    new TextStyle(textComponents[i].style).setShadow(null), textComponents[i].text
            );
        }

        this.symbols.addSymbol("mana_cost", representation, textComponentsWithShadows);
    }

    protected void addSymbols() {
        TextStyle foreground = new TextStyle().setFontName(this.getSymbolFont()).setColor(Color.BLACK);

        TextStyle generic = new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getGenericColor());
        TextStyle white = new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getWhiteColor());
        TextStyle blue = new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getBlueColor());
        TextStyle black = new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getBlackColor());
        TextStyle red = new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getRedColor());
        TextStyle green = new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getGreenColor());

        String[] colors = new String[] {"W", "U", "B", "R", "G"};
        TextStyle[] styles = new TextStyle[] {white, blue, black, red, green};

        // Colored mana symbols
        this.addSymbolWithShadow("{W}", new TextComponent(white, "o"), new TextComponent(foreground, "w"));
        this.addSymbolWithShadow("{U}", new TextComponent(blue, "o"), new TextComponent(foreground, "u"));
        this.addSymbolWithShadow("{B}", new TextComponent(black, "o"), new TextComponent(foreground, "b"));
        this.addSymbolWithShadow("{R}", new TextComponent(red, "o"), new TextComponent(foreground, "r"));
        this.addSymbolWithShadow("{G}", new TextComponent(green, "o"), new TextComponent(foreground, "g"));

        // Colorless mana symbol
        this.addSymbolWithShadow("{C}", new TextComponent(generic, "o"), new TextComponent(foreground, "c"));

        this.addSymbolWithShadow("{X}", new TextComponent(generic, "o"), new TextComponent(foreground, "x"));

        // Twobrid mana symbols
        for (int i = 0; i < colors.length; ++i) {
            this.addSymbolWithShadow("{2/" + colors[i] + "}",
                    new TextComponent(styles[i], "Q"),
                    new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getGenericColor()).setShadow(null), "q"),
                    new TextComponent(foreground, "L"),
                    new TextComponent(foreground, "S")
            );
        }

        // Hybrid mana symbols
        this.addSymbolWithShadow("{W/U}",
                new TextComponent(blue, "Q"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getWhiteColor()).setShadow(null), "q"),
                new TextComponent(foreground, "L"),
                new TextComponent(foreground, "S")
        );

        this.addSymbolWithShadow("{W/B}",
                new TextComponent(black, "Q"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getWhiteColor()).setShadow(null), "q"),
                new TextComponent(foreground, "L"),
                new TextComponent(foreground, "T")
        );

        this.addSymbolWithShadow("{B/R}",
                new TextComponent(red, "Q"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getBlackColor()).setShadow(null), "q"),
                new TextComponent(foreground, "N"),
                new TextComponent(foreground, "U")
        );

        this.addSymbolWithShadow("{B/G}",
                new TextComponent(green, "Q"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getBlackColor()).setShadow(null), "q"),
                new TextComponent(foreground, "N"),
                new TextComponent(foreground, "V")
        );

        this.addSymbolWithShadow("{U/B}",
                new TextComponent(black, "Q"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getBlueColor()).setShadow(null), "q"),
                new TextComponent(foreground, "M"),
                new TextComponent(foreground, "T")
        );

        this.addSymbolWithShadow("{U/R}",
                new TextComponent(red, "Q"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getBlueColor()).setShadow(null), "q"),
                new TextComponent(foreground, "M"),
                new TextComponent(foreground, "U")
        );

        this.addSymbolWithShadow("{R/G}",
                new TextComponent(green, "Q"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getRedColor()).setShadow(null), "q"),
                new TextComponent(foreground, "O"),
                new TextComponent(foreground, "V")
        );

        this.addSymbolWithShadow("{R/W}",
                new TextComponent(white, "Q"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getRedColor()).setShadow(null), "q"),
                new TextComponent(foreground, "O"),
                new TextComponent(foreground, "R")
        );

        this.addSymbolWithShadow("{G/W}",
                new TextComponent(white, "Q"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getGreenColor()).setShadow(null), "q"),
                new TextComponent(foreground, "P"),
                new TextComponent(foreground, "R")
        );

        this.addSymbolWithShadow("{G/U}",
                new TextComponent(blue, "Q"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(this.getGreenColor()).setShadow(null), "q"),
                new TextComponent(foreground, "P"),
                new TextComponent(foreground, "S")
        );

        // Colored phyrexian mana symbols
        for (int i = 0; i < colors.length; ++i) {
            this.addSymbolWithShadow("{" + colors[i] + "/P}", new TextComponent(styles[i], "Q"), new TextComponent(foreground, "p"));
        }

        // Colorless phyrexian mana symbol
        this.addSymbolWithShadow("{P}", new TextComponent(generic, "Q"), new TextComponent(foreground, "p"));

        // Generic mana 0-9
        for (int i = 0; i < 10; ++i) {
            this.addSymbolWithShadow("{" + i + "}", new TextComponent(generic, "o"), new TextComponent(foreground, "" + i));
        }

        // Large generic mana symbols
        this.addSymbolWithShadow("{10}", new TextComponent(generic, "o"), new TextComponent(foreground, "A"));
        this.addSymbolWithShadow("{11}", new TextComponent(generic, "o"), new TextComponent(foreground, "B"));
        this.addSymbolWithShadow("{12}", new TextComponent(generic, "o"), new TextComponent(foreground, "C"));
        this.addSymbolWithShadow("{13}", new TextComponent(generic, "o"), new TextComponent(foreground, "D"));
        this.addSymbolWithShadow("{14}", new TextComponent(generic, "o"), new TextComponent(foreground, "E"));
        this.addSymbolWithShadow("{15}", new TextComponent(generic, "o"), new TextComponent(foreground, "F"));
        this.addSymbolWithShadow("{16}", new TextComponent(generic, "o"), new TextComponent(foreground, "G"));
        this.addSymbolWithShadow("{20}", new TextComponent(generic, "o"), new TextComponent(foreground, "H"));

        // Tap symbol
        this.addSymbolWithShadow("{T}", new TextComponent(generic, "o"), new TextComponent(foreground, "t"));

        // Untap symbol
        this.addSymbolWithShadow("{Q}",
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(Color.BLACK), "o"),
                new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(Color.WHITE), "l")
        );

        // Energy symbol
        this.symbols.addSymbol("{E}", new TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(Color.BLACK), "e"));
    }
    
    public static String truncateFlash(Card card, String oracle) {
        if (card.getOption(MTGOptions.TRUNCATE_FLASH).getValue()) {
            List<String> lines = oracle.lines().toList();

            if (lines.size() > 1) {
                String firstLine = lines.get(1);
                StringBuilder oracleText = new StringBuilder();

                for (String keyword : Card.KEYWORD_ABILITIES) {
                    if (oracleText.isEmpty() && firstLine.toLowerCase(Locale.ROOT).startsWith(keyword)) {
                        oracleText.append("Flash, ")
                                .append(Character.toLowerCase(firstLine.charAt(0)))
                                .append(firstLine.substring(1));
                        break;
                    }
                }

                for (int i = 2; i < lines.size(); ++i) {
                    oracleText.append('\n').append(lines.get(i));
                }

                return oracleText.toString();
            }
        }

        return oracle;
    }
}
