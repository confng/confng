package org.confng.internal;

import org.confng.api.ConfNGKey;
import org.confng.api.ConfigSourceInfo;
import org.confng.api.ConfigurationException;
import org.confng.sources.ConfigSource;
import org.confng.sources.EnvSource;
import org.confng.sources.SystemPropertySource;
import org.confng.sources.TestNGParameterSource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Internal helper class responsible for resolving configuration values.
 * This class handles the resolution of configuration values from multiple sources
 * according to their precedence order.
 *
 * <p>This is an internal class and should not be used directly by clients.
 * Use {@link org.confng.ConfNG} instead.</p>
 *
 * @author Bharat Kumar Malviya
 * @since 1.0
 */
public final class ConfigResolver {

    private final Map<String, String> resolvedValues = new ConcurrentHashMap<>();
    private final Map<String, String> resolvedSources = new ConcurrentHashMap<>();
    private final Map<String, Integer> resolvedPriorities = new ConcurrentHashMap<>();
    private volatile boolean isResolved = false;
    private final Supplier<List<ConfigSource>> sourcesSupplier;
    private final Supplier<List<ConfNGKey>> keyDiscoverer;

    public ConfigResolver(Supplier<List<ConfigSource>> sourcesSupplier, Supplier<List<ConfNGKey>> keyDiscoverer) {
        this.sourcesSupplier = sourcesSupplier;
        this.keyDiscoverer = keyDiscoverer;
    }

    /**
     * Invalidates the current resolution cache.
     */
    public void invalidate() {
        isResolved = false;
        resolvedValues.clear();
        resolvedSources.clear();
        resolvedPriorities.clear();
    }

    /**
     * Resolves all configuration values from all sources.
     */
    public void resolveAll() {
        resolveAll(null);
    }

    /**
     * Resolves configuration values for the specified keys.
     */
    public void resolveAll(Set<String> keys) {
        if (isResolved && keys == null) {
            return;
        }

        List<ConfigSource> sources = sourcesSupplier.get();
        Set<String> keysToResolve = keys;

        if (keysToResolve == null) {
            keysToResolve = new HashSet<>();
            List<ConfNGKey> discoveredKeys = keyDiscoverer.get();
            for (ConfNGKey configKey : discoveredKeys) {
                keysToResolve.add(configKey.getKey());
            }

            for (ConfigSource source : sources) {
                if (source instanceof EnvSource) {
                    keysToResolve.addAll(System.getenv().keySet());
                } else if (source instanceof SystemPropertySource) {
                    for (Object key : System.getProperties().keySet()) {
                        keysToResolve.add(key.toString());
                    }
                }
            }
        }

        if (keys == null) {
            resolvedValues.clear();
            resolvedSources.clear();
            resolvedPriorities.clear();
        }

        for (String key : keysToResolve) {
            if (keys != null && resolvedValues.containsKey(key)) {
                continue;
            }

            for (ConfigSource source : sources) {
                Optional<String> value = source.get(key);
                if (value.isPresent()) {
                    resolvedValues.put(key, value.get());
                    resolvedSources.put(key, source.getName());
                    resolvedPriorities.put(key, source.getPriority());
                    break;
                }
            }
        }

        if (keys == null) {
            isResolved = true;
        }
    }

    /**
     * Gets a configuration value for the given key.
     */
    public String get(ConfNGKey key) {
        String k = key.getKey();
        List<ConfigSource> sources = sourcesSupplier.get();

        // Check TestNGParameterSource first if it's the highest priority
        if (!sources.isEmpty() && sources.get(0) instanceof TestNGParameterSource) {
            for (ConfigSource source : sources) {
                if (source instanceof TestNGParameterSource) {
                    Optional<String> value = source.get(k);
                    if (value.isPresent()) {
                        return value.get();
                    }
                }
            }
        }

        ensureResolved();
        String value = resolvedValues.get(k);
        if (value == null && !resolvedValues.containsKey(k)) {
            Set<String> singleKey = new HashSet<>();
            singleKey.add(k);
            resolveAll(singleKey);
            value = resolvedValues.get(k);
        }
        return value != null ? value : key.getDefaultValue();
    }

    /**
     * Gets a configuration value as an Optional.
     * Returns Optional.empty() if the value is not found and has no default.
     *
     * @param key the configuration key
     * @return an Optional containing the value if present, empty otherwise
     * @since 1.1.0
     */
    public Optional<String> getOptional(ConfNGKey key) {
        String value = get(key);
        return Optional.ofNullable(value);
    }

    /**
     * Gets a required configuration value.
     * Throws ConfigurationException if the value is not found and has no default.
     *
     * @param key the configuration key
     * @return the configuration value (never null)
     * @throws ConfigurationException if the value is not found and has no default
     * @since 1.1.0
     */
    public String getRequired(ConfNGKey key) {
        String value = get(key);
        if (value == null) {
            throw new ConfigurationException(key.getKey());
        }
        return value;
    }

    /**
     * Gets a configuration value with a fallback default.
     * Returns the provided default if the value is not found and the key has no default.
     *
     * @param key the configuration key
     * @param defaultValue the fallback default value
     * @return the configuration value, or the provided default if not found
     * @since 1.1.0
     */
    public String getOrDefault(ConfNGKey key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets a configuration value as an integer.
     */
    public Integer getInt(ConfNGKey key) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            String displayValue = key.isSensitive() ? "***MASKED***" : value;
            throw new IllegalArgumentException(
                "Expected integer for key '" + key.getKey() + "' but got: " + displayValue, e);
        }
    }

    /**
     * Gets a configuration value as a boolean.
     */
    public Boolean getBoolean(ConfNGKey key) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        String displayValue = key.isSensitive() ? "***MASKED***" : value;
        throw new IllegalArgumentException(
            "Expected boolean for key '" + key.getKey() + "' but got: " + displayValue);
    }

    /**
     * Gets a configuration value as a long.
     *
     * @param key the configuration key
     * @return the configuration value as long, or null if not found and no default
     * @throws IllegalArgumentException if the value cannot be converted to long
     */
    public Long getLong(ConfNGKey key) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            String displayValue = key.isSensitive() ? "***MASKED***" : value;
            throw new IllegalArgumentException(
                "Expected long for key '" + key.getKey() + "' but got: " + displayValue, e);
        }
    }

    /**
     * Gets a configuration value as a double.
     *
     * @param key the configuration key
     * @return the configuration value as double, or null if not found and no default
     * @throws IllegalArgumentException if the value cannot be converted to double
     */
    public Double getDouble(ConfNGKey key) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            String displayValue = key.isSensitive() ? "***MASKED***" : value;
            throw new IllegalArgumentException(
                "Expected double for key '" + key.getKey() + "' but got: " + displayValue, e);
        }
    }

    /**
     * Gets a configuration value as a list of strings.
     * Values are split by comma and trimmed.
     *
     * @param key the configuration key
     * @return the configuration value as a list, or null if not found and no default
     */
    public java.util.List<String> getList(ConfNGKey key) {
        return getList(key, ",");
    }

    /**
     * Gets a configuration value as a list of strings with a custom delimiter.
     *
     * @param key the configuration key
     * @param delimiter the delimiter to split values
     * @return the configuration value as a list, or null if not found and no default
     */
    public java.util.List<String> getList(ConfNGKey key, String delimiter) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        if (value.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        String[] parts = value.split(java.util.regex.Pattern.quote(delimiter));
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Gets a configuration value as a Duration.
     * Supports formats: "30s" (seconds), "5m" (minutes), "2h" (hours), "1d" (days),
     * "500ms" (milliseconds), or ISO-8601 duration format (e.g., "PT30S").
     *
     * @param key the configuration key
     * @return the configuration value as Duration, or null if not found and no default
     * @throws IllegalArgumentException if the value cannot be parsed as a duration
     */
    public java.time.Duration getDuration(ConfNGKey key) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        try {
            // Try ISO-8601 format first (e.g., "PT30S", "PT5M", "P1D")
            if (trimmed.startsWith("p") || trimmed.startsWith("-p")) {
                return java.time.Duration.parse(value.trim().toUpperCase());
            }

            // Parse simple formats: 30s, 5m, 2h, 1d, 500ms
            if (trimmed.endsWith("ms")) {
                long millis = Long.parseLong(trimmed.substring(0, trimmed.length() - 2).trim());
                return java.time.Duration.ofMillis(millis);
            } else if (trimmed.endsWith("s")) {
                long seconds = Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim());
                return java.time.Duration.ofSeconds(seconds);
            } else if (trimmed.endsWith("m")) {
                long minutes = Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim());
                return java.time.Duration.ofMinutes(minutes);
            } else if (trimmed.endsWith("h")) {
                long hours = Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim());
                return java.time.Duration.ofHours(hours);
            } else if (trimmed.endsWith("d")) {
                long days = Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim());
                return java.time.Duration.ofDays(days);
            } else {
                // Try parsing as plain milliseconds
                long millis = Long.parseLong(trimmed);
                return java.time.Duration.ofMillis(millis);
            }
        } catch (Exception e) {
            String displayValue = key.isSensitive() ? "***MASKED***" : value;
            throw new IllegalArgumentException(
                "Expected duration for key '" + key.getKey() + "' but got: " + displayValue +
                ". Supported formats: 30s, 5m, 2h, 1d, 500ms, or ISO-8601 (PT30S)", e);
        }
    }

    /**
     * Gets a configuration value with masking for sensitive data.
     */
    public String getForDisplay(ConfNGKey key) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        return key.isSensitive() ? "***MASKED***" : value;
    }

    /**
     * Gets all configuration values for display purposes.
     */
    public String getAllForDisplay(ConfNGKey... keys) {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration Values:\n");
        for (ConfNGKey key : keys) {
            String value = getForDisplay(key);
            sb.append("  ").append(key.getKey()).append(" = ").append(value);
            if (key.isSensitive()) {
                sb.append(" (sensitive)");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Forces re-resolution of all configuration values.
     */
    public void refresh() {
        invalidate();
        resolveAll();
    }

    /**
     * Gets the number of resolved configuration values.
     */
    public int getResolvedValueCount() {
        return resolvedValues.size();
    }

    /**
     * Checks if configuration values have been resolved.
     */
    public boolean isResolved() {
        return isResolved;
    }

    /**
     * Gets all resolved configuration keys.
     */
    public Set<String> getResolvedKeys() {
        ensureResolved();
        return new HashSet<>(resolvedValues.keySet());
    }

    private void ensureResolved() {
        if (!isResolved) {
            resolveAll();
        }
    }

    /**
     * Gets a configuration value for a given key string.
     */
    public String getConfigValue(String key) {
        List<ConfigSource> sources = sourcesSupplier.get();
        for (ConfigSource source : sources) {
            Optional<String> value = source.get(key);
            if (value.isPresent()) {
                return value.get();
            }
        }
        return null;
    }

    /**
     * Gets a configuration value using case-insensitive key matching.
     */
    public String getConfigValueCaseInsensitive(String key) {
        if (key == null) {
            return null;
        }

        String value = getConfigValue(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        String lowerKey = key.toLowerCase();
        if (!lowerKey.equals(key)) {
            value = getConfigValue(lowerKey);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        String upperKey = key.toUpperCase();
        if (!upperKey.equals(key)) {
            value = getConfigValue(upperKey);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        String titleKey = key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase();
        if (!titleKey.equals(key) && !titleKey.equals(lowerKey) && !titleKey.equals(upperKey)) {
            value = getConfigValue(titleKey);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        return null;
    }

    /**
     * Gets information about which source provided the value for a configuration key.
     *
     * @param key the configuration key
     * @return source information including source name, priority, and value
     * @since 1.1.0
     */
    public ConfigSourceInfo getSourceInfo(ConfNGKey key) {
        String k = key.getKey();
        String value = get(key);

        if (value == null) {
            return ConfigSourceInfo.notFound(k);
        }

        // Check if value came from a source or from default
        String sourceName = resolvedSources.get(k);
        if (sourceName != null) {
            int priority = resolvedPriorities.getOrDefault(k, 0);
            return new ConfigSourceInfo(k, sourceName, priority, value, key.isSensitive(), false);
        }

        // Value came from default
        return ConfigSourceInfo.fromDefault(k, value, key.isSensitive());
    }

    /**
     * Gets source information for all resolved configuration keys.
     *
     * @param keys the configuration keys to get info for
     * @return map of key name to source information
     * @since 1.1.0
     */
    public java.util.Map<String, ConfigSourceInfo> getAllSourceInfo(ConfNGKey... keys) {
        java.util.Map<String, ConfigSourceInfo> result = new java.util.LinkedHashMap<>();
        for (ConfNGKey key : keys) {
            result.put(key.getKey(), getSourceInfo(key));
        }
        return result;
    }

    /**
     * Gets source information for all resolved configuration keys.
     *
     * @param keys the configuration keys to get info for
     * @return map of key name to source information
     * @since 1.1.0
     */
    public java.util.Map<String, ConfigSourceInfo> getAllSourceInfo(java.util.List<ConfNGKey> keys) {
        java.util.Map<String, ConfigSourceInfo> result = new java.util.LinkedHashMap<>();
        for (ConfNGKey key : keys) {
            result.put(key.getKey(), getSourceInfo(key));
        }
        return result;
    }

    /**
     * Gets all configuration values that match a given prefix.
     *
     * @param prefix the prefix to match (e.g., "database." or "app.")
     * @return map of matching keys to their values
     * @since 1.1.0
     */
    public java.util.Map<String, String> getByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new java.util.LinkedHashMap<>();
        }

        ensureResolved();
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();

        // Check resolved values
        for (java.util.Map.Entry<String, String> entry : resolvedValues.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        // Also check all sources for keys that might not be in resolved values
        List<ConfigSource> sources = sourcesSupplier.get();
        for (ConfigSource source : sources) {
            // For sources that support key enumeration, we could add more keys
            // For now, we rely on resolved values which includes discovered keys
        }

        return result;
    }

    /**
     * Gets all configuration keys that match a given prefix.
     *
     * @param prefix the prefix to match (e.g., "database." or "app.")
     * @return set of matching keys
     * @since 1.1.0
     */
    public Set<String> getKeysWithPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new HashSet<>();
        }

        ensureResolved();
        Set<String> result = new HashSet<>();

        for (String key : resolvedValues.keySet()) {
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }

        return result;
    }

    /**
     * Gets all configuration values that match a given prefix, with source information.
     *
     * @param prefix the prefix to match
     * @param keys the ConfNGKey implementations to check for prefix matching
     * @return map of matching keys to their source information
     * @since 1.1.0
     */
    public java.util.Map<String, ConfigSourceInfo> getByPrefixWithInfo(String prefix, java.util.List<ConfNGKey> keys) {
        if (prefix == null || prefix.isEmpty()) {
            return new java.util.LinkedHashMap<>();
        }

        java.util.Map<String, ConfigSourceInfo> result = new java.util.LinkedHashMap<>();
        for (ConfNGKey key : keys) {
            if (key.getKey().startsWith(prefix)) {
                result.put(key.getKey(), getSourceInfo(key));
            }
        }
        return result;
    }

    /**
     * Gets a configuration value by string key directly from registered sources.
     *
     * <p>This method bypasses the ConfNGKey enum and looks up values directly
     * from all registered configuration sources.</p>
     *
     * @param key the configuration key as a string
     * @return the configuration value, or null if not found
     * @since 1.1.0
     */
    public String getFromSources(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        ensureResolved();

        // Check sources in priority order
        List<ConfigSource> sources = sourcesSupplier.get();
        for (ConfigSource source : sources) {
            Optional<String> value = source.get(key);
            if (value.isPresent()) {
                return value.get();
            }
        }

        return null;
    }
}
