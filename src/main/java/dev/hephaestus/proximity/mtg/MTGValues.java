package dev.hephaestus.proximity.mtg;

import dev.hephaestus.proximity.api.Value;
import dev.hephaestus.proximity.api.json.JsonArray;
import dev.hephaestus.proximity.api.json.JsonObject;

public final class MTGValues {
    // Values to be used by templates
    public static final Value<Boolean> DOUBLE_SIDED = Value.createBoolean("mtg", "double_sided");
    public static final Value<JsonObject> FLIPPED = Value.createObject("mtg", "flipped");
    public static final Value<Boolean> FRONT_FACE = Value.createBoolean("mtg", "front_face");
    public static final Value<Integer> COLOR_COUNT = Value.createInteger("mtg", "color_count");
    public static final Value<Boolean> HYBRID = Value.createBoolean("mtg", "hybrid");
    public static final Value<String> MUTATE_TEXT = Value.createString("mtg", "mutate_text");
    public static final Value<JsonArray> TYPES = Value.createArray("mtg", "types");
    public static final Value<String> MAIN_TYPES = Value.createString("mtg", "main_types");
    public static final Value<Integer> TYPE_COUNT = Value.createInteger("mtg", "type_count");
    public static final Value<Boolean> IS_CARD_BACK = Value.createBoolean("mtg", "is_card_back");
    public static final Value<Boolean> IS_ORIGINAL_TWO_SIDED_CARD = Value.createBoolean("mtg", "is_original_two_sided_card");
    public static final Value<String> MANA_ABILITY = Value.createString("mtg", "mana_ability");

    // Options
    public static final Value<Boolean> USE_CARD_BACK = Value.createBoolean("options", "use_card_back");
    public static final Value<Boolean> SAVE_FILE_WITH_CARD_NUMBER = Value.createBoolean("options", "save_file_with_card_number");
    public static final Value<ArtSource> ART_SOURCE = Value.createEnum(ArtSource::valueOf, "options", "art_source");
    public static final Value<Boolean> REMINDER_TEXT = Value.createBoolean("options", "reminder_text");
    public static final Value<String> SET_SYMBOL = Value.createString("options", "set_symbol");
    public static final Value<Boolean> REMOVE_ORIGINAL_CARD = Value.createBoolean("options", "remove_original_card");
    public static final Value<Integer> MAX_ARTIST_NAME_LENGTH = Value.createInteger("options", "max_artist_name_length");
    public static final Value<String> FOLDER = Value.createString("options", "folder");

    private MTGValues() {
    }
}
