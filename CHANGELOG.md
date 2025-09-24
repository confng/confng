# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

## [1.0.1] - 2025-01-24

### Added
- **Automatic TestNG Parameter Injection**: Zero-configuration parameter injection via built-in `TestNGParameterListener`
- **Service Loader Integration**: TestNG listener automatically loads via TestNG's service loader mechanism
- **Thread-Safe Parameter Handling**: Thread-local storage for concurrent test execution
- **Multi-Level Parameter Support**: Automatic capture of suite, test, and method-level parameters
- **Proper Parameter Precedence**: Method > Test > Suite parameter precedence automatically enforced
- **Lifecycle Management**: Automatic cleanup of parameters after test completion
- **TestNGParameterSource**: New configuration source for TestNG parameters with priority 80

### Changed
- **TestNG-First Design**: Clarified that ConfNG is built for TestNG projects with extensible support for other Java applications
- **TestNG Integration**: Enhanced from basic integration to automatic parameter injection features
- **Java Version Requirement**: Updated minimum Java version from 8 to 11
- **Source Precedence**: TestNG parameters now have high precedence (priority 80) when TestNG is detected
- **Documentation**: Updated all documentation to reflect TestNG-first design with extensible support
- **Messaging**: Emphasized that ConfNG is built for TestNG but can be used in other Java applications

### Technical Details
- Added `TestNGParameterListener` class implementing multiple TestNG listener interfaces
- Added `TestNGParameterSource` class for managing TestNG parameters with thread-local context
- Enhanced `ConfNG` class to handle TestNG parameter source with special priority handling
- Added service loader configuration for automatic listener registration

## [1.0.0] - 2025-01-09

### Added
- Initial release of ConfNG
- Core configuration management functionality
- Support for multiple configuration sources:
  - Environment variables (`EnvSource`)
  - System properties (`SystemPropertySource`)
  - Properties files (`PropertiesSource`)
  - JSON files (`JsonSource`)
  - Secret managers (`SecretManagerSource`)
- Type-safe configuration keys using enums
- Configurable source precedence
- Auto-discovery of configuration keys using reflection
- Sensitive data masking for secure logging
- TestNG integration
- Comprehensive test suite
- Maven Central publishing support
- Complete documentation and examples

### Features
- **Multiple Configuration Sources**: Load from env vars, system props, files, and custom sources
- **Type Safety**: Enum-based keys with compile-time checking
- **Precedence Control**: Configurable source priority
- **Auto-discovery**: Automatic scanning for configuration keys
- **Extensible**: Easy custom source implementation
- **Secure**: Built-in sensitive data masking
- **Java 8+ Compatible**: Works with Java 8 and later

### Dependencies
- `com.google.code.gson:gson:2.11.0` - JSON parsing
- `org.reflections:reflections:0.10.2` - Reflection utilities
- `org.testng:testng:7.11.0` - Testing framework (test scope)

[Unreleased]: https://github.com/confng/confng/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/confng/confng/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/confng/confng/releases/tag/v1.0.0