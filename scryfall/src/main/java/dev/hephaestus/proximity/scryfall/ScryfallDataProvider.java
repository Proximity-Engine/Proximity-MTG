package dev.hephaestus.proximity.scryfall;


import dev.hephaestus.proximity.app.api.plugins.DataProvider;
import dev.hephaestus.proximity.app.api.plugins.ImportHandler;
import dev.hephaestus.proximity.mtg.cards.BaseMagicCard;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Paint;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Consumer;

public class ScryfallDataProvider implements DataProvider<BaseMagicCard> {
    private final DeckListImporter importer = new DeckListImporter();

    @Override
    public Class<BaseMagicCard> getDataClass() {
        return BaseMagicCard.class;
    }

    @Override
    public Pane createHeaderElement() {
        Label cardNameLabel = new Label("Card Name");

        cardNameLabel.setTextFill(Paint.valueOf("#FFFFFFC0"));
        cardNameLabel.setStyle("-fx-font-weight: bold;");
        cardNameLabel.setPadding(new Insets(0, 0, 0, 8));

        Pane cardName = new AnchorPane(cardNameLabel);

        HBox.setHgrow(cardName, Priority.ALWAYS);

        Label setCode = new Label("Set Code");

        setCode.setPadding(new Insets(0, 0, 0, 8));
        setCode.setTextFill(Paint.valueOf("#FFFFFFC0"));
        setCode.setStyle("-fx-font-weight: bold;");
        setCode.setPrefWidth(150);

        Label collectorNumber = new Label("Collector Number");

        collectorNumber.setPadding(new Insets(0, 0, 0, 8));
        collectorNumber.setTextFill(Paint.valueOf("#FFFFFFC0"));
        collectorNumber.setStyle("-fx-font-weight: bold;");
        collectorNumber.setPrefWidth(150);

        return new HBox(cardName, setCode, collectorNumber);
    }

    @Override
    public ScryfallDataWidget createDataEntryElement(Context context) {
        return new ScryfallDataWidget(context);
    }

    @Override
    public BufferedImage crop(BufferedImage image) throws IOException {
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        BufferedImage mask = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        mask.getGraphics().drawImage(ImageIO.read(this.getClass().getModule().getResourceAsStream("preview/base.png")).getScaledInstance(image.getWidth(), image.getHeight(), Image.SCALE_SMOOTH), 0, 0, null, null);
        output.createGraphics().drawImage(ImageIO.read(this.getClass().getModule().getResourceAsStream("preview/base.png")).getScaledInstance(image.getWidth(), image.getHeight(), Image.SCALE_SMOOTH), 0, 0, output.getWidth(), output.getHeight(), null, null);

        int[] inColors = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        int[] maskColors = mask.getRGB(0, 0, mask.getWidth(), mask.getHeight(), null, 0, mask.getWidth());

        for (int i = 0; i < inColors.length; ++i) {
            inColors[i] = (inColors[i] & 0x00FFFFFF) | (maskColors[i] & 0xFF000000);
        }

        output.setRGB(0, 0, image.getWidth(), image.getHeight(), inColors, 0, image.getWidth());

        return output;
    }

    @Override
    public void addMenuItems(Context context, Consumer<ImportHandler> menuConsumer) {
        menuConsumer.accept(this.importer);
    }
}
