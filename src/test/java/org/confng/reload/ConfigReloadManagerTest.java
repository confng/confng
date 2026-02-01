package org.confng.reload;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/**
 * Tests for ConfigReloadManager.
 */
public class ConfigReloadManagerTest {

    @BeforeMethod
    public void setUp() {
        ConfigReloadManager.reset();
    }

    @AfterMethod
    public void tearDown() {
        ConfigReloadManager.reset();
    }

    @Test
    public void testSingletonInstance() {
        ConfigReloadManager instance1 = ConfigReloadManager.getInstance();
        ConfigReloadManager instance2 = ConfigReloadManager.getInstance();
        assertSame(instance1, instance2, "Should return same singleton instance");
    }

    @Test
    public void testResetCreatesFreshInstance() {
        ConfigReloadManager instance1 = ConfigReloadManager.getInstance();
        ConfigReloadManager.reset();
        ConfigReloadManager instance2 = ConfigReloadManager.getInstance();
        assertNotSame(instance1, instance2, "After reset, should return new instance");
    }

    @Test
    public void testAutoReloadDisabledByDefault() {
        ConfigReloadManager manager = ConfigReloadManager.getInstance();
        assertFalse(manager.isAutoReloadEnabled(), "Auto-reload should be disabled by default");
    }

    @Test
    public void testAddAndRemoveChangeListener() {
        ConfigReloadManager manager = ConfigReloadManager.getInstance();
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        
        ConfigChangeListener listener = event -> listenerCalled.set(true);
        
        manager.addChangeListener(listener);
        // Listener is added - we can verify by removing it (no exception)
        manager.removeChangeListener(listener);
        // Should not throw
    }

    @Test
    public void testAddNullListenerDoesNotThrow() {
        ConfigReloadManager manager = ConfigReloadManager.getInstance();
        // Should not throw
        manager.addChangeListener(null);
    }

    @Test
    public void testRemoveNonExistentListenerDoesNotThrow() {
        ConfigReloadManager manager = ConfigReloadManager.getInstance();
        ConfigChangeListener listener = event -> {};
        // Should not throw even if listener was never added
        manager.removeChangeListener(listener);
    }

    @Test
    public void testRegisterLoadedFile() {
        ConfigReloadManager manager = ConfigReloadManager.getInstance();
        // Should not throw
        manager.registerLoadedFile("/some/path/config.properties");
    }

    @Test
    public void testCloseDisablesAutoReload() throws Exception {
        ConfigReloadManager manager = ConfigReloadManager.getInstance();
        manager.enableAutoReload();
        assertTrue(manager.isAutoReloadEnabled(), "Auto-reload should be enabled");
        
        manager.close();
        assertFalse(manager.isAutoReloadEnabled(), "Auto-reload should be disabled after close");
    }

    @Test
    public void testDisableAutoReloadWhenNotEnabled() {
        ConfigReloadManager manager = ConfigReloadManager.getInstance();
        assertFalse(manager.isAutoReloadEnabled());
        // Should not throw
        manager.disableAutoReload();
        assertFalse(manager.isAutoReloadEnabled());
    }

    @Test
    public void testEnableAutoReloadTwiceDoesNotThrow() throws Exception {
        ConfigReloadManager manager = ConfigReloadManager.getInstance();
        manager.enableAutoReload();
        assertTrue(manager.isAutoReloadEnabled());
        // Second call should be idempotent
        manager.enableAutoReload();
        assertTrue(manager.isAutoReloadEnabled());
    }

    @Test
    public void testEnableAutoReloadWithCustomDebounce() throws Exception {
        ConfigReloadManager manager = ConfigReloadManager.getInstance();
        manager.enableAutoReload(500);
        assertTrue(manager.isAutoReloadEnabled());
    }

    @Test
    public void testConfigChangeEventProperties() {
        java.util.Set<String> changedKeys = java.util.Collections.singleton("app.name");
        java.util.Map<String, String> oldValues = java.util.Collections.singletonMap("app.name", "old");
        java.util.Map<String, String> newValues = java.util.Collections.singletonMap("app.name", "new");
        String source = "test-source";
        
        ConfigChangeListener.ConfigChangeEvent event = 
            new ConfigChangeListener.ConfigChangeEvent(changedKeys, oldValues, newValues, source);
        
        assertEquals(event.getChangedKeys(), changedKeys);
        assertTrue(event.hasChanged("app.name"));
        assertFalse(event.hasChanged("other.key"));
        assertEquals(event.getOldValue("app.name"), "old");
        assertEquals(event.getNewValue("app.name"), "new");
        assertEquals(event.getSource(), source);
        assertTrue(event.getTimestamp() > 0);
    }

    @Test
    public void testConfigChangeEventGetAllValues() {
        java.util.Map<String, String> oldValues = new java.util.HashMap<>();
        oldValues.put("key1", "old1");
        oldValues.put("key2", "old2");
        
        java.util.Map<String, String> newValues = new java.util.HashMap<>();
        newValues.put("key1", "new1");
        newValues.put("key2", "new2");
        
        ConfigChangeListener.ConfigChangeEvent event = 
            new ConfigChangeListener.ConfigChangeEvent(oldValues.keySet(), oldValues, newValues, "source");
        
        assertEquals(event.getOldValues(), oldValues);
        assertEquals(event.getNewValues(), newValues);
    }
}

