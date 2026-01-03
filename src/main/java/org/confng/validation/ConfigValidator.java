package org.confng.validation;

import org.confng.api.ConfNGKey;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;

/**
 * Validates configuration values against defined constraints.
 *
 * <p>This class scans ConfNGKey enum constants for validation annotations
 * and checks that the resolved configuration values satisfy all constraints.</p>
 *
 * <p>Supported annotations:</p>
 * <ul>
 *   <li>{@link Required} - Value must be present</li>
 *   <li>{@link NotEmpty} - Value must not be empty or blank</li>
 *   <li>{@link Pattern} - Value must match a regex pattern</li>
 *   <li>{@link Range} - Numeric value must be within a range</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ValidationResult result = ConfNG.validate();
 * if (!result.isValid()) {
 *     throw new RuntimeException(result.getSummary());
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see ValidationResult
 * @see ValidationError
 */
public class ConfigValidator {

    private final Function<ConfNGKey, String> valueResolver;

    /**
     * Creates a new ConfigValidator with the given value resolver.
     *
     * @param valueResolver function to resolve configuration values
     */
    public ConfigValidator(Function<ConfNGKey, String> valueResolver) {
        this.valueResolver = valueResolver;
    }

    /**
     * Validates all discovered ConfNGKey implementations.
     *
     * @param keys the configuration keys to validate
     * @return the validation result
     */
    public ValidationResult validate(List<ConfNGKey> keys) {
        List<ValidationError> errors = new ArrayList<>();

        for (ConfNGKey key : keys) {
            validateKey(key, errors);
        }

        return new ValidationResult(errors);
    }

    /**
     * Validates a single configuration key.
     */
    private void validateKey(ConfNGKey key, List<ValidationError> errors) {
        String value = valueResolver.apply(key);
        String keyName = key.getKey();
        boolean sensitive = key.isSensitive();

        // Get the enum field to check for annotations
        Field field = getEnumField(key);
        if (field == null) {
            return; // Not an enum, skip annotation-based validation
        }

        // Check @Required
        if (field.isAnnotationPresent(Required.class)) {
            Required required = field.getAnnotation(Required.class);
            if (value == null) {
                String message = required.message().isEmpty() 
                    ? "Required configuration '" + keyName + "' is not set"
                    : required.message();
                errors.add(new ValidationError(keyName, ValidationError.ConstraintType.REQUIRED, 
                    message, null, sensitive));
            }
        }

        // Skip further validation if value is null
        if (value == null) {
            return;
        }

        // Check @NotEmpty
        if (field.isAnnotationPresent(NotEmpty.class)) {
            NotEmpty notEmpty = field.getAnnotation(NotEmpty.class);
            if (value.trim().isEmpty()) {
                String message = notEmpty.message().isEmpty()
                    ? "Configuration '" + keyName + "' must not be empty"
                    : notEmpty.message();
                errors.add(new ValidationError(keyName, ValidationError.ConstraintType.NOT_EMPTY,
                    message, value, sensitive));
            }
        }

        // Check @Pattern
        if (field.isAnnotationPresent(Pattern.class)) {
            Pattern pattern = field.getAnnotation(Pattern.class);
            try {
                if (!value.matches(pattern.regex())) {
                    String message = pattern.message().isEmpty()
                        ? "Configuration '" + keyName + "' does not match pattern: " + pattern.regex()
                        : pattern.message();
                    errors.add(new ValidationError(keyName, ValidationError.ConstraintType.PATTERN,
                        message, value, sensitive));
                }
            } catch (PatternSyntaxException e) {
                errors.add(new ValidationError(keyName, ValidationError.ConstraintType.PATTERN,
                    "Invalid regex pattern: " + pattern.regex(), value, sensitive));
            }
        }

        // Check @Range
        if (field.isAnnotationPresent(Range.class)) {
            Range range = field.getAnnotation(Range.class);
            validateRange(keyName, value, range, sensitive, errors);
        }
    }

    /**
     * Validates a numeric value against a Range constraint.
     */
    private void validateRange(String keyName, String value, Range range,
                               boolean sensitive, List<ValidationError> errors) {
        try {
            long numValue = Long.parseLong(value.trim());
            if (numValue < range.min() || numValue > range.max()) {
                String message = range.message().isEmpty()
                    ? buildRangeMessage(keyName, range.min(), range.max())
                    : range.message();
                errors.add(new ValidationError(keyName, ValidationError.ConstraintType.RANGE,
                    message, value, sensitive));
            }
        } catch (NumberFormatException e) {
            String displayValue = sensitive ? "***MASKED***" : value;
            errors.add(new ValidationError(keyName, ValidationError.ConstraintType.TYPE,
                "Configuration '" + keyName + "' must be a valid number, got: " + displayValue,
                value, sensitive));
        }
    }

    /**
     * Builds a range error message.
     */
    private String buildRangeMessage(String keyName, long min, long max) {
        if (min == Long.MIN_VALUE) {
            return "Configuration '" + keyName + "' must be at most " + max;
        } else if (max == Long.MAX_VALUE) {
            return "Configuration '" + keyName + "' must be at least " + min;
        } else {
            return "Configuration '" + keyName + "' must be between " + min + " and " + max;
        }
    }

    /**
     * Gets the enum field for a ConfNGKey.
     */
    private Field getEnumField(ConfNGKey key) {
        if (!(key instanceof Enum)) {
            return null;
        }
        Enum<?> enumValue = (Enum<?>) key;
        try {
            return enumValue.getDeclaringClass().getField(enumValue.name());
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}

