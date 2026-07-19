package kohgylw.kiftd.newcore.infrastructure.crypto.impl;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import kohgylw.kiftd.newcore.infrastructure.crypto.CryptoService;
import kohgylw.kiftd.printer.Printer;

@Service
@Primary
public class CryptoServiceImpl implements CryptoService {

	private static final String CIPHER_TYPE = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;
	private static final String RSA_CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
	private static final Base64.Encoder ENCODER = Base64.getEncoder();
	private static final Base64.Decoder DECODER = Base64.getDecoder();
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final KeyFactory rsaKeyFactory;

	public CryptoServiceImpl() throws NoSuchAlgorithmException {
		this.rsaKeyFactory = KeyFactory.getInstance("RSA");
	}

	@Override
	public String encrypt(String base64Key, String content) throws Exception {
		SecretKey key = new SecretKeySpec(DECODER.decode(base64Key), "AES");
		byte[] iv = new byte[GCM_IV_LENGTH];
		SECURE_RANDOM.nextBytes(iv);
		Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
		GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, spec);
		byte[] ciphertext = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
		ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
		buffer.put(iv);
		buffer.put(ciphertext);
		return ENCODER.encodeToString(buffer.array());
	}

	@Override
	public String decrypt(String base64Key, String ciphertext) throws Exception {
		SecretKey key = new SecretKeySpec(DECODER.decode(base64Key), "AES");
		byte[] decoded = DECODER.decode(ciphertext);
		ByteBuffer buffer = ByteBuffer.wrap(decoded);
		byte[] iv = new byte[GCM_IV_LENGTH];
		buffer.get(iv);
		byte[] encrypted = new byte[buffer.remaining()];
		buffer.get(encrypted);
		Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
		GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
		cipher.init(Cipher.DECRYPT_MODE, key, spec);
		return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
	}

	@Override
	public String generateRandomAesKey() throws NoSuchAlgorithmException {
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(256);
		return ENCODER.encodeToString(kg.generateKey().getEncoded());
	}

	@Override
	public String rsaDecrypt(String context, String privateKey) {
		final byte[] b = DECODER.decode(privateKey);
		final byte[] s = DECODER.decode(context);
		final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(b);
		try {
			final PrivateKey key = rsaKeyFactory.generatePrivate(spec);
			final Cipher cipher = Cipher.getInstance(RSA_CIPHER_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key);
			final byte[] f = cipher.doFinal(s);
			return new String(f, StandardCharsets.UTF_8);
		} catch (Exception e) {
			Printer.instance.print(e.getMessage());
			Printer.instance.print("错误：RSA解密失败。");
		}
		return null;
	}
}
