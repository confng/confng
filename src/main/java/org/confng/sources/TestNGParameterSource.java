package org.confng.sources;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration source that provides values from TestNG parameters.
 * 
 * <p>This source captures parameters from TestNG test execution including:</p>
 * <ul>
 *   <li>Suite-level parameters (from testng.xml suite)</li>
 *   <li>Test-level parameters (from testng.xml test)</li>
 *   <li>Method-level parameters (from @Parameters annotation)</li>
 * </ul>
 * 
 * <p>Parameter precedence (highest to lowest):</p>
 * <ol>
 *   <li>Method-level parameters</li>
 *   <li>Test-level parameters</li>
 *   <li>Suite-level parameters</li>
 * </ol>
 * 
 * <p>This source is automatically registered when using the TestNGParameterListener
 * which is loaded via TestNG's service loader mechanism.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 * @see org.confng.testng.TestNGParameterListener
 */
public class TestNGParameterSource implements ConfigSource {

    private final Map<String, String> suiteParameters = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> testParameters = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> methodParameters = new ConcurrentHashMap<>();
    
    // Thread-local storage for current method and test context
    private final ThreadLocal<String> currentMethod = new ThreadLocal<>();
    private final ThreadLocal<String> currentTest = new ThreadLocal<>();

    @Override
    public String getName() {
        return "TestNGParameterSource";
    }

    @Override
    public Optional<String> get(String key) {
        // Check method-level parameters first (highest priority)
        String method = currentMethod.get();
        if (method != null) {
            Map<String, String> methodParams = methodParameters.get(method);
            if (methodParams != null && methodParams.containsKey(key)) {
                return Optional.of(methodParams.get(key));
            }
        }

        // Check test-level parameters for current test context
        String test = currentTest.get();
        if (test != null) {
            Map<String, String> testParams = testParameters.get(test);
            if (testParams != null && testParams.containsKey(key)) {
                return Optional.of(testParams.get(key));
            }
        }

        // Check suite-level parameters (lowest priority)
        if (suiteParameters.containsKey(key)) {
            return Optional.of(suiteParameters.get(key));
        }

        return Optional.empty();
    }

    @Override
    public int getPriority() {
        return 80; // High priority - TestNG parameters should override most other sources
    }

    /**
     * Adds suite-level parameters from TestNG suite configuration.
     * 
     * @param parameters the suite parameters to add
     */
    public void addSuiteParameters(Map<String, String> parameters) {
        if (parameters != null) {
            suiteParameters.putAll(parameters);
        }
    }

    /**
     * Adds test-level parameters for a specific test context.
     *
     * @param testName the name of the test
     * @param parameters the test parameters to add
     */
    public void addTestParameters(String testName, Map<String, String> parameters) {
        if (testName != null && parameters != null) {
            testParameters.put(testName, new ConcurrentHashMap<>(parameters));
            currentTest.set(testName);
        }
    }

    /**
     * Adds method-level parameters for a specific test method.
     * 
     * @param methodName the name of the test method
     * @param parameters the method parameters to add
     */
    public void addMethodParameters(String methodName, Map<String, String> parameters) {
        if (methodName != null && parameters != null) {
            methodParameters.put(methodName, new ConcurrentHashMap<>(parameters));
            currentMethod.set(methodName);
        }
    }

    /**
     * Clears suite-level parameters.
     */
    public void clearSuiteParameters() {
        suiteParameters.clear();
    }

    /**
     * Clears test-level parameters for a specific test context.
     *
     * @param testName the name of the test to clear parameters for
     */
    public void clearTestParameters(String testName) {
        if (testName != null) {
            testParameters.remove(testName);
            if (testName.equals(currentTest.get())) {
                currentTest.remove();
            }
        }
    }

    /**
     * Clears method-level parameters for a specific method.
     * 
     * @param methodName the name of the method to clear parameters for
     */
    public void clearMethodParameters(String methodName) {
        if (methodName != null) {
            methodParameters.remove(methodName);
            if (methodName.equals(currentMethod.get())) {
                currentMethod.remove();
            }
        }
    }

    /**
     * Clears all parameters (suite, test, and method level).
     */
    public void clearAllParameters() {
        suiteParameters.clear();
        testParameters.clear();
        methodParameters.clear();
        currentMethod.remove();
        currentTest.remove();
    }

    /**
     * Gets all suite parameters.
     * 
     * @return a copy of the suite parameters
     */
    public Map<String, String> getSuiteParameters() {
        return new ConcurrentHashMap<>(suiteParameters);
    }

    /**
     * Gets all test parameters for all test contexts.
     *
     * @return a copy of all test parameters
     */
    public Map<String, Map<String, String>> getTestParameters() {
        return new ConcurrentHashMap<>(testParameters);
    }

    /**
     * Gets test parameters for a specific test context.
     *
     * @param testName the name of the test
     * @return a copy of the test parameters, or empty map if not found
     */
    public Map<String, String> getTestParameters(String testName) {
        Map<String, String> params = testParameters.get(testName);
        return params != null ? new ConcurrentHashMap<>(params) : new ConcurrentHashMap<>();
    }

    /**
     * Gets method parameters for a specific method.
     * 
     * @param methodName the name of the method
     * @return a copy of the method parameters, or empty map if not found
     */
    public Map<String, String> getMethodParameters(String methodName) {
        Map<String, String> params = methodParameters.get(methodName);
        return params != null ? new ConcurrentHashMap<>(params) : new ConcurrentHashMap<>();
    }

    /**
     * Gets all parameters in precedence order.
     * Method parameters override test parameters, which override suite parameters.
     * 
     * @return a map containing all parameters with proper precedence
     */
    public Map<String, String> getAllParameters() {
        Map<String, String> allParams = new ConcurrentHashMap<>();
        // Add suite parameters first (lowest priority)
        allParams.putAll(suiteParameters);
        // Add test parameters for current test context (medium priority)
        String test = currentTest.get();
        if (test != null) {
            Map<String, String> testParams = testParameters.get(test);
            if (testParams != null) {
                allParams.putAll(testParams);
            }
        }
        // Add method parameters (highest priority)
        String method = currentMethod.get();
        if (method != null) {
            Map<String, String> methodParams = methodParameters.get(method);
            if (methodParams != null) {
                allParams.putAll(methodParams);
            }
        }
        return allParams;
    }

    /**
     * Gets the name of the current test context.
     *
     * @return the current test name, or null if not set
     */
    public String getCurrentTestName() {
        return currentTest.get();
    }

    /**
     * Sets the name of the current test context for the thread.
     *
     * @param testName the test name to set as current
     */
    public void setCurrentTestName(String testName) {
        currentTest.set(testName);
    }
}
