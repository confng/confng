package org.confng.api;

/**
 * Interface for configuration keys used by ConfNG.
 * 
 * <p>This interface defines the contract for configuration keys that can be used
 * with the ConfNG configuration management system. Implementations should provide
 * a unique key identifier, default value, and sensitivity marking for proper
 * handling in logs and output.</p>
 * 
 * <p>Typically implemented by enums to provide type-safe configuration keys
 * with compile-time checking.</p>
 * 
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * public enum MyConfig implements ConfNGKey {
 *     DATABASE_URL("db.url", "localhost:5432", false),
 *     API_KEY("api.key", null, true);
 *     
 *     // Implementation details...
 * }
 * }</pre>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @version 1.0.0
 * @since 1.0
 * @see org.confng.ConfNG
 */
public interface ConfNGKey {
    
    /**
     * Returns the configuration key identifier.
     * 
     * <p>This key will be used to look up configuration values from various
     * sources such as environment variables, system properties, properties files,
     * and JSON configuration files.</p>
     * 
     * @return the configuration key identifier, never null
     */
    String getKey();
    
    /**
     * Returns the default value for this configuration key.
     * 
     * <p>This value will be returned when no configuration source provides
     * a value for this key. Can be null if no default is appropriate.</p>
     * 
     * @return the default value, may be null
     */
    String getDefaultValue();
    
    /**
     * Indicates whether this configuration key contains sensitive information.
     * 
     * <p>Sensitive configurations (like passwords, API keys, tokens) should be
     * masked in logs and debug output to prevent accidental exposure of
     * sensitive information.</p>
     * 
     * @return true if this configuration contains sensitive data, false otherwise
     */
    boolean isSensitive();
}
