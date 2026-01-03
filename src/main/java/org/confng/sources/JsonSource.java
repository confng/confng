package org.confng.sources;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

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
 * @see org.confng.sources.AbstractFileConfigSource
 */
public class JsonSource extends AbstractFileConfigSource<JsonObject> {

    private static final Gson GSON = new Gson();

    /**
     * Creates a new JsonSource from the given file path (supports both filesystem and classpath).
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param filePath path to the JSON file (filesystem or classpath)
     * @throws IllegalStateException if the file cannot be loaded
     */
    public JsonSource(String filePath) {
        super(filePath);
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
        super(filePath, environment);
    }

    /**
     * Creates a new JsonSource from the given file.
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param file path to the JSON file
     * @throws IllegalStateException if the file cannot be loaded
     */
    public JsonSource(Path file) {
        super(file);
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
        super(file, environment);
    }

    @Override
    protected String getSourceTypePrefix() {
        return "Json";
    }

    @Override
    protected JsonObject parseContent(Reader reader) throws IOException {
        return GSON.fromJson(reader, JsonObject.class);
    }

    @Override
    protected JsonObject extractEnvironmentSection(JsonObject content, String environment, String filePath) {
        JsonElement envElement = content.get(environment);
        if (envElement == null || !envElement.isJsonObject()) {
            throw new IllegalStateException("Environment section '" + environment +
                "' not found in JSON file: " + filePath);
        }
        return envElement.getAsJsonObject();
    }

    @Override
    protected String getValueFromRoot(String key) {
        JsonElement element = getNestedElement(root, key);
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonPrimitive()) {
            return element.getAsJsonPrimitive().getAsString();
        }

        return element.toString();
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
