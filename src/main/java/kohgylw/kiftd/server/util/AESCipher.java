package kohgylw.kiftd.server.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 *
 * <h2>AES加密解密工具类</h2>
 * <p>
 * 该工具类提供基于AES-GCM模式的加密和解密功能，用于生成和管理永久资源链接（FileChain）的加密密钥。
 * 支持生成256位随机AES密钥、使用密钥加密文件ID生成安全链接，以及解密链接还原文件ID。
 * 使用GCM（Galois/Counter Mode）认证加密模式，确保数据的机密性和完整性。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class AESCipher {

	private static final String CIPHER_TYPE = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;
	private Base64.Encoder encoder;
	private Base64.Decoder decoder;
	private SecureRandom secureRandom;

	public AESCipher() {
		encoder = Base64.getEncoder();
		decoder = Base64.getDecoder();
		secureRandom = new SecureRandom();
	}

	public String generateRandomKey() throws NoSuchAlgorithmException {
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(256);
		return encoder.encodeToString(kg.generateKey().getEncoded());
	}

	public String encrypt(String base64Key, String content) throws Exception {
		SecretKey key = new SecretKeySpec(decoder.decode(base64Key), "AES");
		byte[] iv = new byte[GCM_IV_LENGTH];
		secureRandom.nextBytes(iv);
		Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
		GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, spec);
		byte[] ciphertext = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
		ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
		buffer.put(iv);
		buffer.put(ciphertext);
		return encoder.encodeToString(buffer.array());
	}

	public String decrypt(String base64Key, String ciphertext) throws Exception {
		SecretKey key = new SecretKeySpec(decoder.decode(base64Key), "AES");
		byte[] decoded = decoder.decode(ciphertext);
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

}