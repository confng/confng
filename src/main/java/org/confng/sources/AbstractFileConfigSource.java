package org.confng.sources;

import org.confng.util.FileResolver;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Abstract base class for file-based configuration sources.
 * 
 * <p>This class provides common functionality for loading configuration from files,
 * including support for both filesystem and classpath resources, and environment-specific
 * configuration sections.</p>
 * 
 * <p>Subclasses must implement the parsing logic for their specific file format.</p>
 * 
 * @param <T> the type of the parsed configuration object (e.g., JsonObject, Map, TomlTable)
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0.3
 * @see org.confng.sources.ConfigSource
 */
public abstract class AbstractFileConfigSource<T> implements ConfigSource {

    /** The parsed configuration root object */
    protected final T root;
    
    /** The source name for display purposes */
    protected final String sourceName;
    
    /** The environment section that was loaded, or null for all top-level keys */
    protected final String environment;

    /**
     * Creates a new file-based config source from the given file path.
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param filePath path to the configuration file (filesystem or classpath)
     * @throws IllegalStateException if the file cannot be loaded
     */
    protected AbstractFileConfigSource(String filePath) {
        this(filePath, null);
    }

    /**
     * Creates a new file-based config source from the given file path for a specific environment.
     * When an environment is specified, only configuration from that section is loaded.
     *
     * @param filePath path to the configuration file (filesystem or classpath)
     * @param environment the environment section to load, or null for all top-level keys
     * @throws IllegalStateException if the file cannot be loaded or the environment section doesn't exist
     */
    protected AbstractFileConfigSource(String filePath, String environment) {
        this.environment = environment;
        this.sourceName = buildSourceName(filePath, environment);
        this.root = loadFromFilePath(filePath, environment);
    }

    /**
     * Creates a new file-based config source from the given Path.
     * Loads all top-level keys (not nested in environment sections).
     *
     * @param file path to the configuration file
     * @throws IllegalStateException if the file cannot be loaded
     */
    protected AbstractFileConfigSource(Path file) {
        this(file, null);
    }

    /**
     * Creates a new file-based config source from the given Path for a specific environment.
     * When an environment is specified, only configuration from that section is loaded.
     *
     * @param file path to the configuration file
     * @param environment the environment section to load, or null for all top-level keys
     * @throws IllegalStateException if the file cannot be loaded or the environment section doesn't exist
     */
    protected AbstractFileConfigSource(Path file, String environment) {
        this.environment = environment;
        this.sourceName = buildSourceName(file.toString(), environment);
        this.root = loadFromPath(file, environment, file.toString());
    }

    /**
     * Builds the source name for display purposes.
     *
     * @param filePath the file path
     * @param environment the environment, or null
     * @return the formatted source name
     */
    private String buildSourceName(String filePath, String environment) {
        String prefix = getSourceTypePrefix();
        if (environment != null && !environment.isEmpty()) {
            return prefix + "(" + filePath + ")[" + environment + "]";
        } else {
            return prefix + "(" + filePath + ")";
        }
    }

    /**
     * Returns the source type prefix for the source name (e.g., "Json", "Yaml", "Toml").
     *
     * @return the source type prefix
     */
    protected abstract String getSourceTypePrefix();

    /**
     * Parses the configuration content from a Reader.
     *
     * @param reader the reader to parse from
     * @return the parsed configuration object
     * @throws IOException if parsing fails
     */
    protected abstract T parseContent(Reader reader) throws IOException;

    /**
     * Extracts the environment-specific section from the parsed configuration.
     *
     * @param content the full parsed configuration
     * @param environment the environment section to extract
     * @param filePath the file path for error messages
     * @return the environment-specific configuration section
     * @throws IllegalStateException if the environment section doesn't exist
     */
    protected abstract T extractEnvironmentSection(T content, String environment, String filePath);

    /**
     * Gets a value from the parsed configuration using the given key.
     * Supports dot notation for nested keys (e.g., "app.name").
     *
     * @param key the configuration key
     * @return the value as a string, or null if not found
     */
    protected abstract String getValueFromRoot(String key);

    /**
     * Loads configuration from a string file path (supports classpath).
     */
    private T loadFromFilePath(String filePath, String environment) {
        try (Reader reader = FileResolver.openReader(filePath)) {
            T content = parseContent(reader);
            
            if (content == null) {
                return null;
            }

            if (environment != null && !environment.isEmpty()) {
                return extractEnvironmentSection(content, environment, filePath);
            } else {
                return content;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + getSourceTypePrefix().toLowerCase() + 
                " file: " + filePath, e);
        }
    }

    /**
     * Loads configuration from a Path.
     */
    private T loadFromPath(Path file, String environment, String displayPath) {
        try (Reader reader = Files.newBufferedReader(file)) {
            T content = parseContent(reader);
            
            if (content == null) {
                return null;
            }

            if (environment != null && !environment.isEmpty()) {
                return extractEnvironmentSection(content, environment, displayPath);
            } else {
                return content;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + getSourceTypePrefix().toLowerCase() + 
                " file: " + displayPath, e);
        }
    }

    @Override
    public String getName() {
        return sourceName;
    }

    @Override
    public Optional<String> get(String key) {
        if (root == null) {
            return Optional.empty();
        }
        
        String value = getValueFromRoot(key);
        return value != null ? Optional.of(value) : Optional.empty();
    }

    @Override
    public int getPriority() {
        return 30; // Medium-low priority for file-based sources
    }
}

