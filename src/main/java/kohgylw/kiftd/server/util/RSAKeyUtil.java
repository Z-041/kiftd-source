package kohgylw.kiftd.server.util;

import java.util.*;

import org.springframework.stereotype.Component;

import kohgylw.kiftd.printer.Printer;

import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;

/**
 *
 * <h2>RSA密钥对生成与存储工具类</h2>
 * <p>
 * 该工具类负责生成和管理RSA非对称加密密钥对（2048位），
 * 用于前端登录密码的加密传输。密钥对会在首次启动时自动生成，
 * 并持久化存储至 conf/rsa.key 文件中，以便服务器重启后继续使用。
 * 公钥会提供给前端用于加密用户密码，私钥用于服务端解密。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class RSAKeyUtil {
	private static final int KEY_SIZE = 2048;
	private static final String KEY_FILE_NAME = "rsa.key";
	private Key publicKey;
	private Key privateKey;
	private Base64.Encoder encoder;
	private String publicKeyStr;
	private String privateKeyStr;

	public RSAKeyUtil() {
		this.encoder = Base64.getEncoder();
		try {
			Path keysDir = Paths.get(ConfigureReader.instance().getPath(), "conf");
			Path keyFile = keysDir.resolve(KEY_FILE_NAME);
			if (Files.exists(keyFile)) {
				List<String> lines = Files.readAllLines(keyFile, StandardCharsets.UTF_8);
				if (lines.size() >= 2) {
					String savedPublicKey = lines.get(0);
					String savedPrivateKey = lines.get(1);
					KeyFactory kf = KeyFactory.getInstance("RSA");
					X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(
							Base64.getDecoder().decode(savedPublicKey));
					this.publicKey = kf.generatePublic(pubSpec);
					PKCS8EncodedKeySpec priSpec = new PKCS8EncodedKeySpec(
							Base64.getDecoder().decode(savedPrivateKey));
					this.privateKey = kf.generatePrivate(priSpec);
					this.publicKeyStr = savedPublicKey;
					this.privateKeyStr = savedPrivateKey;
					Printer.instance.print("RSA密钥对已从" + keyFile.toAbsolutePath() + "载入。");
					return;
				}
			}
			final KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
			g.initialize(KEY_SIZE);
			final KeyPair pair = g.genKeyPair();
			this.publicKey = pair.getPublic();
			this.privateKey = pair.getPrivate();
			this.publicKeyStr = new String(this.encoder.encode(this.publicKey.getEncoded()), StandardCharsets.UTF_8);
			this.privateKeyStr = new String(this.encoder.encode(this.privateKey.getEncoded()), StandardCharsets.UTF_8);
			Files.createDirectories(keysDir);
			Files.write(keyFile, Arrays.asList(this.publicKeyStr, this.privateKeyStr),
					StandardCharsets.UTF_8);
			Printer.instance.print("RSA密钥对已生成并保存至" + keyFile.toAbsolutePath() + "。");
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

	public int getKeySize() {
		return KEY_SIZE;
	}
}