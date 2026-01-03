package org.confng.api;

/**
 * Exception thrown when a required configuration value is missing or invalid.
 *
 * <p>This exception is used by methods like {@link org.confng.ConfNG#getRequired(ConfNGKey)}
 * to indicate that a mandatory configuration value could not be found in any
 * configuration source.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try {
 *     String dbUrl = ConfNG.getRequired(MyConfig.DATABASE_URL);
 * } catch (ConfigurationException e) {
 *     // Handle missing required configuration
 *     System.err.println("Missing required config: " + e.getKey());
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see org.confng.ConfNG#getRequired(ConfNGKey)
 */
public class ConfigurationException extends RuntimeException {

    private final String key;
    private final String message;

    /**
     * Creates a new ConfigurationException for a missing required configuration.
     *
     * @param key the configuration key that was missing
     */
    public ConfigurationException(String key) {
        super("Required configuration key '" + key + "' is not set and has no default value");
        this.key = key;
        this.message = getMessage();
    }

    /**
     * Creates a new ConfigurationException with a custom message.
     *
     * @param key the configuration key
     * @param message the error message
     */
    public ConfigurationException(String key, String message) {
        super(message);
        this.key = key;
        this.message = message;
    }

    /**
     * Creates a new ConfigurationException with a cause.
     *
     * @param key the configuration key
     * @param message the error message
     * @param cause the underlying cause
     */
    public ConfigurationException(String key, String message, Throwable cause) {
        super(message, cause);
        this.key = key;
        this.message = message;
    }

    /**
     * Gets the configuration key that caused this exception.
     *
     * @return the configuration key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getErrorMessage() {
        return message;
    }
}

