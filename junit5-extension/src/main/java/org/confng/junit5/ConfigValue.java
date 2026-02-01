package org.confng.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for injecting configuration values into JUnit 5 test method parameters.
 *
 * <p>Use this annotation on test method parameters to have ConfNG automatically
 * inject the configuration value at test execution time.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * @ExtendWith(ConfNGExtension.class)
 * class MyTest {
 *     
 *     @Test
 *     void testWithConfig(@ConfigValue(key = "database.url") String dbUrl) {
 *         assertNotNull(dbUrl);
 *     }
 *     
 *     @Test
 *     void testWithDefault(@ConfigValue(key = "timeout", defaultValue = "30") int timeout) {
 *         assertTrue(timeout > 0);
 *     }
 *     
 *     @Test
 *     void testWithEnumKey(@ConfigValue(keyEnum = MyConfig.class, keyName = "API_KEY") String apiKey) {
 *         assertNotNull(apiKey);
 *     }
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see ConfNGExtension
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ConfigValue {
    
    /**
     * The configuration key to look up.
     * Either this or {@link #keyEnum()} with {@link #keyName()} must be specified.
     *
     * @return the configuration key
     */
    String key() default "";
    
    /**
     * The ConfNGKey enum class containing the configuration key.
     * Use with {@link #keyName()} to specify which enum constant to use.
     *
     * @return the enum class implementing ConfNGKey
     */
    Class<?> keyEnum() default Void.class;
    
    /**
     * The name of the enum constant in {@link #keyEnum()}.
     *
     * @return the enum constant name
     */
    String keyName() default "";
    
    /**
     * Default value to use if the configuration key is not found.
     *
     * @return the default value
     */
    String defaultValue() default "";
    
    /**
     * Whether the configuration value is required.
     * If true and the value is not found, the test will fail.
     *
     * @return true if required
     */
    boolean required() default false;
}

