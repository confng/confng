package org.confng;

import org.confng.api.ConfNGKey;
import org.confng.sources.*;
import org.confng.util.FileResolver;

import org.reflections.Reflections;

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
 * then system properties, followed by any loaded properties, JSON, YAML, or TOML files.</p>
 *
 * <p>This class supports integration with secret managers like AWS Secrets Manager
 * and HashiCorp Vault, providing secure access to sensitive configuration data.</p>
 *
 * <h2>Global vs Environment-Specific Configuration</h2>
 * <p>ConfNG supports a powerful pattern for managing configurations across multiple environments
 * by separating global/common settings from environment-specific overrides:</p>
 *
 * <h3>Global/Common Configuration</h3>
 * <p>Define baseline configuration that applies to all environments in global configuration files:</p>
 * <ul>
 *   <li>global.properties, global.json, global.yaml, global.toml</li>
 *   <li>common.properties, common.json, common.yaml, common.toml</li>
 * </ul>
 * <pre>
 * // Load global configuration (applies to all environments)
 * ConfNG.loadGlobalConfig();
 * </pre>
 *
 * <h3>Environment-Specific Configuration</h3>
 * <p>Override global settings with environment-specific values (env1, env2, env3, etc.):</p>
 * <pre>
 * // Load environment-specific files (overrides global config)
 * ConfNG.loadConfigForEnvironment("env1");  // Loads env1.properties, env1.json, env1.yaml, env1.toml
 *
 * // Or automatically detect environment from APP_ENV, ENVIRONMENT, or ENV variables
 * String env = ConfNG.autoLoadConfig();  // Automatically loads config for detected environment
 * </pre>
 *
 * <h3>Recommended Loading Order</h3>
 * <p>For best practices, load global configuration first, then environment-specific:</p>
 * <pre>
 * // 1. Load global/common configuration
 * ConfNG.loadGlobalConfig();
 *
 * // 2. Load environment-specific configuration (overrides global)
 * ConfNG.loadConfigForEnvironment("prod");
 * </pre>
 *
 * <h3>Example Configuration Structure</h3>
 * <p>global.json (shared across all environments):</p>
 * <pre>
 * {
 *   "api.timeout": "30",
 *   "retry.count": "3",
 *   "log.level": "INFO"
 * }
 * </pre>
 *
 * <p>uat.json (UAT-specific overrides):</p>
 * <pre>
 * {
 *   "api.url": "https://uat-api.example.com",
 *   "api.timeout": "60",
 *   "database.host": "uat-db.example.com"
 * }
 * </pre>
 *
 * <p>Result for UAT: api.timeout=60 (overridden), retry.count=3 (inherited from global),
 * log.level=INFO (inherited from global), plus UAT-specific api.url and database.host</p>
 *
 * <h2>Additional Configuration Patterns</h2>
 *
 * <h3>Environment Sections Within Files</h3>
 * <p>Store multiple environments in a single file with sections:</p>
 * <pre>
 * // Load specific environment section from a file
 * ConfNG.loadJson("config.json", "uat");    // Loads only the "uat" section
 * ConfNG.loadYaml("config.yaml", "prod");   // Loads only the "prod" section
 * ConfNG.loadToml("config.toml", "qa");     // Loads only the "qa" section
 * </pre>
 *
 * <h3>TestNG Integration</h3>
 * <p>When using TestNG, the {@link org.confng.testng.TestNGParameterListener} automatically
 * loads global configuration followed by environment-specific configuration at suite startup.
 * Simply add an "environment" or "env" parameter to your testng.xml:</p>
 * <pre>
 * &lt;suite name="My Test Suite"&gt;
 *   &lt;parameter name="environment" value="uat"/&gt;
 *   ...
 * &lt;/suite&gt;
 * </pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 * @see org.confng.api.ConfNGKey
 * @see org.confng.sources.ConfigSource
 * @see org.confng.testng.TestNGParameterListener
 */
public class ConfNG {

    /**
     * Internal configuration keys used by ConfNG itself.
     * These keys are not part of the public API and are used for internal configuration management.
     */
    private enum SystemConfigKeys implements ConfNGKey {
        /**
         * Environment name key that checks APP_ENV, ENVIRONMENT, and ENV in order.
         * Defaults to "local" if none are set.
         */
        ENVIRONMENT("APP_ENV", "local", false);

        private final String key;
        private final String defaultValue;
        private final boolean sensitive;

        SystemConfigKeys(String key, String defaultValue, boolean sensitive) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.sensitive = sensitive;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getDefaultValue() {
            return defaultValue;
        }

        @Override
        public boolean isSensitive() {
            return sensitive;
        }
    }

    private static final List<ConfigSource> sources = new ArrayList<>();
    private static final Map<String, String> resolvedValues = new HashMap<>();
    private static boolean isResolved = false;

    static {
        // Default precedence: Env, System properties, Properties files, JSON files, YAML files, TOML files
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

    /**
     * Adds a configuration source with automatic priority-based ordering.
     * Sources with higher priority values are placed earlier in the resolution chain.
     * 
     * @param source the configuration source to add
     */
    public static void addSource(ConfigSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Configuration source cannot be null");
        }

        // Find the correct position based on priority
        int insertIndex = 0;
        int sourcePriority = source.getPriority();
        
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).getPriority() < sourcePriority) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }
        
        sources.add(insertIndex, source);
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
     * Loads a configuration file by auto-detecting its type based on the file extension.
     * Supports .properties, .json, .yaml, .yml, and .toml files.
     * Supports loading from both filesystem and classpath (including JAR resources).
     * If the file doesn't exist, it will be silently skipped.
     *
     * @param filePath path to the configuration file (filesystem or classpath)
     */
    public static void load(String filePath) {
        // Resolve the file using FileResolver
        FileResolver.ResolvedFile resolved = FileResolver.resolve(filePath);

        // If file doesn't exist in either location, skip silently
        if (resolved == null || !resolved.exists()) {
            return;
        }

        String fileName = resolved.getFileName().toLowerCase();

        // Check file type and load accordingly
        if (fileName.endsWith(".properties")) {
            try {
                registerSource(new PropertiesSource(filePath));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load properties file: " + filePath, e);
            }
        } else if (fileName.endsWith(".json")) {
            try {
                registerSource(new JsonSource(filePath));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load JSON file: " + filePath, e);
            }
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            try {
                registerSource(new YamlSource(filePath));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load YAML file: " + filePath, e);
            }
        } else if (fileName.endsWith(".toml")) {
            try {
                registerSource(new TomlSource(filePath));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load TOML file: " + filePath, e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileName +
                ". Supported types: .properties, .json, .yaml, .yml, .toml");
        }
    }

    /**
     * Loads a configuration file with environment-specific section by auto-detecting its type.
     * Supports .json, .yaml, .yml, and .toml files with environment sections.
     * Supports loading from both filesystem and classpath (including JAR resources).
     * Properties files don't support environment sections.
     * If the file doesn't exist, it will be silently skipped.
     *
     * @param filePath path to the configuration file (filesystem or classpath)
     * @param environment the environment section to load (e.g., "uat", "beta", "qa", "prod")
     */
    public static void load(String filePath, String environment) {
        // Resolve the file using FileResolver
        FileResolver.ResolvedFile resolved = FileResolver.resolve(filePath);

        // If file doesn't exist in either location, skip silently
        if (resolved == null || !resolved.exists()) {
            return;
        }

        String fileName = resolved.getFileName().toLowerCase();

        // Check file type and load accordingly
        if (fileName.endsWith(".properties")) {
            throw new IllegalArgumentException("Properties files don't support environment sections. " +
                "Use load(filePath) instead or use separate files per environment.");
        } else if (fileName.endsWith(".json")) {
            try {
                registerSource(new JsonSource(filePath, environment));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load JSON file: " + filePath +
                    " for environment: " + environment, e);
            }
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            try {
                registerSource(new YamlSource(filePath, environment));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load YAML file: " + filePath +
                    " for environment: " + environment, e);
            }
        } else if (fileName.endsWith(".toml")) {
            try {
                registerSource(new TomlSource(filePath, environment));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load TOML file: " + filePath +
                    " for environment: " + environment, e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileName +
                ". Supported types for environment sections: .json, .yaml, .yml, .toml");
        }
    }

    /**
     * Loads a properties file as a configuration source.
     * Supports loading from both filesystem and classpath (including JAR resources).
     * If the file doesn't exist, it's silently skipped.
     * If the file exists but is invalid, throws a runtime exception.
     *
     * @param filePath path to the properties file (filesystem or classpath)
     */
    public static void loadProperties(String filePath) {
        if (!FileResolver.exists(filePath)) {
            return; // Skip silently if file doesn't exist
        }
        try {
            registerSource(new PropertiesSource(filePath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load properties file: " + filePath, e);
        }
    }

    /**
     * Loads a JSON file as a configuration source.
     * Supports loading from both filesystem and classpath (including JAR resources).
     * If the file doesn't exist, it's silently skipped.
     * If the file exists but is invalid, throws a runtime exception.
     *
     * @param filePath path to the JSON file (filesystem or classpath)
     */
    public static void loadJson(String filePath) {
        if (!FileResolver.exists(filePath)) {
            return; // Skip silently if file doesn't exist
        }
        try {
            registerSource(new JsonSource(filePath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON file: " + filePath, e);
        }
    }

    /**
     * Loads a YAML file as a configuration source.
     * Supports loading from both filesystem and classpath (including JAR resources).
     * If the file doesn't exist, it's silently skipped.
     * If the file exists but is invalid, throws a runtime exception.
     *
     * @param filePath path to the YAML file (filesystem or classpath)
     */
    public static void loadYaml(String filePath) {
        if (!FileResolver.exists(filePath)) {
            return; // Skip silently if file doesn't exist
        }
        try {
            registerSource(new YamlSource(filePath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML file: " + filePath, e);
        }
    }

    /**
     * Loads a TOML file as a configuration source.
     * Loads all top-level keys (not nested in environment sections).
     * Supports loading from both filesystem and classpath (including JAR resources).
     * If the file doesn't exist, it's silently skipped.
     * If the file exists but is invalid, throws a runtime exception.
     *
     * @param filePath path to the TOML file (filesystem or classpath)
     */
    public static void loadToml(String filePath) {
        if (!FileResolver.exists(filePath)) {
            return; // Skip silently if file doesn't exist
        }
        try {
            registerSource(new TomlSource(filePath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load TOML file: " + filePath, e);
        }
    }

    /**
     * Loads a TOML file as a configuration source for a specific environment.
     * Only configuration from the specified environment section is loaded.
     * Supports loading from both filesystem and classpath (including JAR resources).
     * If the file doesn't exist, it's silently skipped.
     * If the file exists but is invalid or the environment section doesn't exist, throws a runtime exception.
     *
     * @param filePath path to the TOML file (filesystem or classpath)
     * @param environment the environment section to load (e.g., "uat", "beta", "qa", "prod")
     */
    public static void loadToml(String filePath, String environment) {
        if (!FileResolver.exists(filePath)) {
            return; // Skip silently if file doesn't exist
        }
        try {
            registerSource(new TomlSource(filePath, environment));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load TOML file: " + filePath +
                " for environment: " + environment, e);
        }
    }

    /**
     * Loads a JSON file as a configuration source for a specific environment.
     * Only configuration from the specified environment section is loaded.
     * Supports loading from both filesystem and classpath (including JAR resources).
     * If the file doesn't exist, it's silently skipped.
     * If the file exists but is invalid or the environment section doesn't exist, throws a runtime exception.
     *
     * @param filePath path to the JSON file (filesystem or classpath)
     * @param environment the environment section to load (e.g., "uat", "beta", "qa", "prod")
     */
    public static void loadJson(String filePath, String environment) {
        if (!FileResolver.exists(filePath)) {
            return; // Skip silently if file doesn't exist
        }
        try {
            registerSource(new JsonSource(filePath, environment));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON file: " + filePath +
                " for environment: " + environment, e);
        }
    }

    /**
     * Loads a YAML file as a configuration source for a specific environment.
     * Only configuration from the specified environment section is loaded.
     * Supports loading from both filesystem and classpath (including JAR resources).
     * If the file doesn't exist, it's silently skipped.
     * If the file exists but is invalid or the environment section doesn't exist, throws a runtime exception.
     *
     * @param filePath path to the YAML file (filesystem or classpath)
     * @param environment the environment section to load (e.g., "uat", "beta", "qa", "prod")
     */
    public static void loadYaml(String filePath, String environment) {
        if (!FileResolver.exists(filePath)) {
            return; // Skip silently if file doesn't exist
        }
        try {
            registerSource(new YamlSource(filePath, environment));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML file: " + filePath +
                " for environment: " + environment, e);
        }
    }

    /**
     * Loads global/common configuration files that apply to all environments.
     * Automatically looks for and loads the following files if they exist:
     * <ul>
     *   <li>global.properties</li>
     *   <li>global.json</li>
     *   <li>global.yaml</li>
     *   <li>global.toml</li>
     *   <li>common.properties</li>
     *   <li>common.json</li>
     *   <li>common.yaml</li>
     *   <li>common.toml</li>
     * </ul>
     *
     * <p>Files that don't exist are silently skipped. This method should be called before
     * loading environment-specific configuration to establish a baseline configuration that
     * can be overridden by environment-specific values.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Load global configuration first
     * ConfNG.loadGlobalConfig();
     *
     * // Then load environment-specific configuration (which can override global values)
     * ConfNG.loadConfigForEnvironment("uat");
     * </pre>
     */
    public static void loadGlobalConfig() {
        // Load global configuration files (silently skip if they don't exist)
        loadProperties("global.properties");
        loadJson("global.json");
        loadYaml("global.yaml");
        loadToml("global.toml");

        // Also load common configuration files (alternative naming convention)
        loadProperties("common.properties");
        loadJson("common.json");
        loadYaml("common.yaml");
        loadToml("common.toml");
    }

    /**
     * Loads all environment-specific configuration files for the given environment.
     * Automatically looks for and loads the following files if they exist:
     * <ul>
     *   <li>{environment}.properties</li>
     *   <li>{environment}.json</li>
     *   <li>{environment}.yaml</li>
     *   <li>{environment}.toml</li>
     * </ul>
     *
     * <p>Files that don't exist are silently skipped. This method provides a convenient
     * way to load all configuration for a specific environment with a single call.</p>
     *
     * <p><strong>Note:</strong> For best practices, call {@link #loadGlobalConfig()} before
     * this method to load global/common configuration that applies to all environments.
     * Environment-specific values will override global values due to the loading order.</p>
     *
     * @param environment the environment name (e.g., "env1", "env2", "env3")
     */
    public static void loadConfigForEnvironment(String environment) {
        if (environment == null || environment.isEmpty()) {
            throw new IllegalArgumentException("Environment cannot be null or empty");
        }

        // Load all environment-specific files (silently skip if they don't exist)
        loadProperties(environment + ".properties");
        loadJson(environment + ".json");
        loadYaml(environment + ".yaml");
        loadToml(environment + ".toml");
    }

    /**
     * Automatically detects the current environment and loads appropriate configuration files.
     *
     * <p>The environment is detected by checking the following configuration keys in order
     * (respecting ConfNG's source precedence):</p>
     * <ol>
     *   <li>APP_ENV (case-insensitive: app_env, App_Env, etc.)</li>
     *   <li>ENVIRONMENT (case-insensitive: environment, Environment, etc.)</li>
     *   <li>ENV (case-insensitive: env, Env, etc.)</li>
     * </ol>
     *
     * <p>If no value is found in any configuration source, defaults to "local".</p>
     *
     * <p>This method uses ConfNG's own configuration resolution system, which means the
     * environment can be set via any configuration source (environment variables, system
     * properties, TestNG parameters, configuration files, etc.) according to the standard
     * precedence order.</p>
     *
     * <p><strong>Case Insensitivity:</strong> The key lookup is case-insensitive. For example,
     * setting {@code export app_env=production} or {@code export APP_ENV=production} will both work.</p>
     *
     * <p>Once the environment is detected, this method calls {@link #loadConfigForEnvironment(String)}
     * to load all environment-specific configuration files.</p>
     *
     * @return the detected environment name
     */
    public static String autoLoadConfig() {
        String environment = getEnvironmentName();
        loadConfigForEnvironment(environment);
        return environment;
    }

    /**
     * Detects the current environment name by checking multiple configuration keys in order.
     *
     * <p>This method checks the following keys in order (case-insensitive), returning the first non-null, non-empty value:</p>
     * <ol>
     *   <li>APP_ENV (or app_env, App_Env, etc.)</li>
     *   <li>ENVIRONMENT (or environment, Environment, etc.)</li>
     *   <li>ENV (or env, Env, etc.)</li>
     * </ol>
     *
     * <p>If none of these keys have a value, returns "local" as the default.</p>
     *
     * <p>This method uses ConfNG's configuration resolution system, respecting source precedence.
     * For example, if APP_ENV is set as a TestNG parameter, it will take precedence over
     * APP_ENV set as an environment variable.</p>
     *
     * <p><strong>Case Insensitivity:</strong> The key lookup is case-insensitive, so APP_ENV, app_env,
     * App_Env, etc. are all treated as the same key.</p>
     *
     * @return the detected environment name, never null
     */
    public static String getEnvironmentName() {
        // Check environment keys in order (case-insensitive)
        String[] environmentKeys = {"APP_ENV", "ENVIRONMENT", "ENV"};

        for (String key : environmentKeys) {
            String environment = getConfigValueCaseInsensitive(key);
            if (environment != null && !environment.isEmpty()) {
                return environment;
            }
        }

        // Default to "local"
        return "local";
    }

    /**
     * Gets a configuration value for a given key string by checking all sources in precedence order.
     * This is a helper method for internal use that doesn't require a ConfNGKey.
     *
     * @param key the configuration key to look up
     * @return the configuration value, or null if not found
     */
    private static String getConfigValue(String key) {
        for (ConfigSource source : sources) {
            Optional<String> value = source.get(key);
            if (value.isPresent()) {
                return value.get();
            }
        }
        return null;
    }

    /**
     * Gets a configuration value for a given key string by checking all sources in precedence order,
     * using case-insensitive key matching.
     *
     * <p>This method tries multiple case variations of the key:</p>
     * <ul>
     *   <li>Original case (e.g., "APP_ENV")</li>
     *   <li>Lowercase (e.g., "app_env")</li>
     *   <li>Uppercase (e.g., "APP_ENV")</li>
     *   <li>Title case (e.g., "App_Env")</li>
     * </ul>
     *
     * <p>The first non-null, non-empty value found is returned.</p>
     *
     * @param key the configuration key to look up (will be tried in multiple case variations)
     * @return the configuration value, or null if not found in any case variation
     */
    private static String getConfigValueCaseInsensitive(String key) {
        if (key == null) {
            return null;
        }

        // Try original case first
        String value = getConfigValue(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // Try lowercase
        String lowerKey = key.toLowerCase();
        if (!lowerKey.equals(key)) {
            value = getConfigValue(lowerKey);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        // Try uppercase
        String upperKey = key.toUpperCase();
        if (!upperKey.equals(key)) {
            value = getConfigValue(upperKey);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        // Try title case (first letter uppercase, rest lowercase)
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
        String k = key.getKey();
        // Check if the highest-priority source is TestNGParameterSource
        if (!sources.isEmpty() && sources.get(0) instanceof org.confng.sources.TestNGParameterSource) {
            for (ConfigSource source : sources) {
                if (source instanceof org.confng.sources.TestNGParameterSource) {
                    Optional<String> value = source.get(k);
                    if (value.isPresent()) {
                        return value.get();
                    }
                }
            }
        }
        // Otherwise, use cache as before
        ensureResolved();
        String value = resolvedValues.get(k);
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
