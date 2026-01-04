# ConfNG

ConfNG is a lightweight configuration management library built for TestNG projects with extensible support for other Java applications. Named after TestNG, it provides zero-configuration integration features for TestNG projects while maintaining compatibility with other Java applications. It simplifies loading and resolving configuration values from multiple sources like environment variables, system properties, properties files, JSON files, YAML files, TOML files, and custom sources.

## Features

- **Automatic TestNG Parameter Injection**: Zero-configuration parameter injection via built-in listener that automatically captures suite, test, and method-level parameters
- **Multiple Configuration Sources**: Environment variables, system properties, properties files, JSON files, YAML files, TOML files, and custom sources
- **Precedence-based Resolution**: Configurable source priority with sensible defaults
- **Type Safety**: Enum-based configuration keys with compile-time checking
- **Auto-discovery**: Automatic scanning for configuration keys using reflection
- **Extensible**: Easy to add custom configuration sources
- **TestNG-First Design**: Built specifically for TestNG projects with automatic parameter injection features, extensible for other Java applications
- **Java 11+ Compatible**: Works with Java 11 and later versions

## Installation

Add ConfNG to your Gradle project:

```gradle
dependencies {
    implementation 'org.confng:confng:1.1.0'
}
```

Or Maven:

```xml
<dependency>
    <groupId>org.confng</groupId>
    <artifactId>confng</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle Plugin (Recommended)

For seamless system property forwarding, add the ConfNG Gradle plugin:

```gradle
plugins {
    id 'org.confng' version '1.1.0'
}
```

This automatically forwards all `-D` flags to the test JVM:

```bash
./gradlew test -Dbrowser=firefox -Ddatabase.url=jdbc:mysql://localhost/test
```

No additional configuration needed - properties are automatically available via `ConfNG.get()`.

## Quick Start

### 1. Define Configuration Keys

Create an enum that implements `ConfNGKey`:

```java
import org.confng.api.ConfNGKey;

public enum TestConfig implements ConfNGKey {
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

// TestNG parameters are automatically injected via built-in listener!
// No manual configuration required for testng.xml parameters

// Load additional configuration files (optional - skipped if file doesn't exist)
ConfNG.loadProperties("test.properties");
ConfNG.loadJson("config.json");
ConfNG.loadYaml("config.yaml");
ConfNG.loadToml("config.toml");

// Add custom source
ConfNG.registerSource(new CustomConfigSource());
```

### 3. Use Configuration in TestNG Tests

```java
// Primary use case: TestNG test projects

@Test
public void testWithConfiguration() {
    String browser = ConfNG.get(TestConfig.BROWSER);
    Integer timeout = ConfNG.getInt(TestConfig.TIMEOUT);
    Boolean headless = ConfNG.getBoolean(TestConfig.HEADLESS);
    
    // Use configuration values in your test
    WebDriver driver = createDriver(browser, headless);
    driver.manage().timeouts().implicitlyWait(timeout, TimeUnit.SECONDS);
}

// Also works in other Java applications
public class ConfigService {
    public void loadConfig() {
        String dbUrl = ConfNG.get(AppConfig.DATABASE_URL);
        String apiKey = ConfNG.get(AppConfig.API_KEY);
        // Your application logic...
    }
}
```

## Configuration Sources

ConfNG supports multiple configuration sources with the following default precedence (highest to lowest):

1. **Environment Variables** - `EnvSource`
2. **System Properties** - `SystemPropertySource`
3. **TestNG Parameters (Method Level)** - `TestNGParameterSource` (automatically injected)
4. **TestNG Parameters (Test Level)** - `TestNGParameterSource` (automatically injected)
5. **TestNG Parameters (Suite Level)** - `TestNGParameterSource` (automatically injected)
6. **Properties Files** - `PropertiesSource`
7. **JSON Files** - `JsonSource`
8. **YAML Files** - `YamlSource`
9. **TOML Files** - `TomlSource`
10. **Secret Managers** - `SecretManagerSource` (base class for custom implementations)
11. **Custom Sources** - User-defined implementations

### TestNG Integration Features

ConfNG is built for TestNG projects and automatically activates integration features when TestNG is detected on the classpath via a built-in listener that loads through TestNG's service loader mechanism. No manual configuration is required!

```xml
<!-- testng.xml -->
<suite name="Test-Suite">
    <parameter name="browser" value="chrome"/>
    <parameter name="base.url" value="https://staging.example.com"/>
    <test name="Smoke-Tests">
        <parameter name="browser" value="firefox"/> <!-- Overrides suite level -->
        <parameter name="timeout" value="45"/>
        <classes><class name="com.example.SmokeTest"/></classes>
    </test>
</suite>
```

```java
// TestNG test class - automatic parameter injection!
public class SmokeTest {
    @Test
    public void testAutomaticParameterInjection() {
        // ✨ TestNG feature: parameters automatically injected!
        String browser = ConfNG.get(TestConfig.BROWSER);     // "firefox" (test level)
        String baseUrl = ConfNG.get(TestConfig.BASE_URL);    // "https://staging.example.com" (suite level)
        Integer timeout = ConfNG.getInt(TestConfig.TIMEOUT); // 45 (test level)
        
        // TestNG precedence: Method > Test > Suite > Other sources
    }
}

// Extensible: works in other Java applications too
public class DatabaseService {
    public void connect() {
        // Same ConfNG API, no TestNG features (since TestNG not present)
        ConfNG.loadProperties("app.properties");
        String dbUrl = ConfNG.get(AppConfig.DATABASE_URL);
        // Your application logic...
    }
}
```

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

### YAML Files

```yaml
# config.yaml
browser: chrome
base.url: https://api.example.com
timeout: 30

# Environment-specific sections
uat:
  database:
    host: uat-db.example.com
  api:
    url: https://uat-api.example.com

prod:
  database:
    host: prod-db.example.com
  api:
    url: https://prod-api.example.com
```

```java
// Load YAML file (all top-level keys)
ConfNG.loadYaml("config.yaml");

// Load specific environment section from YAML file
ConfNG.loadYaml("config.yaml", "uat");    // Loads only the "uat" section
ConfNG.loadYaml("config.yaml", "prod");   // Loads only the "prod" section
```

### TOML Files

```toml
# config.toml
browser = "chrome"
base.url = "https://api.example.com"
timeout = 30

[features]
parallel = true
headless = false

# Environment-specific sections
[uat]
database.host = "uat-db.example.com"
api.url = "https://uat-api.example.com"

[prod]
database.host = "prod-db.example.com"
api.url = "https://prod-api.example.com"
```

```java
// Load TOML file (all top-level keys)
ConfNG.loadToml("config.toml");

// Load specific environment section from TOML file
ConfNG.loadToml("config.toml", "uat");    // Loads only the "uat" section
ConfNG.loadToml("config.toml", "qa");     // Loads only the "qa" section
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

- Java 11 or later
- TestNG (optional - only needed for special TestNG integration features)

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
