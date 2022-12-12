import dev.hephaestus.proximity.app.api.plugins.DataProvider;
import dev.hephaestus.proximity.scryfall.ScryfallDataProvider;

module dev.hephaestus.proximity.scryfall {
    requires dev.hephaestus.proximity.mtg;
    requires dev.hephaestus.proximity.app;
    requires dev.hephaestus.proximity.json;
    requires dev.hephaestus.proximity.utils;
    requires org.jetbrains.annotations;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.net.http;
    requires java.desktop;

    exports dev.hephaestus.proximity.scryfall;
    provides DataProvider with ScryfallDataProvider;
}