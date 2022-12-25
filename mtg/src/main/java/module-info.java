module dev.hephaestus.proximity.mtg {
    requires java.desktop;
    requires dev.hephaestus.proximity.app;
    requires dev.hephaestus.proximity.json;
    requires dev.hephaestus.proximity.utils;
    requires javafx.graphics;
    requires org.jetbrains.annotations;

    exports dev.hephaestus.proximity.mtg;
    exports dev.hephaestus.proximity.mtg.cards;
    exports dev.hephaestus.proximity.mtg.data;
}