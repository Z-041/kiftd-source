package kohgylw.kiftd.server.util;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import kohgylw.kiftd.printer.Printer;

/**
 * <h2>永久外链主密钥管理器</h2>
 * <p>
 * 负责从环境变量 {@code KIFTD_CHAIN_MASTER_KEY} 读取用于加密 {@code chain_aes_key}
 * 的主密钥。若未配置，则每次启动生成一个随机会话主密钥并打印警告；此时永久外链
 * 仅在当前服务器运行周期内有效，重启后需重新生成。
 * </p>
 */
@Component
public class ChainKeyMaster {

	private static final String ENV_KEY = "KIFTD_CHAIN_MASTER_KEY";
	private static final String WRAP_PREFIX = "ENC:";
	private static final int RANDOM_KEY_LENGTH = 32;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final String masterKey;
	private final AESCipher cipher;

	public ChainKeyMaster(AESCipher cipher) {
		this.cipher = cipher;
		String envKey = System.getenv(ENV_KEY);
		if (envKey != null && !envKey.isEmpty()) {
			this.masterKey = envKey;
			Printer.instance.print("已从环境变量加载永久外链主密钥。");
		} else {
			byte[] bytes = new byte[RANDOM_KEY_LENGTH];
			RANDOM.nextBytes(bytes);
			this.masterKey = Base64.getEncoder().encodeToString(bytes);
			Printer.instance.print("警告：未配置环境变量 " + ENV_KEY
					+ "，已生成临时主密钥。重启后历史永久外链将失效，建议配置固定主密钥。");
		}
	}

	public String wrap(String plainKey) {
		if (plainKey == null) {
			return null;
		}
		try {
			return WRAP_PREFIX + cipher.encrypt(masterKey, plainKey);
		} catch (Exception e) {
			throw new RuntimeException("永久外链密钥加密失败", e);
		}
	}

	public String unwrap(String storedKey) {
		if (storedKey == null) {
			return null;
		}
		if (storedKey.startsWith(WRAP_PREFIX)) {
			try {
				return cipher.decrypt(masterKey, storedKey.substring(WRAP_PREFIX.length()));
			} catch (Exception e) {
				throw new RuntimeException("永久外链密钥解密失败，请检查 " + ENV_KEY + " 是否一致", e);
			}
		}
		// 历史明文密钥，直接返回以便迁移
		return storedKey;
	}

	public boolean isWrapped(String storedKey) {
		return storedKey != null && storedKey.startsWith(WRAP_PREFIX);
	}
}
