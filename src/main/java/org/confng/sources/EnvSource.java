package org.confng.sources;

import java.util.Map;
import java.util.Optional;

/**
 * Configuration source that reads from environment variables.
 * 
 * <p>This source provides access to system environment variables with
 * optional custom environment map for testing purposes.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 * @see org.confng.sources.ConfigSource
 */
public class EnvSource implements ConfigSource {

    private final Map<String, String> environment;

    /**
     * Creates a new EnvSource using System.getenv().
     */
    public EnvSource() {
        this(System.getenv());
    }

    /**
     * Creates a new EnvSource with custom environment (for testing).
     * 
     * @param environment the environment variables to use
     */
    public EnvSource(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public String getName() {
        return "Environment";
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(environment.get(key));
    }

    @Override
    public int getPriority() {
        return 60; // High priority for environment variables
    }
}
