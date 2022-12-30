package dev.hephaestus.proximity.mtg;

import dev.hephaestus.proximity.app.api.options.FileOption;
import dev.hephaestus.proximity.app.api.options.StringOption;
import dev.hephaestus.proximity.app.api.options.ToggleOption;
import javafx.stage.FileChooser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MTGOptions {
    private static final Pattern PATTERN = Pattern.compile(".+\\((?<name>.+)\\)\\..+");

    public static final ToggleOption<Card> TRUNCATE_FLASH = new ToggleOption<>("truncate_flash", false);
    public static final ToggleOption<Card> SHOW_REMINDER_TEXT = new ToggleOption<>("show_reminder_text", false);
    public static final ToggleOption<Card> FLAVOR_BAR = new ToggleOption<>("flavor_bar", true);
    public static final FileOption<Card> CUSTOM_ART = new FileOption<>("custom_art", null,
            new FileChooser.ExtensionFilter("All image types", "*.png", "*.jpg", "*.jpeg", "*.jpeg", "*.jif", "*.jfif", "*.jfi"),
            new FileChooser.ExtensionFilter("PNG", "*.png"),
            new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg", "*.jpeg", "*.jif", "*.jfif", "*.jfi")
    ) {
        @Override
        public Widget createControl(Card renderJob) {
            Widget widget = super.createControl(renderJob);

            widget.getValueProperty().addListener(((observable, oldValue, newValue) -> {
                Matcher matcher = PATTERN.matcher(newValue.getFileName().toString());

                if (matcher.matches()) {
                    renderJob.getOption(MTGOptions.ARTIST).setValue(matcher.group("name"));
                }
            }));

            return widget;
        }
    };

    public static final StringOption<Card> ARTIST = new StringOption<>("artist", card -> card.json.getString("artist"));

    public MTGOptions() {
    }
}
