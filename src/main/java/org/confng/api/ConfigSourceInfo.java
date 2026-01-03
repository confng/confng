package org.confng.api;

/**
 * Contains information about which configuration source provided a value.
 *
 * <p>This class is used for debugging and diagnostics to understand
 * where configuration values are coming from in the resolution chain.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ConfigSourceInfo info = ConfNG.getSourceInfo(MyConfig.DATABASE_URL);
 * System.out.println("Value came from: " + info.getSourceName());
 * System.out.println("Priority: " + info.getPriority());
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see org.confng.ConfNG#getSourceInfo(ConfNGKey)
 */
public class ConfigSourceInfo {

    private final String key;
    private final String sourceName;
    private final int priority;
    private final String value;
    private final boolean sensitive;
    private final boolean fromDefault;

    /**
     * Creates a new ConfigSourceInfo.
     *
     * @param key the configuration key
     * @param sourceName the name of the source that provided the value
     * @param priority the priority of the source
     * @param value the resolved value
     * @param sensitive whether the value is sensitive
     * @param fromDefault whether the value came from the key's default
     */
    public ConfigSourceInfo(String key, String sourceName, int priority, 
                           String value, boolean sensitive, boolean fromDefault) {
        this.key = key;
        this.sourceName = sourceName;
        this.priority = priority;
        this.value = value;
        this.sensitive = sensitive;
        this.fromDefault = fromDefault;
    }

    /**
     * Creates a ConfigSourceInfo for a value from the key's default.
     *
     * @param key the configuration key
     * @param value the default value
     * @param sensitive whether the value is sensitive
     * @return a ConfigSourceInfo indicating the value is from default
     */
    public static ConfigSourceInfo fromDefault(String key, String value, boolean sensitive) {
        return new ConfigSourceInfo(key, "Default", 0, value, sensitive, true);
    }

    /**
     * Creates a ConfigSourceInfo for a missing value.
     *
     * @param key the configuration key
     * @return a ConfigSourceInfo indicating no value was found
     */
    public static ConfigSourceInfo notFound(String key) {
        return new ConfigSourceInfo(key, null, 0, null, false, false);
    }

    /**
     * Gets the configuration key.
     *
     * @return the key name
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the name of the source that provided the value.
     *
     * @return the source name, or null if not found
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Gets the priority of the source.
     *
     * @return the source priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Gets the resolved value.
     * Returns "***MASKED***" for sensitive values.
     *
     * @return the value or masked placeholder
     */
    public String getValue() {
        if (sensitive && value != null) {
            return "***MASKED***";
        }
        return value;
    }

    /**
     * Gets the raw value without masking.
     * Use with caution for sensitive values.
     *
     * @return the raw value
     */
    public String getRawValue() {
        return value;
    }

    /**
     * Checks if the value is sensitive.
     *
     * @return true if sensitive
     */
    public boolean isSensitive() {
        return sensitive;
    }

    /**
     * Checks if the value came from the key's default.
     *
     * @return true if from default
     */
    public boolean isFromDefault() {
        return fromDefault;
    }

    /**
     * Checks if a value was found.
     *
     * @return true if a value was found
     */
    public boolean isFound() {
        return value != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConfigSourceInfo{key='").append(key).append("'");
        if (sourceName != null) {
            sb.append(", source='").append(sourceName).append("'");
            sb.append(", priority=").append(priority);
        }
        sb.append(", value='").append(getValue()).append("'");
        if (fromDefault) {
            sb.append(", fromDefault=true");
        }
        sb.append("}");
        return sb.toString();
    }
}

