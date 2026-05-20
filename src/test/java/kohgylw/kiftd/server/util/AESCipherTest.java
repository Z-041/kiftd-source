package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class AESCipherTest {

    private final AESCipher cipher = new AESCipher();

    @Test
    void testGenerateRandomKeyReturnsBase64() {
        assertDoesNotThrow(() -> {
            String key = cipher.generateRandomKey();
            assertNotNull(key);
            assertFalse(key.isEmpty());
        });
    }

    @Test
    void testGenerateRandomKeyLength() throws Exception {
        String key = cipher.generateRandomKey();
        assertTrue(key.length() > 30,
                "AES-256 key encoded in Base64 should be 44 characters");
    }

    @Test
    void testGenerateRandomKeyUnique() throws Exception {
        String key1 = cipher.generateRandomKey();
        String key2 = cipher.generateRandomKey();
        assertNotEquals(key1, key2);
    }

    @Test
    void testEncryptAndDecryptRoundTrip() throws Exception {
        String key = cipher.generateRandomKey();
        String original = "file-id-12345";
        String encrypted = cipher.encrypt(key, original);
        String decrypted = cipher.decrypt(key, encrypted);

        assertEquals(original, decrypted);
    }

    @Test
    void testEncryptAndDecryptLongContent() throws Exception {
        String key = cipher.generateRandomKey();
        String original = "this-is-a-very-long-file-id-that-needs-encryption-for-security-purposes-1234567890";
        String encrypted = cipher.encrypt(key, original);
        String decrypted = cipher.decrypt(key, encrypted);

        assertEquals(original, decrypted);
    }

    @Test
    void testEncryptAndDecryptEmptyContent() throws Exception {
        String key = cipher.generateRandomKey();
        String original = "";
        String encrypted = cipher.encrypt(key, original);
        String decrypted = cipher.decrypt(key, encrypted);

        assertEquals(original, decrypted);
    }

    @Test
    void testEncryptProducesDifferentOutputEachTime() throws Exception {
        String key = cipher.generateRandomKey();
        String original = "same-content";
        String encrypted1 = cipher.encrypt(key, original);
        String encrypted2 = cipher.encrypt(key, original);

        assertNotEquals(encrypted1, encrypted2,
                "AES-GCM uses random IV, so same plaintext should produce different ciphertext");
    }

    @Test
    void testDecryptWithWrongKeyFails() {
        assertThrows(Exception.class, () -> {
            String key1 = cipher.generateRandomKey();
            String key2 = cipher.generateRandomKey();
            String encrypted = cipher.encrypt(key1, "secret-data");
            cipher.decrypt(key2, encrypted);
        });
    }

    @Test
    void testDecryptWithTamperedCiphertextFails() {
        assertThrows(Exception.class, () -> {
            String key = cipher.generateRandomKey();
            String encrypted = cipher.encrypt(key, "secret-data");
            String tampered = encrypted.substring(0, encrypted.length() - 1);
            cipher.decrypt(key, tampered);
        });
    }

}