package org.confng.metrics;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks configuration access metrics including lookup counts, cache hits, and timing.
 *
 * <p>This class provides observability into configuration usage patterns, which can
 * help identify frequently accessed keys, cache effectiveness, and potential issues.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Enable metrics collection
 * ConfigMetrics.getInstance().setEnabled(true);
 * 
 * // Get metrics
 * long lookups = ConfigMetrics.getInstance().getTotalLookups();
 * long cacheHits = ConfigMetrics.getInstance().getCacheHits();
 * double hitRate = ConfigMetrics.getInstance().getCacheHitRate();
 * 
 * // Get per-key metrics
 * Map<String, Long> keyLookups = ConfigMetrics.getInstance().getKeyLookupCounts();
 * 
 * // Export metrics
 * String json = ConfigMetrics.getInstance().toJson();
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Slf4j
public class ConfigMetrics {
    
    private static ConfigMetrics instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    private volatile boolean enabled = false;
    
    private final AtomicLong totalLookups = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong defaultValueUsed = new AtomicLong(0);
    private final AtomicLong missingRequiredKeys = new AtomicLong(0);
    private final AtomicLong decryptionAttempts = new AtomicLong(0);
    private final AtomicLong decryptionFailures = new AtomicLong(0);
    
    private final Map<String, AtomicLong> keyLookupCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> sourceLookupCounts = new ConcurrentHashMap<>();
    
    private volatile long startTimeMs = System.currentTimeMillis();
    
    private ConfigMetrics() {}
    
    /**
     * Gets the singleton instance.
     */
    public static ConfigMetrics getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new ConfigMetrics();
                }
            }
        }
        return instance;
    }
    
    /**
     * Enables or disables metrics collection.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            log.info("ConfigMetrics enabled");
        }
    }
    
    /**
     * Checks if metrics collection is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Records a configuration lookup.
     */
    public void recordLookup(String key, boolean cacheHit, boolean usedDefault) {
        if (!enabled) return;
        
        totalLookups.incrementAndGet();
        
        if (cacheHit) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
        
        if (usedDefault) {
            defaultValueUsed.incrementAndGet();
        }
        
        keyLookupCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Records a source lookup.
     */
    public void recordSourceLookup(String sourceName) {
        if (!enabled) return;
        sourceLookupCounts.computeIfAbsent(sourceName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Records a missing required key.
     */
    public void recordMissingRequired(String key) {
        if (!enabled) return;
        missingRequiredKeys.incrementAndGet();
        log.warn("Missing required configuration key: {}", key);
    }
    
    /**
     * Records a decryption attempt.
     */
    public void recordDecryption(boolean success) {
        if (!enabled) return;
        decryptionAttempts.incrementAndGet();
        if (!success) {
            decryptionFailures.incrementAndGet();
        }
    }
    
    // Getters
    public long getTotalLookups() { return totalLookups.get(); }
    public long getCacheHits() { return cacheHits.get(); }
    public long getCacheMisses() { return cacheMisses.get(); }
    public long getDefaultValueUsed() { return defaultValueUsed.get(); }
    public long getMissingRequiredKeys() { return missingRequiredKeys.get(); }
    public long getDecryptionAttempts() { return decryptionAttempts.get(); }
    public long getDecryptionFailures() { return decryptionFailures.get(); }
    
    public double getCacheHitRate() {
        long total = totalLookups.get();
        return total > 0 ? (double) cacheHits.get() / total : 0.0;
    }
    
    public Map<String, Long> getKeyLookupCounts() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        keyLookupCounts.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    
    public Map<String, Long> getSourceLookupCounts() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        sourceLookupCounts.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    
    public long getUptimeMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    /**
     * Exports metrics as JSON string.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"totalLookups\": ").append(totalLookups.get()).append(",\n");
        sb.append("  \"cacheHits\": ").append(cacheHits.get()).append(",\n");
        sb.append("  \"cacheMisses\": ").append(cacheMisses.get()).append(",\n");
        sb.append("  \"cacheHitRate\": ").append(String.format("%.4f", getCacheHitRate())).append(",\n");
        sb.append("  \"defaultValueUsed\": ").append(defaultValueUsed.get()).append(",\n");
        sb.append("  \"missingRequiredKeys\": ").append(missingRequiredKeys.get()).append(",\n");
        sb.append("  \"decryptionAttempts\": ").append(decryptionAttempts.get()).append(",\n");
        sb.append("  \"decryptionFailures\": ").append(decryptionFailures.get()).append(",\n");
        sb.append("  \"uptimeMs\": ").append(getUptimeMs()).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Resets all metrics.
     */
    public void reset() {
        totalLookups.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        defaultValueUsed.set(0);
        missingRequiredKeys.set(0);
        decryptionAttempts.set(0);
        decryptionFailures.set(0);
        keyLookupCounts.clear();
        sourceLookupCounts.clear();
        startTimeMs = System.currentTimeMillis();
        log.info("ConfigMetrics reset");
    }

    /**
     * Logs a summary of current metrics.
     */
    public void logSummary() {
        log.info("ConfigMetrics Summary:");
        log.info("  Total Lookups: {}", totalLookups.get());
        log.info("  Cache Hit Rate: {}%", String.format("%.2f", getCacheHitRate() * 100));
        log.info("  Default Values Used: {}", defaultValueUsed.get());
        log.info("  Missing Required Keys: {}", missingRequiredKeys.get());
        log.info("  Uptime: {} ms", getUptimeMs());
    }

    /**
     * Gets the lookup count for a specific key.
     *
     * @param key the configuration key
     * @return the number of times this key was looked up
     */
    public long getLookupCount(String key) {
        AtomicLong count = keyLookupCounts.get(key);
        return count != null ? count.get() : 0;
    }

    /**
     * Gets the lookup count for a specific source.
     *
     * @param sourceName the source name
     * @return the number of times this source was queried
     */
    public long getSourceLookupCount(String sourceName) {
        AtomicLong count = sourceLookupCounts.get(sourceName);
        return count != null ? count.get() : 0;
    }
}
