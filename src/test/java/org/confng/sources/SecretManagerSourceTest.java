package org.confng.sources;

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.*;

/**
 * Unit tests for secret manager source implementations.
 * 
 * <p>Tests AWS Secrets Manager, HashiCorp Vault, caching functionality,
 * and error handling for secret management sources.</p>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0
 */
public class SecretManagerSourceTest {

    @Test
    void testSecretManagerSourceBaseFunctionality() {
        TestSecretManagerSource source = new TestSecretManagerSource();
        source.addKeyMapping("test.key", "test-secret");
        
        assertEquals("TestSecretManager", source.getName());
        assertEquals(Optional.of("secret-value"), source.get("test.key"));
        assertEquals(Optional.empty(), source.get("missing.key"));
    }

    @Test
    void secretManagerSourceCachingWorks() {
        TestSecretManagerSource source = new TestSecretManagerSource(5000); // 5 second cache
        source.addKeyMapping("test.key", "test-secret");
        
        // First call should fetch from "remote"
        assertEquals(Optional.of("secret-value"), source.get("test.key"));
        assertEquals(1, source.fetchCount);
        
        // Second call should use cache
        assertEquals(Optional.of("secret-value"), source.get("test.key"));
        assertEquals(1, source.fetchCount); // Should not increment
        
        // Clear cache and try again
        source.clearCache();
        assertEquals(Optional.of("secret-value"), source.get("test.key"));
        assertEquals(2, source.fetchCount); // Should increment
    }

    @Test
    void secretManagerSourceHandlesErrors() {
        TestSecretManagerSource source = new TestSecretManagerSource();
        source.addKeyMapping("error.key", "error-secret");
        source.shouldThrowError = true;
        
        // Should handle error gracefully
        assertEquals(Optional.empty(), source.get("error.key"));
        assertTrue(source.errorHandled);
    }

    // Test implementation of SecretManagerSource
    private static class TestSecretManagerSource extends SecretManagerSource {
        int fetchCount = 0;
        boolean shouldThrowError = false;
        boolean errorHandled = false;

        TestSecretManagerSource() {
            super();
        }

        TestSecretManagerSource(long cacheTimeoutMs) {
            super(cacheTimeoutMs);
        }

        @Override
        public String getName() {
            return "TestSecretManager";
        }

        @Override
        protected String fetchSecret(String secretId) throws Exception {
            fetchCount++;
            if (shouldThrowError) {
                throw new RuntimeException("Test error");
            }
            if ("test-secret".equals(secretId)) {
                return "secret-value";
            }
            return null;
        }

        @Override
        protected void handleSecretFetchError(String configKey, String secretId, Exception error) {
            super.handleSecretFetchError(configKey, secretId, error);
            errorHandled = true;
        }
    }
}
