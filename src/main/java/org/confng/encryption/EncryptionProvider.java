package org.confng.encryption;

/**
 * Interface for encryption/decryption providers.
 *
 * <p>Implement this interface to provide custom encryption/decryption logic
 * for configuration values marked with {@link Encrypted}.</p>
 *
 * <h3>Example Implementation:</h3>
 * <pre>{@code
 * public class MyEncryptionProvider implements EncryptionProvider {
 *     private final SecretKey key;
 *     
 *     public MyEncryptionProvider(String keyBase64) {
 *         byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
 *         this.key = new SecretKeySpec(keyBytes, "AES");
 *     }
 *     
 *     @Override
 *     public String decrypt(String encryptedValue) throws EncryptionException {
 *         // Decryption logic
 *     }
 *     
 *     @Override
 *     public String encrypt(String plainValue) throws EncryptionException {
 *         // Encryption logic
 *     }
 * }
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see Encrypted
 * @see AesEncryptionProvider
 */
public interface EncryptionProvider {
    
    /**
     * Decrypts an encrypted value.
     *
     * @param encryptedValue the encrypted value (typically Base64 encoded)
     * @return the decrypted plain text value
     * @throws EncryptionException if decryption fails
     */
    String decrypt(String encryptedValue) throws EncryptionException;
    
    /**
     * Encrypts a plain text value.
     *
     * @param plainValue the plain text value to encrypt
     * @return the encrypted value (typically Base64 encoded)
     * @throws EncryptionException if encryption fails
     */
    String encrypt(String plainValue) throws EncryptionException;
    
    /**
     * Gets the name of this encryption provider.
     *
     * @return the provider name
     */
    default String getName() {
        return getClass().getSimpleName();
    }
    
    /**
     * Checks if a value appears to be encrypted.
     *
     * <p>The default implementation checks for common encrypted value prefixes
     * like "ENC(" or "{cipher}".</p>
     *
     * @param value the value to check
     * @return true if the value appears to be encrypted
     */
    default boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.startsWith("ENC(") && value.endsWith(")") ||
               value.startsWith("{cipher}") ||
               value.startsWith("{aes}") ||
               value.startsWith("{encrypted}");
    }
    
    /**
     * Extracts the encrypted payload from a wrapped value.
     *
     * <p>For example, extracts "abc123" from "ENC(abc123)".</p>
     *
     * @param wrappedValue the wrapped encrypted value
     * @return the extracted payload
     */
    default String extractPayload(String wrappedValue) {
        if (wrappedValue.startsWith("ENC(") && wrappedValue.endsWith(")")) {
            return wrappedValue.substring(4, wrappedValue.length() - 1);
        }
        if (wrappedValue.startsWith("{cipher}")) {
            return wrappedValue.substring(8);
        }
        if (wrappedValue.startsWith("{aes}")) {
            return wrappedValue.substring(5);
        }
        if (wrappedValue.startsWith("{encrypted}")) {
            return wrappedValue.substring(11);
        }
        return wrappedValue;
    }
}

