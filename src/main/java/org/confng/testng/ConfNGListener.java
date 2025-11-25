package org.confng.testng;

import org.testng.IInvokedMethod;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;

/**
 * Interface for custom ConfNG listeners that can be executed before or after
 * the standard TestNGParameterListener.
 * 
 * <p>Implement this interface to add custom logic to the ConfNG listener chain.
 * Listeners are executed in priority order, where lower priority values execute first.</p>
 * 
 * <h3>Priority Guidelines:</h3>
 * <ul>
 *   <li><strong>Negative numbers</strong>: Pre-processing listeners (execute before TestNGParameterListener)</li>
 *   <li><strong>0</strong>: TestNGParameterListener (default ConfNG listener)</li>
 *   <li><strong>Positive numbers</strong>: Post-processing listeners (execute after TestNGParameterListener)</li>
 * </ul>
 * 
 * <h3>Example Usage:</h3>
 * <pre>
 * public class CustomPreListener implements ConfNGListener {
 *     {@literal @}Override
 *     public int getPriority() {
 *         return -10; // Execute before TestNGParameterListener (negative priority)
 *     }
 *
 *     {@literal @}Override
 *     public void onSuiteStart(ISuite suite) {
 *         System.out.println("Custom pre-processing before ConfNG loads config");
 *         // Your custom logic here
 *     }
 * }
 *
 * public class CustomPostListener implements ConfNGListener {
 *     {@literal @}Override
 *     public int getPriority() {
 *         return 10; // Execute after TestNGParameterListener (positive priority)
 *     }
 *
 *     {@literal @}Override
 *     public void onSuiteStart(ISuite suite) {
 *         System.out.println("Custom post-processing after ConfNG loads config");
 *         // Your custom logic here
 *     }
 * }
 * </pre>
 * 
 * <h3>Registration:</h3>
 * <p>Register your listener using Java's ServiceLoader mechanism by creating a file:</p>
 * <pre>
 * META-INF/services/org.confng.testng.ConfNGListener
 * </pre>
 * <p>Add your listener class name(s) to this file:</p>
 * <pre>
 * com.example.CustomPreListener
 * com.example.CustomPostListener
 * </pre>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0.3
 * @see TestNGListenerChain
 * @see TestNGParameterListener
 */
public interface ConfNGListener {
    
    /**
     * Returns the priority of this listener. Lower values execute first.
     *
     * <p>Priority ranges:</p>
     * <ul>
     *   <li>Negative numbers: Pre-processing (before TestNGParameterListener)</li>
     *   <li>0: TestNGParameterListener (default)</li>
     *   <li>Positive numbers: Post-processing (after TestNGParameterListener)</li>
     * </ul>
     *
     * @return the priority value (can be negative, zero, or positive)
     */
    int getPriority();
    
    /**
     * Called when a test suite starts.
     * 
     * @param suite the test suite
     */
    default void onSuiteStart(ISuite suite) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a test suite finishes.
     * 
     * @param suite the test suite
     */
    default void onSuiteFinish(ISuite suite) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a test starts.
     * 
     * @param result the test result
     */
    default void onTestStart(ITestResult result) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a test succeeds.
     * 
     * @param result the test result
     */
    default void onTestSuccess(ITestResult result) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a test fails.
     * 
     * @param result the test result
     */
    default void onTestFailure(ITestResult result) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a test is skipped.
     *
     * @param result the test result
     */
    default void onTestSkipped(ITestResult result) {
        // Default implementation does nothing
    }

    /**
     * Called when a test fails but is within the success percentage.
     *
     * @param result the test result
     */
    default void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        // Default implementation does nothing
    }

    /**
     * Called when a test fails with timeout.
     *
     * @param result the test result
     */
    default void onTestFailedWithTimeout(ITestResult result) {
        // Default implementation does nothing
    }

    /**
     * Called when a test context starts (ITestListener).
     *
     * @param context the test context
     */
    default void onStart(ITestContext context) {
        // Default implementation does nothing
    }

    /**
     * Called when a test context finishes (ITestListener).
     *
     * @param context the test context
     */
    default void onFinish(ITestContext context) {
        // Default implementation does nothing
    }

    /**
     * Called before a method is invoked (IInvokedMethodListener).
     *
     * @param method the invoked method
     * @param testResult the test result
     */
    default void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        // Default implementation does nothing
    }

    /**
     * Called after a method is invoked (IInvokedMethodListener).
     *
     * @param method the invoked method
     * @param testResult the test result
     */
    default void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        // Default implementation does nothing
    }

    /**
     * Called before a method is invoked (simplified version for convenience).
     *
     * @param testContext the test context
     * @param methodName the method name
     */
    default void beforeMethodInvocation(ITestContext testContext, String methodName) {
        // Default implementation does nothing
    }

    /**
     * Called after a method is invoked (simplified version for convenience).
     *
     * @param testContext the test context
     * @param methodName the method name
     */
    default void afterMethodInvocation(ITestContext testContext, String methodName) {
        // Default implementation does nothing
    }
}

