package org.confng.reload;

import lombok.extern.slf4j.Slf4j;
import org.confng.ConfNG;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages configuration reloading and change notifications.
 *
 * <p>This class coordinates file watching, configuration reloading, and
 * notifying listeners when configuration values change.</p>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Slf4j
public class ConfigReloadManager implements AutoCloseable {
    
    private static ConfigReloadManager instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    private final List<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final Set<String> loadedFiles = new LinkedHashSet<>();
    private FileWatchService watchService;
    private volatile boolean autoReloadEnabled = false;
    private long debounceMs = 1000;
    
    private ConfigReloadManager() {}
    
    /**
     * Gets the singleton instance of the reload manager.
     */
    public static ConfigReloadManager getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new ConfigReloadManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Registers a file that was loaded for potential watching.
     */
    public void registerLoadedFile(String filePath) {
        loadedFiles.add(filePath);
        if (autoReloadEnabled && watchService != null) {
            try {
                watchService.watchFile(Paths.get(filePath));
            } catch (IOException e) {
                log.warn("Failed to watch file: {}", filePath, e);
            }
        }
    }
    
    /**
     * Adds a configuration change listener.
     */
    public void addChangeListener(ConfigChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a configuration change listener.
     */
    public void removeChangeListener(ConfigChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Enables automatic reloading when configuration files change.
     */
    public void enableAutoReload() throws IOException {
        enableAutoReload(debounceMs);
    }
    
    /**
     * Enables automatic reloading with custom debounce time.
     *
     * @param debounceMs minimum time between reloads for the same file
     */
    public void enableAutoReload(long debounceMs) throws IOException {
        if (autoReloadEnabled) {
            return;
        }
        
        this.debounceMs = debounceMs;
        this.watchService = new FileWatchService(this::onFileChanged, debounceMs);
        
        // Watch all previously loaded files
        for (String filePath : loadedFiles) {
            try {
                Path path = Paths.get(filePath);
                if (path.toFile().exists()) {
                    watchService.watchFile(path);
                }
            } catch (IOException e) {
                log.warn("Failed to watch file: {}", filePath, e);
            }
        }
        
        watchService.start();
        autoReloadEnabled = true;
        log.info("Auto-reload enabled for {} files", loadedFiles.size());
    }
    
    /**
     * Disables automatic reloading.
     */
    public void disableAutoReload() {
        if (!autoReloadEnabled) {
            return;
        }
        
        if (watchService != null) {
            watchService.close();
            watchService = null;
        }
        
        autoReloadEnabled = false;
        log.info("Auto-reload disabled");
    }
    
    /**
     * Checks if auto-reload is enabled.
     */
    public boolean isAutoReloadEnabled() {
        return autoReloadEnabled;
    }
    
    private void onFileChanged(Path changedFile) {
        log.info("Configuration file changed: {}", changedFile);
        
        // Capture old values
        Map<String, String> oldValues = captureCurrentValues();
        
        // Trigger refresh
        ConfNG.refresh();
        
        // Capture new values and compute diff
        Map<String, String> newValues = captureCurrentValues();
        Set<String> changedKeys = computeChangedKeys(oldValues, newValues);
        
        if (!changedKeys.isEmpty()) {
            notifyListeners(changedKeys, oldValues, newValues, changedFile.toString());
        }
    }
    
    private Map<String, String> captureCurrentValues() {
        Map<String, String> values = new HashMap<>();
        for (String key : ConfNG.getResolvedKeys()) {
            values.put(key, ConfNG.getByPrefix("").get(key));
        }
        return values;
    }

    private Set<String> computeChangedKeys(Map<String, String> oldValues, Map<String, String> newValues) {
        Set<String> changed = new HashSet<>();

        // Check for changed or new keys
        for (Map.Entry<String, String> entry : newValues.entrySet()) {
            String key = entry.getKey();
            String newValue = entry.getValue();
            String oldValue = oldValues.get(key);

            if (!Objects.equals(oldValue, newValue)) {
                changed.add(key);
            }
        }

        // Check for removed keys
        for (String key : oldValues.keySet()) {
            if (!newValues.containsKey(key)) {
                changed.add(key);
            }
        }

        return changed;
    }

    private void notifyListeners(Set<String> changedKeys, Map<String, String> oldValues,
                                  Map<String, String> newValues, String source) {
        ConfigChangeListener.ConfigChangeEvent event =
                new ConfigChangeListener.ConfigChangeEvent(changedKeys, oldValues, newValues, source);

        for (ConfigChangeListener listener : listeners) {
            try {
                listener.onConfigChange(event);
            } catch (Exception e) {
                log.error("Error notifying config change listener", e);
            }
        }
    }

    /**
     * Manually triggers a reload and notifies listeners.
     */
    public void triggerReload(String source) {
        Map<String, String> oldValues = captureCurrentValues();
        ConfNG.refresh();
        Map<String, String> newValues = captureCurrentValues();
        Set<String> changedKeys = computeChangedKeys(oldValues, newValues);

        if (!changedKeys.isEmpty()) {
            notifyListeners(changedKeys, oldValues, newValues, source);
        }
    }

    @Override
    public void close() {
        disableAutoReload();
        listeners.clear();
        loadedFiles.clear();
    }

    /**
     * Resets the singleton instance. Useful for testing.
     */
    public static void reset() {
        synchronized (INSTANCE_LOCK) {
            if (instance != null) {
                instance.close();
                instance = null;
            }
        }
    }
}

