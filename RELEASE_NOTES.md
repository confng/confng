# ConfNG 1.1.1 Release Notes

**Release Date:** February 2026

We're excited to announce ConfNG 1.1.1, a release introducing the **Gradle Plugin** and powerful new configuration management features!

---

## 🎉 Highlights

- **New Gradle Plugin** - Zero-configuration system property forwarding
- **Enhanced Type Support** - New typed getters for Long, Double, List, and Duration
- **Explicit Null Handling** - Optional/Required methods for fail-fast configuration
- **Validation Framework** - Annotation-based configuration validation
- **Source Diagnostics** - Debug where configuration values come from
- **Prefix Retrieval** - Query configuration by prefix

---

## 🔌 New: Gradle Plugin

The new `org.confng` Gradle plugin automatically forwards all system properties (`-D` flags) from the Gradle command line to the test JVM—no more manual `systemProperty` configuration!

### Installation

```groovy
plugins {
    id 'java'
    id 'org.confng' version '1.1.1'
}
```

### Usage

```bash
# Properties are automatically forwarded to tests
./gradlew test -Dbrowser=firefox -Dbase.url=https://staging.example.com
```

```java
// Access in your tests - just works!
String browser = ConfNG.get(AppConfig.BROWSER);  // "firefox"
```

### Configuration Options

```groovy
confng {
    forwardSystemProperties = true           // Enable/disable (default: true)
    includePatterns = ['app.', 'database.']  // Only forward matching prefixes
    excludePatterns = ['secret.', 'password'] // Never forward sensitive props
}
```

---

## 📊 New: Typed Configuration Getters

New type-safe getter methods for common data types:

```java
// Long values
Long maxRecords = ConfNG.getLong(AppConfig.MAX_RECORDS);

// Double values
Double taxRate = ConfNG.getDouble(AppConfig.TAX_RATE);

// List values (comma-separated by default)
List<String> origins = ConfNG.getList(AppConfig.ALLOWED_ORIGINS);
// "http://localhost,http://example.com" → ["http://localhost", "http://example.com"]

// List with custom delimiter
List<String> features = ConfNG.getList(AppConfig.FEATURES, ";");

// Duration values (human-readable formats)
Duration timeout = ConfNG.getDuration(AppConfig.SESSION_TIMEOUT);
// Supports: "30s", "5m", "2h", "1d", "500ms", "PT30S" (ISO-8601)
```

---

## ✅ New: Optional/Required Configuration

Explicit methods for handling missing configuration values:

```java
// Optional - returns Optional<String>, never null
Optional<String> apiKey = ConfNG.getOptional(AppConfig.API_KEY);
apiKey.ifPresent(key -> client.setApiKey(key));

// Required - throws ConfigurationException if missing
String databaseUrl = ConfNG.getRequired(DatabaseConfig.URL);

// With explicit fallback default
String browser = ConfNG.getOrDefault(AppConfig.BROWSER, "firefox");
Integer timeout = ConfNG.getOrDefault(AppConfig.TIMEOUT, 60);
```

---

## 🔍 New: Validation Framework

Annotation-based validation for configuration integrity:

```java
public enum AppConfig implements ConfNGKey {
    @Required
    DATABASE_URL("database.url"),

    @NotEmpty
    API_KEY("api.key", "default-key"),

    @Range(min = 1, max = 65535)
    SERVER_PORT("server.port", "8080"),

    @Pattern(regex = "^(debug|info|warn|error)$")
    LOG_LEVEL("log.level", "info");

    // ... implementation
}

// Validate all configuration
ValidationResult result = ConfNG.validate(AppConfig.values());
if (!result.isValid()) {
    for (ValidationError error : result.getErrors()) {
        System.err.println(error.getKey() + ": " + error.getMessage());
    }
}
```

---

## 🔬 New: Source Diagnostics

Debug exactly where each configuration value comes from:

```java
ConfigSourceInfo info = ConfNG.getSourceInfo(AppConfig.DATABASE_URL);

System.out.println("Key: " + info.getKey());           // "database.url"
System.out.println("Value: " + info.getValue());       // "jdbc:mysql://..."
System.out.println("Source: " + info.getSourceName()); // "Environment", "SystemProperties", etc.
System.out.println("Found: " + info.isFound());        // true/false
System.out.println("Default: " + info.isFromDefault()); // true if using default

// Get source info for all keys
Map<String, ConfigSourceInfo> allInfo = ConfNG.getAllSourceInfo(AppConfig.values());
```

---

## 🏷️ New: Prefix-based Retrieval

Retrieve all configuration values with a common prefix:

```java
// Get all values with prefix
Map<String, String> dbConfig = ConfNG.getByPrefix("db.");
// Returns: {"db.host": "localhost", "db.port": "5432", "db.name": "mydb"}

// Get just the key names
Set<String> apiKeys = ConfNG.getKeysWithPrefix("api.");
// Returns: {"api.url", "api.key", "api.version"}

// Use case: feature flags
Map<String, String> features = ConfNG.getByPrefix("feature.");
for (Map.Entry<String, String> entry : features.entrySet()) {
    String featureName = entry.getKey().substring("feature.".length());
    boolean enabled = Boolean.parseBoolean(entry.getValue());
    featureManager.setEnabled(featureName, enabled);
}
```

---

## 📦 Installation

### Gradle

```groovy
dependencies {
    implementation 'org.confng:confng:1.1.1'
}

// Optional: Add the plugin for automatic property forwarding
plugins {
    id 'org.confng' version '1.1.1'
}
```

### Maven

```xml
<dependency>
    <groupId>org.confng</groupId>
    <artifactId>confng</artifactId>
    <version>1.1.1</version>
</dependency>
```

---

## 🔄 Migration from 1.0.x

This release is **fully backward compatible**. Simply update your version number:

```groovy
// Before
implementation 'org.confng:confng:1.0.3'

// After
implementation 'org.confng:confng:1.1.1'
```

No code changes required. All existing APIs continue to work as expected.

---

## 📚 Documentation

- **Website:** [https://confng.org](https://confng.org)
- **API Docs:** [https://docs.confng.org](https://docs.confng.org)
- **GitHub:** [https://github.com/confng/confng](https://github.com/confng/confng)
- **Examples:** [https://github.com/confng/confng-playground](https://github.com/confng/confng-playground)

---

## 🙏 Contributors

Thank you to everyone who contributed to this release!

---

## 📝 Full Changelog

See the [GitHub Releases](https://github.com/confng/confng/releases/tag/v1.1.1) page for the complete list of changes.

