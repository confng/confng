package org.confng.sources;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.confng.util.FileResolver;

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
 * <p>Supports environment-specific configuration sections. When an environment
 * is specified, only configuration from that section is loaded. For example:</p>
 *
 * <pre>
 * {
 *   "uat": {
 *     "database.host": "uat-db.example.com",
 *     "api.url": "https://uat-api.example.com"
 *   },
 *   "prod": {
 *     "database.host": "prod-db.example.com",
 *     "api.url": "https://prod-api.example.com"
 *   }
 * }
 * </pre>
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
    private final String environment;

    /**
     * Creates a new JsonSource from the given file path (supports both filesystem and classpath).
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param filePath path to the JSON file (filesystem or classpath)
     * @throws IllegalStateException if the file cannot be loaded
     */
    public JsonSource(String filePath) {
        this(filePath, null);
    }

    /**
     * Creates a new JsonSource from the given file path for a specific environment (supports both filesystem and classpath).
     * When an environment is specified, only configuration from that section is loaded.
     *
     * @param filePath path to the JSON file (filesystem or classpath)
     * @param environment the environment section to load (e.g., "uat", "prod"), or null for all top-level keys
     * @throws IllegalStateException if the file cannot be loaded or the environment section doesn't exist
     */
    public JsonSource(String filePath, String environment) {
        this.environment = environment;

        if (environment != null && !environment.isEmpty()) {
            this.sourceName = "Json(" + filePath + ")[" + environment + "]";
        } else {
            this.sourceName = "Json(" + filePath + ")";
        }

        this.root = loadJsonFromString(filePath, environment);
    }

    /**
     * Creates a new JsonSource from the given file.
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param file path to the JSON file
     * @throws IllegalStateException if the file cannot be loaded
     */
    public JsonSource(Path file) {
        this(file, null);
    }

    /**
     * Creates a new JsonSource from the given file for a specific environment.
     * When an environment is specified, only configuration from that section is loaded.
     *
     * @param file path to the JSON file
     * @param environment the environment section to load (e.g., "env1", "env2"), or null for all top-level keys
     * @throws IllegalStateException if the file cannot be loaded or the environment section doesn't exist
     */
    public JsonSource(Path file, String environment) {
        this.environment = environment;

        if (environment != null && !environment.isEmpty()) {
            this.sourceName = "Json(" + file.toString() + ")[" + environment + "]";
        } else {
            this.sourceName = "Json(" + file.toString() + ")";
        }

        this.root = loadJson(file, environment, file.toString());
    }

    /**
     * Helper method to load JSON from a String path (supports classpath).
     */
    private JsonObject loadJsonFromString(String filePath, String environment) {
        try (Reader reader = FileResolver.openReader(filePath)) {
            JsonObject fullJson = GSON.fromJson(reader, JsonObject.class);

            // If environment is specified, extract that section
            if (environment != null && !environment.isEmpty()) {
                JsonElement envElement = fullJson.get(environment);
                if (envElement == null || !envElement.isJsonObject()) {
                    throw new IllegalStateException("Environment section '" + environment +
                        "' not found in JSON file: " + filePath);
                }
                return envElement.getAsJsonObject();
            } else {
                // Load all top-level keys
                return fullJson;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load json file: " + filePath, e);
        }
    }

    /**
     * Helper method to load JSON from a Path.
     */
    private JsonObject loadJson(Path file, String environment, String displayPath) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject fullJson = GSON.fromJson(reader, JsonObject.class);

            // If environment is specified, extract that section
            if (environment != null && !environment.isEmpty()) {
                JsonElement envElement = fullJson.get(environment);
                if (envElement == null || !envElement.isJsonObject()) {
                    throw new IllegalStateException("Environment section '" + environment +
                        "' not found in JSON file: " + displayPath);
                }
                return envElement.getAsJsonObject();
            } else {
                // Load all top-level keys
                return fullJson;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load json file: " + displayPath, e);
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
