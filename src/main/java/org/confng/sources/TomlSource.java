package org.confng.sources;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

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
 * @see org.confng.sources.AbstractFileConfigSource
 */
public class TomlSource extends AbstractFileConfigSource<TomlTable> {

    /**
     * Creates a new TomlSource from the given file path (supports both filesystem and classpath).
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param filePath path to the TOML file (filesystem or classpath)
     * @throws IllegalStateException if the file cannot be loaded
     */
    public TomlSource(String filePath) {
        super(filePath);
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
        super(filePath, environment);
    }

    /**
     * Creates a new TomlSource from the given file.
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param file path to the TOML file
     * @throws IllegalStateException if the file cannot be loaded
     */
    public TomlSource(Path file) {
        super(file);
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
        super(file, environment);
    }

    @Override
    protected String getSourceTypePrefix() {
        return "Toml";
    }

    @Override
    protected TomlTable parseContent(Reader reader) throws IOException {
        TomlParseResult parseResult = Toml.parse(reader);
        if (parseResult.hasErrors()) {
            throw new IOException("Failed to parse TOML content. Errors: " + parseResult.errors());
        }
        return parseResult;
    }

    @Override
    protected TomlTable extractEnvironmentSection(TomlTable content, String environment, String filePath) {
        TomlTable envTable = content.getTable(environment);
        if (envTable == null) {
            throw new IllegalStateException("Environment section '" + environment +
                "' not found in TOML file: " + filePath);
        }
        return envTable;
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
