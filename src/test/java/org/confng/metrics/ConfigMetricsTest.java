package org.confng.metrics;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ConfigMetrics}.
 */
public class ConfigMetricsTest {

    private ConfigMetrics metrics;

    @BeforeMethod
    public void setUp() {
        metrics = ConfigMetrics.getInstance();
        metrics.reset();
        metrics.setEnabled(true);
    }

    @AfterMethod
    public void tearDown() {
        metrics.reset();
    }

    @Test(description = "Should get singleton instance")
    public void shouldGetSingletonInstance() {
        ConfigMetrics instance1 = ConfigMetrics.getInstance();
        ConfigMetrics instance2 = ConfigMetrics.getInstance();
        assertSame(instance1, instance2);
    }

    @Test(description = "Should track lookup counts")
    public void shouldTrackLookupCounts() {
        assertEquals(metrics.getTotalLookups(), 0);
        
        metrics.recordLookup("test.key", false, false);
        assertEquals(metrics.getTotalLookups(), 1);
        
        metrics.recordLookup("test.key2", false, false);
        metrics.recordLookup("test.key3", false, false);
        assertEquals(metrics.getTotalLookups(), 3);
    }

    @Test(description = "Should track cache hits")
    public void shouldTrackCacheHits() {
        assertEquals(metrics.getCacheHits(), 0);
        
        metrics.recordLookup("test.key", true, false);
        assertEquals(metrics.getCacheHits(), 1);
        
        metrics.recordLookup("test.key", true, false);
        assertEquals(metrics.getCacheHits(), 2);
    }

    @Test(description = "Should track cache misses")
    public void shouldTrackCacheMisses() {
        assertEquals(metrics.getCacheMisses(), 0);
        
        metrics.recordLookup("test.key", false, false);
        assertEquals(metrics.getCacheMisses(), 1);
    }

    @Test(description = "Should track default value usage")
    public void shouldTrackDefaultValueUsage() {
        assertEquals(metrics.getDefaultValueUsed(), 0);
        
        metrics.recordLookup("test.key", false, true);
        assertEquals(metrics.getDefaultValueUsed(), 1);
    }

    @Test(description = "Should track decryption attempts")
    public void shouldTrackDecryptionAttempts() {
        assertEquals(metrics.getDecryptionAttempts(), 0);
        
        metrics.recordDecryption(true);
        assertEquals(metrics.getDecryptionAttempts(), 1);
    }

    @Test(description = "Should track decryption failures")
    public void shouldTrackDecryptionFailures() {
        assertEquals(metrics.getDecryptionFailures(), 0);
        
        metrics.recordDecryption(false);
        assertEquals(metrics.getDecryptionAttempts(), 1);
        assertEquals(metrics.getDecryptionFailures(), 1);
    }

    @Test(description = "Should calculate cache hit rate correctly")
    public void shouldCalculateCacheHitRateCorrectly() {
        // No lookups yet
        assertEquals(metrics.getCacheHitRate(), 0.0, 0.001);
        
        // 3 hits, 1 miss = 75% hit ratio
        metrics.recordLookup("key1", true, false);
        metrics.recordLookup("key2", true, false);
        metrics.recordLookup("key3", true, false);
        metrics.recordLookup("key4", false, false);
        
        assertEquals(metrics.getCacheHitRate(), 0.75, 0.001);
    }

    @Test(description = "Should track per-key lookup counts")
    public void shouldTrackPerKeyLookupCounts() {
        metrics.recordLookup("key1", false, false);
        metrics.recordLookup("key1", false, false);
        metrics.recordLookup("key2", false, false);
        
        Map<String, Long> keyCounts = metrics.getKeyLookupCounts();
        assertEquals(keyCounts.get("key1").longValue(), 2);
        assertEquals(keyCounts.get("key2").longValue(), 1);
    }

    @Test(description = "Should track source lookup counts")
    public void shouldTrackSourceLookupCounts() {
        metrics.recordSourceLookup("EnvSource");
        metrics.recordSourceLookup("EnvSource");
        metrics.recordSourceLookup("PropertiesSource");
        
        Map<String, Long> sourceCounts = metrics.getSourceLookupCounts();
        assertEquals(sourceCounts.get("EnvSource").longValue(), 2);
        assertEquals(sourceCounts.get("PropertiesSource").longValue(), 1);
    }

    @Test(description = "Should reset all metrics")
    public void shouldResetAllMetrics() {
        metrics.recordLookup("key", true, true);
        metrics.recordDecryption(false);
        metrics.recordSourceLookup("source");
        
        metrics.reset();
        
        assertEquals(metrics.getTotalLookups(), 0);
        assertEquals(metrics.getCacheHits(), 0);
        assertEquals(metrics.getCacheMisses(), 0);
        assertEquals(metrics.getDefaultValueUsed(), 0);
        assertEquals(metrics.getDecryptionAttempts(), 0);
        assertEquals(metrics.getDecryptionFailures(), 0);
        assertTrue(metrics.getKeyLookupCounts().isEmpty());
        assertTrue(metrics.getSourceLookupCounts().isEmpty());
    }

    @Test(description = "Should generate JSON output")
    public void shouldGenerateJsonOutput() {
        metrics.recordLookup("key", true, false);
        
        String json = metrics.toJson();
        
        assertNotNull(json);
        assertTrue(json.contains("\"totalLookups\""));
        assertTrue(json.contains("\"cacheHits\""));
        assertTrue(json.contains("\"cacheHitRate\""));
    }

    @Test(description = "Should track uptime")
    public void shouldTrackUptime() {
        long uptime = metrics.getUptimeMs();
        assertTrue(uptime >= 0);
    }

    @Test(description = "Should not record when disabled")
    public void shouldNotRecordWhenDisabled() {
        metrics.setEnabled(false);
        
        metrics.recordLookup("key", true, false);
        assertEquals(metrics.getTotalLookups(), 0);
    }
}

