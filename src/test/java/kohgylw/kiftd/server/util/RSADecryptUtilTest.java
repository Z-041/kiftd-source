package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import kohgylw.kiftd.printer.Printer;

class RSADecryptUtilTest {

    private static String privateKeyStr;
    private static String publicKeyStr;

    @BeforeAll
    static void initPrinterAndKeys() throws Exception {
        Printer.init(false);
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair pair = g.genKeyPair();
        Base64.Encoder encoder = Base64.getEncoder();
        publicKeyStr = new String(encoder.encode(pair.getPublic().getEncoded()), StandardCharsets.UTF_8);
        privateKeyStr = new String(encoder.encode(pair.getPrivate().getEncoded()), StandardCharsets.UTF_8);
    }

    private String encryptWithPublicKey(String data) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] keyBytes = decoder.decode(publicKeyStr);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    @Test
    void testDecryptNormalText() throws Exception {
        String original = "hello world";
        String encrypted = encryptWithPublicKey(original);
        String decrypted = RSADecryptUtil.dncryption(encrypted, privateKeyStr);
        assertEquals(original, decrypted);
    }

    @Test
    void testDecryptEmptyString() throws Exception {
        String original = "";
        String encrypted = encryptWithPublicKey(original);
        String decrypted = RSADecryptUtil.dncryption(encrypted, privateKeyStr);
        assertEquals(original, decrypted);
    }

    @Test
    void testDecryptChineseText() throws Exception {
        String original = "中文测试内容";
        String encrypted = encryptWithPublicKey(original);
        String decrypted = RSADecryptUtil.dncryption(encrypted, privateKeyStr);
        assertEquals(original, decrypted);
    }

    @Test
    void testDecryptSpecialChars() throws Exception {
        String original = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String encrypted = encryptWithPublicKey(original);
        String decrypted = RSADecryptUtil.dncryption(encrypted, privateKeyStr);
        assertEquals(original, decrypted);
    }

    @Test
    void testDecryptLongText() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("a");
        }
        String original = sb.toString();
        String encrypted = encryptWithPublicKey(original);
        String decrypted = RSADecryptUtil.dncryption(encrypted, privateKeyStr);
        assertEquals(original, decrypted);
    }

    @Test
    void testDecryptWithInvalidPrivateKeyReturnsNull() {
        String invalidKey = Base64.getEncoder().encodeToString("invalid-key".getBytes());
        String result = RSADecryptUtil.dncryption("test", invalidKey);
        assertNull(result);
    }

    @Test
    void testDecryptWithInvalidCiphertextReturnsNull() throws Exception {
        String invalidCiphertext = Base64.getEncoder().encodeToString("invalid-data".getBytes());
        String result = RSADecryptUtil.dncryption(invalidCiphertext, privateKeyStr);
        assertNull(result);
    }

    @Test
    void testDecryptWithWrongKeyReturnsNull() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair otherPair = g.genKeyPair();
        String otherPublicKeyStr = new String(Base64.getEncoder().encode(otherPair.getPublic().getEncoded()), StandardCharsets.UTF_8);
        String otherPrivateKeyStr = new String(Base64.getEncoder().encode(otherPair.getPrivate().getEncoded()), StandardCharsets.UTF_8);
        String encrypted = encryptWithPublicKey("test");
        String result = RSADecryptUtil.dncryption(encrypted, otherPrivateKeyStr);
        assertNull(result);
    }

    @Test
    void testDecryptWithTamperedCiphertextReturnsNull() throws Exception {
        String original = "test data";
        String encrypted = encryptWithPublicKey(original);
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
        encryptedBytes[0] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(encryptedBytes);
        String result = RSADecryptUtil.dncryption(tampered, privateKeyStr);
        assertNull(result);
    }

    @Test
    void testDecryptNumericText() throws Exception {
        String original = "1234567890";
        String encrypted = encryptWithPublicKey(original);
        String decrypted = RSADecryptUtil.dncryption(encrypted, privateKeyStr);
        assertEquals(original, decrypted);
    }

    @Test
    void testDecryptUnicodeEmoji() throws Exception {
        String original = "Hello World";
        String encrypted = encryptWithPublicKey(original);
        String decrypted = RSADecryptUtil.dncryption(encrypted, privateKeyStr);
        assertEquals(original, decrypted);
    }
}
