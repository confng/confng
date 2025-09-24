package org.confng.sources;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.testng.Assert.*;

/**
 * Unit tests for various ConfigSource implementations.
 * 
 * <p>Tests environment variables, system properties, properties files,
 * JSON files, and error handling scenarios.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 */
public class ConfigSourceTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("confng-test");
    }

    @Test
    public void envSourceReturnsEnvironmentValues() {
        Map<String, String> env = new HashMap<>();
        env.put("TEST_KEY", "test_value");
        
        EnvSource source = new EnvSource(env);
        
        assertEquals("Environment", source.getName());
        assertEquals(Optional.of("test_value"), source.get("TEST_KEY"));
        assertEquals(Optional.empty(), source.get("MISSING_KEY"));
    }

    @Test
    public void systemPropertySourceReturnsSystemProperties() {
        Properties props = new Properties();
        props.setProperty("test.key", "test.value");
        
        SystemPropertySource source = new SystemPropertySource(props);
        
        assertEquals("SystemProperties", source.getName());
        assertEquals(Optional.of("test.value"), source.get("test.key"));
        assertEquals(Optional.empty(), source.get("missing.key"));
    }

    @Test
    public void propertiesSourceLoadsFromFile() throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        Files.writeString(propsFile, "key1=value1\nkey2=value2\n");
        
        PropertiesSource source = new PropertiesSource(propsFile);
        
        assertTrue(source.getName().contains("test.properties"));
        assertEquals(Optional.of("value1"), source.get("key1"));
        assertEquals(Optional.of("value2"), source.get("key2"));
        assertEquals(Optional.empty(), source.get("missing"));
    }

    @Test
    public void propertiesSourceThrowsOnMissingFile() {
        Path missingFile = tempDir.resolve("missing.properties");
        
        expectThrows(IllegalStateException.class, () -> 
            new PropertiesSource(missingFile));
    }

    @Test
    public void jsonSourceLoadsFromFile() throws IOException {
        Path jsonFile = tempDir.resolve("test.json");
        Files.writeString(jsonFile, "{\"key1\":\"value1\",\"key2\":42,\"key3\":true}");
        
        JsonSource source = new JsonSource(jsonFile);
        
        assertTrue(source.getName().contains("test.json"));
        assertEquals(Optional.of("value1"), source.get("key1"));
        assertEquals(Optional.of("42"), source.get("key2"));
        assertEquals(Optional.of("true"), source.get("key3"));
        assertEquals(Optional.empty(), source.get("missing"));
    }

    @Test
    void jsonSourceThrowsOnMissingFile() {
        Path missingFile = tempDir.resolve("missing.json");
        
        expectThrows(IllegalStateException.class, () -> 
            new JsonSource(missingFile));
    }

    @Test
    void jsonSourceHandlesNullValues() throws IOException {
        Path jsonFile = tempDir.resolve("test.json");
        Files.writeString(jsonFile, "{\"key1\":null,\"key2\":\"value2\"}");
        
        JsonSource source = new JsonSource(jsonFile);
        
        assertEquals(Optional.empty(), source.get("key1"));
        assertEquals(Optional.of("value2"), source.get("key2"));
    }
}
