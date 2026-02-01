package org.confng.generator;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.testng.Assert.*;

/**
 * Tests for ConfigKeyGenerator.
 */
public class ConfigKeyGeneratorTest {

    private Path tempDir;
    private Path outputDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("confng-generator-test");
        outputDir = Files.createTempDirectory("confng-generator-output");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        deleteDirectory(tempDir);
        deleteDirectory(outputDir);
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    public void testGenerateFromProperties() throws IOException {
        // Create a test properties file
        Path propsFile = tempDir.resolve("test.properties");
        try (FileWriter writer = new FileWriter(propsFile.toFile())) {
            writer.write("app.name=TestApp\n");
            writer.write("app.port=8080\n");
            writer.write("app.debug=true\n");
        }

        ConfigKeyGenerator generator = new ConfigKeyGenerator();
        generator.setPackageName("com.test.config");
        generator.setEnumName("TestConfig");
        generator.generateFromProperties(propsFile.toString(), outputDir.toString());

        // Verify the generated file exists
        Path generatedFile = outputDir.resolve("com/test/config/TestConfig.java");
        assertTrue(Files.exists(generatedFile), "Generated file should exist");

        // Verify content
        String content = Files.readString(generatedFile);
        assertTrue(content.contains("package com.test.config;"));
        assertTrue(content.contains("public enum TestConfig implements ConfNGKey"));
        assertTrue(content.contains("APP_NAME"));
        assertTrue(content.contains("APP_PORT"));
        assertTrue(content.contains("APP_DEBUG"));
    }

    @Test
    public void testGenerateFromYaml() throws IOException {
        // Create a test YAML schema file
        Path yamlFile = tempDir.resolve("schema.yaml");
        try (FileWriter writer = new FileWriter(yamlFile.toFile())) {
            writer.write("database:\n");
            writer.write("  host:\n");
            writer.write("    type: String\n");
            writer.write("    default: localhost\n");
            writer.write("    description: Database host\n");
            writer.write("  port:\n");
            writer.write("    type: Integer\n");
            writer.write("    default: 5432\n");
            writer.write("    required: true\n");
        }

        ConfigKeyGenerator generator = new ConfigKeyGenerator();
        generator.setPackageName("com.test.db");
        generator.setEnumName("DbConfig");
        generator.generateFromYaml(yamlFile.toString(), outputDir.toString());

        Path generatedFile = outputDir.resolve("com/test/db/DbConfig.java");
        assertTrue(Files.exists(generatedFile), "Generated file should exist");

        String content = Files.readString(generatedFile);
        assertTrue(content.contains("DATABASE_HOST"));
        assertTrue(content.contains("DATABASE_PORT"));
        assertTrue(content.contains("\"database.host\""));
        assertTrue(content.contains("\"database.port\""));
    }

    @Test
    public void testGenerateFromJson() throws IOException {
        // Create a test JSON schema file
        Path jsonFile = tempDir.resolve("schema.json");
        try (FileWriter writer = new FileWriter(jsonFile.toFile())) {
            writer.write("{\n");
            writer.write("  \"server\": {\n");
            writer.write("    \"host\": {\n");
            writer.write("      \"type\": \"String\",\n");
            writer.write("      \"default\": \"0.0.0.0\"\n");
            writer.write("    },\n");
            writer.write("    \"port\": {\n");
            writer.write("      \"type\": \"Integer\",\n");
            writer.write("      \"default\": \"8080\",\n");
            writer.write("      \"required\": true\n");
            writer.write("    }\n");
            writer.write("  }\n");
            writer.write("}\n");
        }

        ConfigKeyGenerator generator = new ConfigKeyGenerator();
        generator.setPackageName("com.test.server");
        generator.setEnumName("ServerConfig");
        generator.generateFromJson(jsonFile.toString(), outputDir.toString());

        Path generatedFile = outputDir.resolve("com/test/server/ServerConfig.java");
        assertTrue(Files.exists(generatedFile), "Generated file should exist");

        String content = Files.readString(generatedFile);
        assertTrue(content.contains("SERVER_HOST"));
        assertTrue(content.contains("SERVER_PORT"));
    }

    @Test
    public void testSetGenerateJavadoc() throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        try (FileWriter writer = new FileWriter(propsFile.toFile())) {
            writer.write("app.name=Test\n");
        }

        ConfigKeyGenerator generator = new ConfigKeyGenerator();
        generator.setPackageName("com.test");
        generator.setEnumName("NoDocConfig");
        generator.setGenerateJavadoc(false);
        generator.generateFromProperties(propsFile.toString(), outputDir.toString());

        Path generatedFile = outputDir.resolve("com/test/NoDocConfig.java");
        String content = Files.readString(generatedFile);
        // When javadoc is disabled, should not contain the class-level javadoc
        assertFalse(content.contains("Auto-generated configuration keys"));
    }

    @Test
    public void testDefaultPackageAndEnumName() throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        try (FileWriter writer = new FileWriter(propsFile.toFile())) {
            writer.write("key=value\n");
        }

        ConfigKeyGenerator generator = new ConfigKeyGenerator();
        // Use defaults
        generator.generateFromProperties(propsFile.toString(), outputDir.toString());

        // Default package is org.confng.generated, default enum is GeneratedConfig
        Path generatedFile = outputDir.resolve("org/confng/generated/GeneratedConfig.java");
        assertTrue(Files.exists(generatedFile), "Generated file should exist with defaults");
    }

    @Test
    public void testGeneratesSensitiveFieldForPasswordKeys() throws IOException {
        Path propsFile = tempDir.resolve("sensitive.properties");
        try (FileWriter writer = new FileWriter(propsFile.toFile())) {
            writer.write("db.password=secret123\n");
            writer.write("api.key=abc123\n");
            writer.write("auth.token=xyz789\n");
            writer.write("app.name=TestApp\n");
        }

        ConfigKeyGenerator generator = new ConfigKeyGenerator();
        generator.setPackageName("com.test.sensitive");
        generator.setEnumName("SensitiveConfig");
        generator.generateFromProperties(propsFile.toString(), outputDir.toString());

        Path generatedFile = outputDir.resolve("com/test/sensitive/SensitiveConfig.java");
        assertTrue(Files.exists(generatedFile), "Generated file should exist");

        String content = Files.readString(generatedFile);
        // Verify isSensitive() method is generated
        assertTrue(content.contains("isSensitive()"), "Should contain isSensitive method");
        // Verify sensitive field is used
        assertTrue(content.contains("private final boolean sensitive"), "Should have sensitive field");
        // Verify password key is marked as sensitive (true)
        assertTrue(content.contains("DB_PASSWORD(\"db.password\"") && content.contains("true)"),
            "Password key should be marked as sensitive");
        // Verify non-sensitive key is marked as not sensitive (false)
        assertTrue(content.contains("APP_NAME(\"app.name\"") && content.contains("false)"),
            "Non-sensitive key should not be marked as sensitive");
    }

    @Test
    public void testGeneratesConfNGKeyInterface() throws IOException {
        Path propsFile = tempDir.resolve("interface.properties");
        try (FileWriter writer = new FileWriter(propsFile.toFile())) {
            writer.write("test.key=value\n");
        }

        ConfigKeyGenerator generator = new ConfigKeyGenerator();
        generator.setPackageName("com.test.iface");
        generator.setEnumName("InterfaceConfig");
        generator.generateFromProperties(propsFile.toString(), outputDir.toString());

        Path generatedFile = outputDir.resolve("com/test/iface/InterfaceConfig.java");
        String content = Files.readString(generatedFile);

        // Verify it implements ConfNGKey
        assertTrue(content.contains("implements ConfNGKey"), "Should implement ConfNGKey");
        // Verify all required methods are present
        assertTrue(content.contains("public String getKey()"), "Should have getKey method");
        assertTrue(content.contains("public String getDefaultValue()"), "Should have getDefaultValue method");
        assertTrue(content.contains("public boolean isSensitive()"), "Should have isSensitive method");
    }
}

