package dev.hephaestus.proximity.mtg;

import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.api.tasks.TemplateModification;
import dev.hephaestus.proximity.mtg.preparation.Miscellaneous;
import dev.hephaestus.proximity.xml.RenderableData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtDiscovery implements TemplateModification {
    private static final Pattern ART_FILE = Pattern.compile(".+\\((?<artist>.+)\\)\\..+");
    private static final Path ART = Path.of("art");

    @Override
    public void apply(JsonObject card, RenderableData.XMLElement layers) {
        ArtSource artSource = MTGValues.ART_SOURCE.get(card);

        switch (artSource) {
            case BEST:
            case SCRYFALL:
                apply(card.getAsString("image_uris", "art_crop"), layers);
            case LOCAL:
                String cardName = card.getAsString("name").replaceAll(Miscellaneous.FILE_CHARS, " ");
                String setCode = card.getAsString("set");
                String collectorNumber = card.getAsString("collector_number");

                Path path = ART;
                String[] tries = new String[]{cardName, setCode, collectorNumber};

                for (String level : tries) {
                    path = path.resolve(level);

                    if (Files.isDirectory(path.getParent())) {
                        try {
                            for (Path child : Files.newDirectoryStream(path.getParent())) {
                                String fileName = child.getFileName().toString().toLowerCase(Locale.ROOT);
                                Matcher matcher = ART_FILE.matcher(fileName);

                                if (matcher.matches() && fileName.startsWith(level.toLowerCase(Locale.ROOT))) {
                                    matcher = ART_FILE.matcher(child.getFileName().toString());

                                    if (matcher.matches() && matcher.group("artist") != null) {
                                        apply(child.toUri().toString(), layers);
                                        card.addProperty("artist", matcher.group("artist"));
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            case NONE:
        }
    }

    private void apply(String url, RenderableData.XMLElement layers) {
        layers.iterate((layer, i) -> {
            if (layer.getTagName().equals("Image") && layer.getId().equals("art")) {
                layer.setAttribute("url", url);
            } else {
                apply(url, layer);
            }
        });
    }
}
