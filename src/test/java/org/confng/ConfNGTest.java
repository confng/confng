package org.confng;

import org.confng.api.ConfNGKey;
import org.confng.sources.EnvSource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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
        AUTH_TOKEN("authToken", null, false);

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
    public void envHasPrecedenceOverSystemPropertiesByDefault() {
        Map<String, String> fakeEnv = new HashMap<>();
        fakeEnv.put("browser", "envChrome");
        System.setProperty("browser", "sysFirefox");
        ConfNG.clearSourcesAndUseDefaults();
        ConfNG.registerSourceAt(0, new EnvSource(fakeEnv));
        assertEquals(ConfNG.get(TestConfigKey.BROWSER), "envChrome");
        System.clearProperty("browser");
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
}
