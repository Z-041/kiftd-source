package kohgylw.kiftd.server.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtil {

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 100000;
	private static final int KEY_LENGTH = 256;
	private static final int SALT_LENGTH = 16;
	private static final String HASH_PREFIX = "PBKDF2$";
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Base64.Encoder ENCODER = Base64.getEncoder();
	private static final Base64.Decoder DECODER = Base64.getDecoder();

	public static String hashPassword(String password) {
		byte[] salt = new byte[SALT_LENGTH];
		SECURE_RANDOM.nextBytes(salt);
		byte[] hash = deriveKey(password, salt);
		return HASH_PREFIX + ENCODER.encodeToString(salt) + "$" + ENCODER.encodeToString(hash);
	}

	public static boolean verifyPassword(String password, String stored) {
		if (password == null) {
			password = "";
		}
		if (stored == null) {
			// 使用 dummy 盐值与哈希执行完整派生，避免"账户不存在"与"密码错误"之间出现时序差
			byte[] salt = new byte[SALT_LENGTH];
			byte[] expectedHash = new byte[KEY_LENGTH / 8];
			byte[] actualHash = deriveKey(password, salt);
			return ConstantTimeComparator.isEqual(expectedHash, actualHash);
		}
		if (stored.startsWith(HASH_PREFIX)) {
			String[] parts = stored.substring(HASH_PREFIX.length()).split("\\$", 2);
			if (parts.length == 2) {
				try {
					byte[] salt = DECODER.decode(parts[0]);
					byte[] expectedHash = DECODER.decode(parts[1]);
					// 校验盐值和哈希长度是否合法，防止格式损坏
					if (salt.length != SALT_LENGTH || expectedHash.length != KEY_LENGTH / 8) {
						// 格式损坏，使用dummy派生保持时序一致
						byte[] dummySalt = new byte[SALT_LENGTH];
						byte[] dummyHash = new byte[KEY_LENGTH / 8];
						byte[] actualHash = deriveKey(password, dummySalt);
						return ConstantTimeComparator.isEqual(dummyHash, actualHash);
					}
					byte[] actualHash = deriveKey(password, salt);
					return ConstantTimeComparator.isEqual(expectedHash, actualHash);
				} catch (IllegalArgumentException e) {
					// Base64解码失败（格式损坏），使用dummy派生保持时序一致，避免泄露信息
					byte[] dummySalt = new byte[SALT_LENGTH];
					byte[] dummyHash = new byte[KEY_LENGTH / 8];
					byte[] actualHash = deriveKey(password, dummySalt);
					return ConstantTimeComparator.isEqual(dummyHash, actualHash);
				}
			}
			// 格式不正确（只有一个$分隔符但内容不足），使用dummy派生保持时序一致
			byte[] dummySalt = new byte[SALT_LENGTH];
			byte[] dummyHash = new byte[KEY_LENGTH / 8];
			byte[] actualHash = deriveKey(password, dummySalt);
			return ConstantTimeComparator.isEqual(dummyHash, actualHash);
		}
		// 旧版明文密码也使用恒定时间比较，避免逐字符短路返回
		return constantTimeStringEquals(stored, password);
	}

	public static boolean isPasswordHashed(String stored) {
		return stored != null && stored.startsWith(HASH_PREFIX);
	}

	private static boolean constantTimeStringEquals(String a, String b) {
		if (a == null || b == null) {
			return a == b;
		}
		byte[] ab = a.getBytes(StandardCharsets.UTF_8);
		byte[] bb = b.getBytes(StandardCharsets.UTF_8);
		return ConstantTimeComparator.isEqual(ab, bb);
	}

	private static byte[] deriveKey(String password, byte[] salt) {
		try {
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
			return factory.generateSecret(spec).getEncoded();
		} catch (Exception e) {
			throw new RuntimeException("Password hashing failed", e);
		}
	}

	private static class ConstantTimeComparator {
		static boolean isEqual(byte[] a, byte[] b) {
			if (a.length != b.length) {
				return false;
			}
			int result = 0;
			for (int i = 0; i < a.length; i++) {
				result |= a[i] ^ b[i];
			}
			return result == 0;
		}
	}
}
