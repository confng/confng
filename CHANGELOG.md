# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project setup and documentation

### Changed

### Deprecated

### Removed

### Fixed

### Security

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

[Unreleased]: https://github.com/confng/confng/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/confng/confng/releases/tag/v1.0.0