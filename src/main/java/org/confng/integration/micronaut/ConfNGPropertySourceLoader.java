package org.confng.integration.micronaut;

import org.confng.ConfNG;
import org.confng.api.ConfNGKey;

import java.util.*;

/**
 * Micronaut PropertySourceLoader adapter for ConfNG.
 *
 * <p>This class allows ConfNG to be used as a PropertySourceLoader in Micronaut applications,
 * enabling seamless integration with Micronaut's configuration system.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Register in META-INF/services/io.micronaut.context.env.PropertySourceLoader
 * // Add: org.confng.integration.micronaut.ConfNGPropertySourceLoader
 * 
 * // Or programmatically:
 * @Factory
 * public class ConfNGFactory {
 *     
 *     @Singleton
 *     public PropertySourceLoader confngLoader() {
 *         return new ConfNGPropertySourceLoader(MyConfig.class);
 *     }
 * }
 * 
 * // Then use @Value or @Property annotations
 * @Singleton
 * public class MyService {
 *     @Value("${app.name}")
 *     private String appName;
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
public class ConfNGPropertySourceLoader {
    
    private static final String SOURCE_NAME = "confng";
    private static final int ORDER = -100; // High priority
    
    private final Set<String> propertyNames;
    private final Map<String, Object> properties;
    
    /**
     * Creates a ConfNG property source loader with the given key class.
     */
    public <E extends Enum<E> & ConfNGKey> ConfNGPropertySourceLoader(Class<E> keyClass) {
        this.propertyNames = new LinkedHashSet<>();
        this.properties = new LinkedHashMap<>();
        
        for (E key : keyClass.getEnumConstants()) {
            propertyNames.add(key.getKey());
            String value = ConfNG.get(key);
            if (value != null) {
                properties.put(key.getKey(), value);
            }
        }
    }
    
    /**
     * Creates a ConfNG property source loader that loads all available properties.
     */
    public ConfNGPropertySourceLoader() {
        this.propertyNames = new LinkedHashSet<>();
        this.properties = new LinkedHashMap<>();
    }
    
    /**
     * Gets the extensions this loader supports.
     */
    public Set<String> getExtensions() {
        return Collections.singleton("confng");
    }
    
    /**
     * Checks if this loader is enabled.
     */
    public boolean isEnabled() {
        return true;
    }
    
    /**
     * Gets the order of this loader (lower = higher priority).
     */
    public int getOrder() {
        return ORDER;
    }
    
    /**
     * Loads properties from ConfNG.
     */
    public Map<String, Object> load(String resourceName) {
        // Refresh properties from ConfNG
        Map<String, Object> result = new LinkedHashMap<>();

        for (String key : propertyNames) {
            String value = ConfNG.getFromSources(key);
            if (value != null) {
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * Gets the source name.
     */
    public String getName() {
        return SOURCE_NAME;
    }

    /**
     * Gets all property names.
     */
    public Set<String> getPropertyNames() {
        return Collections.unmodifiableSet(propertyNames);
    }

    /**
     * Gets a property value.
     */
    public Object get(String key) {
        return ConfNG.getFromSources(key);
    }

    /**
     * Creates a Micronaut-compatible PropertySource map.
     */
    public Map<String, Object> asMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : propertyNames) {
            String value = ConfNG.getFromSources(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }
}

