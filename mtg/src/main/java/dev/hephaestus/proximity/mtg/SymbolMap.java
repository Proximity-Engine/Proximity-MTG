package dev.hephaestus.proximity.mtg;

import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.Word;

import java.util.HashMap;
import java.util.Map;

public final class SymbolMap {
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

    public Word getSymbol(String layer, String representation) {
        Word symbol = this.symbolsByContext.containsKey(layer)
                ? this.symbolsByContext.get(layer).get(representation)
                : this.defaultSymbols.get(representation);

        if (symbol == null) {
            throw new RuntimeException("Symbol does not exist: " + representation);
        }

        return symbol;
    }
}
