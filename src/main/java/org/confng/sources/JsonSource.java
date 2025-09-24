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
        
        JsonElement element = getNestedElement(root, key);
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        
        if (element.isJsonPrimitive()) {
            return Optional.of(element.getAsJsonPrimitive().getAsString());
        }
        
        return Optional.of(element.toString());
    }

    @Override
    public int getPriority() {
        return 30; // Medium-low priority for JSON files
    }
    
    /**
     * Retrieves a nested JSON element using dot notation.
     * For example, "app.name" will look for {"app": {"name": "value"}}
     */
    private JsonElement getNestedElement(JsonObject obj, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        String[] parts = key.split("\\.");
        JsonElement current = obj;
        
        for (String part : parts) {
            if (current == null || !current.isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject().get(part);
        }
        
        return current;
    }
}
