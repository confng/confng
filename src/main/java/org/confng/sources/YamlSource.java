package org.confng.sources;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Map;

/**
 * Configuration source that reads from YAML files.
 *
 * <p>This source loads configuration from YAML files using SnakeYAML
 * for parsing. Supports both primitive values and nested objects.</p>
 *
 * <p>Supports environment-specific configuration sections. When an environment
 * is specified, only configuration from that section is loaded. For example:</p>
 *
 * <pre>
 * uat:
 *   database:
 *     host: uat-db.example.com
 *   api:
 *     url: https://uat-api.example.com
 *
 * prod:
 *   database:
 *     host: prod-db.example.com
 *   api:
 *     url: https://prod-api.example.com
 * </pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 * @see org.confng.sources.ConfigSource
 * @see org.confng.sources.AbstractFileConfigSource
 */
public class YamlSource extends AbstractFileConfigSource<Map<String, Object>> {

    private static final Yaml YAML = new Yaml();

    /**
     * Creates a new YamlSource from the given file path (supports both filesystem and classpath).
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param filePath path to the YAML file (filesystem or classpath)
     * @throws IllegalStateException if the file cannot be loaded
     */
    public YamlSource(String filePath) {
        super(filePath);
    }

    /**
     * Creates a new YamlSource from the given file.
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param file path to the YAML file
     * @throws IllegalStateException if the file cannot be loaded
     */
    public YamlSource(Path file) {
        super(file);
    }

    /**
     * Creates a new YamlSource from the given file path for a specific environment (supports both filesystem and classpath).
     * When an environment is specified, only configuration from that section is loaded.
     *
     * @param filePath path to the YAML file (filesystem or classpath)
     * @param environment the environment section to load (e.g., "uat", "prod"), or null for all top-level keys
     * @throws IllegalStateException if the file cannot be loaded or the environment section doesn't exist
     */
    public YamlSource(String filePath, String environment) {
        super(filePath, environment);
    }

    /**
     * Creates a new YamlSource from the given file for a specific environment.
     * When an environment is specified, only configuration from that section is loaded.
     *
     * @param file path to the YAML file
     * @param environment the environment section to load (e.g., "env1", "env2"), or null for all top-level keys
     * @throws IllegalStateException if the file cannot be loaded or the environment section doesn't exist
     */
    public YamlSource(Path file, String environment) {
        super(file, environment);
    }

    @Override
    protected String getSourceTypePrefix() {
        return "Yaml";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseContent(Reader reader) throws IOException {
        Object loaded = YAML.load(reader);
        if (!(loaded instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) loaded;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> extractEnvironmentSection(Map<String, Object> content, String environment, String filePath) {
        Object envSection = content.get(environment);
        if (envSection == null || !(envSection instanceof Map)) {
            throw new IllegalStateException("Environment section '" + environment +
                "' not found in YAML file: " + filePath);
        }
        return (Map<String, Object>) envSection;
    }

    @Override
    protected String getValueFromRoot(String key) {
        Object element = getNestedElement(root, key);
        if (element == null) {
            return null;
        }
        return element.toString();
    }

    /**
     * Retrieves a nested YAML element using dot notation.
     * For example, "app.name" will look for {app: {name: value}}
     */
    @SuppressWarnings("unchecked")
    private Object getNestedElement(Map<String, Object> obj, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        String[] parts = key.split("\\.");
        Object current = obj;

        for (String part : parts) {
            if (current == null || !(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
        }

        return current;
    }
}
