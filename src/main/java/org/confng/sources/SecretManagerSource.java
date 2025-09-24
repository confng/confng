package org.confng.sources;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base class for secret manager implementations.
 * 
 * <p>This class provides common functionality for secret manager sources
 * such as caching, key mapping, and error handling.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 * @see org.confng.sources.ConfigSource
 */
public abstract class SecretManagerSource implements ConfigSource {
    
    protected final Map<String, String> keyMappings = new HashMap<>();
    protected final Map<String, String> secretCache = new HashMap<>();
    protected final boolean cacheEnabled;
    protected final long cacheTimeoutMs;
    protected final Map<String, Long> cacheTimestamps = new HashMap<>();
    
    /**
     * Creates a new secret manager source with caching enabled.
     * 
     * @param cacheTimeoutMs cache timeout in milliseconds (0 = no timeout)
     */
    protected SecretManagerSource(long cacheTimeoutMs) {
        this.cacheEnabled = true;
        this.cacheTimeoutMs = cacheTimeoutMs;
    }
    
    /**
     * Creates a new secret manager source with caching disabled.
     */
    protected SecretManagerSource() {
        this.cacheEnabled = false;
        this.cacheTimeoutMs = 0;
    }
    
    /**
     * Maps a configuration key to a secret identifier.
     * 
     * @param configKey the configuration key
     * @param secretId the secret identifier in the secret manager
     */
    public void addKeyMapping(String configKey, String secretId) {
        keyMappings.put(configKey, secretId);
    }
    
    @Override
    public Optional<String> get(String key) {
        String secretId = keyMappings.get(key);
        if (secretId == null) {
            return Optional.empty();
        }
        
        try {
            // Check cache if enabled
            if (cacheEnabled) {
                String cachedValue = getCachedSecret(secretId);
                if (cachedValue != null) {
                    return Optional.of(cachedValue);
                }
            }
            
            // Fetch from secret manager
            String secretValue = fetchSecret(secretId);
            if (secretValue != null) {
                // Cache the value if caching is enabled
                if (cacheEnabled) {
                    cacheSecret(secretId, secretValue);
                }
                return Optional.of(secretValue);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            handleSecretFetchError(key, secretId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Fetches a secret from the secret manager.
     * 
     * @param secretId the secret identifier
     * @return the secret value, or null if not found
     * @throws Exception if an error occurs while fetching the secret
     */
    protected abstract String fetchSecret(String secretId) throws Exception;
    
    /**
     * Handles errors that occur while fetching secrets.
     * 
     * @param configKey the configuration key that was requested
     * @param secretId the secret identifier
     * @param error the error that occurred
     */
    protected void handleSecretFetchError(String configKey, String secretId, Exception error) {
        System.err.println("Failed to fetch secret for key '" + configKey + 
                          "' (secretId: " + secretId + "): " + error.getMessage());
    }
    
    /**
     * Gets a cached secret value if it exists and hasn't expired.
     */
    private String getCachedSecret(String secretId) {
        if (!secretCache.containsKey(secretId)) {
            return null;
        }
        
        // Check if cache has expired
        if (cacheTimeoutMs > 0) {
            Long timestamp = cacheTimestamps.get(secretId);
            if (timestamp != null && System.currentTimeMillis() - timestamp > cacheTimeoutMs) {
                // Cache expired, remove it
                secretCache.remove(secretId);
                cacheTimestamps.remove(secretId);
                return null;
            }
        }
        
        return secretCache.get(secretId);
    }
    
    /**
     * Caches a secret value.
     */
    private void cacheSecret(String secretId, String value) {
        secretCache.put(secretId, value);
        if (cacheTimeoutMs > 0) {
            cacheTimestamps.put(secretId, System.currentTimeMillis());
        }
    }
    
    /**
     * Clears the secret cache.
     */
    public void clearCache() {
        secretCache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * Gets the number of cached secrets.
     */
    public int getCacheSize() {
        return secretCache.size();
    }

    @Override
    public int getPriority() {
        return 100; // Highest priority for secret managers
    }
}
