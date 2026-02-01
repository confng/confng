package org.confng.integration;

import org.confng.ConfNG;
import org.confng.api.ConfNGKey;
import org.confng.integration.micronaut.ConfNGPropertySourceLoader;
import org.confng.integration.spring.ConfNGPropertySource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Tests for Spring and Micronaut integration classes.
 */
public class IntegrationTest {

    // Test enum for integration
    public enum TestConfig implements ConfNGKey {
        APP_NAME("app.name", "IntegrationTestApp", false),
        APP_PORT("app.port", "9090", false),
        APP_DEBUG("app.debug", "false", false);

        private final String key;
        private final String defaultValue;
        private final boolean sensitive;

        TestConfig(String key, String defaultValue, boolean sensitive) {
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

    @BeforeMethod
    public void setUp() {
        ConfNG.clearSourcesAndUseDefaults();
    }

    @AfterMethod
    public void tearDown() {
        ConfNG.clearSourcesAndUseDefaults();
    }

    // Spring Integration Tests

    @Test
    public void testSpringPropertySourceWithKeyClass() {
        ConfNGPropertySource source = new ConfNGPropertySource("confng", TestConfig.class);
        assertEquals(source.getName(), "confng");
    }

    @Test
    public void testSpringPropertySourceWithNameOnly() {
        ConfNGPropertySource source = new ConfNGPropertySource("my-source");
        assertEquals(source.getName(), "my-source");
    }

    @Test
    public void testSpringPropertySourceGetPropertyNames() {
        ConfNGPropertySource source = new ConfNGPropertySource("confng", TestConfig.class);
        String[] names = source.getPropertyNames();
        assertEquals(names.length, 3);
    }

    @Test
    public void testSpringPropertySourceContainsProperty() {
        ConfNGPropertySource source = new ConfNGPropertySource("confng", TestConfig.class);
        assertTrue(source.containsProperty("app.name"));
        assertTrue(source.containsProperty("app.port"));
        assertFalse(source.containsProperty("nonexistent.key"));
    }

    @Test
    public void testSpringPropertySourceGetProperty() {
        ConfNGPropertySource source = new ConfNGPropertySource("confng", TestConfig.class);
        // Property value comes from ConfNG.getFromSources which may return null if not loaded
        Object value = source.getProperty("app.name");
        // Value may be null if no sources loaded, but method should not throw
    }

    @Test
    public void testSpringPropertySourceToSpringPropertySource() {
        ConfNGPropertySource source = new ConfNGPropertySource("confng", TestConfig.class);
        Object springSource = source.toSpringPropertySource();
        assertNotNull(springSource, "Should return Spring adapter");
    }

    // Micronaut Integration Tests

    @Test
    public void testMicronautPropertySourceLoaderWithKeyClass() {
        ConfNGPropertySourceLoader loader = new ConfNGPropertySourceLoader(TestConfig.class);
        assertEquals(loader.getName(), "confng");
    }

    @Test
    public void testMicronautPropertySourceLoaderDefaultConstructor() {
        ConfNGPropertySourceLoader loader = new ConfNGPropertySourceLoader();
        assertEquals(loader.getName(), "confng");
    }

    @Test
    public void testMicronautPropertySourceLoaderGetPropertyNames() {
        ConfNGPropertySourceLoader loader = new ConfNGPropertySourceLoader(TestConfig.class);
        Set<String> names = loader.getPropertyNames();
        assertEquals(names.size(), 3);
        assertTrue(names.contains("app.name"));
        assertTrue(names.contains("app.port"));
        assertTrue(names.contains("app.debug"));
    }

    @Test
    public void testMicronautPropertySourceLoaderGetExtensions() {
        ConfNGPropertySourceLoader loader = new ConfNGPropertySourceLoader();
        Set<String> extensions = loader.getExtensions();
        assertTrue(extensions.contains("confng"));
    }

    @Test
    public void testMicronautPropertySourceLoaderIsEnabled() {
        ConfNGPropertySourceLoader loader = new ConfNGPropertySourceLoader();
        assertTrue(loader.isEnabled());
    }

    @Test
    public void testMicronautPropertySourceLoaderGetOrder() {
        ConfNGPropertySourceLoader loader = new ConfNGPropertySourceLoader();
        assertEquals(loader.getOrder(), -100);
    }

    @Test
    public void testMicronautPropertySourceLoaderLoad() {
        ConfNGPropertySourceLoader loader = new ConfNGPropertySourceLoader(TestConfig.class);
        Map<String, Object> props = loader.load("test-resource");
        assertNotNull(props);
    }

    @Test
    public void testMicronautPropertySourceLoaderGet() {
        ConfNGPropertySourceLoader loader = new ConfNGPropertySourceLoader(TestConfig.class);
        // Get returns value from ConfNG.getFromSources
        Object value = loader.get("app.name");
        // May be null if no sources loaded
    }

    @Test
    public void testMicronautPropertySourceLoaderAsMap() {
        ConfNGPropertySourceLoader loader = new ConfNGPropertySourceLoader(TestConfig.class);
        Map<String, Object> map = loader.asMap();
        assertNotNull(map);
    }

    @Test
    public void testMicronautPropertySourceLoaderPropertyNamesUnmodifiable() {
        ConfNGPropertySourceLoader loader = new ConfNGPropertySourceLoader(TestConfig.class);
        Set<String> names = loader.getPropertyNames();
        try {
            names.add("new.key");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
}

