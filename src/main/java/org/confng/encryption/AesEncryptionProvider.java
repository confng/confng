package org.confng.encryption;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption provider for secure configuration values.
 *
 * <p>This provider uses AES-256 in GCM mode, which provides both confidentiality
 * and authenticity. The IV is prepended to the ciphertext for storage.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Create provider with a 256-bit key
 * byte[] keyBytes = ... // 32 bytes for AES-256
 * AesEncryptionProvider provider = new AesEncryptionProvider(keyBytes);
 * 
 * // Or from Base64 encoded key
 * AesEncryptionProvider provider = AesEncryptionProvider.fromBase64Key("base64EncodedKey");
 * 
 * // Or from environment variable
 * AesEncryptionProvider provider = AesEncryptionProvider.fromEnvironment();
 * 
 * // Register with ConfNG
 * ConfNG.setEncryptionProvider(provider);
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
public class AesEncryptionProvider implements EncryptionProvider {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ENV_KEY_NAME = "CONFNG_ENCRYPTION_KEY";
    
    private final SecretKey secretKey;
    private final SecureRandom secureRandom;
    
    /**
     * Creates a new AES encryption provider with the given key bytes.
     *
     * @param keyBytes the AES key (must be 16, 24, or 32 bytes for AES-128/192/256)
     */
    public AesEncryptionProvider(byte[] keyBytes) {
        if (keyBytes == null || (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32)) {
            throw new IllegalArgumentException("AES key must be 16, 24, or 32 bytes");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Creates a new AES encryption provider with the given SecretKey.
     *
     * @param secretKey the AES secret key
     */
    public AesEncryptionProvider(SecretKey secretKey) {
        if (secretKey == null) {
            throw new IllegalArgumentException("Secret key cannot be null");
        }
        this.secretKey = secretKey;
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Creates an AES encryption provider from a Base64-encoded key.
     *
     * @param base64Key the Base64-encoded key
     * @return the encryption provider
     */
    public static AesEncryptionProvider fromBase64Key(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new AesEncryptionProvider(keyBytes);
    }
    
    /**
     * Creates an AES encryption provider from the CONFNG_ENCRYPTION_KEY environment variable.
     *
     * @return the encryption provider
     * @throws EncryptionException if the environment variable is not set
     */
    public static AesEncryptionProvider fromEnvironment() {
        String keyBase64 = System.getenv(ENV_KEY_NAME);
        if (keyBase64 == null || keyBase64.isEmpty()) {
            throw new EncryptionException("Environment variable " + ENV_KEY_NAME + " is not set");
        }
        return fromBase64Key(keyBase64);
    }
    
    /**
     * Creates an AES encryption provider from a system property or environment variable.
     *
     * @param propertyName the system property or environment variable name
     * @return the encryption provider
     */
    public static AesEncryptionProvider fromProperty(String propertyName) {
        String keyBase64 = System.getProperty(propertyName);
        if (keyBase64 == null || keyBase64.isEmpty()) {
            keyBase64 = System.getenv(propertyName);
        }
        if (keyBase64 == null || keyBase64.isEmpty()) {
            throw new EncryptionException("Property/environment variable " + propertyName + " is not set");
        }
        return fromBase64Key(keyBase64);
    }
    
    @Override
    public String decrypt(String encryptedValue) throws EncryptionException {
        try {
            String payload = extractPayload(encryptedValue);
            byte[] decoded = Base64.getDecoder().decode(payload);
            
            // Extract IV and ciphertext
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            
            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            return new String(plaintext);
        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt value", e);
        }
    }
    
    @Override
    public String encrypt(String plainValue) throws EncryptionException {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Encrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] ciphertext = cipher.doFinal(plainValue.getBytes());
            
            // Combine IV and ciphertext
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            
            // Encode and wrap
            return "ENC(" + Base64.getEncoder().encodeToString(buffer.array()) + ")";
        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt value", e);
        }
    }
    
    @Override
    public String getName() {
        return "AES-GCM";
    }
}

