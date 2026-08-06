package kohgylw.kiftd.server.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.springframework.stereotype.Component;

import kohgylw.kiftd.printer.Printer;

/**
 *
 * <h2>RSA临时密钥对生成工具类</h2>
 * <p>
 * 该工具类负责生成和管理RSA非对称加密密钥对（2048位），
 * 用于前端登录密码的加密传输。密钥对在每个服务器实例生命周期内生成一次，
 * 私钥仅保留在内存中，不会持久化到磁盘，以避免明文私钥泄露风险。
 * 公钥会提供给前端用于加密用户密码，私钥用于服务端解密。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class RSAKeyUtil {
	private static final int KEY_SIZE = 2048;
	private static final Base64.Encoder ENCODER = Base64.getEncoder();

	private Key publicKey;
	private Key privateKey;
	private String publicKeyStr;
	private String privateKeyStr;

	public RSAKeyUtil() {
		try {
			// 每次启动生成新的临时密钥对，私钥不落盘，避免明文持久化风险
			final KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
			g.initialize(KEY_SIZE);
			final KeyPair pair = g.genKeyPair();
			this.publicKey = pair.getPublic();
			this.privateKey = pair.getPrivate();
			this.publicKeyStr = new String(ENCODER.encode(this.publicKey.getEncoded()), StandardCharsets.UTF_8);
			this.privateKeyStr = new String(ENCODER.encode(this.privateKey.getEncoded()), StandardCharsets.UTF_8);
			Printer.instance.print("RSA临时密钥对已生成，私钥仅保留在内存中。");
		} catch (Exception e) {
			Printer.instance.print(e.getMessage());
			Printer.instance.print("错误：RSA密钥生成失败。");
		}
	}

	public String getPublicKey() {
		return this.publicKeyStr;
	}

	public String getPrivateKey() {
		return this.privateKeyStr;
	}
}