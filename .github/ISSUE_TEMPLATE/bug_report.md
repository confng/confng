---
name: Bug report
about: Create a report to help us improve ConfNG
title: '[BUG] '
labels: 'bug'
assignees: ''
---

## Bug Description

A clear and concise description of what the bug is.

## Steps to Reproduce

1. Go to '...'
2. Configure '...'
3. Call method '...'
4. See error

## Expected Behavior

A clear and concise description of what you expected to happen.

## Actual Behavior

A clear and concise description of what actually happened.

## Code Sample

```java
// Minimal code sample that reproduces the issue
public enum TestConfig implements ConfigKey {
    MY_KEY("my.key");
    // ...
}

// Usage that causes the bug
String value = ConfNG.get(TestConfig.MY_KEY);
```

## Configuration

**ConfNG Version:** [e.g. 1.0.0]

**Java Version:** [e.g. Java 11, Java 17]

**Operating System:** [e.g. Windows 10, macOS 12, Ubuntu 20.04]

**Build Tool:** [e.g. Gradle 7.6, Maven 3.8.6]

## Configuration Sources

Which configuration sources are you using?
- [ ] Environment Variables
- [ ] System Properties  
- [ ] Properties Files
- [ ] JSON Files
- [ ] Secret Manager
- [ ] Custom Sources

## Error Messages

```
Paste any error messages, stack traces, or logs here
```

## Additional Context

Add any other context about the problem here, such as:
- Configuration file contents (remove sensitive data)
- Environment setup details
- Related issues or discussions

## Possible Solution

If you have ideas about what might be causing the issue or how to fix it, please share them here.