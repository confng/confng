package org.confng.reload;

import java.util.Map;
import java.util.Set;

/**
 * Listener interface for receiving notifications when configuration values change.
 *
 * <p>Implement this interface to be notified when configuration files are reloaded
 * and values have changed. This is useful for long-running applications that need
 * to react to configuration changes without restarting.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * ConfNG.addChangeListener(new ConfigChangeListener() {
 *     @Override
 *     public void onConfigChange(ConfigChangeEvent event) {
 *         System.out.println("Config changed: " + event.getChangedKeys());
 *         if (event.hasChanged("database.pool.size")) {
 *             reconfigureConnectionPool(event.getNewValue("database.pool.size"));
 *         }
 *     }
 * });
 * 
 * // Enable auto-reload to watch for file changes
 * ConfNG.enableAutoReload();
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see ConfigChangeEvent
 * @see org.confng.ConfNG#addChangeListener(ConfigChangeListener)
 */
@FunctionalInterface
public interface ConfigChangeListener {
    
    /**
     * Called when configuration values have changed.
     *
     * @param event the change event containing details about what changed
     */
    void onConfigChange(ConfigChangeEvent event);
    
    /**
     * Event object containing details about configuration changes.
     */
    class ConfigChangeEvent {
        private final Set<String> changedKeys;
        private final Map<String, String> oldValues;
        private final Map<String, String> newValues;
        private final String source;
        private final long timestamp;
        
        public ConfigChangeEvent(Set<String> changedKeys, Map<String, String> oldValues, 
                                  Map<String, String> newValues, String source) {
            this.changedKeys = changedKeys;
            this.oldValues = oldValues;
            this.newValues = newValues;
            this.source = source;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Gets the set of keys that have changed.
         */
        public Set<String> getChangedKeys() {
            return changedKeys;
        }
        
        /**
         * Checks if a specific key has changed.
         */
        public boolean hasChanged(String key) {
            return changedKeys.contains(key);
        }
        
        /**
         * Gets the old value for a key before the change.
         */
        public String getOldValue(String key) {
            return oldValues.get(key);
        }
        
        /**
         * Gets the new value for a key after the change.
         */
        public String getNewValue(String key) {
            return newValues.get(key);
        }
        
        /**
         * Gets all old values.
         */
        public Map<String, String> getOldValues() {
            return oldValues;
        }
        
        /**
         * Gets all new values.
         */
        public Map<String, String> getNewValues() {
            return newValues;
        }
        
        /**
         * Gets the source that triggered the change (e.g., file path).
         */
        public String getSource() {
            return source;
        }
        
        /**
         * Gets the timestamp when the change was detected.
         */
        public long getTimestamp() {
            return timestamp;
        }
    }
}

