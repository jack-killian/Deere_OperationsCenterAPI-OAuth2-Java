# Java 17 to Java 21 LTS Upgrade Plan

This document provides a comprehensive plan for migrating the Deere Operations Center API OAuth2 Java application from Java 17 to Java 21 LTS, including MVP test coverage recommendations.

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Pre-Upgrade Steps](#pre-upgrade-steps)
3. [Configuration Changes](#configuration-changes)
4. [Dependency Verification](#dependency-verification)
5. [MVP Test Coverage Plan](#mvp-test-coverage-plan)
6. [Rollback Plan](#rollback-plan)
7. [Post-Upgrade Validation](#post-upgrade-validation)

---

## Executive Summary

This upgrade plan covers the migration from Java 17 to Java 21 LTS for the OAuth2 example application. Java 21 is a Long-Term Support (LTS) release that brings significant improvements including virtual threads, pattern matching for switch, record patterns, and sequenced collections. The current application dependencies have been verified for Java 21 compatibility, and this document outlines the step-by-step process to complete the upgrade safely.

---

## Pre-Upgrade Steps

### 1. Environment Preparation

Before starting the upgrade, ensure the following prerequisites are met:

**Install Java 21 JDK**: Download and install a Java 21 JDK distribution. Recommended distributions include Eclipse Temurin (Adoptium), Amazon Corretto, or Azul Zulu. Verify the installation by running `java -version` and confirming the output shows version 21.

**Update Maven**: Ensure Maven 3.6.3 or later is installed, as this is required by the maven-shade-plugin 3.6.0. Run `mvn -version` to verify.

**Backup Current State**: Create a backup branch or tag of the current working state before making any changes. This provides a quick rollback point if needed.

### 2. Codebase Review

Review the codebase for potential Java 21 compatibility issues:

**Internal API Usage**: Check for any usage of internal JDK APIs (sun.*, jdk.internal.*). Java 21 enforces stronger encapsulation of these APIs. The current codebase does not appear to use any internal APIs directly.

**Deprecated API Usage**: Review for deprecated APIs that may have been removed in Java 21. The current application uses standard APIs that remain available.

**Reflection Usage**: Identify any reflection-based access to private fields or methods. The application uses standard reflection patterns through Guava and Unirest that are compatible with Java 21.

### 3. Dependency Audit

Document all current dependencies and their versions:

| Dependency | Current Version | Java 21 Compatible |
|------------|-----------------|-------------------|
| Javalin | 6.3.0 | Yes |
| SLF4J API | 2.0.13 | Yes |
| SLF4J Simple | 2.0.13 | Yes |
| Guava | 33.2.1-jre | Yes |
| Unirest Java Core | 4.4.0 | Yes |
| Unirest Modules Gson | 4.4.0 | Yes |
| Mustache Compiler | 0.9.14 | Yes |
| maven-shade-plugin | 3.6.0 | Yes |

---

## Configuration Changes

### 1. Update pom.xml

The primary configuration change is updating the maven-compiler-plugin source and target versions from 17 to 21.

**Current Configuration (lines 20-23):**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>17</source>
        <target>17</target>
    </configuration>
</plugin>
```

**Updated Configuration:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>21</source>
        <target>21</target>
    </configuration>
</plugin>
```

### 2. Update .java-version File

Update the `.java-version` file in the project root from `17` to `21`. This file is used by version managers like jenv and sdkman to automatically switch Java versions.

### 3. Update GitHub Actions Workflow

Update `.github/workflows/maven.yml` to use Java 21:

**Current Configuration (lines 12-16):**
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    distribution: 'zulu'
    java-version: 17.0.5+8
```

**Updated Configuration:**
```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    distribution: 'zulu'
    java-version: '21'
```

---

## Dependency Verification

### Verification Process

For each dependency, follow this verification process:

**Step 1: Check Release Notes** - Review the dependency's release notes and changelog for Java 21 compatibility statements.

**Step 2: Check Issue Tracker** - Search the dependency's issue tracker for any reported Java 21 compatibility issues.

**Step 3: Compile Test** - After updating the Java version, run `mvn compile` to verify the dependency compiles without errors.

**Step 4: Runtime Test** - Run the application and exercise all functionality to verify runtime compatibility.

### Dependency-Specific Verification

**Javalin 6.3.0**: Javalin 6.x is built with Java 11 as the minimum requirement and has been tested with Java 21. The framework uses Jetty 12 internally, which fully supports Java 21. Verification steps include starting the web server, testing all HTTP routes, and verifying static file serving.

**SLF4J 2.0.13**: SLF4J 2.x is fully compatible with Java 21. The logging facade uses standard Java APIs and has no known compatibility issues. Verify by checking that log output appears correctly during application startup and operation.

**Guava 33.2.1-jre**: The `-jre` suffix indicates this is the JRE flavor (not Android). Guava 33.x supports Java 8 through Java 21. The application uses `ImmutableMap`, `Strings`, and `Throwables` from Guava, all of which are stable APIs. Verify by testing the OAuth flow which exercises these utilities.

**Unirest 4.4.0**: Unirest 4.x requires Java 11+ and is compatible with Java 21. The library uses the Java HTTP client internally. Verify by testing all HTTP operations including the OAuth token exchange, API calls, and the well-known endpoint fetch.

**Mustache Compiler 0.9.14**: The Mustache Java library is compatible with Java 21. It uses standard Java reflection and I/O APIs. Verify by loading and rendering the main.mustache and error.mustache templates.

**maven-shade-plugin 3.6.0**: Version 3.5.1 specifically added improvements for Java 21 compatibility (MSHADE-459). Version 3.6.0 includes these improvements. Verify by running `mvn package` and confirming the shaded JAR is created successfully.

### Optional Dependency Updates

While not required for Java 21 compatibility, consider updating to the latest patch versions for security and bug fixes:

| Dependency | Current | Latest Available | Recommendation |
|------------|---------|------------------|----------------|
| Javalin | 6.3.0 | 6.7.0 | Optional update |
| SLF4J | 2.0.13 | 2.0.17 | Optional update |
| Guava | 33.2.1-jre | 33.4.x-jre | Optional update |
| Unirest | 4.4.0 | 4.7.0 | Optional update |

---

## MVP Test Coverage Plan

### Test Categories

The MVP test coverage plan is organized into five categories that cover the critical functionality of the application.

### 1. OAuth2 Flow Tests

These tests verify the core OAuth2 authentication functionality.

**startOIDC() Method Tests (Application.java lines 62-67)**:
- Test that settings are populated from form parameters
- Test that the redirect URL is correctly constructed with all required OAuth parameters (client_id, response_type, scope, redirect_uri, state)
- Test that the authorization endpoint is fetched from the well-known metadata

**processCallback() Method Tests (Application.java lines 69-103)**:
- Test successful token exchange with valid authorization code
- Test error handling when callback contains an error parameter
- Test that tokens (access_token, refresh_token, id_token) are correctly stored in Settings
- Test organization access check after successful token exchange
- Test exception handling during token exchange

**refreshAccessToken() Method Tests (Application.java lines 134-155)**:
- Test successful token refresh with valid refresh token
- Test that new tokens are stored after refresh
- Test error handling when refresh fails

**callTheApi() Method Tests (Application.java lines 187-198)**:
- Test successful API call with valid access token
- Test that API response is stored in settings
- Test error handling for failed API calls

**getLocationFromMeta() Method Tests (Application.java lines 119-124)**:
- Test that metadata is fetched from well-known endpoint
- Test that metadata is cached after first fetch
- Test extraction of authorization_endpoint and token_endpoint

### 2. API Integration Tests

These tests verify the HTTP client functionality and API interactions.

**Api.java Tests**:
- Test GET request with Bearer token authentication
- Test correct Accept header (application/vnd.deere.axiom.v3+json)
- Test JSON response parsing

**Settings.java Tests**:
- Test populate() method correctly extracts form parameters
- Test getBasicAuthHeader() returns correctly encoded credentials
- Test updateTokenInfo() correctly parses token response
- Test getAccessTokenDetails() correctly decodes JWT payload
- Test getExpiration() correctly calculates token expiration time

### 3. Build and Runtime Tests

These tests verify the build process and application startup.

**Maven Compilation Tests**:
- Run `mvn clean compile` and verify no compilation errors
- Verify all source files compile with Java 21 source/target

**Fat JAR Creation Tests**:
- Run `mvn package` and verify the shaded JAR is created
- Verify the JAR manifest contains the correct Main-Class entry
- Verify the JAR size is reasonable (all dependencies included)

**JAR Execution Tests**:
- Run `java -jar target/oauth2-example-1.0.jar` and verify startup
- Verify the application binds to port 9090
- Verify the startup log message appears

### 4. Web Server Tests

These tests verify the Javalin web server functionality.

**Server Startup Tests**:
- Test that Javalin starts on port 9090
- Test that static files are served from the assets directory
- Test that the Mustache template renderer is configured

**HTTP Route Tests**:
- Test GET `/` returns the main page with settings form
- Test POST `/` initiates OAuth flow (redirects to authorization endpoint)
- Test GET `/callback` processes the OAuth callback
- Test GET `/refresh-access-token` refreshes the access token
- Test POST `/call-api` makes an authenticated API call

**Mustache Template Rendering Tests**:
- Test main.mustache renders with empty settings
- Test main.mustache renders with populated access token
- Test error.mustache renders with error message
- Test that template variables are correctly substituted

### 5. Dependency Compatibility Tests

These tests verify that all dependencies work correctly with Java 21.

**Unirest Tests**:
- Test HTTP GET requests
- Test HTTP POST requests with form data
- Test JSON response parsing
- Test custom headers (Authorization, Accept)

**Guava Tests**:
- Test ImmutableMap.of() creates immutable maps
- Test Strings.isNullOrEmpty() null checking
- Test Strings.nullToEmpty() null conversion
- Test Throwables.getStackTraceAsString() exception formatting

**SLF4J Tests**:
- Test logger creation with LoggerFactory
- Test log output at INFO level
- Test log output at ERROR level
- Test parameterized logging

### Test Implementation Recommendations

**Unit Tests**: Use JUnit 5 for unit tests. Mock external dependencies using Mockito. Focus on testing individual methods in isolation.

**Integration Tests**: Use Javalin's test tools or a lightweight HTTP client to test the web server routes. Consider using WireMock to mock external OAuth and API endpoints.

**Test Directory Structure**:
```
src/test/java/com/deere/isg/examples/
    ApplicationTest.java      # OAuth flow and route tests
    SettingsTest.java         # Settings class tests
    ApiTest.java              # API client tests
    TemplateRenderingTest.java # Mustache template tests
```

**Test Dependencies to Add**:
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin-testtools</artifactId>
    <version>${javalin.version}</version>
    <scope>test</scope>
</dependency>
```

---

## Rollback Plan

### Immediate Rollback

If critical issues are discovered during or immediately after the upgrade, follow these steps:

**Step 1**: Stop any running instances of the application.

**Step 2**: Revert the configuration changes by checking out the previous commit or restoring from backup:
```bash
git checkout HEAD~1 -- pom.xml .java-version .github/workflows/maven.yml
```

**Step 3**: Rebuild the application with Java 17:
```bash
mvn clean package
```

**Step 4**: Restart the application and verify functionality.

### Staged Rollback

If issues are discovered after deployment to production:

**Step 1**: Document the specific issues encountered, including error messages, stack traces, and steps to reproduce.

**Step 2**: Create a hotfix branch from the pre-upgrade state.

**Step 3**: Deploy the hotfix to restore service.

**Step 4**: Investigate and resolve the issues in a development environment before attempting the upgrade again.

### Rollback Triggers

Consider rolling back if any of the following occur:

- Application fails to start after upgrade
- OAuth flow is broken (users cannot authenticate)
- API calls fail consistently
- Significant performance degradation (>50% increase in response time)
- Memory leaks or excessive resource consumption
- Unhandled exceptions in production logs

---

## Post-Upgrade Validation

### Validation Checklist

After completing the upgrade, verify the following:

**Build Validation**:
- [ ] `mvn clean compile` completes without errors
- [ ] `mvn package` creates the shaded JAR successfully
- [ ] No deprecation warnings related to Java 21

**Startup Validation**:
- [ ] Application starts without errors
- [ ] Port 9090 is bound successfully
- [ ] Startup log message appears: "Application Started please navigate to http://localhost:9090"

**Functional Validation**:
- [ ] Home page loads at http://localhost:9090
- [ ] Static assets (CSS, JS) load correctly
- [ ] OAuth configuration form displays
- [ ] OAuth flow completes successfully (requires valid client credentials)
- [ ] Token refresh works correctly
- [ ] API calls return expected responses

**CI/CD Validation**:
- [ ] GitHub Actions workflow passes
- [ ] Build artifacts are created correctly

### Performance Baseline

Establish a performance baseline after the upgrade:

**Startup Time**: Measure the time from `java -jar` to "Application Started" message. Compare with Java 17 baseline.

**Memory Usage**: Monitor heap usage during normal operation. Java 21 may show different GC behavior.

**Response Time**: Measure response times for key endpoints (/, /callback, /call-api). Compare with Java 17 baseline.

### Monitoring Recommendations

After the upgrade, monitor the following metrics:

- JVM heap usage and GC activity
- HTTP response times and error rates
- CPU utilization
- Thread count (especially if using virtual threads in the future)

### Documentation Updates

After successful validation, update the following documentation:

- README.md: Update Java version requirements
- Any deployment documentation
- Developer onboarding guides

---

## Appendix: Java 21 Features Available for Future Use

After upgrading to Java 21, the following features become available for future development:

**Virtual Threads (JEP 444)**: Lightweight threads that can dramatically improve throughput for I/O-bound applications. Consider using for the HTTP client operations.

**Pattern Matching for switch (JEP 441)**: Simplifies code that needs to test an expression against multiple patterns.

**Record Patterns (JEP 440)**: Enables more powerful pattern matching with record types.

**Sequenced Collections (JEP 431)**: New interfaces for collections with defined encounter order.

**String Templates (Preview)**: Simplified string formatting with embedded expressions.

These features can be adopted incrementally after the initial upgrade is complete and stable.
