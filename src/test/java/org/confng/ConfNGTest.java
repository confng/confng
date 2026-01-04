package org.confng;

import org.confng.api.ConfNGKey;
import org.confng.sources.EnvSource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import org.confng.api.ConfigurationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.*;

/**
 * Unit tests for ConfNG configuration management.
 * 
 * <p>Tests various configuration sources, precedence rules, type conversions,
 * and sensitive data handling.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 */
public class ConfNGTest {

    /**
     * Test configuration keys for unit testing.
     */
    enum TestConfigKey implements ConfNGKey {
        BROWSER("browser", "chrome", false),
        TIMEOUT("timeout", "30", false),
        HEADLESS("headless", "false", false),
        API_KEY("api.key", null, true),
        APP_NAME("app.name", null, false),
        APP_VERSION("app.version", null, false),
        DB_HOST("db.host", null, false),
        DB_PORT("db.port", null, false),
        ENVIRONMENT("environment", "local", false),
        TEST_CLASSPATH("test.classpath", null, false),
        TEST_VALUE("test.value", null, false),
        PROJECT_NAME("project.name", null, false),
        AUTH_TOKEN("authToken", null, false),
        // New keys for typed getter tests
        LONG_VALUE("long.value", null, false),
        DOUBLE_VALUE("double.value", null, false),
        LIST_VALUE("list.value", null, false),
        DURATION_VALUE("duration.value", null, false),
        SENSITIVE_NUMBER("sensitive.number", null, true);

        private final String key;
        private final String defaultValue;
        private final boolean sensitive;

        TestConfigKey(String key, String defaultValue, boolean sensitive) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.sensitive = sensitive;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getDefaultValue() {
            return defaultValue;
        }

        @Override
        public boolean isSensitive() {
            return sensitive;
        }
    }

    @AfterMethod
    public void reset() {
        ConfNG.clearSourcesAndUseDefaults();
    }

    @Test
    public void resolvesFromEnv() {
        Map<String, String> fakeEnv = new HashMap<>();
        fakeEnv.put("browser", "envChrome");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.registerSourceAt(0, new EnvSource(fakeEnv));
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "envChrome");
    }

    @Test
    public void systemPropertiesHasPrecedenceOverEnvByDefault() {
        Map<String, String> fakeEnv = new HashMap<>();
        fakeEnv.put("browser", "envChrome");
        System.setProperty("browser", "sysFirefox");
        try {
            ConfNG.clearSourcesAndUseDefaults();
            ConfNG.registerSource(new EnvSource(fakeEnv));
            assertEquals(ConfNG.get(TestConfigKey.BROWSER), "sysFirefox");
        } finally {
            System.clearProperty("browser");
        }
    }

    @Test
    public void resolvesFromPropertiesFile() throws IOException {
        Path tmp = Files.createTempFile("confng", ".properties");
        Files.writeString(tmp, "browser=propsEdge\n");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.loadProperties(tmp.toString());
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "propsEdge");
    }

    @Test
    public void resolvesFromJsonFile() throws IOException {
        Path tmp = Files.createTempFile("confng", ".json");
        Files.writeString(tmp, "{\n  \"browser\": \"jsonSafari\"\n}\n");
        System.clearProperty("browser");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.loadJson(tmp.toString());
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "jsonSafari");
    }

    @Test
    public void returnsDefaultWhenMissing() {
        ConfNG.clearSourcesAndUseDefaults();
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "chrome");
    }

    @Test
    public void typedGettersWork() {
        System.setProperty("timeout", "45");
        System.setProperty("headless", "true");
        
        assertEquals(Integer.valueOf(45), ConfNG.getInt(TestConfigKey.TIMEOUT));
        assertEquals(ConfNG.getBoolean(TestConfigKey.HEADLESS), Boolean.TRUE);
        
        System.clearProperty("timeout");
        System.clearProperty("headless");
    }
    
    @Test
    public void sensitiveConfigurationIsMasked() {
        System.setProperty("api.key", "secret-key-123");
        
        String actualValue = ConfNG.get(TestConfigKey.API_KEY);
        assertEquals(actualValue, "secret-key-123");
        
        String maskedValue = ConfNG.getForDisplay(TestConfigKey.API_KEY);
        assertEquals(maskedValue, "***MASKED***");
        
        System.clearProperty("api.key");
    }
    
    @Test
    public void nonSensitiveConfigurationIsNotMasked() {
        System.setProperty("browser", "firefox");

        String actualValue = ConfNG.get(TestConfigKey.BROWSER);
        assertEquals(actualValue, "firefox");

        String displayValue = ConfNG.getForDisplay(TestConfigKey.BROWSER);
        assertEquals(displayValue, "firefox");

        System.clearProperty("browser");
    }

    @Test
    public void genericLoadAutoDetectsPropertiesFile() throws IOException {
        Path tmp = Files.createTempFile("config", ".properties");
        Files.writeString(tmp, "browser=autoDetectedChrome\n");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.load(tmp.toString());
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "autoDetectedChrome");
        Files.deleteIfExists(tmp);
    }

    @Test
    public void genericLoadAutoDetectsJsonFile() throws IOException {
        Path tmp = Files.createTempFile("config", ".json");
        Files.writeString(tmp, "{\"browser\":\"autoDetectedFirefox\"}");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.load(tmp.toString());
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "autoDetectedFirefox");
        Files.deleteIfExists(tmp);
    }

    @Test
    public void genericLoadAutoDetectsYamlFile() throws IOException {
        Path tmp = Files.createTempFile("config", ".yaml");
        Files.writeString(tmp, "browser: autoDetectedEdge\n");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.load(tmp.toString());
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "autoDetectedEdge");
        Files.deleteIfExists(tmp);
    }

    @Test
    public void genericLoadAutoDetectsYmlFile() throws IOException {
        Path tmp = Files.createTempFile("config", ".yml");
        Files.writeString(tmp, "browser: autoDetectedSafari\n");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.load(tmp.toString());
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "autoDetectedSafari");
        Files.deleteIfExists(tmp);
    }

    @Test
    public void genericLoadAutoDetectsTomlFile() throws IOException {
        Path tmp = Files.createTempFile("config", ".toml");
        Files.writeString(tmp, "browser = \"autoDetectedOpera\"\n");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.load(tmp.toString());
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "autoDetectedOpera");
        Files.deleteIfExists(tmp);
    }

    @Test
    public void genericLoadSilentlySkipsMissingFile() {
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.load("nonexistent-config.json");
        // Should not throw exception
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "chrome"); // default value
    }

    @Test
    public void genericLoadThrowsOnUnsupportedFileType() throws IOException {
        Path tmp = Files.createTempFile("config", ".xml");
        Files.writeString(tmp, "<config></config>");
        ConfNG.clearSourcesAndUseDefaults();

        try {
            ConfNG.load(tmp.toString());
            fail("Should have thrown IllegalArgumentException for unsupported file type");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unsupported file type"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void genericLoadWithEnvironmentAutoDetectsJsonFile() throws IOException {
        Path tmp = Files.createTempFile("config", ".json");
        Files.writeString(tmp, "{\"env1\":{\"browser\":\"env1Browser\"}}");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.load(tmp.toString(), "env1");
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "env1Browser");
        Files.deleteIfExists(tmp);
    }

    @Test
    public void genericLoadWithEnvironmentAutoDetectsYamlFile() throws IOException {
        Path tmp = Files.createTempFile("config", ".yaml");
        Files.writeString(tmp, "env1:\n  browser: env1YamlBrowser\n");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.load(tmp.toString(), "env1");
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "env1YamlBrowser");
        Files.deleteIfExists(tmp);
    }

    @Test
    public void genericLoadWithEnvironmentAutoDetectsTomlFile() throws IOException {
        Path tmp = Files.createTempFile("config", ".toml");
        Files.writeString(tmp, "[env1]\nbrowser = \"env1TomlBrowser\"\n");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.load(tmp.toString(), "env1");
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "env1TomlBrowser");
        Files.deleteIfExists(tmp);
    }

    @Test
    public void genericLoadWithEnvironmentThrowsOnPropertiesFile() throws IOException {
        Path tmp = Files.createTempFile("config", ".properties");
        Files.writeString(tmp, "browser=test\n");
        ConfNG.clearSourcesAndUseDefaults();

        try {
            ConfNG.load(tmp.toString(), "env1");
            fail("Should have thrown IllegalArgumentException for properties file with environment");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Properties files don't support environment sections"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void testGetEnvironmentNameReturnsLocal() {
        ConfNG.clearSourcesAndUseDefaults();
        String env = ConfNG.getEnvironmentName();
        assertEquals(env, "local");
    }

    @Test
    public void testLoadFromClasspath() {
        ConfNG.clearSourcesAndUseDefaults();

        // Load file from classpath (not filesystem)
        ConfNG.load("classpath-test.yaml");

        // Verify values were loaded from classpath
        assertEquals(ConfNG.get(TestConfigKey.TEST_CLASSPATH), "loaded-from-classpath");
        assertEquals(ConfNG.get(TestConfigKey.TEST_VALUE), "123");
    }

    @Test
    public void testLoadGlobalTomlWithEnvironmentSection() {
        ConfNG.clearSourcesAndUseDefaults();

        // Load global.toml which has base config and environment sections
        ConfNG.load("global.toml");

        // Verify base values are loaded
        assertEquals(ConfNG.get(TestConfigKey.PROJECT_NAME), "example-project");

        // Load environment-specific section from the same global.toml
        ConfNG.load("global.toml", "env1");

        // Verify environment-specific values are loaded
        assertEquals(ConfNG.get(TestConfigKey.AUTH_TOKEN), "example-env1-token-replace-with-real");
    }

    // ==================== Typed Getter Tests ====================

    @Test
    public void getLongReturnsLongValue() {
        System.setProperty("long.value", "9223372036854775807");
        try {
            Long result = ConfNG.getLong(TestConfigKey.LONG_VALUE);
            assertEquals(result, Long.valueOf(Long.MAX_VALUE));
        } finally {
            System.clearProperty("long.value");
        }
    }

    @Test
    public void getLongReturnsNullWhenMissing() {
        ConfNG.clearSourcesAndUseDefaults();
        assertNull(ConfNG.getLong(TestConfigKey.LONG_VALUE));
    }

    @Test
    public void getLongThrowsOnInvalidValue() {
        System.setProperty("long.value", "not-a-number");
        try {
            ConfNG.getLong(TestConfigKey.LONG_VALUE);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Expected long"));
        } finally {
            System.clearProperty("long.value");
        }
    }

    @Test
    public void getDoubleReturnsDoubleValue() {
        System.setProperty("double.value", "3.14159");
        try {
            Double result = ConfNG.getDouble(TestConfigKey.DOUBLE_VALUE);
            assertEquals(result, 3.14159, 0.00001);
        } finally {
            System.clearProperty("double.value");
        }
    }

    @Test
    public void getDoubleReturnsNullWhenMissing() {
        ConfNG.clearSourcesAndUseDefaults();
        assertNull(ConfNG.getDouble(TestConfigKey.DOUBLE_VALUE));
    }

    @Test
    public void getDoubleThrowsOnInvalidValue() {
        System.setProperty("double.value", "not-a-number");
        try {
            ConfNG.getDouble(TestConfigKey.DOUBLE_VALUE);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Expected double"));
        } finally {
            System.clearProperty("double.value");
        }
    }

    @Test
    public void getListReturnsListOfStrings() {
        System.setProperty("list.value", "a, b, c, d");
        try {
            List<String> result = ConfNG.getList(TestConfigKey.LIST_VALUE);
            assertEquals(result.size(), 4);
            assertEquals(result.get(0), "a");
            assertEquals(result.get(1), "b");
            assertEquals(result.get(2), "c");
            assertEquals(result.get(3), "d");
        } finally {
            System.clearProperty("list.value");
        }
    }

    @Test
    public void getListWithCustomDelimiter() {
        System.setProperty("list.value", "a|b|c");
        try {
            List<String> result = ConfNG.getList(TestConfigKey.LIST_VALUE, "|");
            assertEquals(result.size(), 3);
            assertEquals(result.get(0), "a");
            assertEquals(result.get(1), "b");
            assertEquals(result.get(2), "c");
        } finally {
            System.clearProperty("list.value");
        }
    }

    @Test
    public void getListReturnsEmptyListForEmptyValue() {
        System.setProperty("list.value", "");
        try {
            List<String> result = ConfNG.getList(TestConfigKey.LIST_VALUE);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        } finally {
            System.clearProperty("list.value");
        }
    }

    @Test
    public void getListReturnsNullWhenMissing() {
        ConfNG.clearSourcesAndUseDefaults();
        assertNull(ConfNG.getList(TestConfigKey.LIST_VALUE));
    }

    @Test
    public void getDurationReturnsSecondsFormat() {
        System.setProperty("duration.value", "30s");
        try {
            Duration result = ConfNG.getDuration(TestConfigKey.DURATION_VALUE);
            assertEquals(result, Duration.ofSeconds(30));
        } finally {
            System.clearProperty("duration.value");
        }
    }

    @Test
    public void getDurationReturnsMinutesFormat() {
        System.setProperty("duration.value", "5m");
        try {
            Duration result = ConfNG.getDuration(TestConfigKey.DURATION_VALUE);
            assertEquals(result, Duration.ofMinutes(5));
        } finally {
            System.clearProperty("duration.value");
        }
    }

    @Test
    public void getDurationReturnsHoursFormat() {
        System.setProperty("duration.value", "2h");
        try {
            Duration result = ConfNG.getDuration(TestConfigKey.DURATION_VALUE);
            assertEquals(result, Duration.ofHours(2));
        } finally {
            System.clearProperty("duration.value");
        }
    }

    @Test
    public void getDurationReturnsDaysFormat() {
        System.setProperty("duration.value", "1d");
        try {
            Duration result = ConfNG.getDuration(TestConfigKey.DURATION_VALUE);
            assertEquals(result, Duration.ofDays(1));
        } finally {
            System.clearProperty("duration.value");
        }
    }

    @Test
    public void getDurationReturnsMillisecondsFormat() {
        System.setProperty("duration.value", "500ms");
        try {
            Duration result = ConfNG.getDuration(TestConfigKey.DURATION_VALUE);
            assertEquals(result, Duration.ofMillis(500));
        } finally {
            System.clearProperty("duration.value");
        }
    }

    @Test
    public void getDurationReturnsIso8601Format() {
        System.setProperty("duration.value", "PT30S");
        try {
            Duration result = ConfNG.getDuration(TestConfigKey.DURATION_VALUE);
            assertEquals(result, Duration.ofSeconds(30));
        } finally {
            System.clearProperty("duration.value");
        }
    }

    @Test
    public void getDurationReturnsNullWhenMissing() {
        ConfNG.clearSourcesAndUseDefaults();
        assertNull(ConfNG.getDuration(TestConfigKey.DURATION_VALUE));
    }

    @Test
    public void getDurationThrowsOnInvalidValue() {
        System.setProperty("duration.value", "invalid-duration");
        try {
            ConfNG.getDuration(TestConfigKey.DURATION_VALUE);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Expected duration"));
        } finally {
            System.clearProperty("duration.value");
        }
    }

    @Test
    public void typedGettersMaskSensitiveValuesInErrors() {
        System.setProperty("sensitive.number", "not-a-number");
        try {
            ConfNG.getLong(TestConfigKey.SENSITIVE_NUMBER);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("***MASKED***"));
            assertFalse(e.getMessage().contains("not-a-number"));
        } finally {
            System.clearProperty("sensitive.number");
        }
    }

    // ==================== Optional/Required Configuration Tests ====================

    @Test
    public void getOptionalReturnsValueWhenPresent() {
        System.setProperty("browser", "firefox");
        try {
            Optional<String> result = ConfNG.getOptional(TestConfigKey.BROWSER);
            assertTrue(result.isPresent());
            assertEquals(result.get(), "firefox");
        } finally {
            System.clearProperty("browser");
        }
    }

    @Test
    public void getOptionalReturnsDefaultWhenMissing() {
        ConfNG.clearSourcesAndUseDefaults();
        Optional<String> result = ConfNG.getOptional(TestConfigKey.BROWSER);
        assertTrue(result.isPresent());
        assertEquals(result.get(), "chrome"); // default value
    }

    @Test
    public void getOptionalReturnsEmptyWhenMissingAndNoDefault() {
        ConfNG.clearSourcesAndUseDefaults();
        Optional<String> result = ConfNG.getOptional(TestConfigKey.APP_NAME);
        assertFalse(result.isPresent());
    }

    @Test
    public void getRequiredReturnsValueWhenPresent() {
        System.setProperty("browser", "edge");
        try {
            String result = ConfNG.getRequired(TestConfigKey.BROWSER);
            assertEquals(result, "edge");
        } finally {
            System.clearProperty("browser");
        }
    }

    @Test
    public void getRequiredReturnsDefaultWhenMissing() {
        ConfNG.clearSourcesAndUseDefaults();
        String result = ConfNG.getRequired(TestConfigKey.BROWSER);
        assertEquals(result, "chrome"); // default value
    }

    @Test
    public void getRequiredThrowsWhenMissingAndNoDefault() {
        ConfNG.clearSourcesAndUseDefaults();
        try {
            ConfNG.getRequired(TestConfigKey.APP_NAME);
            fail("Should have thrown ConfigurationException");
        } catch (ConfigurationException e) {
            assertEquals(e.getKey(), "app.name");
            assertTrue(e.getMessage().contains("app.name"));
        }
    }

    @Test
    public void getOrDefaultReturnsValueWhenPresent() {
        System.setProperty("browser", "safari");
        try {
            String result = ConfNG.getOrDefault(TestConfigKey.BROWSER, "fallback");
            assertEquals(result, "safari");
        } finally {
            System.clearProperty("browser");
        }
    }

    @Test
    public void getOrDefaultReturnsKeyDefaultWhenMissing() {
        ConfNG.clearSourcesAndUseDefaults();
        String result = ConfNG.getOrDefault(TestConfigKey.BROWSER, "fallback");
        assertEquals(result, "chrome"); // key's default takes precedence
    }

    @Test
    public void getOrDefaultReturnsFallbackWhenMissingAndNoKeyDefault() {
        ConfNG.clearSourcesAndUseDefaults();
        String result = ConfNG.getOrDefault(TestConfigKey.APP_NAME, "fallback-app");
        assertEquals(result, "fallback-app");
    }

    @Test
    public void getOrDefaultReturnsNullFallbackWhenProvided() {
        ConfNG.clearSourcesAndUseDefaults();
        String result = ConfNG.getOrDefault(TestConfigKey.APP_NAME, null);
        assertNull(result);
    }

    // ==================== Configuration Source Diagnostics Tests ====================

    @Test
    public void getSourceInfoReturnsSourceName() {
        System.setProperty("browser", "firefox");
        try {
            ConfNG.clearSourcesAndUseDefaults();
            ConfNG.refresh(); // Force re-resolution

            org.confng.api.ConfigSourceInfo info = ConfNG.getSourceInfo(TestConfigKey.BROWSER);

            assertNotNull(info);
            assertTrue(info.isFound());
            assertEquals(info.getSourceName(), "SystemProperties");
            assertEquals(info.getValue(), "firefox");
            assertFalse(info.isFromDefault());
        } finally {
            System.clearProperty("browser");
        }
    }

    @Test
    public void getSourceInfoReturnsDefaultWhenNoSource() {
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.refresh();

        org.confng.api.ConfigSourceInfo info = ConfNG.getSourceInfo(TestConfigKey.BROWSER);

        assertNotNull(info);
        assertTrue(info.isFound());
        assertTrue(info.isFromDefault());
        assertEquals(info.getSourceName(), "Default");
        assertEquals(info.getValue(), "chrome");
    }

    @Test
    public void getSourceInfoReturnsNotFoundWhenMissing() {
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.refresh();

        org.confng.api.ConfigSourceInfo info = ConfNG.getSourceInfo(TestConfigKey.APP_NAME);

        assertNotNull(info);
        assertFalse(info.isFound());
        assertNull(info.getSourceName());
    }

    @Test
    public void getSourceInfoMasksSensitiveValues() {
        System.setProperty("api.key", "secret-value");
        try {
            ConfNG.clearSourcesAndUseDefaults();
            ConfNG.refresh();

            org.confng.api.ConfigSourceInfo info = ConfNG.getSourceInfo(TestConfigKey.API_KEY);

            assertTrue(info.isSensitive());
            assertEquals(info.getValue(), "***MASKED***");
            assertEquals(info.getRawValue(), "secret-value");
        } finally {
            System.clearProperty("api.key");
        }
    }

    @Test
    public void getSourceInfoReturnsPriority() {
        System.setProperty("browser", "edge");
        try {
            ConfNG.clearSourcesAndUseDefaults();
            ConfNG.refresh();

            org.confng.api.ConfigSourceInfo info = ConfNG.getSourceInfo(TestConfigKey.BROWSER);

            assertEquals(info.getPriority(), 70);
        } finally {
            System.clearProperty("browser");
        }
    }

    @Test
    public void getAllSourceInfoReturnsMultipleKeys() {
        System.setProperty("browser", "chrome");
        System.setProperty("timeout", "60");
        try {
            ConfNG.clearSourcesAndUseDefaults();
            ConfNG.refresh();

            java.util.Map<String, org.confng.api.ConfigSourceInfo> infos =
                ConfNG.getAllSourceInfo(TestConfigKey.BROWSER, TestConfigKey.TIMEOUT);

            assertEquals(infos.size(), 2);
            assertTrue(infos.containsKey("browser"));
            assertTrue(infos.containsKey("timeout"));
            assertEquals(infos.get("browser").getValue(), "chrome");
            assertEquals(infos.get("timeout").getValue(), "60");
        } finally {
            System.clearProperty("browser");
            System.clearProperty("timeout");
        }
    }

    @Test
    public void getSourceInfoToStringContainsDetails() {
        System.setProperty("browser", "safari");
        try {
            ConfNG.clearSourcesAndUseDefaults();
            ConfNG.refresh();

            org.confng.api.ConfigSourceInfo info = ConfNG.getSourceInfo(TestConfigKey.BROWSER);
            String str = info.toString();

            assertTrue(str.contains("browser"));
            assertTrue(str.contains("SystemProperties"));
            assertTrue(str.contains("safari"));
        } finally {
            System.clearProperty("browser");
        }
    }

    // ==================== Prefix-based Configuration Retrieval Tests ====================

    @Test
    public void getByPrefixReturnsMatchingKeys() {
        System.setProperty("db.host", "localhost");
        System.setProperty("db.port", "5432");
        System.setProperty("db.user", "admin");
        System.setProperty("api.url", "https://api.example.com");
        try {
            ConfNG.clearSourcesAndUseDefaults();
            ConfNG.refresh();

            java.util.Map<String, String> dbConfig = ConfNG.getByPrefix("db.");

            assertEquals(dbConfig.size(), 3);
            assertEquals(dbConfig.get("db.host"), "localhost");
            assertEquals(dbConfig.get("db.port"), "5432");
            assertEquals(dbConfig.get("db.user"), "admin");
            assertFalse(dbConfig.containsKey("api.url"));
        } finally {
            System.clearProperty("db.host");
            System.clearProperty("db.port");
            System.clearProperty("db.user");
            System.clearProperty("api.url");
        }
    }

    @Test
    public void getByPrefixReturnsEmptyMapForNoMatches() {
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.refresh();

        java.util.Map<String, String> result = ConfNG.getByPrefix("nonexistent.");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getByPrefixReturnsEmptyMapForNullPrefix() {
        java.util.Map<String, String> result = ConfNG.getByPrefix(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getByPrefixReturnsEmptyMapForEmptyPrefix() {
        java.util.Map<String, String> result = ConfNG.getByPrefix("");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getKeysWithPrefixReturnsMatchingKeys() {
        System.setProperty("app.name", "MyApp");
        System.setProperty("app.version", "1.0");
        System.setProperty("other.key", "value");
        try {
            ConfNG.clearSourcesAndUseDefaults();
            ConfNG.refresh();

            java.util.Set<String> appKeys = ConfNG.getKeysWithPrefix("app.");

            assertEquals(appKeys.size(), 2);
            assertTrue(appKeys.contains("app.name"));
            assertTrue(appKeys.contains("app.version"));
            assertFalse(appKeys.contains("other.key"));
        } finally {
            System.clearProperty("app.name");
            System.clearProperty("app.version");
            System.clearProperty("other.key");
        }
    }

    @Test
    public void getKeysWithPrefixReturnsEmptySetForNoMatches() {
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.refresh();

        java.util.Set<String> result = ConfNG.getKeysWithPrefix("nonexistent.");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getByPrefixWithInfoReturnsSourceInfo() {
        System.setProperty("db.host", "localhost");
        System.setProperty("db.port", "5432");
        try {
            ConfNG.clearSourcesAndUseDefaults();
            ConfNG.refresh();

            java.util.List<ConfNGKey> keys = java.util.Arrays.asList(
                TestConfigKey.DB_HOST, TestConfigKey.DB_PORT, TestConfigKey.BROWSER
            );
            java.util.Map<String, org.confng.api.ConfigSourceInfo> dbInfo =
                ConfNG.getByPrefixWithInfo("db.", keys);

            assertEquals(dbInfo.size(), 2);
            assertTrue(dbInfo.containsKey("db.host"));
            assertTrue(dbInfo.containsKey("db.port"));
            assertEquals(dbInfo.get("db.host").getValue(), "localhost");
            assertEquals(dbInfo.get("db.port").getValue(), "5432");
        } finally {
            System.clearProperty("db.host");
            System.clearProperty("db.port");
        }
    }
}
