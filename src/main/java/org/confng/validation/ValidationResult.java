package org.confng.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains the results of configuration validation.
 *
 * <p>This class holds a collection of validation errors and provides
 * methods to check if validation passed and to retrieve error details.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ValidationResult result = ConfNG.validate();
 * if (!result.isValid()) {
 *     System.err.println("Configuration validation failed:");
 *     for (ValidationError error : result.getErrors()) {
 *         System.err.println("  - " + error.getMessage());
 *     }
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see ValidationError
 * @see ConfigValidator
 */
public class ValidationResult {

    private final List<ValidationError> errors;

    /**
     * Creates a new ValidationResult with the given errors.
     *
     * @param errors the list of validation errors
     */
    public ValidationResult(List<ValidationError> errors) {
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    /**
     * Creates an empty (valid) ValidationResult.
     *
     * @return a valid result with no errors
     */
    public static ValidationResult valid() {
        return new ValidationResult(Collections.emptyList());
    }

    /**
     * Checks if the validation passed (no errors).
     *
     * @return true if there are no validation errors
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Checks if the validation failed (has errors).
     *
     * @return true if there are validation errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Gets the number of validation errors.
     *
     * @return the error count
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Gets all validation errors.
     *
     * @return an unmodifiable list of errors
     */
    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Gets validation errors for a specific key.
     *
     * @param key the configuration key
     * @return list of errors for the key
     */
    public List<ValidationError> getErrorsForKey(String key) {
        return errors.stream()
            .filter(e -> e.getKey().equals(key))
            .collect(Collectors.toList());
    }

    /**
     * Gets validation errors of a specific constraint type.
     *
     * @param constraintType the constraint type
     * @return list of errors of that type
     */
    public List<ValidationError> getErrorsByType(ValidationError.ConstraintType constraintType) {
        return errors.stream()
            .filter(e -> e.getConstraintType() == constraintType)
            .collect(Collectors.toList());
    }

    /**
     * Gets a formatted summary of all validation errors.
     *
     * @return a human-readable summary
     */
    public String getSummary() {
        if (isValid()) {
            return "Configuration validation passed.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration validation failed with ").append(errors.size()).append(" error(s):\n");
        for (ValidationError error : errors) {
            sb.append("  - [").append(error.getKey()).append("] ").append(error.getMessage()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getSummary();
    }
}

