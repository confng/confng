package org.confng.integration.spring;

import org.confng.ConfNG;
import org.confng.api.ConfNGKey;
import org.confng.sources.ConfigSource;

import java.util.*;

/**
 * Spring PropertySource adapter for ConfNG.
 *
 * <p>This class allows ConfNG to be used as a PropertySource in Spring applications,
 * enabling seamless integration with Spring's Environment abstraction.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * @Configuration
 * public class ConfNGConfig {
 *     
 *     @Bean
 *     public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
 *         PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
 *         
 *         // Add ConfNG as a property source
 *         MutablePropertySources sources = new MutablePropertySources();
 *         sources.addFirst(new ConfNGPropertySource("confng", MyConfig.class));
 *         configurer.setPropertySources(sources);
 *         
 *         return configurer;
 *     }
 * }
 * 
 * // Then use @Value annotations as normal
 * @Component
 * public class MyService {
 *     @Value("${app.name}")
 *     private String appName;
 * }
 * }</pre>
 *
 * <h3>Spring Boot Auto-Configuration:</h3>
 * <pre>{@code
 * @Configuration
 * @AutoConfigureBefore(PropertyPlaceholderAutoConfiguration.class)
 * public class ConfNGAutoConfiguration {
 *     
 *     @Bean
 *     public ConfNGPropertySource confngPropertySource() {
 *         return new ConfNGPropertySource("confng", MyConfig.class);
 *     }
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
public class ConfNGPropertySource {
    
    private final String name;
    private final Set<String> propertyNames;
    
    /**
     * Creates a ConfNG property source with the given name and key class.
     */
    public <E extends Enum<E> & ConfNGKey> ConfNGPropertySource(String name, Class<E> keyClass) {
        this.name = name;
        this.propertyNames = new LinkedHashSet<>();
        
        for (E key : keyClass.getEnumConstants()) {
            propertyNames.add(key.getKey());
        }
    }
    
    /**
     * Creates a ConfNG property source that exposes all registered sources.
     */
    public ConfNGPropertySource(String name) {
        this.name = name;
        this.propertyNames = new LinkedHashSet<>();
    }
    
    /**
     * Gets the name of this property source.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets a property value by name.
     */
    public Object getProperty(String name) {
        return ConfNG.getFromSources(name);
    }

    /**
     * Checks if this property source contains the given property.
     */
    public boolean containsProperty(String name) {
        if (!propertyNames.isEmpty()) {
            return propertyNames.contains(name);
        }
        // If no specific keys, check if any source has the value
        return ConfNG.getFromSources(name) != null;
    }
    
    /**
     * Gets all property names.
     */
    public String[] getPropertyNames() {
        return propertyNames.toArray(new String[0]);
    }
    
    /**
     * Creates a Spring Environment-compatible PropertySource.
     * 
     * <p>Note: This method returns an Object to avoid compile-time dependency on Spring.
     * Cast to org.springframework.core.env.PropertySource in Spring applications.</p>
     */
    public Object toSpringPropertySource() {
        return new SpringPropertySourceAdapter(this);
    }
    
    /**
     * Internal adapter class that can be cast to Spring's PropertySource.
     * This avoids a hard dependency on Spring at compile time.
     */
    private static class SpringPropertySourceAdapter {
        private final ConfNGPropertySource source;
        
        SpringPropertySourceAdapter(ConfNGPropertySource source) {
            this.source = source;
        }
        
        public String getName() {
            return source.getName();
        }
        
        public Object getProperty(String name) {
            return source.getProperty(name);
        }
        
        public boolean containsProperty(String name) {
            return source.containsProperty(name);
        }
        
        public String[] getPropertyNames() {
            return source.getPropertyNames();
        }
    }
}

