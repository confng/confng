package org.confng.validation;

import org.confng.ConfNG;
import org.confng.api.ConfNGKey;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for the configuration validation framework.
 *
 * @author Bharat Kumar Malviya
 * @since 1.1.0
 */
public class ValidationTest {

    /**
     * Test configuration keys with validation annotations.
     */
    enum ValidatedConfig implements ConfNGKey {
        @Required
        REQUIRED_KEY("required.key", null, false),

        @Required
        REQUIRED_WITH_DEFAULT("required.with.default", "default-value", false),

        @NotEmpty
        NOT_EMPTY_KEY("not.empty.key", null, false),

        @Required
        @NotEmpty
        REQUIRED_NOT_EMPTY("required.not.empty", null, false),

        @Pattern(regex = "^[a-zA-Z0-9]+$")
        ALPHANUMERIC_KEY("alphanumeric.key", null, false),

        @Pattern(regex = "^https?://.*", message = "Must be a valid URL")
        URL_KEY("url.key", null, false),

        @Range(min = 1, max = 100)
        PERCENTAGE("percentage", null, false),

        @Range(min = 1, max = 65535)
        PORT("port", "8080", false),

        @Range(min = 0)
        NON_NEGATIVE("non.negative", null, false),

        @Required
        SENSITIVE_REQUIRED("sensitive.required", null, true),

        NO_VALIDATION("no.validation", null, false);

        private final String key;
        private final String defaultValue;
        private final boolean sensitive;

        ValidatedConfig(String key, String defaultValue, boolean sensitive) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.sensitive = sensitive;
        }

        @Override
        public String getKey() { return key; }

        @Override
        public String getDefaultValue() { return defaultValue; }

        @Override
        public boolean isSensitive() { return sensitive; }
    }

    @AfterMethod
    public void reset() {
        ConfNG.clearSourcesAndUseDefaults();
        // Clear all test properties
        for (ValidatedConfig config : ValidatedConfig.values()) {
            System.clearProperty(config.getKey());
        }
    }

    @Test
    public void validatePassesWhenAllConstraintsSatisfied() {
        System.setProperty("required.key", "value");
        System.setProperty("not.empty.key", "value");
        System.setProperty("required.not.empty", "value");
        System.setProperty("alphanumeric.key", "abc123");
        System.setProperty("url.key", "https://example.com");
        System.setProperty("percentage", "50");
        System.setProperty("non.negative", "0");
        System.setProperty("sensitive.required", "secret");

        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.values());
        ValidationResult result = ConfNG.validate(keys);

        assertTrue(result.isValid());
        assertEquals(result.getErrorCount(), 0);
    }

    @Test
    public void validateFailsForMissingRequiredValue() {
        // Don't set required.key
        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.REQUIRED_KEY);
        ValidationResult result = ConfNG.validate(keys);

        assertFalse(result.isValid());
        assertEquals(result.getErrorCount(), 1);
        assertEquals(result.getErrors().get(0).getConstraintType(), 
            ValidationError.ConstraintType.REQUIRED);
        assertTrue(result.getErrors().get(0).getMessage().contains("required.key"));
    }

    @Test
    public void validatePassesForRequiredWithDefault() {
        // Don't set required.with.default - should use default
        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.REQUIRED_WITH_DEFAULT);
        ValidationResult result = ConfNG.validate(keys);

        assertTrue(result.isValid());
    }

    @Test
    public void validateFailsForEmptyValue() {
        System.setProperty("not.empty.key", "   ");

        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.NOT_EMPTY_KEY);
        ValidationResult result = ConfNG.validate(keys);

        assertFalse(result.isValid());
        assertEquals(result.getErrors().get(0).getConstraintType(),
            ValidationError.ConstraintType.NOT_EMPTY);
    }

    @Test
    public void validateFailsForPatternMismatch() {
        System.setProperty("alphanumeric.key", "abc-123"); // Contains hyphen

        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.ALPHANUMERIC_KEY);
        ValidationResult result = ConfNG.validate(keys);

        assertFalse(result.isValid());
        assertEquals(result.getErrors().get(0).getConstraintType(),
            ValidationError.ConstraintType.PATTERN);
    }

    @Test
    public void validateUsesCustomPatternMessage() {
        System.setProperty("url.key", "not-a-url");

        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.URL_KEY);
        ValidationResult result = ConfNG.validate(keys);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("Must be a valid URL"));
    }

    @Test
    public void validateFailsForValueOutOfRange() {
        System.setProperty("percentage", "150"); // > 100

        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.PERCENTAGE);
        ValidationResult result = ConfNG.validate(keys);

        assertFalse(result.isValid());
        assertEquals(result.getErrors().get(0).getConstraintType(),
            ValidationError.ConstraintType.RANGE);
        assertTrue(result.getErrors().get(0).getMessage().contains("between 1 and 100"));
    }

    @Test
    public void validateFailsForNonNumericRangeValue() {
        System.setProperty("percentage", "not-a-number");

        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.PERCENTAGE);
        ValidationResult result = ConfNG.validate(keys);

        assertFalse(result.isValid());
        assertEquals(result.getErrors().get(0).getConstraintType(),
            ValidationError.ConstraintType.TYPE);
    }

    @Test
    public void validatePassesForValueInRange() {
        System.setProperty("percentage", "50");

        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.PERCENTAGE);
        ValidationResult result = ConfNG.validate(keys);

        assertTrue(result.isValid());
    }

    @Test
    public void validatePassesForDefaultInRange() {
        // PORT has default "8080" which is in range 1-65535
        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.PORT);
        ValidationResult result = ConfNG.validate(keys);

        assertTrue(result.isValid());
    }

    @Test
    public void validateMasksSensitiveValuesInErrors() {
        // Don't set sensitive.required
        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.SENSITIVE_REQUIRED);
        ValidationResult result = ConfNG.validate(keys);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).isSensitive());
    }

    @Test
    public void validateCollectsMultipleErrors() {
        // Don't set required.key and required.not.empty
        List<ConfNGKey> keys = Arrays.asList(
            ValidatedConfig.REQUIRED_KEY,
            ValidatedConfig.REQUIRED_NOT_EMPTY
        );
        ValidationResult result = ConfNG.validate(keys);

        assertFalse(result.isValid());
        assertEquals(result.getErrorCount(), 2);
    }

    @Test
    public void validateSkipsKeysWithoutAnnotations() {
        // NO_VALIDATION has no annotations
        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.NO_VALIDATION);
        ValidationResult result = ConfNG.validate(keys);

        assertTrue(result.isValid());
    }

    @Test
    public void validationResultGetErrorsForKey() {
        System.setProperty("required.not.empty", ""); // Empty value

        List<ConfNGKey> keys = Arrays.asList(ValidatedConfig.REQUIRED_NOT_EMPTY);
        ValidationResult result = ConfNG.validate(keys);

        List<ValidationError> errors = result.getErrorsForKey("required.not.empty");
        assertEquals(errors.size(), 1);
    }

    @Test
    public void validationResultGetErrorsByType() {
        // Don't set required.key and required.not.empty
        List<ConfNGKey> keys = Arrays.asList(
            ValidatedConfig.REQUIRED_KEY,
            ValidatedConfig.REQUIRED_NOT_EMPTY
        );
        ValidationResult result = ConfNG.validate(keys);

        List<ValidationError> requiredErrors =
            result.getErrorsByType(ValidationError.ConstraintType.REQUIRED);
        assertEquals(requiredErrors.size(), 2);
    }

    @Test
    public void validationResultSummaryContainsAllErrors() {
        List<ConfNGKey> keys = Arrays.asList(
            ValidatedConfig.REQUIRED_KEY,
            ValidatedConfig.REQUIRED_NOT_EMPTY
        );
        ValidationResult result = ConfNG.validate(keys);

        String summary = result.getSummary();
        assertTrue(summary.contains("required.key"));
        assertTrue(summary.contains("required.not.empty"));
        assertTrue(summary.contains("2 error(s)"));
    }

    @Test
    public void validateWithVarargs() {
        System.setProperty("required.key", "value");

        ValidationResult result = ConfNG.validate(
            ValidatedConfig.REQUIRED_KEY,
            ValidatedConfig.NO_VALIDATION
        );

        assertTrue(result.isValid());
    }
}

