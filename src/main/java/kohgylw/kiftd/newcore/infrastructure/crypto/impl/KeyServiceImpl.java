package kohgylw.kiftd.newcore.infrastructure.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import kohgylw.kiftd.newcore.infrastructure.crypto.KeyService;
import kohgylw.kiftd.printer.Printer;

@Service
@Primary
public class KeyServiceImpl implements KeyService {

	private static final int RSA_KEY_SIZE = 2048;
	private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int PBKDF2_ITERATIONS = 100000;
	private static final int PBKDF2_KEY_LENGTH = 256;
	private static final int SALT_LENGTH = 16;
	private static final String HASH_PREFIX = "PBKDF2$";

	private final Base64.Encoder encoder;
	private final Base64.Decoder decoder;
	private final SecureRandom secureRandom;
	private final SecretKeyFactory secretKeyFactory;

	private String publicKeyStr;
	private String privateKeyStr;

	public KeyServiceImpl() throws NoSuchAlgorithmException {
		this.encoder = Base64.getEncoder();
		this.decoder = Base64.getDecoder();
		this.secureRandom = new SecureRandom();
		this.secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
		generateRsaKeyPair();
	}

	private void generateRsaKeyPair() {
		try {
			final KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
			g.initialize(RSA_KEY_SIZE);
			final KeyPair pair = g.genKeyPair();
			final Key publicKey = pair.getPublic();
			final Key privateKey = pair.getPrivate();
			this.publicKeyStr = new String(this.encoder.encode(publicKey.getEncoded()), StandardCharsets.UTF_8);
			this.privateKeyStr = new String(this.encoder.encode(privateKey.getEncoded()), StandardCharsets.UTF_8);
			Printer.instance.print("RSA临时密钥对已生成，私钥仅保留在内存中。");
		} catch (Exception e) {
			Printer.instance.print(e.getMessage());
			Printer.instance.print("错误：RSA密钥生成失败。");
		}
	}

	@Override
	public String getPublicKey() {
		return this.publicKeyStr;
	}

	@Override
	public String getPrivateKey() {
		return this.privateKeyStr;
	}

	@Override
	public int getKeySize() {
		return RSA_KEY_SIZE;
	}

	@Override
	public String hashPassword(String password) {
		byte[] salt = new byte[SALT_LENGTH];
		secureRandom.nextBytes(salt);
		byte[] hash = deriveKey(password, salt);
		return HASH_PREFIX + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
	}

	@Override
	public boolean verifyPassword(String password, String storedHash) {
		if (password == null) {
			password = "";
		}
		if (storedHash == null) {
			byte[] salt = new byte[SALT_LENGTH];
			byte[] expectedHash = new byte[PBKDF2_KEY_LENGTH / 8];
			byte[] actualHash = deriveKey(password, salt);
			return constantTimeIsEqual(expectedHash, actualHash);
		}
		if (storedHash.startsWith(HASH_PREFIX)) {
			String[] parts = storedHash.substring(HASH_PREFIX.length()).split("\\$", 2);
			if (parts.length == 2) {
				try {
					byte[] salt = decoder.decode(parts[0]);
					byte[] expectedHash = decoder.decode(parts[1]);
					if (salt.length != SALT_LENGTH || expectedHash.length != PBKDF2_KEY_LENGTH / 8) {
						byte[] dummySalt = new byte[SALT_LENGTH];
						byte[] dummyHash = new byte[PBKDF2_KEY_LENGTH / 8];
						byte[] actualHash = deriveKey(password, dummySalt);
						return constantTimeIsEqual(dummyHash, actualHash);
					}
					byte[] actualHash = deriveKey(password, salt);
					return constantTimeIsEqual(expectedHash, actualHash);
				} catch (IllegalArgumentException e) {
					byte[] dummySalt = new byte[SALT_LENGTH];
					byte[] dummyHash = new byte[PBKDF2_KEY_LENGTH / 8];
					byte[] actualHash = deriveKey(password, dummySalt);
					return constantTimeIsEqual(dummyHash, actualHash);
				}
			}
			byte[] dummySalt = new byte[SALT_LENGTH];
			byte[] dummyHash = new byte[PBKDF2_KEY_LENGTH / 8];
			byte[] actualHash = deriveKey(password, dummySalt);
			return constantTimeIsEqual(dummyHash, actualHash);
		}
		return constantTimeStringEquals(storedHash, password);
	}

	@Override
	public boolean isPasswordHashed(String storedHash) {
		return storedHash != null && storedHash.startsWith(HASH_PREFIX);
	}

	private byte[] deriveKey(String password, byte[] salt) {
		try {
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
			return secretKeyFactory.generateSecret(spec).getEncoded();
		} catch (Exception e) {
			throw new RuntimeException("Password hashing failed", e);
		}
	}

	private boolean constantTimeIsEqual(byte[] a, byte[] b) {
		if (a.length != b.length) {
			return false;
		}
		int result = 0;
		for (int i = 0; i < a.length; i++) {
			result |= a[i] ^ b[i];
		}
		return result == 0;
	}

	private boolean constantTimeStringEquals(String a, String b) {
		if (a == null || b == null) {
			return a == b;
		}
		byte[] ab = a.getBytes(StandardCharsets.UTF_8);
		byte[] bb = b.getBytes(StandardCharsets.UTF_8);
		return constantTimeIsEqual(ab, bb);
	}
}
