package org.confng.metrics;

import lombok.extern.slf4j.Slf4j;
import org.confng.api.ConfNGKey;
import org.confng.ConfNG;

import java.util.*;

/**
 * Utility for logging loaded configuration at startup with masked sensitive values.
 *
 * <p>This class provides methods to log all loaded configuration values in a
 * structured format, automatically masking sensitive values like passwords,
 * tokens, and secrets.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Log all configuration at startup
 * ConfigLogger.logConfiguration(MyConfig.class);
 * 
 * // Log with custom masking patterns
 * ConfigLogger.builder()
 *     .addSensitivePattern("api.key")
 *     .addSensitivePattern(".*secret.*")
 *     .logConfiguration(MyConfig.class);
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Slf4j
public class ConfigLogger {
    
    private static final String MASK = "****";
    private static final Set<String> DEFAULT_SENSITIVE_PATTERNS = new HashSet<>(Arrays.asList(
            "password", "secret", "token", "key", "credential", "auth", "private"
    ));
    
    private final Set<String> sensitivePatterns;
    private final boolean showSources;
    private final boolean showDefaults;
    private final String logLevel;
    
    private ConfigLogger(Builder builder) {
        this.sensitivePatterns = new HashSet<>(builder.sensitivePatterns);
        this.showSources = builder.showSources;
        this.showDefaults = builder.showDefaults;
        this.logLevel = builder.logLevel;
    }
    
    /**
     * Logs configuration for the given ConfNGKey enum class.
     */
    public static <E extends Enum<E> & ConfNGKey> void logConfiguration(Class<E> keyClass) {
        builder().logConfiguration(keyClass);
    }
    
    /**
     * Creates a new builder for customized logging.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Logs configuration with the current settings.
     */
    public <E extends Enum<E> & ConfNGKey> void doLogConfiguration(Class<E> keyClass) {
        E[] keys = keyClass.getEnumConstants();

        logMessage("=".repeat(60));
        logMessage("Configuration: " + keyClass.getSimpleName());
        logMessage("=".repeat(60));

        String currentSection = "";
        int missingCount = 0;
        int sensitiveCount = 0;

        for (E key : keys) {
            String keyName = key.getKey();
            String section = keyName.contains(".") ? keyName.substring(0, keyName.indexOf('.')) : "";

            if (!section.equals(currentSection)) {
                if (!currentSection.isEmpty()) {
                    logMessage("");
                }
                logMessage("[" + section.toUpperCase() + "]");
                currentSection = section;
            }

            String value = ConfNG.get(key);
            // Use ConfNGKey.isSensitive() first, then fall back to pattern matching
            boolean isSensitiveValue = key.isSensitive() || isSensitive(keyName);
            String displayValue = isSensitiveValue ? MASK : value;

            if (isSensitiveValue) {
                sensitiveCount++;
            }

            StringBuilder line = new StringBuilder();
            line.append("  ").append(keyName).append(" = ").append(displayValue != null ? displayValue : "<null>");

            if (showDefaults && value != null && value.equals(key.getDefaultValue())) {
                line.append(" (default)");
            }

            // Check if value is missing and has no default
            if ((value == null || value.isEmpty()) && key.getDefaultValue() == null) {
                line.append(" [MISSING]");
                missingCount++;
            }

            logMessage(line.toString());
        }

        logMessage("=".repeat(60));
        logMessage("Total keys: " + keys.length + " | Sensitive: " + sensitiveCount + " | Missing: " + missingCount);
        logMessage("=".repeat(60));
    }
    
    private boolean isSensitive(String key) {
        String lowerKey = key.toLowerCase();
        for (String pattern : sensitivePatterns) {
            if (lowerKey.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private void logMessage(String message) {
        switch (logLevel.toLowerCase()) {
            case "debug":
                log.debug(message);
                break;
            case "trace":
                log.trace(message);
                break;
            case "warn":
                log.warn(message);
                break;
            default:
                log.info(message);
        }
    }
    
    public static class Builder {
        private Set<String> sensitivePatterns = new HashSet<>(DEFAULT_SENSITIVE_PATTERNS);
        private boolean showSources = false;
        private boolean showDefaults = true;
        private String logLevel = "info";
        
        public Builder addSensitivePattern(String pattern) {
            this.sensitivePatterns.add(pattern);
            return this;
        }
        
        public Builder clearSensitivePatterns() {
            this.sensitivePatterns.clear();
            return this;
        }
        
        public Builder showSources(boolean show) {
            this.showSources = show;
            return this;
        }
        
        public Builder showDefaults(boolean show) {
            this.showDefaults = show;
            return this;
        }
        
        public Builder logLevel(String level) {
            this.logLevel = level;
            return this;
        }
        
        public ConfigLogger build() {
            return new ConfigLogger(this);
        }
        
        public <E extends Enum<E> & ConfNGKey> void logConfiguration(Class<E> keyClass) {
            build().doLogConfiguration(keyClass);
        }
    }
}

