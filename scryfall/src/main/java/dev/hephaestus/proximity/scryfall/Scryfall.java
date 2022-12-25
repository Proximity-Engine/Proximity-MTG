package dev.hephaestus.proximity.scryfall;

import dev.hephaestus.proximity.app.api.logging.ExceptionUtil;
import dev.hephaestus.proximity.app.api.logging.Log;
import dev.hephaestus.proximity.app.api.util.Result;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.json.api.JsonString;
import dev.hephaestus.proximity.mtg.data.Lang;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scryfall {
    private static final Map<CacheKey, Result<JsonObject>> CACHE = new HashMap<>();
    private static long LAST_SCRYFALL_REQUEST = 0;

    private Scryfall() { }

    private static void cache(Path path, JsonObject value) throws IOException {
        Files.createDirectories(path.getParent());

        value.write(path);
    }

    private static Result<JsonObject> fetchRemoteResource(Log log, CacheKey key) {
        long time = System.currentTimeMillis();

        if (time - LAST_SCRYFALL_REQUEST < 50) {
            try {
                log.write("Sleeping for {}ms", time - LAST_SCRYFALL_REQUEST);
                Thread.sleep(time - LAST_SCRYFALL_REQUEST);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        LAST_SCRYFALL_REQUEST = System.currentTimeMillis();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(key.remoteURI()).GET().build();

        Result<JsonObject> result;

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                if (!Files.exists(key.localFile.getParent())) {
                    Files.createDirectories(key.localFile.getParent());
                }

                Files.copy(new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8)), key.localFile());
                result = Result.of(Json.parseObject(key.localFile()));
            } else {
                JsonObject body = Json.parseObject(new StringReader(response.body()));
                result = Result.error("[%d] %s", response.statusCode(),
                        (body.has("details") ? body.getString("details") : "")
                );
            }
        } catch (IOException | InterruptedException e) {
            result = Result.error(ExceptionUtil.getErrorMessage(e));
        }

        CACHE.put(key, result);

        if (result.isOk()) {
            try {
                cache(key.localFile(), result.get());
            } catch (IOException e) {
                return Result.error("Failed to save result to local cache: %s", ExceptionUtil.getErrorMessage(e));
            }
        }

        return result;
    }

    private static Result<JsonObject> fetch(Log log, URI uri) {
        CacheKey key = CacheKey.of(uri);

        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        } else if (Files.exists(key.localFile())) {
            return loadCachedResponseFromDisk(key);
        } else {
            return fetchRemoteResource(log, key);
        }
    }

    private static Result<JsonObject> fetchMutable(Log log, URI uri) {
        return fetch(log, uri).then(Result::of);
    }

    @NotNull
    private static Result<JsonObject> loadCachedResponseFromDisk(CacheKey key) {
        Result<JsonObject> result;

        try {
            JsonObject json = Json.parseObject(key.localFile());
            result = Result.of(json);

            cache(key.localFile(), json);
        } catch (IOException e) {
            result = Result.error(ExceptionUtil.getErrorMessage(e));
        }

        CACHE.put(key, result);

        return result;
    }

    private static Result<JsonObject> getSet(Log log, String setCode) {
        return fetch(log, URI.create("https://api.scryfall.com/sets/" + setCode));
    }

    private static Result<JsonObject> getCardInfo(Log log, URI uri) {
        CacheKey key = CacheKey.of(uri);

        // Load the cached response if it exists
        if (Files.exists(key.localFile())) {
            return fetch(log, uri);
        } else {
            // Otherwise, fetch and transform the data
            Result<JsonObject> card = fetchMutable(log, uri);

            if (card.isOk()) {
                Result<JsonObject> set = getSet(log, card.get().getString("set"));

                if (set.isOk()) {

                    try {
                        card.get().put("set", set.get());
                        cache(key.localFile(), card.get());
                        CACHE.put(key, Result.of(card.get()));

                        return CACHE.get(key);
                    } catch (IOException e) {
                        return Result.error("Failed to save result to local cache: %s", ExceptionUtil.getErrorMessage(e));
                    }
                } else {
                    return Result.error("Failed to fetch information for set '%s'", card.get().getString("set"));
                }
            } else {
                return card.unwrap();
            }
        }
    }

    public static Result<List<String>> autocomplete(Log log, String userText) {
        return fetchRemoteResource(log, CacheKey.of(URI.create(
                "https://api.scryfall.com/cards/autocomplete?q=" + userText + "&include_extras=true"
        ))).then(object -> {
            JsonArray array = object.getArray("data");
            List<String> list = new ArrayList<>(array.size());

            array.forEach(e -> list.add(((JsonString) e).get()));

            return Result.of(list);
        });
    }

    public static Result<JsonObject> getCardInfo(Log log, String name) {
        return getCardInfo(log, URI.create(
                "https://api.scryfall.com/cards/named?fuzzy="
                        + URLEncoder.encode(name, StandardCharsets.UTF_8))
        );
    }

    public static Result<JsonObject> getCardInfo(Log log, String setCode, String collectorNumber, Lang lang) {
        return getCardInfo(log, URI.create(String.format(
                "https://api.scryfall.com/cards/%s/%s/%s", setCode, collectorNumber, lang.scryfallCode
        )));
    }

    private static record CacheKey(String key, URI remoteURI, Path localFile) {
        public static CacheKey of(URI uri) {
            // Remove leading protocol
            String key = uri.toASCIIString().split("//")[1];

            // Add local path prefix
            Path path = Path.of("scryfall");

            // Descend into folders for organization purposes
            for (String string : key.split("/")) {
                path = path.resolve(URLEncoder.encode(string, StandardCharsets.UTF_8));
            }

            // Ensure we're working with json files here
            path = path.resolveSibling(path.getFileName() + ".json");

            return new CacheKey(key, uri, path);
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }
    }
}
