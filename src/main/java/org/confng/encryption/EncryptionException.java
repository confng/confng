package org.confng.encryption;

/**
 * Exception thrown when encryption or decryption operations fail.
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
public class EncryptionException extends RuntimeException {
    
    public EncryptionException(String message) {
        super(message);
    }
    
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public EncryptionException(Throwable cause) {
        super(cause);
    }
}

