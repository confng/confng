package org.confng;

import org.confng.api.ConfNGKey;
import org.confng.sources.ConfigSource;
import org.confng.sources.EnvSource;
import org.confng.sources.SystemPropertySource;
import org.confng.sources.PropertiesSource;
import org.confng.sources.JsonSource;

import org.reflections.Reflections;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Main configuration management class for ConfNG.
 * 
 * <p>ConfNG provides a unified way to access configuration values from multiple sources
 * with a defined precedence order. By default, it checks environment variables first,
 * then system properties, followed by any loaded properties or JSON files.</p>
 * 
 * <p>This class supports integration with secret managers like AWS Secrets Manager
 * and HashiCorp Vault, providing secure access to sensitive configuration data.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @version 1.0.0
 * @since 1.0
 * @see org.confng.api.ConfNGKey
 * @see org.confng.sources.ConfigSource
 */
public class ConfNG {

    private static final List<ConfigSource> sources = new ArrayList<>();
    private static final Map<String, String> resolvedValues = new HashMap<>();
    private static boolean isResolved = false;

    static {
        // Default precedence: Env, System properties, Properties files, JSON files
        sources.add(new EnvSource());
        sources.add(new SystemPropertySource());
    }

    public static void registerSource(ConfigSource source) {
        sources.add(source);
        invalidateResolution();
    }

    public static void registerSourceAt(int precedenceIndex, ConfigSource source) {
        if (precedenceIndex < 0 || precedenceIndex > sources.size()) {
            throw new IllegalArgumentException("Invalid precedence index: " + precedenceIndex);
        }
        sources.add(precedenceIndex, source);
        invalidateResolution();
    }

    public static void clearSourcesAndUseDefaults() {
        sources.clear();
        sources.add(new EnvSource());
        sources.add(new SystemPropertySource());
        invalidateResolution();
    }

    /**
     * Invalidates the current resolution cache, forcing re-resolution on next access.
     */
    private static void invalidateResolution() {
        isResolved = false;
        resolvedValues.clear();
    }

    /**
     * Loads a properties file as a configuration source.
     * If the file doesn't exist, it's silently skipped.
     * If the file exists but is invalid, throws a runtime exception.
     * 
     * @param filePath path to the properties file
     */
    public static void loadProperties(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return; // Skip silently if file doesn't exist
        }
        try {
            registerSource(new PropertiesSource(path));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load properties file: " + filePath, e);
        }
    }

    /**
     * Loads a JSON file as a configuration source.
     * If the file doesn't exist, it's silently skipped.
     * If the file exists but is invalid, throws a runtime exception.
     * 
     * @param filePath path to the JSON file
     */
    public static void loadJson(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return; // Skip silently if file doesn't exist
        }
        try {
            registerSource(new JsonSource(path));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON file: " + filePath, e);
        }
    }

    /**
     * Loads a custom secret source.
     * 
     * @param source the custom configuration source to add
     */
    public static void loadSecretSource(ConfigSource source) {
        registerSource(source);
    }

    /**
     * Resolves all configuration values from all sources according to precedence.
     * This method is called automatically when needed but can be called manually
     * to force immediate resolution of all values.
     */
    public static void resolveAllValues() {
        resolveAllValues(null);
    }

    /**
     * Resolves configuration values for the specified keys from all sources.
     * If keys is null, attempts to discover all keys from known ConfNGKey implementations.
     * 
     * @param keys specific keys to resolve, or null to discover all keys
     */
    public static void resolveAllValues(Set<String> keys) {
        if (isResolved && keys == null) {
            return; // Already resolved and no specific keys requested
        }

        Set<String> keysToResolve = keys;
        if (keysToResolve == null) {
            // Discover all keys from ConfNGKey implementations
            keysToResolve = new HashSet<>();
            List<ConfNGKey> discoveredKeys = discoverAllConfigKeys();
            for (ConfNGKey configKey : discoveredKeys) {
                keysToResolve.add(configKey.getKey());
            }
            
            // Also add any keys that sources might know about
            for (ConfigSource source : sources) {
                if (source instanceof EnvSource) {
                    // Add all environment variables
                    keysToResolve.addAll(System.getenv().keySet());
                } else if (source instanceof SystemPropertySource) {
                    // Add all system properties
                    for (Object key : System.getProperties().keySet()) {
                        keysToResolve.add(key.toString());
                    }
                }
            }
        }

        // Clear existing resolved values if we're doing a full resolution
        if (keys == null) {
            resolvedValues.clear();
        }

        // Resolve values according to source precedence
        for (String key : keysToResolve) {
            if (keys != null && resolvedValues.containsKey(key)) {
                continue; // Skip if already resolved and we're doing partial resolution
            }
            
            for (ConfigSource source : sources) {
                Optional<String> value = source.get(key);
                if (value.isPresent()) {
                    resolvedValues.put(key, value.get());
                    break; // First source wins (precedence order)
                }
            }
        }

        if (keys == null) {
            isResolved = true;
        }
    }



    /**
     * Gets a configuration value for the given key.
     * 
     * @param key the configuration key
     * @return the configuration value, or the default value if not found
     */
    public static String get(ConfNGKey key) {
        ensureResolved();
        String k = key.getKey();
        String value = resolvedValues.get(k);
        
        // If not found in resolved values, try to resolve this specific key
        if (value == null && !resolvedValues.containsKey(k)) {
            Set<String> singleKey = new HashSet<>();
            singleKey.add(k);
            resolveAllValues(singleKey);
            value = resolvedValues.get(k);
        }
        
        return value != null ? value : key.getDefaultValue();
    }

    /**
     * Ensures that configuration values are resolved.
     * If not already resolved, triggers resolution of all discoverable keys.
     */
    private static void ensureResolved() {
        if (!isResolved) {
            resolveAllValues();
        }
    }

    /**
     * Gets a configuration value as an integer.
     * 
     * @param key the configuration key
     * @return the configuration value as integer, or null if not found and no default
     * @throws IllegalArgumentException if the value cannot be converted to integer
     */
    public static Integer getInt(ConfNGKey key) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            String displayValue = key.isSensitive() ? "***MASKED***" : value;
            throw new IllegalArgumentException("Expected integer for key '" + key.getKey() + "' but got: " + displayValue, e);
        }
    }

    /**
     * Gets a configuration value as a boolean.
     * 
     * @param key the configuration key
     * @return the configuration value as boolean, or null if not found and no default
     * @throws IllegalArgumentException if the value cannot be converted to boolean
     */
    public static Boolean getBoolean(ConfNGKey key) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        String displayValue = key.isSensitive() ? "***MASKED***" : value;
        throw new IllegalArgumentException("Expected boolean for key '" + key.getKey() + "' but got: " + displayValue);
    }

    /**
     * Gets a configuration value with masking for sensitive data.
     * 
     * @param key the configuration key
     * @return the configuration value, masked if sensitive
     */
    public static String getForDisplay(ConfNGKey key) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        return key.isSensitive() ? "***MASKED***" : value;
    }

    /**
     * Gets all configuration values for display purposes.
     * Sensitive values are masked.
     * 
     * @param keys the configuration keys to display
     * @return a formatted string showing all configuration values
     */
    public static String getAllForDisplay(ConfNGKey... keys) {
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
     * Discovers all ConfNGKey implementations in the classpath.
     * 
     * @param basePackages packages to scan, if empty scans entire classpath
     * @return list of discovered configuration keys
     */
    public static List<ConfNGKey> discoverAllConfigKeys(String... basePackages) {
        List<ConfNGKey> discovered = new ArrayList<>();
        List<String> packages = (basePackages == null || basePackages.length == 0)
                ? Arrays.asList("")
                : Arrays.asList(basePackages);
        for (String p : packages) {
            Reflections reflections = (p == null || p.isEmpty()) ? new Reflections() : new Reflections(p);
            Set<Class<? extends ConfNGKey>> subtypes = reflections.getSubTypesOf(ConfNGKey.class);
            for (Class<? extends ConfNGKey> cls : subtypes) {
                if (cls.isEnum()) {
                    Object[] constants = cls.getEnumConstants();
                    if (constants != null) {
                        for (Object c : constants) {
                            discovered.add((ConfNGKey) c);
                        }
                    }
                }
            }
        }
        return discovered;
    }

    /**
     * Forces re-resolution of all configuration values.
     * Useful when configuration sources might have changed.
     */
    public static void refresh() {
        invalidateResolution();
        resolveAllValues();
    }

    /**
     * Gets the number of resolved configuration values.
     * 
     * @return number of resolved values
     */
    public static int getResolvedValueCount() {
        return resolvedValues.size();
    }

    /**
     * Checks if configuration values have been resolved.
     * 
     * @return true if values are resolved, false otherwise
     */
    public static boolean isResolved() {
        return isResolved;
    }

    /**
     * Gets all resolved configuration keys.
     * 
     * @return set of all resolved keys
     */
    public static Set<String> getResolvedKeys() {
        ensureResolved();
        return new HashSet<>(resolvedValues.keySet());
    }
}
