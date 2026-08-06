package kohgylw.kiftd.newcore.infrastructure.crypto.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kohgylw.kiftd.printer.Printer;

class CryptoServiceImplTest {

	private static CryptoServiceImpl service;
	private static String rsaPrivateKey;
	private static String rsaPublicKey;

	@BeforeAll
	static void setUp() throws Exception {
		service = new CryptoServiceImpl();
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair pair = generator.generateKeyPair();
		rsaPrivateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
		rsaPublicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
	}

	@BeforeEach
	void initPrinter() {
		// rsaDecrypt 失败分支会调用 Printer.instance.print，测试中避免 NPE
		Printer.instance = org.mockito.Mockito.mock(Printer.class);
	}

	@AfterEach
	void restorePrinter() {
		Printer.instance = null;
	}

	@Test
	void testEncryptDecrypt_RoundTrip() throws Exception {
		String key = service.generateRandomAesKey();
		String plaintext = "hello kiftd 中文测试";
		String ciphertext = service.encrypt(key, plaintext);

		assertNotEquals(plaintext, ciphertext);
		assertEquals(plaintext, service.decrypt(key, ciphertext));
	}

	@Test
	void testEncryptDecrypt_DifferentKeys_DoNotRoundTrip() throws Exception {
		String keyA = service.generateRandomAesKey();
		String keyB = service.generateRandomAesKey();
		String ciphertext = service.encrypt(keyA, "secret");

		// 用错误的密钥解密应抛出异常或得到错误结果
		assertThrows(Exception.class, () -> service.decrypt(keyB, ciphertext));
	}

	@Test
	void testGenerateRandomAesKey_Returns256BitKey() throws Exception {
		String key = service.generateRandomAesKey();
		byte[] decoded = Base64.getDecoder().decode(key);
		assertEquals(32, decoded.length);
	}

	@Test
	void testGenerateRandomAesKey_ProducesDifferentKeys() throws Exception {
		assertNotEquals(service.generateRandomAesKey(), service.generateRandomAesKey());
	}

	@Test
	void testRsaDecrypt_RoundTrip() throws Exception {
		byte[] publicKeyBytes = Base64.getDecoder().decode(rsaPublicKey);
		PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		String plaintext = "rsa-secret-content";
		String encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes()));

		assertEquals(plaintext, service.rsaDecrypt(encrypted, rsaPrivateKey));
	}

	@Test
	void testRsaDecrypt_InvalidKey_ReturnsNull() {
		String validBase64Garbage = Base64.getEncoder().encodeToString(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
		assertNull(service.rsaDecrypt(validBase64Garbage, validBase64Garbage));
	}
}
