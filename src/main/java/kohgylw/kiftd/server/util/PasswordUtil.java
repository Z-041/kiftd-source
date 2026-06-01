package kohgylw.kiftd.server.util;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
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
		if (stored == null) {
			return false;
		}
		if (stored.startsWith(HASH_PREFIX)) {
			String[] parts = stored.substring(HASH_PREFIX.length()).split("\\$", 2);
			if (parts.length == 2) {
				byte[] salt = DECODER.decode(parts[0]);
				byte[] expectedHash = DECODER.decode(parts[1]);
				byte[] actualHash = deriveKey(password, salt);
				return ConstantTimeComparator.isEqual(expectedHash, actualHash);
			}
			return false;
		}
		return stored.equals(password);
	}

	public static boolean isPasswordHashed(String stored) {
		return stored != null && stored.startsWith(HASH_PREFIX);
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
