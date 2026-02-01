package org.confng.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Maven Mojo that validates configuration files.
 *
 * <p>This mojo validates that configuration files contain all required keys
 * and that values match expected formats.</p>
 *
 * <h3>Example Usage in pom.xml:</h3>
 * <pre>{@code
 * <plugin>
 *     <groupId>org.confng</groupId>
 *     <artifactId>confng-maven-plugin</artifactId>
 *     <version>1.1.1</version>
 *     <executions>
 *         <execution>
 *             <goals>
 *                 <goal>validate</goal>
 *             </goals>
 *         </execution>
 *     </executions>
 *     <configuration>
 *         <configFiles>
 *             <configFile>src/main/resources/application.properties</configFile>
 *         </configFiles>
 *         <requiredKeys>
 *             <key>app.name</key>
 *             <key>app.version</key>
 *         </requiredKeys>
 *     </configuration>
 * </plugin>
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidateConfigMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * List of configuration files to validate.
     */
    @Parameter(property = "confng.configFiles", required = true)
    private List<String> configFiles;

    /**
     * List of required configuration keys.
     */
    @Parameter(property = "confng.requiredKeys")
    private List<String> requiredKeys;

    /**
     * Whether to fail the build on validation errors.
     */
    @Parameter(property = "confng.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Whether to check for empty values.
     */
    @Parameter(property = "confng.checkEmpty", defaultValue = "true")
    private boolean checkEmpty;

    /**
     * Skip validation.
     */
    @Parameter(property = "confng.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("ConfNG: Validation skipped");
            return;
        }

        getLog().info("ConfNG: Validating configuration files...");

        List<String> errors = new ArrayList<>();
        Set<String> allKeys = new HashSet<>();

        for (String configFile : configFiles) {
            File file = new File(project.getBasedir(), configFile);
            
            if (!file.exists()) {
                errors.add("Configuration file not found: " + configFile);
                continue;
            }

            try {
                Properties props = loadProperties(file);
                allKeys.addAll(props.stringPropertyNames());

                // Check for empty values
                if (checkEmpty) {
                    for (String key : props.stringPropertyNames()) {
                        String value = props.getProperty(key);
                        if (value == null || value.trim().isEmpty()) {
                            errors.add("Empty value for key '" + key + "' in " + configFile);
                        }
                    }
                }

                getLog().info("  Validated: " + configFile + " (" + props.size() + " properties)");
            } catch (IOException e) {
                errors.add("Failed to read " + configFile + ": " + e.getMessage());
            }
        }

        // Check required keys
        if (requiredKeys != null) {
            for (String requiredKey : requiredKeys) {
                if (!allKeys.contains(requiredKey)) {
                    errors.add("Required key missing: " + requiredKey);
                }
            }
        }

        // Report results
        if (!errors.isEmpty()) {
            getLog().error("ConfNG: Validation failed with " + errors.size() + " error(s):");
            for (String error : errors) {
                getLog().error("  - " + error);
            }

            if (failOnError) {
                throw new MojoFailureException("Configuration validation failed");
            }
        } else {
            getLog().info("ConfNG: Validation passed");
        }
    }

    private Properties loadProperties(File file) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            if (file.getName().endsWith(".xml")) {
                props.loadFromXML(fis);
            } else {
                props.load(fis);
            }
        }
        return props;
    }
}

