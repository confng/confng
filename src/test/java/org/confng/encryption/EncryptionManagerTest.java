package org.confng.encryption;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link EncryptionManager} and {@link AesEncryptionProvider}.
 */
public class EncryptionManagerTest {

    private EncryptionManager encryptionManager;
    private AesEncryptionProvider aesProvider;
    // 32 bytes for AES-256
    private static final byte[] TEST_KEY_BYTES = "12345678901234567890123456789012".getBytes();

    @BeforeMethod
    public void setUp() {
        EncryptionManager.reset();
        encryptionManager = EncryptionManager.getInstance();
        aesProvider = new AesEncryptionProvider(TEST_KEY_BYTES);
        encryptionManager.setDefaultProvider(aesProvider);
    }

    @AfterMethod
    public void tearDown() {
        EncryptionManager.reset();
    }

    @Test(description = "Should get singleton instance")
    public void shouldGetSingletonInstance() {
        EncryptionManager instance1 = EncryptionManager.getInstance();
        EncryptionManager instance2 = EncryptionManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test(description = "Should encrypt and decrypt value successfully using provider")
    public void shouldEncryptAndDecryptValueUsingProvider() {
        String originalValue = "mySecretPassword123";
        
        String encrypted = aesProvider.encrypt(originalValue);
        assertNotNull(encrypted);
        assertNotEquals(encrypted, originalValue);
        assertTrue(encrypted.startsWith("ENC("));
        
        String decrypted = aesProvider.decrypt(encrypted);
        assertEquals(decrypted, originalValue);
    }

    @Test(description = "Should produce different ciphertext for same plaintext")
    public void shouldProduceDifferentCiphertextForSamePlaintext() {
        String originalValue = "testValue";
        
        String encrypted1 = aesProvider.encrypt(originalValue);
        String encrypted2 = aesProvider.encrypt(originalValue);
        
        // Due to random IV, same plaintext should produce different ciphertext
        assertNotEquals(encrypted1, encrypted2);
        
        // But both should decrypt to the same value
        assertEquals(aesProvider.decrypt(encrypted1), originalValue);
        assertEquals(aesProvider.decrypt(encrypted2), originalValue);
    }

    @Test(description = "Should handle empty string")
    public void shouldHandleEmptyString() {
        String encrypted = aesProvider.encrypt("");
        String decrypted = aesProvider.decrypt(encrypted);
        assertEquals(decrypted, "");
    }

    @Test(description = "Should handle special characters")
    public void shouldHandleSpecialCharacters() {
        String originalValue = "p@$$w0rd!#$%^&*()_+-=[]{}|;':,./<>?";
        
        String encrypted = aesProvider.encrypt(originalValue);
        String decrypted = aesProvider.decrypt(encrypted);
        
        assertEquals(decrypted, originalValue);
    }

    @Test(description = "Should fail decryption with wrong key", expectedExceptions = EncryptionException.class)
    public void shouldFailDecryptionWithWrongKey() {
        String originalValue = "secretData";
        String encrypted = aesProvider.encrypt(originalValue);
        
        // Create a different provider with different key
        byte[] wrongKey = "abcdefghijklmnopqrstuvwxyz123456".getBytes();
        AesEncryptionProvider wrongProvider = new AesEncryptionProvider(wrongKey);
        wrongProvider.decrypt(encrypted);
    }

    @Test(description = "Should detect encrypted values with prefix")
    public void shouldDetectEncryptedValuesWithPrefix() {
        String plainValue = "plaintext";
        String encryptedValue = aesProvider.encrypt("secret");
        
        assertFalse(aesProvider.isEncrypted(plainValue));
        assertTrue(aesProvider.isEncrypted(encryptedValue));
    }

    @Test(description = "Should handle long values")
    public void shouldHandleLongValues() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("LongValue");
        }
        String originalValue = sb.toString();
        
        String encrypted = aesProvider.encrypt(originalValue);
        String decrypted = aesProvider.decrypt(encrypted);
        
        assertEquals(decrypted, originalValue);
    }

    @Test(description = "Should register and get named provider")
    public void shouldRegisterAndGetNamedProvider() {
        encryptionManager.registerProvider("custom", aesProvider);
        
        EncryptionProvider retrieved = encryptionManager.getProvider("custom");
        assertSame(retrieved, aesProvider);
    }

    @Test(description = "Should get default provider when name is empty")
    public void shouldGetDefaultProviderWhenNameIsEmpty() {
        EncryptionProvider retrieved = encryptionManager.getProvider("");
        assertSame(retrieved, aesProvider);
    }

    @Test(description = "Should encrypt using default provider via manager")
    public void shouldEncryptUsingDefaultProviderViaManager() {
        String originalValue = "testValue";
        
        String encrypted = encryptionManager.encrypt(originalValue);
        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC("));
    }

    @Test(description = "Should create provider from Base64 key")
    public void shouldCreateProviderFromBase64Key() {
        String base64Key = java.util.Base64.getEncoder().encodeToString(TEST_KEY_BYTES);
        AesEncryptionProvider provider = AesEncryptionProvider.fromBase64Key(base64Key);
        
        assertNotNull(provider);
        assertEquals(provider.getName(), "AES-GCM");
    }

    @Test(description = "Should reject invalid key length", expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectInvalidKeyLength() {
        byte[] invalidKey = "short".getBytes();
        new AesEncryptionProvider(invalidKey);
    }
}

