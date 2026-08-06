package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import kohgylw.kiftd.printer.Printer;

class RSAKeyUtilTest {

    @BeforeAll
    static void initPrinter() {
        Printer.init(false);
    }

    @Test
    void testGetPublicKeyNotNull() {
        RSAKeyUtil util = new RSAKeyUtil();
        String publicKey = util.getPublicKey();
        assertNotNull(publicKey);
        assertFalse(publicKey.isEmpty());
    }

    @Test
    void testGetPrivateKeyNotNull() {
        RSAKeyUtil util = new RSAKeyUtil();
        String privateKey = util.getPrivateKey();
        assertNotNull(privateKey);
        assertFalse(privateKey.isEmpty());
    }

    @Test
    void testPublicKeyIsValidBase64() {
        RSAKeyUtil util = new RSAKeyUtil();
        String publicKey = util.getPublicKey();
        assertDoesNotThrow(() -> {
            Base64.getDecoder().decode(publicKey);
        });
    }

    @Test
    void testPrivateKeyIsValidBase64() {
        RSAKeyUtil util = new RSAKeyUtil();
        String privateKey = util.getPrivateKey();
        assertDoesNotThrow(() -> {
            Base64.getDecoder().decode(privateKey);
        });
    }

    @Test
    void testPublicKeyIsDifferentFromPrivateKey() {
        RSAKeyUtil util = new RSAKeyUtil();
        String publicKey = util.getPublicKey();
        String privateKey = util.getPrivateKey();
        assertNotEquals(publicKey, privateKey);
    }

    @Test
    void testEachInstanceGeneratesNewKeys() {
        RSAKeyUtil util1 = new RSAKeyUtil();
        RSAKeyUtil util2 = new RSAKeyUtil();
        assertNotEquals(util1.getPublicKey(), util2.getPublicKey());
        assertNotEquals(util1.getPrivateKey(), util2.getPrivateKey());
    }

    @Test
    void testPublicKeyLengthReasonable() {
        RSAKeyUtil util = new RSAKeyUtil();
        String publicKey = util.getPublicKey();
        assertTrue(publicKey.length() > 300, "2048-bit RSA public key should be longer than 300 chars in Base64");
    }

    @Test
    void testPrivateKeyLengthReasonable() {
        RSAKeyUtil util = new RSAKeyUtil();
        String privateKey = util.getPrivateKey();
        assertTrue(privateKey.length() > 1000, "2048-bit RSA private key should be longer than 1000 chars in Base64");
    }

    @Test
    void testKeysCanBeUsedForEncryptionDecryption() throws Exception {
        RSAKeyUtil util = new RSAKeyUtil();
        String publicKeyStr = util.getPublicKey();
        String privateKeyStr = util.getPrivateKey();
        String original = "test-password-123";
        String encrypted = encryptWithPublicKey(original, publicKeyStr);
        assertNotNull(encrypted);
        String decrypted = RSADecryptUtil.dncryption(encrypted, privateKeyStr);
        assertEquals(original, decrypted);
    }

    private String encryptWithPublicKey(String data, String publicKeyStr) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] keyBytes = decoder.decode(publicKeyStr);
        java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(keyBytes);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        java.security.PublicKey publicKey = keyFactory.generatePublic(spec);
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
}
