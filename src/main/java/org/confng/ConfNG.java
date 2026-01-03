package org.confng;

import org.confng.api.ConfNGKey;
import org.confng.api.ConfigurationException;
import org.confng.internal.ConfigDiscovery;
import org.confng.internal.ConfigLoader;
import org.confng.internal.ConfigResolver;
import org.confng.sources.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private static final List<ConfigSource> sources = new CopyOnWriteArrayList<>();
    private static final ConfigResolver resolver = new ConfigResolver(
        () -> sources,
        () -> ConfigDiscovery.discoverAllConfigKeys()
    );

    static {
        // Default precedence: Env, System properties, Properties files, JSON files, YAML files, TOML files
        sources.add(new EnvSource());
        sources.add(new SystemPropertySource());
    }

    public static void registerSource(ConfigSource source) {
        sources.add(source);
        resolver.invalidate();
    }

    public static void registerSourceAt(int precedenceIndex, ConfigSource source) {
        if (precedenceIndex < 0 || precedenceIndex > sources.size()) {
            throw new IllegalArgumentException("Invalid precedence index: " + precedenceIndex);
        }
        sources.add(precedenceIndex, source);
        resolver.invalidate();
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
        resolver.invalidate();
    }

    public static void clearSourcesAndUseDefaults() {
        sources.clear();
        sources.add(new EnvSource());
        sources.add(new SystemPropertySource());
        resolver.invalidate();
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
        ConfigLoader.load(filePath, ConfNG::registerSource);
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
        ConfigLoader.load(filePath, environment, ConfNG::registerSource);
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
        ConfigLoader.loadProperties(filePath, ConfNG::registerSource);
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
        ConfigLoader.loadJson(filePath, null, ConfNG::registerSource);
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
        ConfigLoader.loadYaml(filePath, null, ConfNG::registerSource);
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
        ConfigLoader.loadToml(filePath, null, ConfNG::registerSource);
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
        ConfigLoader.loadToml(filePath, environment, ConfNG::registerSource);
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
        ConfigLoader.loadJson(filePath, environment, ConfNG::registerSource);
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
        ConfigLoader.loadYaml(filePath, environment, ConfNG::registerSource);
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
        return resolver.getConfigValue(key);
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
        return resolver.getConfigValueCaseInsensitive(key);
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
        resolver.resolveAll();
    }

    /**
     * Resolves configuration values for the specified keys from all sources.
     * If keys is null, attempts to discover all keys from known ConfNGKey implementations.
     *
     * @param keys specific keys to resolve, or null to discover all keys
     */
    public static void resolveAllValues(Set<String> keys) {
        resolver.resolveAll(keys);
    }

    /**
     * Gets a configuration value for the given key.
     *
     * @param key the configuration key
     * @return the configuration value, or the default value if not found
     */
    public static String get(ConfNGKey key) {
        return resolver.get(key);
    }

    /**
     * Gets a configuration value as an Optional.
     * Returns Optional.empty() if the value is not found and has no default.
     *
     * <p>This method is useful when you want to handle missing configuration
     * gracefully without null checks:</p>
     * <pre>{@code
     * Optional<String> dbUrl = ConfNG.getOptional(MyConfig.DATABASE_URL);
     * dbUrl.ifPresent(url -> connectToDatabase(url));
     * }</pre>
     *
     * @param key the configuration key
     * @return an Optional containing the value if present, empty otherwise
     * @since 1.1.0
     */
    public static Optional<String> getOptional(ConfNGKey key) {
        return resolver.getOptional(key);
    }

    /**
     * Gets a required configuration value.
     * Throws ConfigurationException if the value is not found and has no default.
     *
     * <p>Use this method when a configuration value is mandatory and the application
     * cannot proceed without it:</p>
     * <pre>{@code
     * try {
     *     String dbUrl = ConfNG.getRequired(MyConfig.DATABASE_URL);
     *     connectToDatabase(dbUrl);
     * } catch (ConfigurationException e) {
     *     System.err.println("Missing required config: " + e.getKey());
     *     System.exit(1);
     * }
     * }</pre>
     *
     * @param key the configuration key
     * @return the configuration value (never null)
     * @throws ConfigurationException if the value is not found and has no default
     * @since 1.1.0
     */
    public static String getRequired(ConfNGKey key) {
        return resolver.getRequired(key);
    }

    /**
     * Gets a configuration value with a fallback default.
     * Returns the provided default if the value is not found and the key has no default.
     *
     * <p>This method is useful when you want to provide a runtime default that
     * differs from the key's compile-time default:</p>
     * <pre>{@code
     * String timeout = ConfNG.getOrDefault(MyConfig.TIMEOUT, "60");
     * }</pre>
     *
     * @param key the configuration key
     * @param defaultValue the fallback default value
     * @return the configuration value, or the provided default if not found
     * @since 1.1.0
     */
    public static String getOrDefault(ConfNGKey key, String defaultValue) {
        return resolver.getOrDefault(key, defaultValue);
    }

    /**
     * Gets a configuration value as an integer.
     *
     * @param key the configuration key
     * @return the configuration value as integer, or null if not found and no default
     * @throws IllegalArgumentException if the value cannot be converted to integer
     */
    public static Integer getInt(ConfNGKey key) {
        return resolver.getInt(key);
    }

    /**
     * Gets a configuration value as a boolean.
     *
     * @param key the configuration key
     * @return the configuration value as boolean, or null if not found and no default
     * @throws IllegalArgumentException if the value cannot be converted to boolean
     */
    public static Boolean getBoolean(ConfNGKey key) {
        return resolver.getBoolean(key);
    }

    /**
     * Gets a configuration value as a long.
     *
     * @param key the configuration key
     * @return the configuration value as long, or null if not found and no default
     * @throws IllegalArgumentException if the value cannot be converted to long
     * @since 1.1.0
     */
    public static Long getLong(ConfNGKey key) {
        return resolver.getLong(key);
    }

    /**
     * Gets a configuration value as a double.
     *
     * @param key the configuration key
     * @return the configuration value as double, or null if not found and no default
     * @throws IllegalArgumentException if the value cannot be converted to double
     * @since 1.1.0
     */
    public static Double getDouble(ConfNGKey key) {
        return resolver.getDouble(key);
    }

    /**
     * Gets a configuration value as a list of strings.
     * Values are split by comma and trimmed.
     *
     * <p>Example: "a, b, c" becomes ["a", "b", "c"]</p>
     *
     * @param key the configuration key
     * @return the configuration value as a list, or null if not found and no default
     * @since 1.1.0
     */
    public static java.util.List<String> getList(ConfNGKey key) {
        return resolver.getList(key);
    }

    /**
     * Gets a configuration value as a list of strings with a custom delimiter.
     *
     * @param key the configuration key
     * @param delimiter the delimiter to split values
     * @return the configuration value as a list, or null if not found and no default
     * @since 1.1.0
     */
    public static java.util.List<String> getList(ConfNGKey key, String delimiter) {
        return resolver.getList(key, delimiter);
    }

    /**
     * Gets a configuration value as a Duration.
     * Supports formats: "30s" (seconds), "5m" (minutes), "2h" (hours), "1d" (days),
     * "500ms" (milliseconds), or ISO-8601 duration format (e.g., "PT30S").
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>"30s" - 30 seconds</li>
     *   <li>"5m" - 5 minutes</li>
     *   <li>"2h" - 2 hours</li>
     *   <li>"1d" - 1 day</li>
     *   <li>"500ms" - 500 milliseconds</li>
     *   <li>"PT30S" - ISO-8601 format for 30 seconds</li>
     * </ul>
     *
     * @param key the configuration key
     * @return the configuration value as Duration, or null if not found and no default
     * @throws IllegalArgumentException if the value cannot be parsed as a duration
     * @since 1.1.0
     */
    public static java.time.Duration getDuration(ConfNGKey key) {
        return resolver.getDuration(key);
    }

    /**
     * Gets a configuration value with masking for sensitive data.
     *
     * @param key the configuration key
     * @return the configuration value, masked if sensitive
     */
    public static String getForDisplay(ConfNGKey key) {
        return resolver.getForDisplay(key);
    }

    /**
     * Gets all configuration values for display purposes.
     * Sensitive values are masked.
     *
     * @param keys the configuration keys to display
     * @return a formatted string showing all configuration values
     */
    public static String getAllForDisplay(ConfNGKey... keys) {
        return resolver.getAllForDisplay(keys);
    }

    /**
     * Discovers all ConfNGKey implementations in the classpath.
     *
     * @param basePackages packages to scan, if empty scans entire classpath
     * @return list of discovered configuration keys
     */
    public static List<ConfNGKey> discoverAllConfigKeys(String... basePackages) {
        return ConfigDiscovery.discoverAllConfigKeys(basePackages);
    }

    /**
     * Forces re-resolution of all configuration values.
     * Useful when configuration sources might have changed.
     */
    public static void refresh() {
        resolver.refresh();
    }

    /**
     * Gets the number of resolved configuration values.
     *
     * @return number of resolved values
     */
    public static int getResolvedValueCount() {
        return resolver.getResolvedValueCount();
    }

    /**
     * Checks if configuration values have been resolved.
     *
     * @return true if values are resolved, false otherwise
     */
    public static boolean isResolved() {
        return resolver.isResolved();
    }

    /**
     * Gets all resolved configuration keys.
     *
     * @return set of all resolved keys
     */
    public static Set<String> getResolvedKeys() {
        return resolver.getResolvedKeys();
    }

    /**
     * Validates all discovered configuration keys against their defined constraints.
     *
     * <p>This method scans all ConfNGKey implementations for validation annotations
     * ({@link org.confng.validation.Required}, {@link org.confng.validation.NotEmpty},
     * {@link org.confng.validation.Pattern}, {@link org.confng.validation.Range})
     * and checks that the resolved values satisfy all constraints.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ValidationResult result = ConfNG.validate();
     * if (!result.isValid()) {
     *     System.err.println(result.getSummary());
     *     throw new RuntimeException("Configuration validation failed");
     * }
     * }</pre>
     *
     * @return the validation result containing any errors found
     * @since 1.1.0
     * @see org.confng.validation.ValidationResult
     * @see org.confng.validation.Required
     * @see org.confng.validation.NotEmpty
     * @see org.confng.validation.Pattern
     * @see org.confng.validation.Range
     */
    public static org.confng.validation.ValidationResult validate() {
        return validate(discoverAllConfigKeys());
    }

    /**
     * Validates the specified configuration keys against their defined constraints.
     *
     * @param keys the configuration keys to validate
     * @return the validation result containing any errors found
     * @since 1.1.0
     * @see #validate()
     */
    public static org.confng.validation.ValidationResult validate(List<ConfNGKey> keys) {
        org.confng.validation.ConfigValidator validator =
            new org.confng.validation.ConfigValidator(ConfNG::get);
        return validator.validate(keys);
    }

    /**
     * Validates the specified configuration keys against their defined constraints.
     *
     * @param keys the configuration keys to validate
     * @return the validation result containing any errors found
     * @since 1.1.0
     * @see #validate()
     */
    public static org.confng.validation.ValidationResult validate(ConfNGKey... keys) {
        return validate(java.util.Arrays.asList(keys));
    }

    /**
     * Gets information about which source provided the value for a configuration key.
     *
     * <p>This method is useful for debugging configuration issues and understanding
     * the precedence of configuration sources:</p>
     * <pre>{@code
     * ConfigSourceInfo info = ConfNG.getSourceInfo(MyConfig.DATABASE_URL);
     * System.out.println("Value: " + info.getValue());
     * System.out.println("Source: " + info.getSourceName());
     * System.out.println("Priority: " + info.getPriority());
     * System.out.println("From default: " + info.isFromDefault());
     * }</pre>
     *
     * @param key the configuration key
     * @return source information including source name, priority, and value
     * @since 1.1.0
     * @see org.confng.api.ConfigSourceInfo
     */
    public static org.confng.api.ConfigSourceInfo getSourceInfo(ConfNGKey key) {
        return resolver.getSourceInfo(key);
    }

    /**
     * Gets source information for multiple configuration keys.
     *
     * @param keys the configuration keys to get info for
     * @return map of key name to source information
     * @since 1.1.0
     * @see #getSourceInfo(ConfNGKey)
     */
    public static java.util.Map<String, org.confng.api.ConfigSourceInfo> getAllSourceInfo(ConfNGKey... keys) {
        return resolver.getAllSourceInfo(keys);
    }

    /**
     * Gets source information for a list of configuration keys.
     *
     * @param keys the configuration keys to get info for
     * @return map of key name to source information
     * @since 1.1.0
     * @see #getSourceInfo(ConfNGKey)
     */
    public static java.util.Map<String, org.confng.api.ConfigSourceInfo> getAllSourceInfo(List<ConfNGKey> keys) {
        return resolver.getAllSourceInfo(keys);
    }

    /**
     * Gets all configuration values that match a given prefix.
     *
     * <p>This method is useful for retrieving groups of related configuration values:</p>
     * <pre>{@code
     * // Get all database-related configuration
     * Map<String, String> dbConfig = ConfNG.getByPrefix("database.");
     * // Returns: {"database.host": "localhost", "database.port": "5432", ...}
     *
     * // Get all API configuration
     * Map<String, String> apiConfig = ConfNG.getByPrefix("api.");
     * }</pre>
     *
     * @param prefix the prefix to match (e.g., "database." or "app.")
     * @return map of matching keys to their values
     * @since 1.1.0
     */
    public static java.util.Map<String, String> getByPrefix(String prefix) {
        return resolver.getByPrefix(prefix);
    }

    /**
     * Gets all configuration keys that match a given prefix.
     *
     * <p>This method returns only the key names, not the values:</p>
     * <pre>{@code
     * Set<String> dbKeys = ConfNG.getKeysWithPrefix("database.");
     * // Returns: {"database.host", "database.port", "database.user", ...}
     * }</pre>
     *
     * @param prefix the prefix to match (e.g., "database." or "app.")
     * @return set of matching keys
     * @since 1.1.0
     */
    public static Set<String> getKeysWithPrefix(String prefix) {
        return resolver.getKeysWithPrefix(prefix);
    }

    /**
     * Gets all configuration values that match a given prefix, with source information.
     *
     * <p>This method combines prefix matching with source diagnostics:</p>
     * <pre>{@code
     * List<ConfNGKey> allKeys = ConfNG.discoverAllConfigKeys();
     * Map<String, ConfigSourceInfo> dbInfo = ConfNG.getByPrefixWithInfo("database.", allKeys);
     * for (ConfigSourceInfo info : dbInfo.values()) {
     *     System.out.println(info.getKey() + " from " + info.getSourceName());
     * }
     * }</pre>
     *
     * @param prefix the prefix to match
     * @param keys the ConfNGKey implementations to check for prefix matching
     * @return map of matching keys to their source information
     * @since 1.1.0
     * @see #getByPrefix(String)
     * @see #getSourceInfo(ConfNGKey)
     */
    public static java.util.Map<String, org.confng.api.ConfigSourceInfo> getByPrefixWithInfo(
            String prefix, List<ConfNGKey> keys) {
        return resolver.getByPrefixWithInfo(prefix, keys);
    }
}
