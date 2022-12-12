package dev.hephaestus.proximity.mtg.data;

public enum Lang {
    EN("en"),
    ES("es", "sp"),
    FR("fr"),
    DE("de"),
    IT("it"),
    PT("pt"),
    JA("ja", "jp"),
    KO("ko", "kr"),
    RU("ru"),
    ZHS("zhs", "cs"),
    ZHT("zht", "ct"),
    PH("ph");

    public final String scryfallCode, printedCode;

    Lang(String scryfallCode, String printedCode) {
        this.scryfallCode = scryfallCode;
        this.printedCode = printedCode;
    }

    Lang(String scryfallCode) {
        this.scryfallCode = scryfallCode;
        this.printedCode = scryfallCode;
    }
}
