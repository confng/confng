package org.confng.metrics;

import org.confng.ConfNG;
import org.confng.api.ConfNGKey;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests for ConfigLogger.
 */
public class ConfigLoggerTest {

    // Test enum for logging
    public enum TestConfig implements ConfNGKey {
        APP_NAME("app.name", "TestApp", false),
        APP_PORT("app.port", "8080", false),
        DB_PASSWORD("db.password", null, true),
        API_SECRET("api.secret", "secret123", true);

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

    @Test
    public void testStaticLogConfiguration() {
        // Should not throw
        ConfigLogger.logConfiguration(TestConfig.class);
    }

    @Test
    public void testBuilderPattern() {
        // Should not throw
        ConfigLogger.builder()
            .addSensitivePattern("custom.secret")
            .showSources(true)
            .showDefaults(true)
            .logLevel("info")
            .logConfiguration(TestConfig.class);
    }

    @Test
    public void testBuilderWithDebugLevel() {
        // Should not throw
        ConfigLogger.builder()
            .logLevel("debug")
            .logConfiguration(TestConfig.class);
    }

    @Test
    public void testBuilderWithTraceLevel() {
        // Should not throw
        ConfigLogger.builder()
            .logLevel("trace")
            .logConfiguration(TestConfig.class);
    }

    @Test
    public void testBuilderWithWarnLevel() {
        // Should not throw
        ConfigLogger.builder()
            .logLevel("warn")
            .logConfiguration(TestConfig.class);
    }

    @Test
    public void testClearSensitivePatterns() {
        // Should not throw
        ConfigLogger.builder()
            .clearSensitivePatterns()
            .addSensitivePattern("mypattern")
            .logConfiguration(TestConfig.class);
    }

    @Test
    public void testShowSourcesFalse() {
        // Should not throw
        ConfigLogger.builder()
            .showSources(false)
            .logConfiguration(TestConfig.class);
    }

    @Test
    public void testShowDefaultsFalse() {
        // Should not throw
        ConfigLogger.builder()
            .showDefaults(false)
            .logConfiguration(TestConfig.class);
    }

    @Test
    public void testBuildReturnsConfigLogger() {
        ConfigLogger logger = ConfigLogger.builder()
            .addSensitivePattern("test")
            .build();
        assertNotNull(logger, "Builder should return ConfigLogger instance");
    }

    @Test
    public void testDoLogConfiguration() {
        ConfigLogger logger = ConfigLogger.builder()
            .showDefaults(true)
            .build();
        // Should not throw
        logger.doLogConfiguration(TestConfig.class);
    }

    @Test
    public void testMultipleSensitivePatterns() {
        // Should not throw
        ConfigLogger.builder()
            .addSensitivePattern("pattern1")
            .addSensitivePattern("pattern2")
            .addSensitivePattern("pattern3")
            .logConfiguration(TestConfig.class);
    }

    @Test
    public void testBuilderChaining() {
        ConfigLogger.Builder builder = ConfigLogger.builder();
        
        // Verify chaining returns same builder
        assertSame(builder.addSensitivePattern("test"), builder);
        assertSame(builder.showSources(true), builder);
        assertSame(builder.showDefaults(true), builder);
        assertSame(builder.logLevel("info"), builder);
        assertSame(builder.clearSensitivePatterns(), builder);
    }
}

