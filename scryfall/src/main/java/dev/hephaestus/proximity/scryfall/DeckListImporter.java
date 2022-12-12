package dev.hephaestus.proximity.scryfall;

import dev.hephaestus.proximity.app.api.logging.ExceptionUtil;
import dev.hephaestus.proximity.app.api.plugins.DataProvider;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import dev.hephaestus.proximity.app.api.plugins.ImportHandler;
import dev.hephaestus.proximity.app.api.Proximity;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonObject;
import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeckListImporter implements ImportHandler {
    private int importCount, importProgress;

    private static final Pattern CARD_ENTRY = Pattern.compile("^(?:(?<count>\\d+[xX]?) )?(?<name>.+?)(?: \\((?<set>.+)\\)(?: (?<collector>[a-zA-Z0-9]+?))?)?$");

    @Override
    public String getText() {
        return "Decklist From Clipboard";
    }

    private void updatePauseText() {
        Platform.runLater(() -> Proximity.setPauseText(String.format("%d/%d", this.importProgress, this.importCount)));
    }

    @Override
    public void createDataWidgets(DataProvider.Context context) {
        if (!Proximity.isPaused() && Clipboard.getSystemClipboard().getContentTypes().contains(DataFormat.PLAIN_TEXT)) {
            String clipboard = Clipboard.getSystemClipboard().getString();

            Thread thread = new Thread(() -> {
                Proximity.pause();

                String[] lines = clipboard.split("\n");
                List<DataWidget<?>> widgets = new ArrayList<>(lines.length);

                this.importCount = (int) Arrays.stream(lines).filter(s -> !s.isBlank()).count();
                this.importProgress = 0;

                this.updatePauseText();

                for (String line : lines) {
                    var widget = parse(context, line.strip());

                    if (widget != null) {
                        widgets.add(widget);
                    }

                    ++this.importProgress;
                    this.updatePauseText();
                }

                Platform.runLater(() -> {
                    Proximity.add(widgets);
                    Proximity.resume();
                });
            });

            thread.setDaemon(true);

            thread.start();
        }
    }

    private static ScryfallDataWidget parse(DataProvider.Context context, String line) {
        Matcher matcher = CARD_ENTRY.matcher(line);

        if (matcher.matches()) {
            String count = matcher.group("count");
            String cardName = matcher.group("name");
            String setCode = matcher.group("set");
            String collectorNumber = matcher.group("collector");

            if (count != null) {
                int cardCount = Integer.decode(count.endsWith("x") ? count.substring(0, count.length() - 1) : count);
                // TODO: Use this
            }

            StringBuilder builder = new StringBuilder();

            if (setCode == null) {
                builder.append("https://api.scryfall.com/cards/named?fuzzy=").append(URLEncoder.encode(cardName, StandardCharsets.UTF_8));
            } else {
                builder.append("https://api.scryfall.com/cards/").append(setCode).append("/");

                if (collectorNumber != null) {
                    builder.append(collectorNumber);
                }
            }

            ScryfallDataWidget widget = new ScryfallDataWidget(context);

            try {
                URL url = Proximity.cache(URI.create(builder.toString()).toURL());
                JsonObject json = Json.parseObject(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
                widget.select(json);
            } catch (FileNotFoundException e) {
                String message = String.format("Card not found: \"%s\"", cardName);
                widget.getErrorProperty().add(message);
                context.log().print(message);
            } catch (IOException e) {
                widget.getErrorProperty().add(ExceptionUtil.getErrorMessage(e));
                context.log().print(e);
            }

            context.log().print("Importing %s", cardName);

            return widget;
        } else {
            return null;
        }
    }
}
