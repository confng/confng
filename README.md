# ConfNG

ConfNG is a lightweight configuration management library built for TestNG projects with extensible support for other Java applications. Named after TestNG, it provides zero-configuration integration features for TestNG projects while maintaining compatibility with other Java applications. It simplifies loading and resolving configuration values from multiple sources like environment variables, system properties, properties files, JSON files, YAML files, TOML files, and custom sources.

## Features

### Core Features
- **Automatic TestNG Parameter Injection**: Zero-configuration parameter injection via built-in listener that automatically captures suite, test, and method-level parameters
- **Multiple Configuration Sources**: Environment variables, system properties, properties files, JSON files, YAML files, TOML files, and custom sources
- **Precedence-based Resolution**: Configurable source priority with sensible defaults
- **Type Safety**: Enum-based configuration keys with compile-time checking
- **Auto-discovery**: Automatic scanning for configuration keys using reflection
- **Extensible**: Easy to add custom configuration sources
- **TestNG-First Design**: Built specifically for TestNG projects with automatic parameter injection features, extensible for other Java applications
- **Java 11+ Compatible**: Works with Java 11 and later versions

### Advanced Features (v1.1.1+)
- **🔐 Encryption/Decryption**: AES-256-GCM encryption for sensitive configuration values
- **🔄 Configuration Reloading**: Automatic file watching and hot-reload with change notifications
- **📊 Metrics & Logging**: Track configuration lookups, cache hits, and log configurations with masked sensitive values
- **🌐 Remote Sources**: HTTP, Consul, and Spring Cloud Config Server integration
- **🔧 Code Generation**: Generate ConfNGKey enums from YAML/JSON/Properties schemas
- **📝 Template Generation**: Generate environment-specific configuration templates
- **🍃 Spring Integration**: Use ConfNG as a Spring PropertySource
- **💧 Micronaut Integration**: Use ConfNG as a Micronaut PropertySourceLoader
- **🧪 JUnit 5 Extension**: First-class JUnit 5 support with `@ConfNGTest` annotation

## Installation

Add ConfNG to your Gradle project:

```gradle
dependencies {
    implementation 'org.confng:confng:1.1.1'
}
```

Or Maven:

```xml
<dependency>
    <groupId>org.confng</groupId>
    <artifactId>confng</artifactId>
    <version>1.1.1</version>
</dependency>
```

### Gradle Plugin (Recommended)

For seamless system property forwarding, add the ConfNG Gradle plugin:

```gradle
plugins {
    id 'org.confng' version '1.1.1'
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

## Advanced Features

### Encryption/Decryption

ConfNG supports AES-256-GCM encryption for sensitive configuration values:

```java
import org.confng.encryption.EncryptionManager;
import org.confng.encryption.AesEncryptionProvider;

// Create a 32-byte key for AES-256
byte[] key = new byte[32];
new SecureRandom().nextBytes(key);
String base64Key = Base64.getEncoder().encodeToString(key);

// Register the encryption provider
AesEncryptionProvider provider = new AesEncryptionProvider(key);
EncryptionManager.getInstance().registerProvider("default", provider);

// Encrypt a value
String encrypted = provider.encrypt("my-secret-password");
// Result: "ENC(base64-encoded-ciphertext)"

// Store encrypted value in config file
// db.password=ENC(base64-encoded-ciphertext)

// ConfNG automatically decrypts values with ENC() prefix
String password = ConfNG.get(MyConfig.DB_PASSWORD); // Returns decrypted value
```

### Configuration Reloading

Enable automatic configuration reloading when files change:

```java
import org.confng.reload.ConfigReloadManager;
import org.confng.reload.ConfigChangeListener;

// Load configuration files
ConfNG.load("config.properties");
ConfNG.load("config.yaml");

// Add a change listener
ConfNG.addChangeListener(event -> {
    System.out.println("Changed keys: " + event.getChangedKeys());

    if (event.hasChanged("database.pool.size")) {
        String oldValue = event.getOldValue("database.pool.size");
        String newValue = event.getNewValue("database.pool.size");
        reconfigureConnectionPool(Integer.parseInt(newValue));
    }
});

// Enable auto-reload (watches files for changes)
ConfNG.enableAutoReload();

// Or with custom debounce time (milliseconds)
ConfNG.enableAutoReload(2000);

// Manually trigger a reload
ConfigReloadManager.getInstance().triggerReload("manual-refresh");

// Disable auto-reload when done
ConfNG.disableAutoReload();
```

### Metrics & Logging

Track configuration usage and log configurations at startup:

```java
import org.confng.metrics.ConfigMetrics;
import org.confng.metrics.ConfigLogger;

// Enable metrics tracking
ConfigMetrics metrics = ConfigMetrics.getInstance();
metrics.setEnabled(true);

// Use configuration normally - metrics are tracked automatically
String value = ConfNG.get(MyConfig.DATABASE_URL);

// Get metrics
long totalLookups = metrics.getTotalLookups();
long cacheHits = metrics.getCacheHits();
double hitRate = metrics.getCacheHitRate();

// Get per-key statistics
long keyLookups = metrics.getLookupCount("database.url");

// Export metrics as JSON
String metricsJson = metrics.toJson();

// Log all configuration at startup with masked sensitive values
ConfigLogger.logConfiguration(MyConfig.class);

// Or with custom settings
ConfigLogger.builder()
    .addSensitivePattern("api.key")
    .addSensitivePattern(".*secret.*")
    .showDefaults(true)
    .logLevel("info")
    .logConfiguration(MyConfig.class);
```

### Remote Configuration Sources

#### HTTP Source

```java
import org.confng.sources.HttpSource;

// Load configuration from HTTP endpoint
HttpSource httpSource = new HttpSource("https://config-server.example.com/api/config");
httpSource.setHeader("Authorization", "Bearer " + token);
httpSource.setRefreshInterval(Duration.ofMinutes(5));
ConfNG.registerSource(httpSource);
```

#### Consul Source

```java
import org.confng.sources.ConsulSource;

// Load configuration from Consul KV store
ConsulSource consulSource = new ConsulSource("http://consul.example.com:8500");
consulSource.setPrefix("myapp/config/");
consulSource.setToken(consulToken);
ConfNG.registerSource(consulSource);
```

#### Spring Cloud Config Source

```java
import org.confng.sources.SpringCloudConfigSource;

// Load configuration from Spring Cloud Config Server
SpringCloudConfigSource configSource = new SpringCloudConfigSource(
    "http://config-server.example.com:8888",
    "myapp",      // application name
    "production"  // profile
);
ConfNG.registerSource(configSource);
```

### Code Generation

Generate ConfNGKey enums from schema files:

```java
import org.confng.generator.ConfigKeyGenerator;

ConfigKeyGenerator generator = new ConfigKeyGenerator();
generator.setPackageName("com.myapp.config");
generator.setEnumName("AppConfig");
generator.setGenerateJavadoc(true);

// Generate from YAML schema
generator.generateFromYaml("config-schema.yaml", "src/main/java");

// Generate from JSON schema
generator.generateFromJson("config-schema.json", "src/main/java");

// Generate from existing properties file
generator.generateFromProperties("application.properties", "src/main/java");
```

**YAML Schema Format:**
```yaml
database:
  host:
    type: String
    default: localhost
    description: Database host address
    required: true
  port:
    type: Integer
    default: 5432
    description: Database port
  password:
    type: String
    description: Database password (sensitive)
    required: true
```

### Template Generation

Generate environment-specific configuration templates:

```java
import org.confng.generator.TemplateGenerator;

TemplateGenerator generator = new TemplateGenerator();
generator.setEnvironments(Arrays.asList("dev", "staging", "production"));
generator.setFormat("yaml");  // or "properties", "json"
generator.setIncludeComments(true);
generator.setMaskSensitiveDefaults(true);

// Generate templates for all environments
generator.generateTemplates(MyConfig.class, "config/templates");

// Generate for specific environment
generator.generateTemplate(MyConfig.class, "production", "config/app-prod.yaml");
```

### Spring Integration

Use ConfNG as a Spring PropertySource:

```java
import org.confng.integration.spring.ConfNGPropertySource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
public class ConfNGConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

        MutablePropertySources sources = new MutablePropertySources();
        sources.addFirst(new ConfNGPropertySource("confng", MyConfig.class));
        configurer.setPropertySources(sources);

        return configurer;
    }
}

// Then use @Value annotations as normal
@Component
public class MyService {
    @Value("${app.name}")
    private String appName;

    @Value("${database.url}")
    private String databaseUrl;
}
```

### Micronaut Integration

Use ConfNG as a Micronaut PropertySourceLoader:

```java
import org.confng.integration.micronaut.ConfNGPropertySourceLoader;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class ConfNGFactory {

    @Singleton
    public PropertySourceLoader confngLoader() {
        return new ConfNGPropertySourceLoader(MyConfig.class);
    }
}

// Then use @Value or @Property annotations
@Singleton
public class MyService {
    @Value("${app.name}")
    private String appName;
}
```

### JUnit 5 Extension

Use ConfNG with JUnit 5 tests:

```java
import org.confng.junit5.ConfNGExtension;
import org.confng.junit5.ConfNGTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ConfNGExtension.class)
@ConfNGTest(
    properties = "test.properties",
    yaml = "test-config.yaml"
)
public class MyJUnit5Test {

    @Test
    void testWithConfiguration() {
        String value = ConfNG.get(TestConfig.API_URL);
        assertNotNull(value);
    }
}
```

Add the JUnit 5 extension dependency:

```gradle
dependencies {
    testImplementation 'org.confng:junit5-extension:1.1.1'
}
```

## Build Tools

### Gradle Plugin

The ConfNG Gradle plugin automatically forwards system properties to test JVMs:

```gradle
plugins {
    id 'org.confng' version '1.1.1'
}

// All -D flags are automatically forwarded to tests
// ./gradlew test -Dbrowser=firefox -Denv=staging
```

### Maven Plugin

The ConfNG Maven plugin provides configuration validation and code generation:

```xml
<plugin>
    <groupId>org.confng</groupId>
    <artifactId>confng-maven-plugin</artifactId>
    <version>1.1.1</version>
    <executions>
        <execution>
            <goals>
                <goal>validate</goal>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <schemaFile>src/main/resources/config-schema.yaml</schemaFile>
        <packageName>com.myapp.config</packageName>
        <enumName>AppConfig</enumName>
    </configuration>
</plugin>
```

## Roadmap

- [x] ~~Encrypted configuration values~~ ✅ Added in v1.1.1
- [x] ~~Configuration change notifications~~ ✅ Added in v1.1.1
- [x] ~~Spring Boot integration~~ ✅ Added in v1.1.1
- [ ] Kubernetes ConfigMap/Secret integration
- [ ] AWS Secrets Manager integration
- [ ] HashiCorp Vault integration
- [ ] Configuration validation annotations

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
