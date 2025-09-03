package org.confng.sources;

import java.util.Optional;

/**
 * Interface for configuration sources used by ConfNG.
 * 
 * <p>This interface defines the contract for configuration sources that can provide
 * configuration values to the ConfNG system. Implementations can read from various
 * sources such as environment variables, system properties, properties files,
 * JSON files, or custom sources.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @version 1.0.0
 * @since 1.0
 * @see org.confng.ConfNG
 */
public interface ConfigSource {
    
    /**
     * Returns the name of this configuration source.
     * 
     * @return the source name, never null
     */
    String getName();
    
    /**
     * Retrieves a configuration value for the given key.
     * 
     * <p>If the key is not found in this source, an empty Optional should be returned.
     * This allows the ConfNG system to try other sources in the resolution chain.</p>
     * 
     * @param key the configuration key to look up
     * @return an Optional containing the value if found, empty otherwise
     */
    Optional<String> get(String key);
}
