package org.confng.sources;

import org.confng.util.FileResolver;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Configuration source that reads from TOML files.
 * 
 * <p>This source loads configuration from TOML files using TomlJ
 * for parsing. Supports both primitive values and nested objects.</p>
 * 
 * <p>Supports environment-specific configuration sections. When an environment
 * is specified, only configuration from that section is loaded. For example:</p>
 * 
 * <pre>
 * [uat]
 * database.host = "uat-db.example.com"
 * api.url = "https://uat-api.example.com"
 * 
 * [prod]
 * database.host = "prod-db.example.com"
 * api.url = "https://prod-api.example.com"
 * </pre>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 * @see org.confng.sources.ConfigSource
 */
public class TomlSource implements ConfigSource {

    private final TomlTable root;
    private final String sourceName;
    private final String environment;

    /**
     * Creates a new TomlSource from the given file path (supports both filesystem and classpath).
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param filePath path to the TOML file (filesystem or classpath)
     * @throws IllegalStateException if the file cannot be loaded
     */
    public TomlSource(String filePath) {
        this(filePath, null);
    }

    /**
     * Creates a new TomlSource from the given file path for a specific environment (supports both filesystem and classpath).
     * When an environment is specified, only configuration from that section is loaded.
     *
     * @param filePath path to the TOML file (filesystem or classpath)
     * @param environment the environment section to load (e.g., "uat", "prod"), or null for all top-level keys
     * @throws IllegalStateException if the file cannot be loaded or the environment section doesn't exist
     */
    public TomlSource(String filePath, String environment) {
        this.environment = environment;

        if (environment != null && !environment.isEmpty()) {
            this.sourceName = "Toml(" + filePath + ")[" + environment + "]";
        } else {
            this.sourceName = "Toml(" + filePath + ")";
        }

        this.root = loadTomlFromString(filePath, environment);
    }

    /**
     * Creates a new TomlSource from the given file.
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param file path to the TOML file
     * @throws IllegalStateException if the file cannot be loaded
     */
    public TomlSource(Path file) {
        this(file, null);
    }

    /**
     * Creates a new TomlSource from the given file for a specific environment.
     * When an environment is specified, only configuration from that section is loaded.
     *
     * @param file path to the TOML file
     * @param environment the environment section to load (e.g., "env1", "env2"), or null for all top-level keys
     * @throws IllegalStateException if the file cannot be loaded or the environment section doesn't exist
     */
    public TomlSource(Path file, String environment) {
        this.environment = environment;

        if (environment != null && !environment.isEmpty()) {
            this.sourceName = "Toml(" + file.toString() + ")[" + environment + "]";
        } else {
            this.sourceName = "Toml(" + file.toString() + ")";
        }

        this.root = loadToml(file, environment, file.toString());
    }

    /**
     * Helper method to load TOML from a String path (supports classpath).
     */
    private TomlTable loadTomlFromString(String filePath, String environment) {
        try (Reader reader = FileResolver.openReader(filePath)) {
            TomlParseResult parseResult = Toml.parse(reader);

            if (parseResult.hasErrors()) {
                throw new IllegalStateException("Failed to parse TOML file: " + filePath +
                    ". Errors: " + parseResult.errors());
            }

            // If environment is specified, extract that section
            if (environment != null && !environment.isEmpty()) {
                TomlTable envTable = parseResult.getTable(environment);
                if (envTable == null) {
                    throw new IllegalStateException("Environment section '" + environment +
                        "' not found in TOML file: " + filePath);
                }
                return envTable;
            } else {
                // Load all top-level keys
                return parseResult;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load TOML file: " + filePath, e);
        }
    }

    /**
     * Helper method to load TOML from a Path.
     */
    private TomlTable loadToml(Path file, String environment, String displayPath) {
        try {
            TomlParseResult parseResult = Toml.parse(file);

            if (parseResult.hasErrors()) {
                throw new IllegalStateException("Failed to parse TOML file: " + displayPath +
                    ". Errors: " + parseResult.errors());
            }

            // If environment is specified, extract that section
            if (environment != null && !environment.isEmpty()) {
                TomlTable envTable = parseResult.getTable(environment);
                if (envTable == null) {
                    throw new IllegalStateException("Environment section '" + environment +
                        "' not found in TOML file: " + displayPath);
                }
                return envTable;
            } else {
                // Load all top-level keys
                return parseResult;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load TOML file: " + displayPath, e);
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
        } else if (element instanceof TomlTable) {
            // For complex objects, return their string representation
            return Optional.of(element.toString());
        }
        
        return Optional.of(element.toString());
    }

    @Override
    public int getPriority() {
        return 30; // Medium-low priority for TOML files (same as JSON and YAML)
    }
    
    /**
     * Retrieves a nested TOML element using dot notation.
     * For example, "app.name" will look for [app] name = "value"
     * or app.name = "value" in dotted key notation.
     */
    private Object getNestedElement(TomlTable table, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        // First, try to get the value directly using the full key
        // This handles TOML's dotted key notation (e.g., database.host = "value")
        Object directValue = table.get(key);
        if (directValue != null) {
            return directValue;
        }
        
        // If not found, try navigating through nested tables
        // This handles TOML's table notation (e.g., [database] host = "value")
        String[] parts = key.split("\\.");
        Object current = table;
        
        for (String part : parts) {
            if (current == null || !(current instanceof TomlTable)) {
                return null;
            }
            current = ((TomlTable) current).get(part);
        }
        
        return current;
    }
}

