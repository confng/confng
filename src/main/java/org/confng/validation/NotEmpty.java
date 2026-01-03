package org.confng.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a configuration key as requiring a non-empty value.
 *
 * <p>When applied to a ConfNGKey enum constant, validation will fail if
 * the configuration value is empty (blank string) or contains only whitespace.</p>
 *
 * <p>Note: This is different from {@link Required} - a value can be present
 * but empty. Use both annotations together if you want to require a non-empty value.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public enum MyConfig implements ConfNGKey {
 *     @Required
 *     @NotEmpty
 *     API_KEY("api.key", null, true),
 *     // ...
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see Required
 * @see ConfigValidator
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotEmpty {
    
    /**
     * Custom error message to use when validation fails.
     * If empty, a default message will be generated.
     *
     * @return the custom error message
     */
    String message() default "";
}

