package org.confng.validation;

/**
 * Represents a single validation error for a configuration key.
 *
 * <p>This class contains details about what validation failed and why,
 * including the key name, the constraint that was violated, and a
 * human-readable error message.</p>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see ValidationResult
 * @see ConfigValidator
 */
public class ValidationError {

    /**
     * Types of validation constraints that can be violated.
     */
    public enum ConstraintType {
        /** Value is required but missing */
        REQUIRED,
        /** Value does not match the expected pattern */
        PATTERN,
        /** Value is outside the allowed range */
        RANGE,
        /** Value is empty when it should not be */
        NOT_EMPTY,
        /** Value cannot be converted to the expected type */
        TYPE
    }

    private final String key;
    private final ConstraintType constraintType;
    private final String message;
    private final String actualValue;
    private final boolean sensitive;

    /**
     * Creates a new ValidationError.
     *
     * @param key the configuration key that failed validation
     * @param constraintType the type of constraint that was violated
     * @param message the error message
     * @param actualValue the actual value that failed validation (may be null)
     * @param sensitive whether the value is sensitive and should be masked
     */
    public ValidationError(String key, ConstraintType constraintType, String message, 
                          String actualValue, boolean sensitive) {
        this.key = key;
        this.constraintType = constraintType;
        this.message = message;
        this.actualValue = actualValue;
        this.sensitive = sensitive;
    }

    /**
     * Gets the configuration key that failed validation.
     *
     * @return the key name
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the type of constraint that was violated.
     *
     * @return the constraint type
     */
    public ConstraintType getConstraintType() {
        return constraintType;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the actual value that failed validation.
     * Returns "***MASKED***" for sensitive values.
     *
     * @return the actual value or masked placeholder
     */
    public String getActualValue() {
        if (sensitive && actualValue != null) {
            return "***MASKED***";
        }
        return actualValue;
    }

    /**
     * Checks if the value is sensitive.
     *
     * @return true if the value is sensitive
     */
    public boolean isSensitive() {
        return sensitive;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationError{key='").append(key).append("'");
        sb.append(", constraint=").append(constraintType);
        sb.append(", message='").append(message).append("'");
        if (actualValue != null) {
            sb.append(", value='").append(getActualValue()).append("'");
        }
        sb.append("}");
        return sb.toString();
    }
}

