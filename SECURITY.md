# Security Policy

## Supported Versions

We actively support the following versions of ConfNG with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security vulnerability in ConfNG, please report it responsibly.

### How to Report

**Please do NOT report security vulnerabilities through public GitHub issues.**

Instead, please report security vulnerabilities by emailing us at:
**security@confng.org**

Include the following information in your report:
- Description of the vulnerability
- Steps to reproduce the issue
- Potential impact and severity
- Any suggested fixes or mitigations
- Your contact information for follow-up

### What to Expect

1. **Acknowledgment**: We will acknowledge receipt of your vulnerability report within 48 hours.

2. **Initial Assessment**: We will provide an initial assessment of the vulnerability within 5 business days, including:
   - Confirmation of the vulnerability
   - Severity assessment
   - Estimated timeline for resolution

3. **Resolution**: We will work to resolve the vulnerability as quickly as possible:
   - **Critical/High**: Within 7 days
   - **Medium**: Within 30 days  
   - **Low**: Within 90 days

4. **Disclosure**: After the vulnerability is fixed:
   - We will release a security update
   - We will publish a security advisory
   - We will credit you for the discovery (unless you prefer to remain anonymous)

### Security Best Practices

When using ConfNG, follow these security best practices:

#### Sensitive Configuration Data

- Use ConfNG's built-in sensitive data masking for secrets
- Mark configuration keys as sensitive when they contain passwords, API keys, or other secrets
- Never log sensitive configuration values in plain text
- Use secure secret management systems for production environments

```java
public enum MyConfig implements ConfigKey {
    API_KEY("api.key", null, true),        // Mark as sensitive
    DATABASE_PASSWORD("db.password", null, true), // Mark as sensitive
    PUBLIC_URL("public.url", null, false); // Not sensitive
    
    // ... implementation
}

// Safe logging - sensitive values are masked
logger.info("Config: {}", ConfNG.getForDisplay(MyConfig.API_KEY));
```

#### Secret Manager Integration

- Implement proper authentication for secret manager sources
- Use least-privilege access for secret retrieval
- Implement proper error handling to avoid information leakage
- Regularly rotate secrets and API keys

#### File-based Configuration

- Protect configuration files with appropriate file permissions
- Avoid storing sensitive data in properties/JSON files in production
- Use encrypted configuration files when necessary
- Validate file paths to prevent directory traversal attacks

#### Environment Variables

- Be cautious with environment variables in containerized environments
- Use secrets management in orchestration platforms (Kubernetes secrets, Docker secrets)
- Avoid logging environment variables that may contain sensitive data

### Vulnerability Categories

We are particularly interested in reports about:

- **Injection vulnerabilities** in configuration parsing
- **Path traversal** vulnerabilities in file loading
- **Information disclosure** through error messages or logging
- **Deserialization** vulnerabilities in JSON/properties parsing
- **Authentication bypass** in secret manager integrations
- **Privilege escalation** through configuration manipulation

### Scope

This security policy applies to:
- The core ConfNG library
- Official configuration source implementations
- Documentation and examples that could lead to security issues

Out of scope:
- Third-party integrations not maintained by the ConfNG team
- Issues in dependencies (please report to the respective projects)
- Social engineering attacks

### Recognition

We appreciate security researchers who help keep ConfNG secure. We will:
- Acknowledge your contribution in our security advisories
- List you in our hall of fame (with your permission)
- Provide ConfNG swag for significant discoveries

Thank you for helping keep ConfNG and our users safe!