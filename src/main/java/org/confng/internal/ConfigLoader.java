package org.confng.internal;

import org.confng.sources.*;
import org.confng.util.FileResolver;

import java.util.function.Consumer;

/**
 * Internal helper class responsible for loading configuration files.
 * This class handles the detection and loading of various configuration file formats.
 *
 * <p>This is an internal class and should not be used directly by clients.
 * Use {@link org.confng.ConfNG} instead.</p>
 *
 * @author Bharat Kumar Malviya
 * @since 1.0
 */
public final class ConfigLoader {

    private ConfigLoader() {
        // Utility class - prevent instantiation
    }

    /**
     * Loads a configuration file by auto-detecting its type based on the file extension.
     *
     * @param filePath path to the configuration file
     * @param sourceRegistrar callback to register the loaded source
     */
    public static void load(String filePath, Consumer<ConfigSource> sourceRegistrar) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }

        FileResolver.ResolvedFile resolved = FileResolver.resolve(filePath);
        if (resolved == null || !resolved.exists()) {
            return;
        }

        String fileName = resolved.getFileName().toLowerCase();
        loadByExtension(filePath, fileName, null, sourceRegistrar);
    }

    /**
     * Loads a configuration file with environment-specific section.
     *
     * @param filePath path to the configuration file
     * @param environment the environment section to load
     * @param sourceRegistrar callback to register the loaded source
     */
    public static void load(String filePath, String environment, Consumer<ConfigSource> sourceRegistrar) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }

        FileResolver.ResolvedFile resolved = FileResolver.resolve(filePath);
        if (resolved == null || !resolved.exists()) {
            return;
        }

        String fileName = resolved.getFileName().toLowerCase();
        loadByExtension(filePath, fileName, environment, sourceRegistrar);
    }

    private static void loadByExtension(String filePath, String fileName, String environment,
                                         Consumer<ConfigSource> sourceRegistrar) {
        if (fileName.endsWith(".properties")) {
            if (environment != null) {
                throw new IllegalArgumentException("Properties files don't support environment sections. " +
                    "Use load(filePath) instead or use separate files per environment.");
            }
            loadProperties(filePath, sourceRegistrar);
        } else if (fileName.endsWith(".json")) {
            loadJson(filePath, environment, sourceRegistrar);
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            loadYaml(filePath, environment, sourceRegistrar);
        } else if (fileName.endsWith(".toml")) {
            loadToml(filePath, environment, sourceRegistrar);
        } else {
            String supportedTypes = environment != null
                ? ".json, .yaml, .yml, .toml"
                : ".properties, .json, .yaml, .yml, .toml";
            throw new IllegalArgumentException("Unsupported file type: " + fileName +
                ". Supported types" + (environment != null ? " for environment sections" : "") + ": " + supportedTypes);
        }
    }

    /**
     * Loads a properties file.
     */
    public static void loadProperties(String filePath, Consumer<ConfigSource> sourceRegistrar) {
        if (filePath == null || filePath.trim().isEmpty() || !FileResolver.exists(filePath)) {
            return;
        }
        try {
            sourceRegistrar.accept(new PropertiesSource(filePath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load properties file: " + filePath, e);
        }
    }

    /**
     * Loads a JSON file.
     */
    public static void loadJson(String filePath, String environment, Consumer<ConfigSource> sourceRegistrar) {
        if (filePath == null || filePath.trim().isEmpty() || !FileResolver.exists(filePath)) {
            return;
        }
        try {
            ConfigSource source = environment != null
                ? new JsonSource(filePath, environment)
                : new JsonSource(filePath);
            sourceRegistrar.accept(source);
        } catch (Exception e) {
            String msg = environment != null
                ? "Failed to load JSON file: " + filePath + " for environment: " + environment
                : "Failed to load JSON file: " + filePath;
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Loads a YAML file.
     */
    public static void loadYaml(String filePath, String environment, Consumer<ConfigSource> sourceRegistrar) {
        if (filePath == null || filePath.trim().isEmpty() || !FileResolver.exists(filePath)) {
            return;
        }
        try {
            ConfigSource source = environment != null
                ? new YamlSource(filePath, environment)
                : new YamlSource(filePath);
            sourceRegistrar.accept(source);
        } catch (Exception e) {
            String msg = environment != null
                ? "Failed to load YAML file: " + filePath + " for environment: " + environment
                : "Failed to load YAML file: " + filePath;
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Loads a TOML file.
     */
    public static void loadToml(String filePath, String environment, Consumer<ConfigSource> sourceRegistrar) {
        if (filePath == null || filePath.trim().isEmpty() || !FileResolver.exists(filePath)) {
            return;
        }
        try {
            ConfigSource source = environment != null
                ? new TomlSource(filePath, environment)
                : new TomlSource(filePath);
            sourceRegistrar.accept(source);
        } catch (Exception e) {
            String msg = environment != null
                ? "Failed to load TOML file: " + filePath + " for environment: " + environment
                : "Failed to load TOML file: " + filePath;
            throw new RuntimeException(msg, e);
        }
    }
}

