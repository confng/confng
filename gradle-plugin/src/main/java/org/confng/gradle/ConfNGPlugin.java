package org.confng.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

/**
 * Gradle plugin that automatically forwards system properties from the Gradle JVM
 * to the test JVM, enabling seamless configuration with ConfNG.
 *
 * <p>When applied, this plugin automatically configures all Test tasks to inherit
 * system properties passed via {@code -D} flags on the Gradle command line.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * plugins {
 *     id 'org.confng' version '1.0.4'
 * }
 * }</pre>
 *
 * <p>After applying the plugin, run tests with system properties:</p>
 * <pre>{@code
 * ./gradlew test -Dbrowser=firefox -Ddatabase.url=jdbc:mysql://localhost/test
 * }</pre>
 *
 * <p>These properties will be automatically available via {@code ConfNG.get()}.</p>
 *
 * @author Bharat Kumar Malviya
 * @since 1.0.4
 */
public class ConfNGPlugin implements Plugin<Project> {

    /** Default constructor. */
    public ConfNGPlugin() {
    }

    @Override
    public void apply(Project project) {
        project.getExtensions().create("confng", ConfNGExtension.class);

        project.afterEvaluate(p -> {
            ConfNGExtension extension = p.getExtensions().getByType(ConfNGExtension.class);

            p.getTasks().withType(Test.class).configureEach(testTask -> {
                if (extension.isForwardSystemProperties()) {
                    forwardSystemProperties(testTask, extension);
                }
            });
        });
    }

    private void forwardSystemProperties(Test testTask, ConfNGExtension extension) {
        System.getProperties().forEach((key, value) -> {
            String keyStr = key.toString();

            if (shouldForwardProperty(keyStr, extension)) {
                testTask.systemProperty(keyStr, value.toString());
            }
        });
    }

    private boolean shouldForwardProperty(String key, ConfNGExtension extension) {
        // Skip Gradle internal properties
        if (key.startsWith("java.") ||
            key.startsWith("sun.") ||
            key.startsWith("os.") ||
            key.startsWith("user.") ||
            key.startsWith("file.") ||
            key.startsWith("path.") ||
            key.startsWith("line.") ||
            key.startsWith("awt.") ||
            key.startsWith("org.gradle.")) {
            return false;
        }

        // Check exclusion patterns
        for (String pattern : extension.getExcludePatterns()) {
            if (key.startsWith(pattern) || key.matches(pattern)) {
                return false;
            }
        }

        // Check inclusion patterns (if specified, only include matching)
        if (!extension.getIncludePatterns().isEmpty()) {
            for (String pattern : extension.getIncludePatterns()) {
                if (key.startsWith(pattern) || key.matches(pattern)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }
}

