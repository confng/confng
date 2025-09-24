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
    
    /**
     * Returns the priority of this configuration source.
     * 
     * <p>Sources with higher priority values are consulted first in the resolution chain.
     * Default priorities:</p>
     * <ul>
     *   <li>100+ - Secret managers and secure sources</li>
     *   <li>80-99 - TestNG parameters and test-specific sources</li>
     *   <li>60-79 - Environment variables</li>
     *   <li>40-59 - System properties</li>
     *   <li>20-39 - Configuration files (JSON, Properties)</li>
     *   <li>0-19 - Default and fallback sources</li>
     * </ul>
     * 
     * @return the priority value, higher values have higher priority
     */
    default int getPriority() {
        return 10; // Default low priority
    }
}
