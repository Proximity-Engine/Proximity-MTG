package dev.hephaestus.proximity.mtg;

import com.google.common.collect.ImmutableList;
import dev.hephaestus.proximity.app.api.Parent;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.elements.Text;
import dev.hephaestus.proximity.app.api.rendering.elements.TextBox;
import dev.hephaestus.proximity.app.api.rendering.util.Alignment;
import dev.hephaestus.proximity.app.api.rendering.util.ImagePosition;
import dev.hephaestus.proximity.app.api.rendering.util.Padding;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.text.Word;
import dev.hephaestus.proximity.mtg.cards.BaseMagicCard;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public abstract class MTGTemplate<D extends BaseMagicCard> extends Template<D> {
    private static final String[] KEYWORD_ABILITIES = new String[] {
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

    protected final TextParser oracleTextParser = new TextParser(this, "\n\n", false, "oracle", MTGTemplate::truncateFlash);
    protected final TextParser flavorTextParser = new TextParser(this, "\n", true, "flavor");
    private final Rectangle textboxBounds, ptBounds;
    private final double spaceAboveFlavorBar;
    private final double spaceBelowFlavorBar;

    protected MTGTemplate(String name, int width, int height, int dpi, boolean addDefaultResourceProvider) {
        super(name, width, height, dpi, addDefaultResourceProvider);
        this.addSymbols();

        this.textboxBounds = this.textboxBounds();
        this.ptBounds = this.powerAndToughnessBounds();
        this.spaceAboveFlavorBar = this.spaceAboveFlavorBar();
        this.spaceBelowFlavorBar = this.spaceBelowFlavorBar();
    }

    final Map<String, Word> defaultSymbols = new HashMap<>();
    final Map<String, Map<String, Word>> symbolsByContext = new HashMap<>();

    public void addSymbol(String layerId, String representation, TextComponent... textComponents) {
        var symbols = this.symbolsByContext.computeIfAbsent(layerId, id -> new HashMap<>());
        var word = new Word(textComponents);

        this.defaultSymbols.putIfAbsent(representation, word);
        symbols.put(representation, word);
    }

    public void addSymbol(String representation, TextComponent... textComponents) {
        var word = new Word(textComponents);

        this.defaultSymbols.putIfAbsent(representation, word);
    }

    protected final ImmutableList<Word> getOracleText(D card, TextStyle baseStyle) {
        return this.oracleTextParser.apply(card.json().getString("oracle_text"), card, baseStyle);
    }

    protected final ImmutableList<Word> getFlavorText(D card, TextStyle baseStyle) {
        return this.flavorTextParser.apply(card.json().getString("flavor_text"), card, baseStyle);
    }

    private Word getSymbol(String layer, String representation) {
        Word symbol = this.symbolsByContext.containsKey(layer)
                ? this.symbolsByContext.get(layer).get(representation)
                : this.defaultSymbols.get(representation);

        if (symbol == null) {
            throw new RuntimeException("Symbol does not exist: " + representation);
        }

        return symbol;
    }
    
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
        this.addSymbol(representation, textComponents);

        TextComponent[] textComponentsWithShadows = new TextComponent[textComponents.length];

        textComponentsWithShadows[0] = textComponents[0];

        for (int i = 1; i < textComponents.length; ++i) {
            textComponentsWithShadows[i] = new TextComponent(
                    new TextStyle(textComponents[i].style).setShadow(null), textComponents[i].text
            );
        }

        this.addSymbol("mana_cost", representation, textComponentsWithShadows);
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
        this.addSymbol("{E}", new dev.hephaestus.proximity.app.api.text.TextComponent(new TextStyle().setFontName(this.getSymbolFont()).setColor(Color.BLACK), "e"));
    }

    private static <D extends BaseMagicCard> String truncateFlash(D card, String oracle) {
        if (card.getOption(MTGOptions.TRUNCATE_FLASH)) {
            List<String> lines = oracle.lines().toList();

            if (lines.size() > 1) {
                String firstLine = lines.get(1);
                StringBuilder oracleText = new StringBuilder();

                for (String keyword : KEYWORD_ABILITIES) {
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

    protected int spaceAboveFlavorBar() {
        return 40;
    }

    protected int spaceBelowFlavorBar() {
        return 40;
    }

    protected Rectangle powerAndToughnessBounds() {
        return new Rectangle(1627, 2549, 368, 172);
    }

    protected Rectangle textboxBounds() {
        return new Rectangle(253, 1842, 1670, 794);
    }

    protected Rectangle artBounds() {
        return new Rectangle(239, 402, 1698, 1232);
    }

    protected void addArt(Parent<D> parent) {
        parent.image("Art", card -> {
            Path customArtPath = card.getOption(MTGOptions.CUSTOM_ART);

            if (customArtPath == null) {
                return new URL(card.json().getObject("image_uris").getString("art_crop"));
            } else {
                return customArtPath.toUri().toURL();
            }
        }).position().set(card -> {
            Rectangle bounds = this.artBounds();

            return new ImagePosition.Cover(bounds.x, bounds.y, bounds.width, bounds.height, Alignment.CENTER, Alignment.CENTER);
        });
    }

    protected void addTextbox(Parent<D> parent) {
        Function<D, Optional<Shape>> ptWrap = card -> card.isCreature() || card.is("vehicle")
                ? Optional.of(this.ptBounds)
                : Optional.empty();

        var oracleStyle = new TextStyle().setFontName("MPlantin").setItalicFontName("MPlantin-Italic").setSize(9.0).setColor(Color.BLACK);

        var oracle = parent.textBox("oracle", 253, this.textboxBounds.y, 1670, height).padding().set(new Padding(20, 20, 20, 20))
                .style().set(oracleStyle).wraps().add(ptWrap)
                .text().add((card, words) -> this.getOracleText(card, oracleStyle).forEach(words));

        oracle.y().set((int) (this.textboxBounds.y + (this.textboxBounds.height - oracle.getBounds().getHeight()) / 2));

        parent.group("flavor", card -> card.json().has("flavor_text"), flavor -> {
            var flavorBar = flavor.image("bar", "accents/flavor_bar").position().set(new ImagePosition.Direct(293, (int) (oracle.getBounds().getMaxY() + this.spaceAboveFlavorBar)))
                    .visibility().set(card -> card.getOption(MTGOptions.FLAVOR_BAR));

            var flavorText = flavor.textBox("text", 260, 0, 1670, 818).padding().set(new Padding(0, 10, 10, 10))
                    .y().set(card -> (int) (card.getOption(MTGOptions.FLAVOR_BAR) ? (flavorBar.getBounds().getMaxY() + this.spaceBelowFlavorBar) : oracle.getBounds().getMaxY() + this.spaceBelowFlavorBar + 9 + this.spaceBelowFlavorBar))
                    .style().set(oracleStyle).wraps().add(ptWrap)
                    .text().add((card, words) -> this.getFlavorText(card, oracleStyle).forEach(words));

            adjust(oracle, flavorBar, flavorText);

            while (oracle.getBounds().getMinY() <= this.textboxBounds.y || flavorText.getBounds().getMaxY() >= this.textboxBounds.getMaxY()) {
                oracleStyle.setSize(oracleStyle.getSize() - 1);
                oracle.style().set(oracleStyle);
                adjust(oracle, flavorBar, flavorText);
            }
        });
    }

    private void adjust(TextBox<?> oracle, Image<?> flavorBar, TextBox<?> flavor) {
        double dY = (this.textboxBounds.height - (oracle.getBounds().getHeight() + (flavorBar.visibility().get() ? this.spaceAboveFlavorBar + flavorBar.getBounds().getHeight() : this.spaceBelowFlavorBar + 9) + this.spaceBelowFlavorBar + flavor.getBounds().getHeight())) / 2;

        oracle.y().set((int) (this.textboxBounds.y + dY));
        flavorBar.position().set(new ImagePosition.Direct(293, (int) (oracle.getBounds().getMaxY() + this.spaceAboveFlavorBar)));
        flavor.y().set(card -> (int) (card.getOption(MTGOptions.FLAVOR_BAR) ? (flavorBar.getBounds().getMaxY() + this.spaceBelowFlavorBar) : oracle.getBounds().getMaxY() + this.spaceBelowFlavorBar + 9 + this.spaceBelowFlavorBar));
    }

    protected TextStyle cardNameStyle(D card) {
        return new TextStyle().setFontName("Beleren2016-Bold").setSize(9.6).setColor(Color.BLACK);
    }

    protected int cardNameX(D card) {
        return 257;
    }

    protected int cardNameY(D card) {
        return 345;
    }

    protected Text<D> addCardName(Parent<D> group) {
        return group.text("Name", D::getName)
                .style().set(this::cardNameStyle)
                .x().set(this::cardNameX)
                .y().set(this::cardNameY);
    }

    protected int manaCostX(D card) {
        return 1938;
    }

    protected int manaCostY(D card) {
        return 340;
    }

    protected TextStyle manaCostStyle(D card) {
        return new TextStyle().setColor(Color.BLACK).setFontName("NDPMTG").setShadow(new TextStyle.Shadow(Color.BLACK, -4, 6)).setSize(11.0);
    }

    protected Alignment manaCostAlignment(D card) {
        return Alignment.END;
    }

    protected Text<D> addManaCost(Parent<D> group) {
        return group.text("mana_cost").x().set(this::manaCostX).y().set(this::manaCostY).style().set(this::manaCostStyle).alignment().set(this::manaCostAlignment).text().add((card, words) -> {
            if (card.json().has("mana_cost")) {
                String manaCost = card.json().getString("mana_cost");

                if (manaCost.isEmpty()) return;

                String[] symbols = manaCost.split("((?<=}))((?=\\{))");

                for (String symbol : symbols) {
                    words.accept(this.getSymbol("mana_cost", symbol));
                }
            }
        });
    }

    /**
     * Creates the name and mana cost
     */
    protected void addNamePlateText(Parent<D> group) {
        Text<D> manaCost = this.addManaCost(group);
        Text<D> name = this.addCardName(group);
        TextStyle style = name.style().get();

        while (!manaCost.getBounds().isEmpty() && name.getBounds().getMaxX() >= manaCost.getBounds().getMinX()) {
            style.setSize(style.getSize() - 1);
        }
    }

    protected TextStyle typeLineStyle(D card) {
        return new TextStyle().setFontName("Beleren2016-Bold").setSize(8.0).setColor(Color.BLACK);
    }

    protected int typeLineX(D card) {
        return 262;
    }

    protected int typeLineY(D card) {
        return 1761;
    }

    protected void addTypeLine(Parent<D> group) {
        group.text("Type Line", D::getTypeLine)
                .style().set(this::typeLineStyle)
                .x().set(this::typeLineX)
                .y().set(this::typeLineY);
    }

    protected TextStyle powerAndToughnessStyle(D card) {
        return new TextStyle().setSize(9.8).setColor(Color.BLACK).setFontName("Beleren2016-Bold");
    }

    protected int powerAndToughnessX(D card) {
        return 1811;
    }

    protected int powerAndToughnessY(D card) {
        return 2669;
    }

    protected Alignment powerAndToughnessAlignment(D card) {
        return Alignment.CENTER;
    }

    protected void addPowerAndToughness(Parent<D> group) {
        group.text("pt", card -> card.getPower() + "/" + card.getToughness())
                .x().set(this::powerAndToughnessX).y().set(this::powerAndToughnessY)
                .style().set(this::powerAndToughnessStyle)
                .alignment().set(this::powerAndToughnessAlignment).visibility()
                .set(card -> card.json().has("power") && card.json().has("toughness"));
    }

    protected void addCollectorInfo(Parent<D> group) {
        group.group("info", info -> {
            TextStyle style = new TextStyle().setFontName("Gotham-Medium-Regular").setColor(Color.WHITE).setSize(4.1);

            info.text("collector_number", card -> {
                String collectorNumber = card.json().getString("collector_number");

                if (collectorNumber.length() < 3) {
                    return "000".substring(collectorNumber.length()) + collectorNumber;
                } else {
                    return collectorNumber;
                }
            }, 220, 2725, style);

            info.text("rarity", card -> (card.json().getString("rarity").substring(0, 1)).toUpperCase(Locale.ROOT), 493, 2725, style);
            info.text("set", card -> (card.json().getString("set") + " \u2022 " + card.json().getString("lang")).toUpperCase(Locale.ROOT), 220, 2775, style);
            info.text("brush", card -> "a", 497, 2771, new TextStyle().setFontName("NDPMTG").setColor(Color.WHITE).setSize(4.9));
            info.text("artist", card -> card.getOption(MTGOptions.ARTIST), 556, 2775, new TextStyle().setFontName("Beleren Small Caps").setColor(Color.WHITE).setSize(4.6));
        });
    }
}
