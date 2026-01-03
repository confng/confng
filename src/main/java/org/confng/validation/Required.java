package org.confng.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a configuration key as required.
 *
 * <p>When applied to a ConfNGKey enum constant, validation will fail if
 * the configuration value is not set and has no default value.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public enum MyConfig implements ConfNGKey {
 *     @Required
 *     DATABASE_URL("db.url", null, false),
 *     
 *     OPTIONAL_SETTING("optional", "default", false);
 *     // ...
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see ConfigValidator
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Required {
    
    /**
     * Custom error message to use when validation fails.
     * If empty, a default message will be generated.
     *
     * @return the custom error message
     */
    String message() default "";
}

