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
 * @version 1.0.0
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
        API_KEY("api.key", null, true);

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
}


