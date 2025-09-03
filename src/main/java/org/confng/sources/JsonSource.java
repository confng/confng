package org.confng.sources;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Configuration source that reads from JSON files.
 * 
 * <p>This source loads configuration from JSON files using Gson
 * for parsing. Supports both primitive values and nested objects.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @version 1.0.0
 * @since 1.0
 * @see org.confng.sources.ConfigSource
 */
public class JsonSource implements ConfigSource {

    private static final Gson GSON = new Gson();
    private final JsonObject root;
    private final String sourceName;

    /**
     * Creates a new JsonSource from the given file.
     * 
     * @param file path to the JSON file
     * @throws IllegalStateException if the file cannot be loaded
     */
    public JsonSource(Path file) {
        this.sourceName = "Json(" + file.toString() + ")";
        try (Reader reader = Files.newBufferedReader(file)) {
            this.root = GSON.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load json file: " + file, e);
        }
    }

    @Override
    public String getName() {
        return sourceName;
    }

    @Override
    public Optional<String> get(String key) {
        if (root == null) return Optional.empty();
        JsonElement el = root.get(key);
        if (el == null || el.isJsonNull()) return Optional.empty();
        if (el.isJsonPrimitive()) {
            return Optional.of(el.getAsJsonPrimitive().getAsString());
        }
        return Optional.of(el.toString());
    }
}