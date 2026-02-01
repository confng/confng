package org.confng.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Properties;

/**
 * Maven Mojo that forwards system properties to the test JVM.
 *
 * <p>This mojo captures system properties matching specified prefixes and makes them
 * available to tests. This is useful for passing configuration from CI/CD pipelines
 * or command line to test execution.</p>
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
 *                 <goal>forward-properties</goal>
 *             </goals>
 *         </execution>
 *     </executions>
 *     <configuration>
 *         <prefixes>
 *             <prefix>app.</prefix>
 *             <prefix>test.</prefix>
 *         </prefixes>
 *     </configuration>
 * </plugin>
 * }</pre>
 *
 * <h3>Command Line Usage:</h3>
 * <pre>
 * mvn test -Dapp.environment=staging -Dtest.browser=chrome
 * </pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Mojo(name = "forward-properties", defaultPhase = LifecyclePhase.INITIALIZE)
public class ForwardPropertiesMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * List of property prefixes to forward.
     * Properties starting with these prefixes will be forwarded to the test JVM.
     */
    @Parameter(property = "confng.prefixes")
    private List<String> prefixes;

    /**
     * Whether to forward all system properties (not recommended for security).
     */
    @Parameter(property = "confng.forwardAll", defaultValue = "false")
    private boolean forwardAll;

    /**
     * List of specific property names to forward.
     */
    @Parameter(property = "confng.properties")
    private List<String> properties;

    /**
     * Whether to log forwarded properties.
     */
    @Parameter(property = "confng.verbose", defaultValue = "false")
    private boolean verbose;

    @Override
    public void execute() throws MojoExecutionException {
        Properties systemProps = System.getProperties();
        Properties projectProps = project.getProperties();
        int forwardedCount = 0;

        getLog().info("ConfNG: Forwarding system properties to project...");

        for (String key : systemProps.stringPropertyNames()) {
            if (shouldForward(key)) {
                String value = systemProps.getProperty(key);
                projectProps.setProperty(key, value);
                forwardedCount++;
                
                if (verbose) {
                    // Mask sensitive values
                    String displayValue = isSensitive(key) ? "****" : value;
                    getLog().info("  Forwarded: " + key + "=" + displayValue);
                }
            }
        }

        getLog().info("ConfNG: Forwarded " + forwardedCount + " properties");
    }

    private boolean shouldForward(String key) {
        // Skip standard Java/Maven properties
        if (key.startsWith("java.") || key.startsWith("sun.") || 
            key.startsWith("os.") || key.startsWith("user.") ||
            key.startsWith("maven.") || key.startsWith("file.") ||
            key.startsWith("line.") || key.startsWith("path.")) {
            return false;
        }

        if (forwardAll) {
            return true;
        }

        // Check specific properties
        if (properties != null && properties.contains(key)) {
            return true;
        }

        // Check prefixes
        if (prefixes != null) {
            for (String prefix : prefixes) {
                if (key.startsWith(prefix)) {
                    return true;
                }
            }
        }

        // Default prefixes if none specified
        if ((prefixes == null || prefixes.isEmpty()) && (properties == null || properties.isEmpty())) {
            return key.startsWith("app.") || key.startsWith("test.") || 
                   key.startsWith("config.") || key.startsWith("confng.");
        }

        return false;
    }

    private boolean isSensitive(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") || lowerKey.contains("secret") ||
               lowerKey.contains("token") || lowerKey.contains("key") ||
               lowerKey.contains("credential") || lowerKey.contains("auth");
    }
}

