package org.confng.junit5;

import lombok.extern.slf4j.Slf4j;
import org.confng.ConfNG;
import org.confng.api.ConfNGKey;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Parameter;

/**
 * JUnit 5 extension that provides ConfNG configuration integration for tests.
 *
 * <p>This extension automatically loads configuration files before tests run and
 * supports injection of configuration values into test method parameters.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Auto-loads config.* files (properties, json, yaml, toml) before all tests</li>
 *   <li>Supports environment-specific configuration via system property or env variable</li>
 *   <li>Injects configuration values into test parameters via {@link ConfigValue}</li>
 *   <li>Supports type conversion for common types (String, int, boolean, long, double)</li>
 * </ul>
 *
 * <h3>Basic Usage:</h3>
 * <pre>{@code
 * @ExtendWith(ConfNGExtension.class)
 * class MyTest {
 *     @Test
 *     void testSomething() {
 *         String value = ConfNG.get(MyConfig.SOME_KEY);
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * <h3>Parameter Injection:</h3>
 * <pre>{@code
 * @ExtendWith(ConfNGExtension.class)
 * class MyTest {
 *     @Test
 *     void testWithInjection(@ConfigValue(key = "db.url") String dbUrl,
 *                            @ConfigValue(key = "timeout", defaultValue = "30") int timeout) {
 *         // dbUrl and timeout are automatically injected
 *     }
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see ConfigValue
 * @see ConfNG
 */
@Slf4j
public class ConfNGExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    private static volatile boolean initialized = false;
    private static final Object INIT_LOCK = new Object();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        initializeConfiguration();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Ensure configuration is loaded even if beforeAll wasn't called
        initializeConfiguration();
    }

    private void initializeConfiguration() {
        if (!initialized) {
            synchronized (INIT_LOCK) {
                if (!initialized) {
                    log.info("[ConfNG] Initializing configuration for JUnit 5 tests...");
                    
                    // Load config.* files
                    ConfNG.load("config.properties");
                    ConfNG.load("config.json");
                    ConfNG.load("config.yaml");
                    ConfNG.load("config.toml");
                    
                    // Auto-detect and load environment-specific config
                    String environment = ConfNG.getEnvironmentName();
                    if (environment != null && !environment.isEmpty() && !environment.equals("local")) {
                        log.info("[ConfNG] Loading configuration for environment: {}", environment);
                        ConfNG.loadConfigForEnvironment(environment);
                    }
                    
                    initialized = true;
                    log.info("[ConfNG] Configuration initialized successfully");
                }
            }
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) 
            throws ParameterResolutionException {
        return parameterContext.isAnnotated(ConfigValue.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) 
            throws ParameterResolutionException {
        ConfigValue annotation = parameterContext.findAnnotation(ConfigValue.class)
                .orElseThrow(() -> new ParameterResolutionException("@ConfigValue annotation not found"));
        
        Parameter parameter = parameterContext.getParameter();
        String value = resolveConfigValue(annotation);
        
        if (value == null && annotation.required()) {
            throw new ParameterResolutionException(
                    "Required configuration value not found for key: " + getKeyDescription(annotation));
        }
        
        return convertValue(value, parameter.getType(), annotation);
    }

    private String resolveConfigValue(ConfigValue annotation) {
        String key = annotation.key();
        
        // If keyEnum is specified, use it to get the ConfNGKey
        if (annotation.keyEnum() != Void.class && !annotation.keyName().isEmpty()) {
            try {
                Class<?> enumClass = annotation.keyEnum();
                if (ConfNGKey.class.isAssignableFrom(enumClass) && enumClass.isEnum()) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) enumClass, annotation.keyName());
                    if (enumValue instanceof ConfNGKey) {
                        return ConfNG.get((ConfNGKey) enumValue);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to resolve enum key: {}.{}", annotation.keyEnum().getName(), annotation.keyName(), e);
            }
        }
        
        // Use string key lookup
        if (!key.isEmpty()) {
            String value = getConfigValueByKey(key);
            if (value == null && !annotation.defaultValue().isEmpty()) {
                return annotation.defaultValue();
            }
            return value;
        }
        
        return annotation.defaultValue().isEmpty() ? null : annotation.defaultValue();
    }

    private String getConfigValueByKey(String key) {
        // Create an ad-hoc ConfNGKey for lookup
        ConfNGKey adHocKey = new ConfNGKey() {
            @Override public String getKey() { return key; }
            @Override public String getDefaultValue() { return null; }
            @Override public boolean isSensitive() { return false; }
        };
        return ConfNG.get(adHocKey);
    }

    private Object convertValue(String value, Class<?> targetType, ConfigValue annotation) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new ParameterResolutionException(
                        "Cannot inject null into primitive type for key: " + getKeyDescription(annotation));
            }
            return null;
        }
        return doConvertValue(value, targetType, annotation);
    }

    private Object doConvertValue(String value, Class<?> targetType, ConfigValue annotation) {
        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(value);
            } else if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(value);
            } else if (targetType == char.class || targetType == Character.class) {
                return value.isEmpty() ? '\0' : value.charAt(0);
            } else {
                throw new ParameterResolutionException(
                        "Unsupported parameter type: " + targetType.getName() + " for key: " + getKeyDescription(annotation));
            }
        } catch (NumberFormatException e) {
            throw new ParameterResolutionException(
                    "Cannot convert value '" + value + "' to " + targetType.getName() + " for key: " + getKeyDescription(annotation), e);
        }
    }

    private String getKeyDescription(ConfigValue annotation) {
        if (!annotation.key().isEmpty()) {
            return annotation.key();
        }
        if (annotation.keyEnum() != Void.class && !annotation.keyName().isEmpty()) {
            return annotation.keyEnum().getSimpleName() + "." + annotation.keyName();
        }
        return "(unknown)";
    }

    /**
     * Resets the initialization state. Useful for testing.
     */
    public static void reset() {
        synchronized (INIT_LOCK) {
            initialized = false;
            ConfNG.clearSourcesAndUseDefaults();
        }
    }
}
