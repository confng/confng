package org.confng.encryption;

import lombok.extern.slf4j.Slf4j;
import org.confng.api.ConfNGKey;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages encryption providers and handles decryption of configuration values.
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Slf4j
public class EncryptionManager {
    
    private static EncryptionManager instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    private EncryptionProvider defaultProvider;
    private final Map<String, EncryptionProvider> namedProviders = new ConcurrentHashMap<>();
    private final Map<String, Encrypted> encryptedKeyCache = new ConcurrentHashMap<>();
    
    private EncryptionManager() {}
    
    /**
     * Gets the singleton instance.
     */
    public static EncryptionManager getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new EncryptionManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Sets the default encryption provider.
     */
    public void setDefaultProvider(EncryptionProvider provider) {
        this.defaultProvider = provider;
        log.info("Default encryption provider set: {}", provider != null ? provider.getName() : "null");
    }
    
    /**
     * Gets the default encryption provider.
     */
    public EncryptionProvider getDefaultProvider() {
        return defaultProvider;
    }
    
    /**
     * Registers a named encryption provider.
     */
    public void registerProvider(String name, EncryptionProvider provider) {
        namedProviders.put(name, provider);
        log.info("Registered encryption provider: {}", name);
    }
    
    /**
     * Gets a named encryption provider.
     */
    public EncryptionProvider getProvider(String name) {
        if (name == null || name.isEmpty()) {
            return defaultProvider;
        }
        return namedProviders.get(name);
    }
    
    /**
     * Decrypts a value if it appears to be encrypted and the key is marked with @Encrypted.
     *
     * @param key the configuration key
     * @param value the value to potentially decrypt
     * @return the decrypted value, or the original value if not encrypted
     */
    public String decryptIfNeeded(ConfNGKey key, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        Encrypted annotation = getEncryptedAnnotation(key);
        if (annotation == null) {
            // Check if value looks encrypted even without annotation
            if (defaultProvider != null && defaultProvider.isEncrypted(value)) {
                return decryptValue(value, defaultProvider, true);
            }
            return value;
        }
        
        EncryptionProvider provider = getProvider(annotation.provider());
        if (provider == null) {
            if (annotation.failOnError()) {
                throw new EncryptionException("No encryption provider available for key: " + key.getKey());
            }
            log.warn("No encryption provider for key {}, returning encrypted value", key.getKey());
            return value;
        }
        
        return decryptValue(value, provider, annotation.failOnError());
    }
    
    /**
     * Decrypts a value using the specified provider.
     */
    public String decryptValue(String value, EncryptionProvider provider, boolean failOnError) {
        if (!provider.isEncrypted(value)) {
            return value;
        }
        
        try {
            String decrypted = provider.decrypt(value);
            log.debug("Successfully decrypted value");
            return decrypted;
        } catch (Exception e) {
            if (failOnError) {
                throw new EncryptionException("Failed to decrypt value", e);
            }
            log.warn("Failed to decrypt value, returning original", e);
            return value;
        }
    }
    
    /**
     * Gets the @Encrypted annotation for a ConfNGKey if present.
     */
    private Encrypted getEncryptedAnnotation(ConfNGKey key) {
        if (key == null) {
            return null;
        }
        
        String cacheKey = key.getClass().getName() + "." + key.getKey();
        return encryptedKeyCache.computeIfAbsent(cacheKey, k -> {
            try {
                if (key.getClass().isEnum()) {
                    Field field = key.getClass().getField(((Enum<?>) key).name());
                    return field.getAnnotation(Encrypted.class);
                }
            } catch (NoSuchFieldException e) {
                log.debug("No field found for key: {}", key.getKey());
            }
            return null;
        });
    }
    
    /**
     * Encrypts a value using the default provider.
     */
    public String encrypt(String plainValue) {
        if (defaultProvider == null) {
            throw new EncryptionException("No default encryption provider configured");
        }
        return defaultProvider.encrypt(plainValue);
    }
    
    /**
     * Encrypts a value using a named provider.
     */
    public String encrypt(String plainValue, String providerName) {
        EncryptionProvider provider = getProvider(providerName);
        if (provider == null) {
            throw new EncryptionException("Encryption provider not found: " + providerName);
        }
        return provider.encrypt(plainValue);
    }
    
    /**
     * Resets the singleton instance. Useful for testing.
     */
    public static void reset() {
        synchronized (INSTANCE_LOCK) {
            if (instance != null) {
                instance.defaultProvider = null;
                instance.namedProviders.clear();
                instance.encryptedKeyCache.clear();
            }
        }
    }
}

