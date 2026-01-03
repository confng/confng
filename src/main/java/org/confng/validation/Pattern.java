package org.confng.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a configuration value matches a regular expression pattern.
 *
 * <p>When applied to a ConfNGKey enum constant, validation will fail if
 * the configuration value does not match the specified regex pattern.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public enum MyConfig implements ConfNGKey {
 *     @Pattern(regex = "^[a-zA-Z0-9_]+$", message = "Username must be alphanumeric")
 *     USERNAME("username", null, false),
 *     
 *     @Pattern(regex = "^https?://.*")
 *     API_URL("api.url", null, false),
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
public @interface Pattern {
    
    /**
     * The regular expression pattern to match against.
     *
     * @return the regex pattern
     */
    String regex();
    
    /**
     * Custom error message to use when validation fails.
     * If empty, a default message will be generated.
     *
     * @return the custom error message
     */
    String message() default "";
}

