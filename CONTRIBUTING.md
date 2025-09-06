# Contributing to ConfNG

Thank you for your interest in contributing to ConfNG! We welcome contributions from the community and are pleased to have you join us.

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct. Please be respectful and constructive in all interactions.

## How to Contribute

### Reporting Issues

Before creating an issue, please:
- Check if the issue already exists in our [issue tracker](https://github.com/confng/confng/issues)
- Use the latest version of ConfNG to verify the issue still exists
- Provide a clear and descriptive title
- Include steps to reproduce the issue
- Add relevant code samples, error messages, and environment details

### Suggesting Features

We welcome feature suggestions! Please:
- Check existing issues and discussions first
- Clearly describe the use case and benefits
- Consider if the feature fits ConfNG's scope and philosophy
- Be open to feedback and discussion

### Pull Requests

1. **Fork the repository** and create your branch from `main`
2. **Set up your development environment**:
   ```bash
   git clone https://github.com/[your-username]/confng.git
   cd confng
   ./gradlew build
   ```

3. **Make your changes**:
   - Follow our coding standards (see below)
   - Add tests for new functionality
   - Update documentation as needed
   - Ensure all tests pass: `./gradlew test`

4. **Commit your changes**:
   - Use clear, descriptive commit messages
   - Follow conventional commit format: `type(scope): description`
   - Examples: `feat: add YAML configuration source`, `fix: handle null values in JSON source`

5. **Submit your pull request**:
   - Provide a clear description of the changes
   - Reference any related issues
   - Include screenshots for UI changes (if applicable)

## Development Setup

### Prerequisites

- Java 11 or later
- Git

### Building the Project

```bash
# Clone the repository
git clone https://github.com/confng/confng.git
cd confng

# Build the project
./gradlew build

# Run tests
./gradlew test

# Generate documentation
./gradlew javadoc
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "org.confng.ConfNGTest"

# Run tests with verbose output
./gradlew test --info
```

## Coding Standards

### Java Code Style

- Use 4 spaces for indentation (no tabs)
- Line length should not exceed 120 characters
- Use meaningful variable and method names
- Follow Java naming conventions (camelCase for methods/variables, PascalCase for classes)
- Add Javadoc comments for public APIs
- Use `@Override` annotation when overriding methods

### Code Organization

- Keep classes focused and cohesive
- Prefer composition over inheritance
- Use interfaces to define contracts
- Follow SOLID principles
- Minimize dependencies between packages

### Testing

- Write unit tests for all new functionality
- Use TestNG for test framework
- Aim for high test coverage (>80%)
- Use descriptive test method names
- Follow AAA pattern (Arrange, Act, Assert)

Example test:
```java
@Test
public void shouldReturnEnvironmentVariableWhenSet() {
    // Arrange
    String key = "TEST_KEY";
    String expectedValue = "test_value";
    
    // Act
    String actualValue = ConfNG.get(TestConfig.TEST_KEY);
    
    // Assert
    assertEquals(actualValue, expectedValue);
}
```

## Documentation

- Update README.md for user-facing changes
- Add Javadoc comments for public APIs
- Include code examples in documentation
- Update CHANGELOG.md for all changes

## Release Process

Releases are handled by maintainers. The process includes:

1. Update version in `build.gradle`
2. Update `CHANGELOG.md`
3. Create release tag
4. Publish to Maven Central
5. Update GitHub release notes

## Getting Help

- Join our discussions on GitHub
- Check existing issues and documentation
- Reach out to maintainers for guidance

## Recognition

Contributors will be recognized in:
- CHANGELOG.md for their contributions
- GitHub contributors list
- Release notes for significant contributions

Thank you for contributing to ConfNG!