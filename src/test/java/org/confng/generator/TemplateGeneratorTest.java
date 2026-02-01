package org.confng.generator;

import org.confng.api.ConfNGKey;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import static org.testng.Assert.*;

/**
 * Tests for TemplateGenerator.
 */
public class TemplateGeneratorTest {

    private Path outputDir;

    // Test enum for template generation
    public enum TestConfig implements ConfNGKey {
        APP_NAME("app.name", "TestApp", false),
        APP_PORT("app.port", "8080", false),
        DB_PASSWORD("db.password", null, true),
        API_KEY("api.key", null, true);

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
    public void setUp() throws IOException {
        outputDir = Files.createTempDirectory("confng-template-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (Files.exists(outputDir)) {
            Files.walk(outputDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    public void testGeneratePropertiesTemplate() throws IOException {
        TemplateGenerator generator = new TemplateGenerator();
        generator.setFormat("properties");
        generator.generateTemplate(TestConfig.class, "dev", outputDir.resolve("app-dev.properties").toString());

        Path generatedFile = outputDir.resolve("app-dev.properties");
        assertTrue(Files.exists(generatedFile), "Template file should exist");

        String content = Files.readString(generatedFile);
        assertTrue(content.contains("app.name="));
        assertTrue(content.contains("app.port="));
        assertTrue(content.contains("db.password="));
    }

    @Test
    public void testGenerateYamlTemplate() throws IOException {
        TemplateGenerator generator = new TemplateGenerator();
        generator.setFormat("yaml");
        generator.generateTemplate(TestConfig.class, "prod", outputDir.resolve("app-prod.yaml").toString());

        Path generatedFile = outputDir.resolve("app-prod.yaml");
        assertTrue(Files.exists(generatedFile), "Template file should exist");

        String content = Files.readString(generatedFile);
        assertTrue(content.contains("app:"));
        assertTrue(content.contains("name:"));
        assertTrue(content.contains("port:"));
    }

    @Test
    public void testGenerateJsonTemplate() throws IOException {
        TemplateGenerator generator = new TemplateGenerator();
        generator.setFormat("json");
        generator.generateTemplate(TestConfig.class, "staging", outputDir.resolve("app-staging.json").toString());

        Path generatedFile = outputDir.resolve("app-staging.json");
        assertTrue(Files.exists(generatedFile), "Template file should exist");

        String content = Files.readString(generatedFile);
        assertTrue(content.contains("{"));
        assertTrue(content.contains("}"));
        assertTrue(content.contains("\"app.name\""));
    }

    @Test
    public void testGenerateTemplatesForAllEnvironments() throws IOException {
        TemplateGenerator generator = new TemplateGenerator();
        generator.setEnvironments(Arrays.asList("dev", "staging", "prod"));
        generator.setFormat("properties");
        generator.generateTemplates(TestConfig.class, outputDir.toString());

        assertTrue(Files.exists(outputDir.resolve("application-dev.properties")));
        assertTrue(Files.exists(outputDir.resolve("application-staging.properties")));
        assertTrue(Files.exists(outputDir.resolve("application-prod.properties")));
    }

    @Test
    public void testMaskSensitiveDefaults() throws IOException {
        TemplateGenerator generator = new TemplateGenerator();
        generator.setFormat("properties");
        generator.setMaskSensitiveDefaults(true);
        generator.generateTemplate(TestConfig.class, "dev", outputDir.resolve("app.properties").toString());

        String content = Files.readString(outputDir.resolve("app.properties"));
        // Password and key should be masked with placeholders
        assertTrue(content.contains("db.password=${DB_PASSWORD}") || content.contains("${DB_PASSWORD}"));
        assertTrue(content.contains("api.key=${API_KEY}") || content.contains("${API_KEY}"));
    }

    @Test
    public void testIncludeDefaults() throws IOException {
        TemplateGenerator generator = new TemplateGenerator();
        generator.setFormat("properties");
        generator.setIncludeDefaults(true);
        generator.setMaskSensitiveDefaults(false);
        generator.generateTemplate(TestConfig.class, "dev", outputDir.resolve("app.properties").toString());

        String content = Files.readString(outputDir.resolve("app.properties"));
        assertTrue(content.contains("app.name=TestApp"));
        assertTrue(content.contains("app.port=8080"));
    }

    @Test
    public void testIncludeComments() throws IOException {
        TemplateGenerator generator = new TemplateGenerator();
        generator.setFormat("properties");
        generator.setIncludeComments(true);
        generator.generateTemplate(TestConfig.class, "dev", outputDir.resolve("app.properties").toString());

        String content = Files.readString(outputDir.resolve("app.properties"));
        assertTrue(content.contains("#"), "Should contain comments");
    }

    @Test
    public void testDisableComments() throws IOException {
        TemplateGenerator generator = new TemplateGenerator();
        generator.setFormat("properties");
        generator.setIncludeComments(false);
        generator.generateTemplate(TestConfig.class, "dev", outputDir.resolve("app.properties").toString());

        String content = Files.readString(outputDir.resolve("app.properties"));
        // Should still have header comments but no section comments
        long commentLines = content.lines().filter(l -> l.startsWith("# ") && !l.contains("template") && !l.contains("Generated")).count();
        assertEquals(commentLines, 0, "Should have no section comments when disabled");
    }
}

