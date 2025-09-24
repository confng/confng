package org.confng.testng;

import org.confng.ConfNG;
import org.confng.sources.TestNGParameterSource;
import org.testng.*;

import java.lang.reflect.Method;
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
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 * @see org.confng.sources.TestNGParameterSource
 */
public class TestNGParameterListener implements ITestListener, IInvokedMethodListener, ISuiteListener, ITestNGListener {

    private static TestNGParameterSource parameterSource;
    private static boolean isRegistered = false;

    /**
     * Called when a suite starts. Captures suite-level parameters.
     */
    @Override
    public void onStart(ISuite suite) {
        ensureParameterSourceRegistered();
        
        try {
            // Use reflection to get parameters to avoid compile-time dependency on TestNG
            Object xmlSuite = suite.getClass().getMethod("getXmlSuite").invoke(suite);
            @SuppressWarnings("unchecked")
            Map<String, String> suiteParameters = (Map<String, String>) xmlSuite.getClass().getMethod("getAllParameters").invoke(xmlSuite);
            if (suiteParameters != null && !suiteParameters.isEmpty()) {
                parameterSource.addSuiteParameters(suiteParameters);
            }
        } catch (Exception e) {
            // Silently ignore if TestNG classes are not available or reflection fails
        }
    }

    /**
     * Called when a test starts. Captures test-level parameters.
     */
    @Override
    public void onTestStart(ITestResult result) {
        ensureParameterSourceRegistered();
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
     */

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        ensureParameterSourceRegistered();
        try {
            ITestContext testContext = testResult.getTestContext();
            String testName = testContext.getCurrentXmlTest().getName();
            System.out.println("[ConfNG] Setting current test context: " + testName);
            parameterSource.setCurrentTestName(testName);

            // Existing logic for method parameters
            Object testMethod = method.getClass().getMethod("getTestMethod").invoke(method);
            String methodName = (String) testMethod.getClass().getMethod("getMethodName").invoke(testMethod);
            Object currentXmlTest = testContext.getClass().getMethod("getCurrentXmlTest").invoke(testContext);
            @SuppressWarnings("unchecked")
            Map<String, String> methodParameters = (Map<String, String>) currentXmlTest.getClass().getMethod("getAllParameters").invoke(currentXmlTest);
            if (methodParameters != null && !methodParameters.isEmpty()) {
                parameterSource.addMethodParameters(methodName, methodParameters);
            }
        } catch (Exception e) {
            // Silently ignore if TestNG classes are not available or reflection fails
        }
    }

    /**
     * Called after a method is invoked. Cleanup method-level parameters.
     */
    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (parameterSource != null) {
            try {
                Object testMethod = method.getClass().getMethod("getTestMethod").invoke(method);
                String methodName = (String) testMethod.getClass().getMethod("getMethodName").invoke(testMethod);
                parameterSource.clearMethodParameters(methodName);
                // Clear the current test context after each method invocation
                System.out.println("[ConfNG] Clearing current test context after method: " + methodName);
                parameterSource.setCurrentTestName(null);
            } catch (Exception e) {
                // Silently ignore if TestNG classes are not available or reflection fails
            }
        }
    }

    /**
     * Called when a test finishes. Cleanup test-level parameters.
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        cleanup(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        cleanup(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        cleanup(result);
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
     * Called when a suite finishes. Cleanup suite-level parameters.
     */
    @Override
    public void onFinish(ISuite suite) {
        if (parameterSource != null) {
            parameterSource.clearSuiteParameters();
        }
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
