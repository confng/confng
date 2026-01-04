package org.confng.gradle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConfNGExtension}.
 */
class ConfNGExtensionTest {

    private ConfNGExtension extension;

    @BeforeEach
    void setUp() {
        extension = new ConfNGExtension();
    }

    @Test
    void defaultForwardSystemPropertiesIsTrue() {
        assertTrue(extension.isForwardSystemProperties());
    }

    @Test
    void setForwardSystemProperties() {
        extension.setForwardSystemProperties(false);
        assertFalse(extension.isForwardSystemProperties());

        extension.setForwardSystemProperties(true);
        assertTrue(extension.isForwardSystemProperties());
    }

    @Test
    void defaultIncludePatternsIsEmpty() {
        assertNotNull(extension.getIncludePatterns());
        assertTrue(extension.getIncludePatterns().isEmpty());
    }

    @Test
    void setIncludePatterns() {
        extension.setIncludePatterns(Arrays.asList("app.", "database."));
        assertEquals(2, extension.getIncludePatterns().size());
        assertTrue(extension.getIncludePatterns().contains("app."));
        assertTrue(extension.getIncludePatterns().contains("database."));
    }

    @Test
    void setIncludePatternsWithNull() {
        extension.setIncludePatterns(null);
        assertNotNull(extension.getIncludePatterns());
        assertTrue(extension.getIncludePatterns().isEmpty());
    }

    @Test
    void defaultExcludePatternsIsEmpty() {
        assertNotNull(extension.getExcludePatterns());
        assertTrue(extension.getExcludePatterns().isEmpty());
    }

    @Test
    void setExcludePatterns() {
        extension.setExcludePatterns(Arrays.asList("secret.", "password"));
        assertEquals(2, extension.getExcludePatterns().size());
        assertTrue(extension.getExcludePatterns().contains("secret."));
        assertTrue(extension.getExcludePatterns().contains("password"));
    }

    @Test
    void setExcludePatternsWithNull() {
        extension.setExcludePatterns(null);
        assertNotNull(extension.getExcludePatterns());
        assertTrue(extension.getExcludePatterns().isEmpty());
    }

    @Test
    void patternsCanBeRetrievedAndModifiedExternally() {
        extension.setIncludePatterns(Arrays.asList("app.", "database."));
        assertEquals(2, extension.getIncludePatterns().size());

        // Verify we can set new patterns
        extension.setIncludePatterns(Arrays.asList("new.", "other.", "third."));
        assertEquals(3, extension.getIncludePatterns().size());
    }
}

