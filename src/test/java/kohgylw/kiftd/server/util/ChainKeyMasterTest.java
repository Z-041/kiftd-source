package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import kohgylw.kiftd.printer.Printer;

class ChainKeyMasterTest {

    @BeforeAll
    static void initPrinter() {
        Printer.init(false);
    }

    @Test
    void testWrapAndUnwrapRoundTrip() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        String originalKey = "test-aes-key-12345";
        String wrapped = master.wrap(originalKey);
        assertNotNull(wrapped);
        assertTrue(wrapped.startsWith("ENC:"));
        String unwrapped = master.unwrap(wrapped);
        assertEquals(originalKey, unwrapped);
    }

    @Test
    void testWrapNullReturnsNull() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        assertNull(master.wrap(null));
    }

    @Test
    void testUnwrapNullReturnsNull() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        assertNull(master.unwrap(null));
    }

    @Test
    void testUnwrapPlaintextKeyReturnsAsIs() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        String plainKey = "plain-text-key";
        assertEquals(plainKey, master.unwrap(plainKey));
    }

    @Test
    void testIsWrappedWithWrappedKey() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        String wrapped = master.wrap("test-key");
        assertTrue(master.isWrapped(wrapped));
    }

    @Test
    void testIsWrappedWithPlainKey() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        assertFalse(master.isWrapped("plain-key"));
    }

    @Test
    void testIsWrappedWithNull() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        assertFalse(master.isWrapped(null));
    }

    @Test
    void testWrapEmptyString() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        String wrapped = master.wrap("");
        assertNotNull(wrapped);
        assertTrue(wrapped.startsWith("ENC:"));
        String unwrapped = master.unwrap(wrapped);
        assertEquals("", unwrapped);
    }

    @Test
    void testWrapLongKey() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("k");
        }
        String longKey = sb.toString();
        String wrapped = master.wrap(longKey);
        assertNotNull(wrapped);
        assertTrue(wrapped.startsWith("ENC:"));
        String unwrapped = master.unwrap(wrapped);
        assertEquals(longKey, unwrapped);
    }

    @Test
    void testWrapSpecialChars() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        String specialKey = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String wrapped = master.wrap(specialKey);
        assertNotNull(wrapped);
        String unwrapped = master.unwrap(wrapped);
        assertEquals(specialKey, unwrapped);
    }

    @Test
    void testWrapChineseChars() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        String chineseKey = "密钥测试";
        String wrapped = master.wrap(chineseKey);
        assertNotNull(wrapped);
        String unwrapped = master.unwrap(wrapped);
        assertEquals(chineseKey, unwrapped);
    }

    @Test
    void testUnwrapWithWrongMasterKeyThrowsException() {
        AESCipher cipher1 = new AESCipher();
        AESCipher cipher2 = new AESCipher();
        ChainKeyMaster master1 = new ChainKeyMaster(cipher1);
        ChainKeyMaster master2 = new ChainKeyMaster(cipher2);
        String wrapped = master1.wrap("test-key");
        assertThrows(RuntimeException.class, () -> {
            master2.unwrap(wrapped);
        });
    }

    @Test
    void testUnwrapWithCorruptedDataThrowsException() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        String corrupted = "ENC:corrupted-base64-data!!!";
        assertThrows(RuntimeException.class, () -> {
            master.unwrap(corrupted);
        });
    }

    @Test
    void testIsWrappedWithEmptyString() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        assertFalse(master.isWrapped(""));
    }

    @Test
    void testUnwrapEmptyString() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        assertEquals("", master.unwrap(""));
    }

    @Test
    void testWrapAndUnwrapMultipleTimesProducesDifferentOutput() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        String originalKey = "same-key";
        String wrapped1 = master.wrap(originalKey);
        String wrapped2 = master.wrap(originalKey);
        assertNotEquals(wrapped1, wrapped2, "AES-GCM uses random IV, so same key should produce different wrapped output");
        assertEquals(master.unwrap(wrapped1), master.unwrap(wrapped2));
    }

    @Test
    void testEachInstanceHasDifferentMasterKey() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master1 = new ChainKeyMaster(cipher);
        ChainKeyMaster master2 = new ChainKeyMaster(cipher);
        String wrappedBy1 = master1.wrap("test-key");
        assertThrows(RuntimeException.class, () -> {
            master2.unwrap(wrappedBy1);
        });
    }

    @Test
    void testWrapDoesNotReturnPlaintext() {
        AESCipher cipher = new AESCipher();
        ChainKeyMaster master = new ChainKeyMaster(cipher);
        String original = "my-secret-key";
        String wrapped = master.wrap(original);
        assertNotEquals(original, wrapped);
        assertFalse(wrapped.contains(original));
    }
}
