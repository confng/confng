package org.confng.testng;

import lombok.extern.slf4j.Slf4j;
import org.confng.ConfNG;
import org.confng.sources.TestNGParameterSource;
import org.testng.*;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.Map;

/**
 * TestNG listener that automatically captures test parameters and makes them available
 * to ConfNG as a configuration source.
 *
 * <p>This listener is automatically loaded via TestNG's service loader mechanism
 * when ConfNG is on the classpath. It captures parameters from:</p>
 * <ul>
 *   <li>Suite parameters (from testng.xml)</li>
 *   <li>Test parameters (from testng.xml)</li>
 *   <li>Method parameters (from @Parameters annotation)</li>
 * </ul>
 *
 * <p>Parameters are made available with the following precedence:</p>
 * <ol>
 *   <li>Method-level parameters (highest priority)</li>
 *   <li>Test-level parameters</li>
 *   <li>Suite-level parameters (lowest priority)</li>
 * </ol>
 *
 * <p>This listener implements {@link ConfNGListener} with priority 0, allowing
 * custom listeners to execute before (negative priority) or after (positive priority)
 * the configuration loading process.</p>
 *
 * <h3>Automatic Configuration Loading</h3>
 * <p>When the test suite starts, this listener automatically loads configuration files
 * in the following order (first loaded = highest precedence):</p>
 * <ol>
 *   <li><strong>Config Files</strong> - Auto-loads standard config files (HIGHEST PRECEDENCE):
 *       <ul>
 *         <li>config.properties, config.json, config.yaml, config.toml</li>
 *         <li>File type is auto-detected based on extension</li>
 *         <li>Files that don't exist are silently skipped</li>
 *       </ul>
 *   </li>
 *   <li><strong>Environment-Specific Configuration</strong> - Loads configuration files
 *       specific to the current environment (MEDIUM PRECEDENCE):
 *       <ul>
 *         <li>If a suite parameter named "environment" or "env" is present in testng.xml,
 *             loads {environment}.properties, {environment}.json, {environment}.yaml, {environment}.toml</li>
 *         <li>If no environment parameter is found, attempts auto-detection by checking
 *             the APP_ENV, ENVIRONMENT, or ENV environment variables</li>
 *         <li>If no environment is detected, defaults to "local"</li>
 *       </ul>
 *   </li>
 *   <li><strong>Global/Common Configuration</strong> - Loads configuration files that apply
 *       to all environments (LOWEST PRECEDENCE, provides defaults):
 *       <ul>
 *         <li>global.properties, global.json, global.yaml, global.toml</li>
 *         <li>common.properties, common.json, common.yaml, common.toml</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>This loading order ensures that config.* files have the highest precedence, followed by
 * environment-specific values, and finally global values as defaults. This allows you to define
 * common defaults in global configuration files, override them per environment, and have
 * project-specific overrides in config.* files.</p>
 *
 * <h3>Example Configuration Structure</h3>
 * <p>config.json (highest precedence):</p>
 * <pre>
 * {
 *   "browser": "chrome",
 *   "headless": "true"
 * }
 * </pre>
 *
 * <p>uat.json (medium precedence):</p>
 * <pre>
 * {
 *   "api.url": "https://uat-api.example.com",
 *   "api.timeout": "60",
 *   "database.host": "uat-db.example.com"
 * }
 * </pre>
 *
 * <p>global.json (lowest precedence, provides defaults):</p>
 * <pre>
 * {
 *   "api.timeout": "30",
 *   "retry.count": "3",
 *   "log.level": "INFO"
 * }
 * </pre>
 *
 * <p>Result for UAT environment: browser=chrome (from config.json), headless=true (from config.json),
 * api.timeout=60 (from uat.json, overrides global), retry.count=3 (from global.json),
 * log.level=INFO (from global.json), api.url and database.host from uat.json</p>
 *
 * <h3>Example testng.xml</h3>
 * <pre>
 * &lt;suite name="My Test Suite"&gt;
 *   &lt;parameter name="environment" value="uat"/&gt;
 *   &lt;test name="My Test"&gt;
 *     &lt;classes&gt;
 *       &lt;class name="com.example.MyTest"/&gt;
 *     &lt;/classes&gt;
 *   &lt;/test&gt;
 * &lt;/suite&gt;
 * </pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 * @see org.confng.sources.TestNGParameterSource
 * @see org.confng.ConfNG#loadGlobalConfig()
 * @see org.confng.ConfNG#loadConfigForEnvironment(String)
 * @see org.confng.ConfNG#autoLoadConfig()
 */
@Slf4j
public class TestNGParameterListener implements ITestListener, IInvokedMethodListener, ISuiteListener, ITestNGListener, ConfNGListener {

    private static TestNGParameterSource parameterSource;
    private static boolean isRegistered = false;
    static final ThreadLocal<Boolean> executingInChain = ThreadLocal.withInitial(() -> false);

    /**
     * Returns the priority of this listener in the listener chain.
     * Priority 0 is the default for TestNGParameterListener, allowing
     * custom listeners to execute before (negative numbers) or after (positive numbers).
     */
    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * Called when a suite starts. Captures suite-level parameters and automatically loads
     * configuration files with auto-detection of file types.
     *
     * <p>Auto-loads the following configuration files (if they exist):</p>
     * <ol>
     *   <li>config.properties, config.json, config.yaml, config.toml - HIGHEST PRECEDENCE</li>
     *   <li>Environment-specific configuration files ({environment}.properties, {environment}.json, etc.)</li>
     *   <li>Global/common configuration files (global.properties, global.json, etc.) - LOWER PRECEDENCE (provides defaults)</li>
     * </ol>
     *
     * <p>This ensures that config.* files have the highest precedence, followed by environment-specific values,
     * and finally global values as defaults.</p>
     */
    @Override
    public void onStart(ISuite suite) {
        ensureParameterSourceRegistered();

        // Initialize listener chain with this listener as the core listener
        TestNGListenerChain.getInstance().initialize(this);

        // Execute the entire listener chain (including this listener's onSuiteStart)
        TestNGListenerChain.getInstance().executeSuiteStart(suite);
    }

    /**
     * Implementation of ConfNGListener.onSuiteStart - this is called by the listener chain.
     * This method contains the actual configuration loading logic.
     */
    @Override
    public void onSuiteStart(ISuite suite) {
        try {
            // Auto-load config.* files first to get base configuration
            log.info("[ConfNG] Auto-loading config files...");
            ConfNG.load("config.properties");
            ConfNG.load("config.json");
            ConfNG.load("config.yaml");
            ConfNG.load("config.toml");
            log.info("[ConfNG] Config files loaded successfully");

            // Use direct TestNG API to get parameters
            XmlSuite xmlSuite = suite.getXmlSuite();
            Map<String, String> suiteParameters = xmlSuite.getAllParameters();
            if (suiteParameters != null && !suiteParameters.isEmpty()) {
                parameterSource.addSuiteParameters(suiteParameters);
            }

            // Detect environment from multiple sources (TestNG params, config files, env vars)
            String environment = null;

            // First check TestNG suite parameters
            if (suiteParameters != null) {
                environment = suiteParameters.get("environment");
                if (environment == null || environment.isEmpty()) {
                    environment = suiteParameters.get("env");
                }
            }

            // If not in TestNG params, check if environment is defined in the loaded config files
            if (environment == null || environment.isEmpty()) {
                environment = ConfNG.getEnvironmentName();
                log.info("[ConfNG] Auto-detected environment from config: {}", environment);
            }

            // Load environment-specific sections from config files
            if (environment != null && !environment.isEmpty() && !environment.equals("local")) {
                log.info("[ConfNG] Loading environment-specific sections from config files for: {}", environment);
                ConfNG.load("config.json", environment);
                ConfNG.load("config.yaml", environment);
                ConfNG.load("config.toml", environment);
                log.info("[ConfNG] Environment-specific sections loaded successfully");
            }

            // Load environment-specific configuration files (e.g., dev.properties, dev.json)
            if (environment != null && !environment.isEmpty()) {
                log.info("[ConfNG] Loading environment-specific configuration files for: {}", environment);
                ConfNG.loadConfigForEnvironment(environment);
                log.info("[ConfNG] Environment-specific configuration loaded successfully");
            }

            // Load global/common configuration LAST (lower precedence, provides defaults)
            // This ensures environment-specific values override global values
            log.info("[ConfNG] Loading global/common configuration files...");
            ConfNG.loadGlobalConfig();
            log.info("[ConfNG] Global configuration loaded successfully");

            // Load environment-specific sections from global files
            if (environment != null && !environment.isEmpty() && !environment.equals("local")) {
                log.info("[ConfNG] Loading environment-specific sections from global files for: {}", environment);
                ConfNG.load("global.json", environment);
                ConfNG.load("global.yaml", environment);
                ConfNG.load("global.toml", environment);
                ConfNG.load("common.json", environment);
                ConfNG.load("common.yaml", environment);
                ConfNG.load("common.toml", environment);
                log.info("[ConfNG] Environment-specific sections from global files loaded successfully");
            }
        } catch (Exception e) {
            // Log warning if configuration loading fails
            System.err.println("[ConfNG] Warning: Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Called when a test starts. Captures test-level parameters.
     * This method satisfies both ITestListener.onTestStart and ConfNGListener.onTestStart.
     *
     * <p>When called by TestNG, delegates to the listener chain.
     * When called by the chain, executes the actual parameter capture logic.</p>
     */
    @Override
    public void onTestStart(ITestResult result) {
        ensureParameterSourceRegistered();

        // If not executing in chain, delegate to chain (TestNG is calling us)
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeTestStart(result);
            return;
        }

        // Otherwise, we're being called by the chain - do the actual work
        try {
            ITestContext testContext = result.getTestContext();
            String testName = testContext.getCurrentXmlTest().getName();
            Map<String, String> testParameters = testContext.getCurrentXmlTest().getAllParameters();
            if (testParameters != null && !testParameters.isEmpty()) {
                parameterSource.addTestParameters(testName, testParameters);
            }
        } catch (Exception e) {
            // Silently ignore if TestNG classes are not available or reflection fails
        }
    }

    /**
     * Called before a method is invoked. Captures method-level parameters.
     * This method satisfies both IInvokedMethodListener and ConfNGListener.
     */
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        ensureParameterSourceRegistered();

        // If not executing in chain, delegate to chain (TestNG is calling us)
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeBeforeInvocation(method, testResult);
            return;
        }

        // Otherwise, we're being called by the chain - do the actual work
        try {
            ITestContext testContext = testResult.getTestContext();
            XmlTest currentXmlTest = testContext.getCurrentXmlTest();
            String testName = currentXmlTest.getName();
            log.debug("[ConfNG] Setting current test context: {}", testName);
            parameterSource.setCurrentTestName(testName);

            // Use direct TestNG API for method parameters
            ITestNGMethod testMethod = method.getTestMethod();
            String methodName = testMethod.getMethodName();

            // Also execute the simplified listener chain for backward compatibility
            TestNGListenerChain.getInstance().executeBeforeMethodInvocation(testContext, methodName);

            Map<String, String> methodParameters = currentXmlTest.getAllParameters();
            if (methodParameters != null && !methodParameters.isEmpty()) {
                parameterSource.addMethodParameters(methodName, methodParameters);
            }
        } catch (Exception e) {
            // Silently ignore if TestNG classes are not available
        }
    }

    /**
     * Called after a method is invoked. Cleanup method-level parameters.
     * This method satisfies both IInvokedMethodListener and ConfNGListener.
     */
    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        // If not executing in chain, delegate to chain (TestNG is calling us)
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeAfterInvocation(method, testResult);
            return;
        }

        // Otherwise, we're being called by the chain - do the actual work
        if (parameterSource != null) {
            try {
                ITestContext testContext = testResult.getTestContext();
                // Use direct TestNG API
                ITestNGMethod testMethod = method.getTestMethod();
                String methodName = testMethod.getMethodName();

                // Also execute the simplified listener chain for backward compatibility
                TestNGListenerChain.getInstance().executeAfterMethodInvocation(testContext, methodName);

                parameterSource.clearMethodParameters(methodName);
                // Clear the current test context after each method invocation
                log.debug("[ConfNG] Clearing current test context after method: {}", methodName);
                parameterSource.setCurrentTestName(null);
            } catch (Exception e) {
                // Silently ignore if TestNG classes are not available
            }
        }
    }

    /**
     * Called when a test succeeds. Cleanup test-level parameters.
     * This method satisfies both ITestListener.onTestSuccess and ConfNGListener.onTestSuccess.
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeTestSuccess(result);
            return;
        }
        cleanup(result);
    }

    /**
     * Called when a test fails. Cleanup test-level parameters.
     * This method satisfies both ITestListener.onTestFailure and ConfNGListener.onTestFailure.
     */
    @Override
    public void onTestFailure(ITestResult result) {
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeTestFailure(result);
            return;
        }
        cleanup(result);
    }

    /**
     * Called when a test is skipped. Cleanup test-level parameters.
     * This method satisfies both ITestListener.onTestSkipped and ConfNGListener.onTestSkipped.
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeTestSkipped(result);
            return;
        }
        cleanup(result);
    }

    /**
     * Called when a test fails but is within the success percentage.
     * This method satisfies both ITestListener and ConfNGListener.
     */
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeTestFailedButWithinSuccessPercentage(result);
            return;
        }
        // Default: do nothing
    }

    /**
     * Called when a test fails with timeout.
     * This method satisfies both ITestListener and ConfNGListener.
     */
    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeTestFailedWithTimeout(result);
            return;
        }
        // Default: treat as regular failure
        cleanup(result);
    }

    /**
     * Called when a test context starts (ITestListener).
     * Delegates to the listener chain.
     */
    @Override
    public void onStart(ITestContext context) {
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeTestContextStart(context);
            return;
        }
        // Default: do nothing
    }

    /**
     * Called when a test context finishes (ITestListener).
     * Note: This is different from onFinish(ISuite) which is for ISuiteListener.
     * Delegates to the listener chain.
     */
    @Override
    public void onFinish(ITestContext context) {
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeTestContextFinish(context);
            return;
        }
        // Default: do nothing
    }

    private void cleanup(Object result) {
        if (parameterSource != null && result instanceof ITestResult) {
            try {
                ITestResult testResult = (ITestResult) result;
                ITestContext testContext = testResult.getTestContext();
                String testName = testContext.getCurrentXmlTest().getName();
                parameterSource.clearTestParameters(testName);
            } catch (Exception e) {
                // Silently ignore if TestNG classes are not available or reflection fails
            }
        }
    }

    /**
     * Called when a suite finishes (TestNG ISuiteListener method).
     * Delegates to the listener chain which will call onSuiteFinish on all listeners.
     */
    @Override
    public void onFinish(ISuite suite) {
        if (!executingInChain.get()) {
            TestNGListenerChain.getInstance().executeSuiteFinish(suite);
            return;
        }

        // When called by the chain, do the cleanup
        if (parameterSource != null) {
            parameterSource.clearSuiteParameters();
        }
    }

    /**
     * Implementation of ConfNGListener.onSuiteFinish - called by the listener chain.
     * Cleanup suite-level parameters.
     */
    @Override
    public void onSuiteFinish(ISuite suite) {
        // This is the same as onFinish for ISuiteListener
        // The actual work is done in onFinish when called by the chain
    }

    /**
     * Ensures the TestNG parameter source is registered with ConfNG.
     * This method is thread-safe and will only register the source once.
     */
    private synchronized void ensureParameterSourceRegistered() {
        if (!isRegistered) {
            parameterSource = new TestNGParameterSource();
            ConfNG.addSource(parameterSource);
            isRegistered = true;
        }
    }

    /**
     * Gets the current TestNG parameter source.
     * This method is primarily for testing purposes.
     * 
     * @return the current parameter source, or null if not initialized
     */
    public static TestNGParameterSource getParameterSource() {
        return parameterSource;
    }
}
