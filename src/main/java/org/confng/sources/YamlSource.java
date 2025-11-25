package org.confng.sources;

import org.confng.util.FileResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

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
 */
public class YamlSource implements ConfigSource {

    private static final Yaml YAML = new Yaml();
    private final Map<String, Object> root;
    private final String sourceName;
    private final String environment;

    /**
     * Creates a new YamlSource from the given file path (supports both filesystem and classpath).
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param filePath path to the YAML file (filesystem or classpath)
     * @throws IllegalStateException if the file cannot be loaded
     */
    public YamlSource(String filePath) {
        this(filePath, null);
    }

    /**
     * Creates a new YamlSource from the given file.
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param file path to the YAML file
     * @throws IllegalStateException if the file cannot be loaded
     */
    public YamlSource(Path file) {
        this(file, null);
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
        this.environment = environment;

        if (environment != null && !environment.isEmpty()) {
            this.sourceName = "Yaml(" + filePath + ")[" + environment + "]";
        } else {
            this.sourceName = "Yaml(" + filePath + ")";
        }

        this.root = loadYamlFromString(filePath, environment);
    }

    /**
     * Creates a new YamlSource from the given file for a specific environment.
     * When an environment is specified, only configuration from that section is loaded.
     *
     * @param file path to the YAML file
     * @param environment the environment section to load (e.g., "env1", "env2"), or null for all top-level keys
     * @throws IllegalStateException if the file cannot be loaded or the environment section doesn't exist
     */
    @SuppressWarnings("unchecked")
    public YamlSource(Path file, String environment) {
        this.environment = environment;

        if (environment != null && !environment.isEmpty()) {
            this.sourceName = "Yaml(" + file.toString() + ")[" + environment + "]";
        } else {
            this.sourceName = "Yaml(" + file.toString() + ")";
        }

        this.root = loadYaml(file, environment, file.toString());
    }

    /**
     * Helper method to load YAML from a String path (supports classpath).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYamlFromString(String filePath, String environment) {
        try (Reader reader = FileResolver.openReader(filePath)) {
            Object loaded = YAML.load(reader);

            if (!(loaded instanceof Map)) {
                return null;
            }

            Map<String, Object> fullYaml = (Map<String, Object>) loaded;

            // If environment is specified, extract that section
            if (environment != null && !environment.isEmpty()) {
                Object envSection = fullYaml.get(environment);
                if (envSection == null || !(envSection instanceof Map)) {
                    throw new IllegalStateException("Environment section '" + environment +
                        "' not found in YAML file: " + filePath);
                }
                return (Map<String, Object>) envSection;
            } else {
                // Load all top-level keys
                return fullYaml;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load yaml file: " + filePath, e);
        }
    }

    /**
     * Helper method to load YAML from a Path.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(Path file, String environment, String displayPath) {
        try (Reader reader = Files.newBufferedReader(file)) {
            Object loaded = YAML.load(reader);

            if (!(loaded instanceof Map)) {
                return null;
            }

            Map<String, Object> fullYaml = (Map<String, Object>) loaded;

            // If environment is specified, extract that section
            if (environment != null && !environment.isEmpty()) {
                Object envSection = fullYaml.get(environment);
                if (envSection == null || !(envSection instanceof Map)) {
                    throw new IllegalStateException("Environment section '" + environment +
                        "' not found in YAML file: " + displayPath);
                }
                return (Map<String, Object>) envSection;
            } else {
                // Load all top-level keys
                return fullYaml;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load yaml file: " + displayPath, e);
        }
    }

    @Override
    public String getName() {
        return sourceName;
    }

    @Override
    public Optional<String> get(String key) {
        if (root == null) return Optional.empty();
        
        Object element = getNestedElement(root, key);
        if (element == null) {
            return Optional.empty();
        }
        
        // Convert the value to string
        if (element instanceof String) {
            return Optional.of((String) element);
        } else if (element instanceof Number || element instanceof Boolean) {
            return Optional.of(element.toString());
        } else if (element instanceof Map || element instanceof Iterable) {
            // For complex objects, return their string representation
            return Optional.of(element.toString());
        }
        
        return Optional.of(element.toString());
    }

    @Override
    public int getPriority() {
        return 30; // Medium-low priority for YAML files (same as JSON)
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

