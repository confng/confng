package org.confng.generator;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates ConfNGKey enum classes from YAML, JSON, or Properties schema files.
 *
 * <p>This tool reads configuration schema files and generates type-safe enum
 * classes that implement ConfNGKey interface.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Generate from YAML schema
 * ConfigKeyGenerator generator = new ConfigKeyGenerator();
 * generator.setPackageName("com.myapp.config");
 * generator.setEnumName("AppConfig");
 * generator.generateFromYaml("config-schema.yaml", "src/main/java");
 * 
 * // Generate from existing properties file
 * generator.generateFromProperties("application.properties", "src/main/java");
 * }</pre>
 *
 * <h3>Schema Format (YAML):</h3>
 * <pre>
 * app:
 *   name:
 *     type: String
 *     required: true
 *     description: Application name
 *   port:
 *     type: Integer
 *     default: 8080
 *     description: Server port
 * </pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Slf4j
public class ConfigKeyGenerator {
    
    private String packageName = "org.confng.generated";
    private String enumName = "GeneratedConfig";
    private boolean generateJavadoc = true;
    private boolean generateDefaultValues = true;
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    public void setEnumName(String enumName) {
        this.enumName = enumName;
    }
    
    public void setGenerateJavadoc(boolean generateJavadoc) {
        this.generateJavadoc = generateJavadoc;
    }
    
    public void setGenerateDefaultValues(boolean generateDefaultValues) {
        this.generateDefaultValues = generateDefaultValues;
    }
    
    /**
     * Generates a ConfNGKey enum from a YAML schema file.
     */
    public void generateFromYaml(String schemaPath, String outputDir) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream is = new FileInputStream(schemaPath)) {
            Map<String, Object> schema = yaml.load(is);
            List<ConfigKeyDef> keys = parseSchema(schema, "");
            generateEnum(keys, outputDir);
        }
    }
    
    /**
     * Generates a ConfNGKey enum from a JSON schema file.
     */
    public void generateFromJson(String schemaPath, String outputDir) throws IOException {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(schemaPath)) {
            JsonObject schema = gson.fromJson(reader, JsonObject.class);
            List<ConfigKeyDef> keys = parseJsonSchema(schema, "");
            generateEnum(keys, outputDir);
        }
    }
    
    /**
     * Generates a ConfNGKey enum from an existing properties file.
     */
    public void generateFromProperties(String propertiesPath, String outputDir) throws IOException {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(propertiesPath)) {
            props.load(is);
        }
        
        List<ConfigKeyDef> keys = new ArrayList<>();
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            ConfigKeyDef def = new ConfigKeyDef();
            def.key = key;
            def.enumName = toEnumName(key);
            def.type = inferType(value);
            def.defaultValue = value;
            def.required = false;
            keys.add(def);
        }
        
        keys.sort(Comparator.comparing(k -> k.enumName));
        generateEnum(keys, outputDir);
    }
    
    private List<ConfigKeyDef> parseSchema(Map<String, Object> schema, String prefix) {
        List<ConfigKeyDef> keys = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) value;
                
                // Check if this is a leaf node with type definition
                if (nested.containsKey("type")) {
                    ConfigKeyDef def = new ConfigKeyDef();
                    def.key = key;
                    def.enumName = toEnumName(key);
                    def.type = (String) nested.get("type");
                    def.defaultValue = nested.get("default") != null ? nested.get("default").toString() : null;
                    def.required = Boolean.TRUE.equals(nested.get("required"));
                    def.description = (String) nested.get("description");
                    keys.add(def);
                } else {
                    // Recurse into nested structure
                    keys.addAll(parseSchema(nested, key));
                }
            }
        }
        
        return keys;
    }
    
    private List<ConfigKeyDef> parseJsonSchema(JsonObject schema, String prefix) {
        List<ConfigKeyDef> keys = new ArrayList<>();
        
        for (Map.Entry<String, JsonElement> entry : schema.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonElement value = entry.getValue();
            
            if (value.isJsonObject()) {
                JsonObject nested = value.getAsJsonObject();

                if (nested.has("type")) {
                    ConfigKeyDef def = new ConfigKeyDef();
                    def.key = key;
                    def.enumName = toEnumName(key);
                    def.type = nested.get("type").getAsString();
                    def.defaultValue = nested.has("default") ? nested.get("default").getAsString() : null;
                    def.required = nested.has("required") && nested.get("required").getAsBoolean();
                    def.description = nested.has("description") ? nested.get("description").getAsString() : null;
                    keys.add(def);
                } else {
                    keys.addAll(parseJsonSchema(nested, key));
                }
            }
        }

        return keys;
    }

    private void generateEnum(List<ConfigKeyDef> keys, String outputDir) throws IOException {
        String packagePath = packageName.replace('.', '/');
        Path outputPath = Path.of(outputDir, packagePath, enumName + ".java");
        Files.createDirectories(outputPath.getParent());

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            writer.println("package " + packageName + ";");
            writer.println();
            writer.println("import org.confng.api.ConfNGKey;");
            writer.println();

            if (generateJavadoc) {
                writer.println("/**");
                writer.println(" * Auto-generated configuration keys.");
                writer.println(" * Generated by ConfNG ConfigKeyGenerator.");
                writer.println(" */");
            }

            writer.println("public enum " + enumName + " implements ConfNGKey {");
            writer.println();

            for (int i = 0; i < keys.size(); i++) {
                ConfigKeyDef def = keys.get(i);

                if (generateJavadoc && def.description != null) {
                    writer.println("    /** " + def.description + " */");
                }

                String defaultVal = def.defaultValue != null ? "\"" + escapeString(def.defaultValue) + "\"" : "null";
                String suffix = (i < keys.size() - 1) ? "," : ";";
                boolean isSensitive = isSensitiveKey(def.key);

                writer.println("    " + def.enumName + "(\"" + def.key + "\", " + defaultVal + ", " + isSensitive + ")" + suffix);
            }

            writer.println();
            writer.println("    private final String key;");
            writer.println("    private final String defaultValue;");
            writer.println("    private final boolean sensitive;");
            writer.println();
            writer.println("    " + enumName + "(String key, String defaultValue, boolean sensitive) {");
            writer.println("        this.key = key;");
            writer.println("        this.defaultValue = defaultValue;");
            writer.println("        this.sensitive = sensitive;");
            writer.println("    }");
            writer.println();
            writer.println("    @Override");
            writer.println("    public String getKey() { return key; }");
            writer.println();
            writer.println("    @Override");
            writer.println("    public String getDefaultValue() { return defaultValue; }");
            writer.println();
            writer.println("    @Override");
            writer.println("    public boolean isSensitive() { return sensitive; }");
            writer.println("}");
        }

        log.info("Generated {} with {} keys at {}", enumName, keys.size(), outputPath);
    }

    private String toEnumName(String key) {
        return key.toUpperCase().replace('.', '_').replace('-', '_');
    }

    private String inferType(String value) {
        if (value == null) return "String";
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return "Boolean";
        try { Integer.parseInt(value); return "Integer"; } catch (NumberFormatException ignored) {}
        try { Long.parseLong(value); return "Long"; } catch (NumberFormatException ignored) {}
        try { Double.parseDouble(value); return "Double"; } catch (NumberFormatException ignored) {}
        return "String";
    }

    private String escapeString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean isSensitiveKey(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") || lowerKey.contains("secret") ||
               lowerKey.contains("token") || lowerKey.contains("key") ||
               lowerKey.contains("credential") || lowerKey.contains("auth") ||
               lowerKey.contains("private");
    }

    private static class ConfigKeyDef {
        String key;
        String enumName;
        String type;
        String defaultValue;
        boolean required;
        String description;
    }
}
