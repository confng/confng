package org.confng.sources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Configuration source that reads from Java properties files.
 * 
 * <p>This source loads configuration from standard Java properties files
 * using the Properties class for parsing.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @version 1.0.0
 * @since 1.0
 * @see org.confng.sources.ConfigSource
 */
public class PropertiesSource implements ConfigSource {

    private final Properties properties = new Properties();
    private final String sourceName;

    /**
     * Creates a new PropertiesSource from the given file.
     * 
     * @param file path to the properties file
     * @throws IllegalStateException if the file cannot be loaded
     */
    public PropertiesSource(Path file) {
        this.sourceName = "Properties(" + file.toString() + ")";
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load properties file: " + file, e);
        }
    }

    @Override
    public String getName() {
        return sourceName;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }
}