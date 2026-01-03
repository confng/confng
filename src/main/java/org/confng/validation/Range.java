package org.confng.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a numeric configuration value is within a specified range.
 *
 * <p>When applied to a ConfNGKey enum constant, validation will fail if
 * the configuration value is not a valid number or is outside the specified range.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public enum MyConfig implements ConfNGKey {
 *     @Range(min = 1, max = 65535)
 *     PORT("server.port", "8080", false),
 *     
 *     @Range(min = 0, max = 100, message = "Percentage must be between 0 and 100")
 *     THRESHOLD("threshold.percent", "50", false),
 *     
 *     @Range(min = 1) // No upper limit
 *     RETRY_COUNT("retry.count", "3", false),
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
public @interface Range {
    
    /**
     * The minimum allowed value (inclusive).
     * Defaults to Long.MIN_VALUE (no minimum).
     *
     * @return the minimum value
     */
    long min() default Long.MIN_VALUE;
    
    /**
     * The maximum allowed value (inclusive).
     * Defaults to Long.MAX_VALUE (no maximum).
     *
     * @return the maximum value
     */
    long max() default Long.MAX_VALUE;
    
    /**
     * Custom error message to use when validation fails.
     * If empty, a default message will be generated.
     *
     * @return the custom error message
     */
    String message() default "";
}

