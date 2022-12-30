package dev.hephaestus.proximity.mtg.cards;

import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.mtg.Card;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MultifacedCard extends Card implements Iterable<CardFace> {
    private final List<CardFace> faces;

    public MultifacedCard(JsonObject json) {
        super(json);

        JsonArray faces = json.getArray("card_faces");
        this.faces = new ArrayList<>(faces.size());

        for (int i = 0; i < faces.size(); ++i) {
            JsonObject faceJson = json.copy();

            faceJson.copyAll((JsonObject) faces.get(i));

            this.faces.add(new CardFace(faceJson));
        }
    }

    public final int numberOfFaces() {
        return this.faces.size();
    }

    protected final CardFace getFace(int i) {
        return this.faces.get(i);
    }

    @NotNull
    @Override
    public Iterator<CardFace> iterator() {
        return this.faces.iterator();
    }
}
