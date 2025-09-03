package org.confng.sources;

import java.util.Optional;
import java.util.Properties;

/**
 * Configuration source that reads from Java system properties.
 * 
 * <p>This source provides access to Java system properties set via
 * -D command line arguments or System.setProperty() calls.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @version 1.0.0
 * @since 1.0
 * @see org.confng.sources.ConfigSource
 */
public class SystemPropertySource implements ConfigSource {

    private final Properties properties;

    /**
     * Creates a new SystemPropertySource using System.getProperties().
     */
    public SystemPropertySource() {
        this.properties = System.getProperties();
    }

    /**
     * Creates a new SystemPropertySource with custom properties (for testing).
     * 
     * @param properties the properties to use
     */
    public SystemPropertySource(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String getName() {
        return "SystemProperties";
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }
}