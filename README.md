# ConfNG

ConfNG is a lightweight configuration management library designed for Java projects using TestNG. It simplifies loading and resolving configuration values from multiple sources like environment variables, system properties, properties files, JSON files, and custom sources.

## Features

- **Multiple Configuration Sources**: Environment variables, system properties, properties files, JSON files, and custom sources
- **Precedence-based Resolution**: Configurable source priority with sensible defaults
- **Type Safety**: Enum-based configuration keys with compile-time checking
- **Auto-discovery**: Automatic scanning for configuration keys using reflection
- **Extensible**: Easy to add custom configuration sources
- **TestNG Integration**: Designed specifically for TestNG test frameworks
- **Java 8+ Compatible**: Works with Java 8 and later versions

## Installation

Add ConfNG to your Gradle project:

```gradle
dependencies {
    implementation 'org.confng:confng:1.0.0'
}
```

Or Maven:

```xml
<dependency>
    <groupId>org.confng</groupId>
    <artifactId>confng</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Define Configuration Keys

Create an enum that implements `ConfigKey`:

```java
import org.confng.api.ConfigKey;

public enum TestConfig implements ConfigKey {
    BROWSER("browser"),
    BASE_URL("base.url"),
    TIMEOUT("timeout"),
    HEADLESS("headless");
    
    private final String key;
    
    TestConfig(String key) {
        this.key = key;
    }
    
    @Override
    public String getKey() {
        return key;
    }
}
```

### 2. Load Configuration Sources

```java
import org.confng.ConfNG;

// Load from properties file (optional - skipped if file doesn't exist)
ConfNG.loadProperties("test.properties");

// Load from JSON file (optional - skipped if file doesn't exist)  
ConfNG.loadJson("config.json");

// Add custom source
ConfNG.registerSource(new CustomConfigSource());
```

### 3. Use Configuration in Tests

```java
@Test
public void testWithConfiguration() {
    String browser = ConfNG.get(TestConfig.BROWSER);
    Integer timeout = ConfNG.getInt(TestConfig.TIMEOUT);
    Boolean headless = ConfNG.getBoolean(TestConfig.HEADLESS);
    
    // Use configuration values in your test
    WebDriver driver = createDriver(browser, headless);
    driver.manage().timeouts().implicitlyWait(timeout, TimeUnit.SECONDS);
}
```

## Configuration Sources

ConfNG supports multiple configuration sources with the following default precedence (highest to lowest):

1. **Environment Variables** - `EnvSource`
2. **System Properties** - `SystemPropertySource`  
3. **Properties Files** - `PropertiesSource`
4. **JSON Files** - `JsonSource`
5. **Secret Managers** - `SecretManagerSource` (base class for custom implementations)
6. **Custom Sources** - User-defined implementations

### Environment Variables

```bash
export browser=chrome
export timeout=30
```

### System Properties

```bash
java -Dbrowser=firefox -Dtimeout=45 MyTest
```

### Properties Files

```properties
# test.properties
browser=edge
base.url=https://staging.example.com
timeout=60
```

### JSON Files

```json
{
  "browser": "safari",
  "base.url": "https://prod.example.com",
  "timeout": 90
}
```

### Secret Managers

ConfNG provides a base class for implementing secret management integrations:

#### Custom Secret Manager

```java
// Extend SecretManagerSource for your secret management system
public class CustomSecretSource extends SecretManagerSource {
    @Override
    public String getName() {
        return "CustomSecretManager";
    }
    
    @Override
    protected String fetchSecret(String secretId) throws Exception {
        // Implement your secret fetching logic
        return secretClient.getSecret(secretId);
    }
}

// Register and configure
CustomSecretSource secretSource = new CustomSecretSource();
secretSource.addKeyMapping("db.password", "prod/database/password");
secretSource.addKeyMapping("api.key", "prod/api/key");
ConfNG.registerSecretManager(secretSource);
```

## Advanced Usage

### Custom Configuration Sources

Implement the `ConfigSource` interface:

```java
import org.confng.sources.ConfigSource;

public class CustomConfigSource implements ConfigSource {
    
    @Override
    public String getName() {
        return "CustomSource";
    }
    
    @Override
    public Optional<String> get(String key) {
        // Implement your custom lookup logic
        return customClient.getValue(key);
    }
}

// Register the custom source
ConfNG.registerSource(new CustomConfigSource());
```

### Source Precedence Management

```java
// Clear all sources and use defaults
ConfNG.clearSourcesAndUseDefaults();

// Add source at specific precedence (0 = highest priority)
ConfNG.registerSourceAt(0, new CustomHighPrioritySource());

// Add source at end (lowest priority)
ConfNG.registerSource(new CustomLowPrioritySource());
```

### Configuration Discovery

```java
// Discover all ConfigKey enums in specific packages
List<ConfigKey> keys = ConfNG.discoverAllConfigKeys("com.mycompany.config");

// Discover all ConfigKey enums in entire classpath
List<ConfigKey> allKeys = ConfNG.discoverAllConfigKeys();
```

### Sensitive Configuration Handling

```java
public enum MyConfig implements ConfigKey {
    API_KEY("api.key", null, true),        // Sensitive
    DATABASE_URL("db.url", "localhost", false); // Not sensitive
    
    // ... constructor and methods
}

// Get actual value
String apiKey = ConfNG.get(MyConfig.API_KEY);

// Get masked value for logging
String maskedKey = ConfNG.getForDisplay(MyConfig.API_KEY); // Returns "***MASKED***"

// Display all configurations with proper masking
String allConfigs = ConfNG.getAllForDisplay(MyConfig.values());
```

## API Reference

### Core Methods

- `ConfNG.get(ConfigKey key)` - Get string value or null if not found
- `ConfNG.getInt(ConfigKey key)` - Get integer value or null if not found
- `ConfNG.getBoolean(ConfigKey key)` - Get boolean value or null if not found

### Source Management

- `ConfNG.loadProperties(String path)` - Load properties file
- `ConfNG.loadJson(String path)` - Load JSON file  
- `ConfNG.loadSecretSource(ConfigSource source)` - Add custom source
- `ConfNG.registerSecretManager(SecretManagerSource source)` - Add secret manager source
- `ConfNG.registerSource(ConfigSource source)` - Add source at end
- `ConfNG.registerSourceAt(int index, ConfigSource source)` - Add source at specific position
- `ConfNG.clearSourcesAndUseDefaults()` - Reset to default sources

### Discovery

- `ConfNG.discoverAllConfigKeys(String... packages)` - Find all ConfigKey implementations

### Sensitive Data Handling

- `ConfNG.getForDisplay(ConfigKey key)` - Get value with masking for sensitive keys
- `ConfNG.getAllForDisplay(ConfigKey... keys)` - Display all configurations with masking

## Error Handling

- **Missing Files**: Properties and JSON files are silently skipped if they don't exist
- **Invalid Files**: Runtime exceptions are thrown for files that exist but cannot be parsed
- **Type Conversion**: `IllegalArgumentException` is thrown for invalid type conversions
- **Missing Values**: Methods return `null` when configuration values are not found

## Requirements

- Java 8 or later
- TestNG (for test integration)

## Roadmap

- [ ] Support for nested JSON configuration paths (e.g., `database.host`)
- [ ] Configuration validation and schema support
- [ ] Encrypted configuration values
- [ ] Configuration change notifications
- [ ] Spring Boot integration
- [ ] Kubernetes ConfigMap/Secret integration

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
