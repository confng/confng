package org.confng.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional tests for {@link ConfNGPlugin}.
 */
class ConfNGPluginFunctionalTest {

    @TempDir
    Path projectDir;

    private File buildFile;
    private File settingsFile;

    @BeforeEach
    void setUp() throws IOException {
        buildFile = projectDir.resolve("build.gradle").toFile();
        settingsFile = projectDir.resolve("settings.gradle").toFile();
        Files.writeString(settingsFile.toPath(), "rootProject.name = 'test-project'");
    }

    @Test
    void pluginAppliesSuccessfully() throws IOException {
        String buildContent =
            "plugins {\n" +
            "    id 'java'\n" +
            "    id 'org.confng'\n" +
            "}\n" +
            "\n" +
            "repositories {\n" +
            "    mavenCentral()\n" +
            "}\n";

        Files.writeString(buildFile.toPath(), buildContent);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("tasks", "--all")
                .build();

        assertTrue(result.getOutput().contains("test"));
    }

    @Test
    void pluginCreatesConfngExtension() throws IOException {
        String buildContent =
            "plugins {\n" +
            "    id 'java'\n" +
            "    id 'org.confng'\n" +
            "}\n" +
            "\n" +
            "repositories {\n" +
            "    mavenCentral()\n" +
            "}\n" +
            "\n" +
            "task printExtension {\n" +
            "    doLast {\n" +
            "        println \"forwardSystemProperties: ${confng.forwardSystemProperties}\"\n" +
            "        println \"includePatterns: ${confng.includePatterns}\"\n" +
            "        println \"excludePatterns: ${confng.excludePatterns}\"\n" +
            "    }\n" +
            "}\n";

        Files.writeString(buildFile.toPath(), buildContent);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("printExtension")
                .build();

        assertTrue(result.getOutput().contains("forwardSystemProperties: true"));
        assertTrue(result.getOutput().contains("includePatterns: []"));
        assertTrue(result.getOutput().contains("excludePatterns: []"));
    }

    @Test
    void extensionCanBeConfigured() throws IOException {
        String buildContent =
            "plugins {\n" +
            "    id 'java'\n" +
            "    id 'org.confng'\n" +
            "}\n" +
            "\n" +
            "repositories {\n" +
            "    mavenCentral()\n" +
            "}\n" +
            "\n" +
            "confng {\n" +
            "    forwardSystemProperties = false\n" +
            "    includePatterns = ['app.', 'database.']\n" +
            "    excludePatterns = ['secret.']\n" +
            "}\n" +
            "\n" +
            "task printExtension {\n" +
            "    doLast {\n" +
            "        println \"forwardSystemProperties: ${confng.forwardSystemProperties}\"\n" +
            "        println \"includePatterns: ${confng.includePatterns}\"\n" +
            "        println \"excludePatterns: ${confng.excludePatterns}\"\n" +
            "    }\n" +
            "}\n";

        Files.writeString(buildFile.toPath(), buildContent);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("printExtension")
                .build();

        assertTrue(result.getOutput().contains("forwardSystemProperties: false"));
        assertTrue(result.getOutput().contains("includePatterns: [app., database.]"));
        assertTrue(result.getOutput().contains("excludePatterns: [secret.]"));
    }

    @Test
    void pluginConfiguresTestTask() throws IOException {
        String buildContent =
            "plugins {\n" +
            "    id 'java'\n" +
            "    id 'org.confng'\n" +
            "}\n" +
            "\n" +
            "repositories {\n" +
            "    mavenCentral()\n" +
            "}\n" +
            "\n" +
            "dependencies {\n" +
            "    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'\n" +
            "}\n" +
            "\n" +
            "test {\n" +
            "    useJUnitPlatform()\n" +
            "}\n";

        Files.writeString(buildFile.toPath(), buildContent);

        // Create a simple test class
        Path testDir = projectDir.resolve("src/test/java/com/example");
        Files.createDirectories(testDir);

        String testContent =
            "package com.example;\n" +
            "\n" +
            "import org.junit.jupiter.api.Test;\n" +
            "import static org.junit.jupiter.api.Assertions.*;\n" +
            "\n" +
            "class SimpleTest {\n" +
            "    @Test\n" +
            "    void testSystemProperty() {\n" +
            "        assertNotNull(System.getProperty(\"java.version\"));\n" +
            "    }\n" +
            "}\n";

        Files.writeString(testDir.resolve("SimpleTest.java"), testContent);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("test", "--info")
                .build();

        assertEquals(SUCCESS, result.task(":test").getOutcome());
    }
}

