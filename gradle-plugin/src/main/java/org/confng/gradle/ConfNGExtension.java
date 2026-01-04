package org.confng.gradle;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension for configuring the ConfNG Gradle plugin.
 *
 * <p>This extension allows customization of which system properties are forwarded
 * to the test JVM.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * confng {
 *     // Disable automatic forwarding (default: true)
 *     forwardSystemProperties = false
 *
 *     // Only forward properties matching these patterns
 *     includePatterns = ['app.', 'database.', 'browser']
 *
 *     // Exclude properties matching these patterns
 *     excludePatterns = ['secret.', 'password']
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @since 1.1.0
 */
public class ConfNGExtension {

    /** Default constructor. */
    public ConfNGExtension() {
    }

    private boolean forwardSystemProperties = true;
    private List<String> includePatterns = new ArrayList<>();
    private List<String> excludePatterns = new ArrayList<>();

    /**
     * Whether to automatically forward system properties to test JVM.
     * Default is {@code true}.
     *
     * @return true if system properties should be forwarded
     */
    public boolean isForwardSystemProperties() {
        return forwardSystemProperties;
    }

    /**
     * Sets whether to automatically forward system properties to test JVM.
     *
     * @param forwardSystemProperties true to enable forwarding
     */
    public void setForwardSystemProperties(boolean forwardSystemProperties) {
        this.forwardSystemProperties = forwardSystemProperties;
    }

    /**
     * Gets the list of patterns for properties to include.
     * If empty, all properties (except excluded) are forwarded.
     *
     * @return list of include patterns
     */
    public List<String> getIncludePatterns() {
        return includePatterns;
    }

    /**
     * Sets the list of patterns for properties to include.
     *
     * @param includePatterns list of patterns (prefix or regex)
     */
    public void setIncludePatterns(List<String> includePatterns) {
        this.includePatterns = includePatterns != null ? includePatterns : new ArrayList<>();
    }

    /**
     * Gets the list of patterns for properties to exclude.
     *
     * @return list of exclude patterns
     */
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    /**
     * Sets the list of patterns for properties to exclude.
     *
     * @param excludePatterns list of patterns (prefix or regex)
     */
    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns != null ? excludePatterns : new ArrayList<>();
    }
}

