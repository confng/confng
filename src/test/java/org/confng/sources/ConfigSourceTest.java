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

    // ==================== YamlSource Tests ====================

    @Test
    public void yamlSourceLoadsFromFile() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "key1: value1\nkey2: 42\nkey3: true\n");

        YamlSource source = new YamlSource(yamlFile);

        assertTrue(source.getName().contains("test.yaml"));
        assertEquals(Optional.of("value1"), source.get("key1"));
        assertEquals(Optional.of("42"), source.get("key2"));
        assertEquals(Optional.of("true"), source.get("key3"));
        assertEquals(Optional.empty(), source.get("missing"));
    }

    @Test
    public void yamlSourceLoadsNestedValues() throws IOException {
        Path yamlFile = tempDir.resolve("nested.yaml");
        Files.writeString(yamlFile, "app:\n  name: MyApp\n  version: 1.0\ndatabase:\n  host: localhost\n  port: 5432\n");

        YamlSource source = new YamlSource(yamlFile);

        assertEquals(Optional.of("MyApp"), source.get("app.name"));
        assertEquals(Optional.of("1.0"), source.get("app.version"));
        assertEquals(Optional.of("localhost"), source.get("database.host"));
        assertEquals(Optional.of("5432"), source.get("database.port"));
    }

    @Test
    public void yamlSourceLoadsEnvironmentSection() throws IOException {
        Path yamlFile = tempDir.resolve("env.yaml");
        // Use nested YAML structure for proper dot notation access
        Files.writeString(yamlFile, "uat:\n  db:\n    host: uat-db.example.com\n  api:\n    url: https://uat-api.example.com\nprod:\n  db:\n    host: prod-db.example.com\n  api:\n    url: https://prod-api.example.com\n");

        YamlSource uatSource = new YamlSource(yamlFile, "uat");
        YamlSource prodSource = new YamlSource(yamlFile, "prod");

        assertEquals(Optional.of("uat-db.example.com"), uatSource.get("db.host"));
        assertEquals(Optional.of("https://uat-api.example.com"), uatSource.get("api.url"));
        assertEquals(Optional.of("prod-db.example.com"), prodSource.get("db.host"));
        assertEquals(Optional.of("https://prod-api.example.com"), prodSource.get("api.url"));
    }

    @Test
    public void yamlSourceThrowsOnMissingFile() {
        Path missingFile = tempDir.resolve("missing.yaml");

        expectThrows(IllegalStateException.class, () ->
            new YamlSource(missingFile));
    }

    @Test
    public void yamlSourceThrowsOnMissingEnvironmentSection() throws IOException {
        Path yamlFile = tempDir.resolve("env.yaml");
        Files.writeString(yamlFile, "uat:\n  key: value\n");

        expectThrows(IllegalStateException.class, () ->
            new YamlSource(yamlFile, "nonexistent"));
    }

    @Test
    public void yamlSourceHandlesNullValues() throws IOException {
        Path yamlFile = tempDir.resolve("null.yaml");
        Files.writeString(yamlFile, "key1: null\nkey2: value2\n");

        YamlSource source = new YamlSource(yamlFile);

        assertEquals(Optional.empty(), source.get("key1"));
        assertEquals(Optional.of("value2"), source.get("key2"));
    }

    @Test
    public void yamlSourceHandlesEmptyFile() throws IOException {
        Path yamlFile = tempDir.resolve("empty.yaml");
        Files.writeString(yamlFile, "");

        YamlSource source = new YamlSource(yamlFile);

        assertEquals(Optional.empty(), source.get("anykey"));
    }

    @Test
    public void yamlSourceHandlesDeeplyNestedValues() throws IOException {
        Path yamlFile = tempDir.resolve("deep.yaml");
        Files.writeString(yamlFile, "level1:\n  level2:\n    level3:\n      key: deepValue\n");

        YamlSource source = new YamlSource(yamlFile);

        assertEquals(Optional.of("deepValue"), source.get("level1.level2.level3.key"));
    }

    @Test
    public void yamlSourceLoadsFromStringPath() throws IOException {
        Path yamlFile = tempDir.resolve("string-path.yaml");
        Files.writeString(yamlFile, "key: value\n");

        YamlSource source = new YamlSource(yamlFile.toString());

        assertEquals(Optional.of("value"), source.get("key"));
    }

    @Test
    public void yamlSourceLoadsFromStringPathWithEnvironment() throws IOException {
        Path yamlFile = tempDir.resolve("string-env.yaml");
        Files.writeString(yamlFile, "env1:\n  key: env1Value\n");

        YamlSource source = new YamlSource(yamlFile.toString(), "env1");

        assertEquals(Optional.of("env1Value"), source.get("key"));
    }

    // ==================== TomlSource Tests ====================

    @Test
    public void tomlSourceLoadsFromFile() throws IOException {
        Path tomlFile = tempDir.resolve("test.toml");
        Files.writeString(tomlFile, "key1 = \"value1\"\nkey2 = 42\nkey3 = true\n");

        TomlSource source = new TomlSource(tomlFile);

        assertTrue(source.getName().contains("test.toml"));
        assertEquals(Optional.of("value1"), source.get("key1"));
        assertEquals(Optional.of("42"), source.get("key2"));
        assertEquals(Optional.of("true"), source.get("key3"));
        assertEquals(Optional.empty(), source.get("missing"));
    }

    @Test
    public void tomlSourceLoadsNestedValues() throws IOException {
        Path tomlFile = tempDir.resolve("nested.toml");
        Files.writeString(tomlFile, "[app]\nname = \"MyApp\"\nversion = \"1.0\"\n\n[database]\nhost = \"localhost\"\nport = 5432\n");

        TomlSource source = new TomlSource(tomlFile);

        assertEquals(Optional.of("MyApp"), source.get("app.name"));
        assertEquals(Optional.of("1.0"), source.get("app.version"));
        assertEquals(Optional.of("localhost"), source.get("database.host"));
        assertEquals(Optional.of("5432"), source.get("database.port"));
    }

    @Test
    public void tomlSourceLoadsDottedKeys() throws IOException {
        Path tomlFile = tempDir.resolve("dotted.toml");
        Files.writeString(tomlFile, "database.host = \"localhost\"\ndatabase.port = 5432\n");

        TomlSource source = new TomlSource(tomlFile);

        assertEquals(Optional.of("localhost"), source.get("database.host"));
        assertEquals(Optional.of("5432"), source.get("database.port"));
    }

    @Test
    public void tomlSourceLoadsEnvironmentSection() throws IOException {
        Path tomlFile = tempDir.resolve("env.toml");
        Files.writeString(tomlFile, "[uat]\ndb_host = \"uat-db.example.com\"\napi_url = \"https://uat-api.example.com\"\n\n[prod]\ndb_host = \"prod-db.example.com\"\napi_url = \"https://prod-api.example.com\"\n");

        TomlSource uatSource = new TomlSource(tomlFile, "uat");
        TomlSource prodSource = new TomlSource(tomlFile, "prod");

        assertEquals(Optional.of("uat-db.example.com"), uatSource.get("db_host"));
        assertEquals(Optional.of("https://uat-api.example.com"), uatSource.get("api_url"));
        assertEquals(Optional.of("prod-db.example.com"), prodSource.get("db_host"));
        assertEquals(Optional.of("https://prod-api.example.com"), prodSource.get("api_url"));
    }

    @Test
    public void tomlSourceThrowsOnMissingFile() {
        Path missingFile = tempDir.resolve("missing.toml");

        expectThrows(IllegalStateException.class, () ->
            new TomlSource(missingFile));
    }

    @Test
    public void tomlSourceThrowsOnMissingEnvironmentSection() throws IOException {
        Path tomlFile = tempDir.resolve("env.toml");
        Files.writeString(tomlFile, "[uat]\nkey = \"value\"\n");

        expectThrows(IllegalStateException.class, () ->
            new TomlSource(tomlFile, "nonexistent"));
    }

    @Test
    public void tomlSourceHandlesEmptyFile() throws IOException {
        Path tomlFile = tempDir.resolve("empty.toml");
        Files.writeString(tomlFile, "");

        TomlSource source = new TomlSource(tomlFile);

        assertEquals(Optional.empty(), source.get("anykey"));
    }

    @Test
    public void tomlSourceHandlesDeeplyNestedValues() throws IOException {
        Path tomlFile = tempDir.resolve("deep.toml");
        Files.writeString(tomlFile, "[level1.level2.level3]\nkey = \"deepValue\"\n");

        TomlSource source = new TomlSource(tomlFile);

        assertEquals(Optional.of("deepValue"), source.get("level1.level2.level3.key"));
    }

    @Test
    public void tomlSourceLoadsFromStringPath() throws IOException {
        Path tomlFile = tempDir.resolve("string-path.toml");
        Files.writeString(tomlFile, "key = \"value\"\n");

        TomlSource source = new TomlSource(tomlFile.toString());

        assertEquals(Optional.of("value"), source.get("key"));
    }

    @Test
    public void tomlSourceLoadsFromStringPathWithEnvironment() throws IOException {
        Path tomlFile = tempDir.resolve("string-env.toml");
        Files.writeString(tomlFile, "[env1]\nkey = \"env1Value\"\n");

        TomlSource source = new TomlSource(tomlFile.toString(), "env1");

        assertEquals(Optional.of("env1Value"), source.get("key"));
    }

    @Test
    public void tomlSourceThrowsOnInvalidSyntax() throws IOException {
        Path tomlFile = tempDir.resolve("invalid.toml");
        Files.writeString(tomlFile, "key = invalid value without quotes\n");

        expectThrows(IllegalStateException.class, () ->
            new TomlSource(tomlFile));
    }

    // ==================== FileResolver Tests ====================

    @Test
    public void fileResolverResolvesFilesystemFile() throws IOException {
        Path testFile = tempDir.resolve("resolver-test.txt");
        Files.writeString(testFile, "test content");

        org.confng.util.FileResolver.ResolvedFile resolved =
            org.confng.util.FileResolver.resolve(testFile.toString());

        assertNotNull(resolved);
        assertTrue(resolved.exists());
        assertTrue(resolved.isFromFilesystem());
        assertFalse(resolved.isFromClasspath());
        assertEquals("resolver-test.txt", resolved.getFileName());
    }

    @Test
    public void fileResolverResolvesClasspathFile() {
        // classpath-test.yaml exists in test resources
        org.confng.util.FileResolver.ResolvedFile resolved =
            org.confng.util.FileResolver.resolve("classpath-test.yaml");

        assertNotNull(resolved);
        assertTrue(resolved.exists());
        assertFalse(resolved.isFromFilesystem());
        assertTrue(resolved.isFromClasspath());
        assertEquals("classpath-test.yaml", resolved.getFileName());
    }

    @Test
    public void fileResolverReturnsNullForMissingFile() {
        org.confng.util.FileResolver.ResolvedFile resolved =
            org.confng.util.FileResolver.resolve("nonexistent-file.txt");

        assertNull(resolved);
    }

    @Test
    public void fileResolverReturnsNullForNullPath() {
        org.confng.util.FileResolver.ResolvedFile resolved =
            org.confng.util.FileResolver.resolve(null);

        assertNull(resolved);
    }

    @Test
    public void fileResolverReturnsNullForEmptyPath() {
        org.confng.util.FileResolver.ResolvedFile resolved =
            org.confng.util.FileResolver.resolve("");

        assertNull(resolved);
    }

    @Test
    public void fileResolverExistsReturnsTrueForExistingFile() throws IOException {
        Path testFile = tempDir.resolve("exists-test.txt");
        Files.writeString(testFile, "test content");

        assertTrue(org.confng.util.FileResolver.exists(testFile.toString()));
    }

    @Test
    public void fileResolverExistsReturnsFalseForMissingFile() {
        assertFalse(org.confng.util.FileResolver.exists("nonexistent-file.txt"));
    }

    @Test
    public void fileResolverExistsReturnsTrueForClasspathFile() {
        assertTrue(org.confng.util.FileResolver.exists("classpath-test.yaml"));
    }

    @Test
    public void fileResolverGetFileNameReturnsCorrectName() throws IOException {
        Path testFile = tempDir.resolve("filename-test.txt");
        Files.writeString(testFile, "test content");

        assertEquals("filename-test.txt", org.confng.util.FileResolver.getFileName(testFile.toString()));
    }

    @Test
    public void fileResolverGetFileNameReturnsNullForMissingFile() {
        assertNull(org.confng.util.FileResolver.getFileName("nonexistent-file.txt"));
    }

    @Test
    public void fileResolverOpenInputStreamReadsFile() throws IOException {
        Path testFile = tempDir.resolve("stream-test.txt");
        Files.writeString(testFile, "stream content");

        try (java.io.InputStream is = org.confng.util.FileResolver.openInputStream(testFile.toString())) {
            byte[] content = is.readAllBytes();
            assertEquals("stream content", new String(content));
        }
    }

    @Test
    public void fileResolverOpenInputStreamThrowsForMissingFile() {
        expectThrows(IOException.class, () ->
            org.confng.util.FileResolver.openInputStream("nonexistent-file.txt"));
    }

    @Test
    public void fileResolverOpenReaderReadsFile() throws IOException {
        Path testFile = tempDir.resolve("reader-test.txt");
        Files.writeString(testFile, "reader content");

        try (java.io.Reader reader = org.confng.util.FileResolver.openReader(testFile.toString())) {
            char[] buffer = new char[100];
            int len = reader.read(buffer);
            assertEquals("reader content", new String(buffer, 0, len));
        }
    }

    @Test
    public void fileResolverOpenReaderThrowsForMissingFile() {
        expectThrows(IOException.class, () ->
            org.confng.util.FileResolver.openReader("nonexistent-file.txt"));
    }

    @Test
    public void fileResolverPrefersFilesystemOverClasspath() throws IOException {
        // Create a file with the same name as a classpath resource
        Path testFile = tempDir.resolve("classpath-test.yaml");
        Files.writeString(testFile, "filesystem version");

        // When resolving with full path, should get filesystem version
        org.confng.util.FileResolver.ResolvedFile resolved =
            org.confng.util.FileResolver.resolve(testFile.toString());

        assertNotNull(resolved);
        assertTrue(resolved.isFromFilesystem());
        assertFalse(resolved.isFromClasspath());
    }
}
