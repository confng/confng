package org.confng.encryption;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark configuration keys that contain encrypted values.
 *
 * <p>When a configuration key is marked with this annotation, ConfNG will
 * automatically decrypt the value when it is retrieved.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * public enum MyConfig implements ConfNGKey {
 *     @Encrypted
 *     DATABASE_PASSWORD("db.password", null, true),
 *     
 *     @Encrypted(provider = "custom")
 *     API_SECRET("api.secret", null, true);
 *     
 *     // ... implementation
 * }
 * }</pre>
 *
 * <h3>Configuration File:</h3>
 * <pre>
 * # Values can be wrapped with ENC() or {cipher} prefix
 * db.password=ENC(aGVsbG8gd29ybGQ=)
 * api.secret={cipher}c29tZSBlbmNyeXB0ZWQgdmFsdWU=
 * </pre>
 *
 * <h3>Setting Up Encryption:</h3>
 * <pre>{@code
 * // Register the encryption provider before loading config
 * ConfNG.setEncryptionProvider(new AesEncryptionProvider(secretKey));
 * 
 * // Or use environment variable for the key
 * ConfNG.setEncryptionProvider(AesEncryptionProvider.fromEnvironment());
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 * @see EncryptionProvider
 * @see AesEncryptionProvider
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Encrypted {
    
    /**
     * The name of the encryption provider to use.
     *
     * <p>If not specified, the default provider registered with ConfNG will be used.</p>
     *
     * @return the provider name, or empty string for default
     */
    String provider() default "";
    
    /**
     * Whether to fail if decryption fails.
     *
     * <p>If true (default), a decryption failure will throw an exception.
     * If false, the original encrypted value will be returned.</p>
     *
     * @return true to fail on decryption error
     */
    boolean failOnError() default true;
}

