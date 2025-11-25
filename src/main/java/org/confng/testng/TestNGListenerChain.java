package org.confng.testng;

import lombok.extern.slf4j.Slf4j;
import org.testng.IInvokedMethod;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the chain of ConfNG listeners and executes them in priority order.
 *
 * <p>This class discovers and manages custom listeners that implement the
 * {@link ConfNGListener} interface. Listeners are executed in priority order,
 * where lower priority values execute first.</p>
 *
 * <p>The chain includes:</p>
 * <ul>
 *   <li>Custom pre-processing listeners (negative priority)</li>
 *   <li>TestNGParameterListener (priority 0)</li>
 *   <li>Custom post-processing listeners (positive priority)</li>
 * </ul>
 *
 * <h3>Listener Discovery:</h3>
 * <p>Custom listeners are discovered using Java's ServiceLoader mechanism.
 * To register a custom listener:</p>
 * <ol>
 *   <li>Implement the {@link ConfNGListener} interface</li>
 *   <li>Create a file: {@code META-INF/services/org.confng.testng.ConfNGListener}</li>
 *   <li>Add your listener class name(s) to the file</li>
 * </ol>
 *
 * <h3>Example:</h3>
 * <pre>
 * // File: META-INF/services/org.confng.testng.ConfNGListener
 * com.example.CustomPreListener
 * com.example.CustomPostListener
 * </pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0.3
 * @see ConfNGListener
 * @see TestNGParameterListener
 */
@Slf4j
public class TestNGListenerChain {
    
    private static final TestNGListenerChain INSTANCE = new TestNGListenerChain();
    private final List<ConfNGListener> listeners = new ArrayList<>();
    private final Map<String, Object> sharedContext = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;
    
    private TestNGListenerChain() {
        // Private constructor for singleton
    }
    
    /**
     * Gets the singleton instance of the listener chain.
     * 
     * @return the listener chain instance
     */
    public static TestNGListenerChain getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initializes the listener chain by discovering and loading all registered listeners.
     * This method is thread-safe and will only initialize once.
     *
     * @param coreListener the core TestNGParameterListener instance to include in the chain
     */
    public synchronized void initialize(ConfNGListener coreListener) {
        if (initialized) {
            return;
        }

        log.info("[ConfNG] Initializing listener chain...");

        try {
            // Add the core TestNGParameterListener first
            if (coreListener != null) {
                listeners.add(coreListener);
                log.info("[ConfNG] Registered core listener: {} (priority: {})",
                    coreListener.getClass().getName(), coreListener.getPriority());
            }

            // Discover custom listeners using ServiceLoader
            ServiceLoader<ConfNGListener> serviceLoader = ServiceLoader.load(ConfNGListener.class);
            for (ConfNGListener listener : serviceLoader) {
                // Skip if it's the same instance as coreListener (avoid duplicates)
                if (listener != coreListener) {
                    listeners.add(listener);
                    log.info("[ConfNG] Discovered custom listener: {} (priority: {})",
                        listener.getClass().getName(), listener.getPriority());
                }
            }

            // Sort listeners by priority (lower priority executes first)
            listeners.sort(Comparator.comparingInt(ConfNGListener::getPriority));

            log.info("[ConfNG] Listener chain initialized with {} listener(s)", listeners.size());

        } catch (Exception e) {
            log.warn("[ConfNG] Warning: Failed to initialize listener chain: {}", e.getMessage());
        }

        initialized = true;
    }
    
    /**
     * Executes all listeners' onSuiteStart method in priority order.
     *
     * @param suite the test suite
     */
    public void executeSuiteStart(ISuite suite) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.onSuiteStart(suite);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".onSuiteStart: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }
    
    /**
     * Executes all listeners' onSuiteFinish method in priority order.
     *
     * @param suite the test suite
     */
    public void executeSuiteFinish(ISuite suite) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.onSuiteFinish(suite);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".onSuiteFinish: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }
    
    /**
     * Executes all listeners' onTestStart method in priority order.
     *
     * @param result the test result
     */
    public void executeTestStart(ITestResult result) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.onTestStart(result);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".onTestStart: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Executes all listeners' onTestSuccess method in priority order.
     *
     * @param result the test result
     */
    public void executeTestSuccess(ITestResult result) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.onTestSuccess(result);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".onTestSuccess: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Executes all listeners' onTestFailure method in priority order.
     *
     * @param result the test result
     */
    public void executeTestFailure(ITestResult result) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.onTestFailure(result);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".onTestFailure: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Executes all listeners' onTestSkipped method in priority order.
     *
     * @param result the test result
     */
    public void executeTestSkipped(ITestResult result) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.onTestSkipped(result);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".onTestSkipped: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }
    
    /**
     * Executes all listeners' beforeMethodInvocation method in priority order.
     *
     * @param testContext the test context
     * @param methodName the method name
     */
    public void executeBeforeMethodInvocation(ITestContext testContext, String methodName) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.beforeMethodInvocation(testContext, methodName);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".beforeMethodInvocation: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Executes all listeners' afterMethodInvocation method in priority order.
     *
     * @param testContext the test context
     * @param methodName the method name
     */
    public void executeAfterMethodInvocation(ITestContext testContext, String methodName) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.afterMethodInvocation(testContext, methodName);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".afterMethodInvocation: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Executes all listeners' onTestFailedButWithinSuccessPercentage method in priority order.
     *
     * @param result the test result
     */
    public void executeTestFailedButWithinSuccessPercentage(ITestResult result) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.onTestFailedButWithinSuccessPercentage(result);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".onTestFailedButWithinSuccessPercentage: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Executes all listeners' onTestFailedWithTimeout method in priority order.
     *
     * @param result the test result
     */
    public void executeTestFailedWithTimeout(ITestResult result) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.onTestFailedWithTimeout(result);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".onTestFailedWithTimeout: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Executes all listeners' onStart method (ITestListener) in priority order.
     *
     * @param context the test context
     */
    public void executeTestContextStart(ITestContext context) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.onStart(context);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".onStart: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Executes all listeners' onFinish method (ITestListener) in priority order.
     *
     * @param context the test context
     */
    public void executeTestContextFinish(ITestContext context) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.onFinish(context);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".onFinish: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Executes all listeners' beforeInvocation method (IInvokedMethodListener) in priority order.
     *
     * @param method the invoked method
     * @param testResult the test result
     */
    public void executeBeforeInvocation(IInvokedMethod method, ITestResult testResult) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.beforeInvocation(method, testResult);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".beforeInvocation: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Executes all listeners' afterInvocation method (IInvokedMethodListener) in priority order.
     *
     * @param method the invoked method
     * @param testResult the test result
     */
    public void executeAfterInvocation(IInvokedMethod method, ITestResult testResult) {
        for (ConfNGListener listener : listeners) {
            try {
                setChainFlag(listener, true);
                listener.afterInvocation(method, testResult);
            } catch (Exception e) {
                System.err.println("[ConfNG] Error in listener " + listener.getClass().getName() +
                    ".afterInvocation: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setChainFlag(listener, false);
            }
        }
    }

    /**
     * Gets the shared context map that can be used to pass data between listeners.
     *
     * @return the shared context map
     */
    public Map<String, Object> getSharedContext() {
        return sharedContext;
    }
    
    /**
     * Gets all registered listeners in priority order.
     * 
     * @return an unmodifiable list of listeners
     */
    public List<ConfNGListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }
    
    /**
     * Clears all listeners and resets the initialization state.
     * This method is primarily for testing purposes.
     */
    synchronized void reset() {
        listeners.clear();
        sharedContext.clear();
        initialized = false;
    }

    /**
     * Helper method to set the chain execution flag for TestNGParameterListener.
     * This prevents infinite recursion when TestNG methods delegate to the chain.
     */
    private void setChainFlag(ConfNGListener listener, boolean value) {
        if (listener instanceof TestNGParameterListener) {
            TestNGParameterListener.executingInChain.set(value);
        }
    }
}

