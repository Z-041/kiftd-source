package kohgylw.kiftd.server.util;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.spec.*;
import java.security.*;
import javax.crypto.*;

import kohgylw.kiftd.printer.Printer;

/**
 *
 * <h2>RSA解密工具类</h2>
 * <p>
 * 该工具类提供RSA私钥解密功能，用于解密前端使用RSA公钥加密后提交的敏感数据
 * （如登录密码、修改密码的请求数据等）。包含Base64解码、密钥工厂和Cipher的静态初始化。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class RSADecryptUtil {
	private static final Base64.Decoder DECODER = Base64.getDecoder();
	private static final KeyFactory KEY_FACTORY;
	private static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";

	static {
		try {
			KEY_FACTORY = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static String dncryption(final String context, final String privateKey) {
		final byte[] b = RSADecryptUtil.DECODER.decode(privateKey);
		final byte[] s = RSADecryptUtil.DECODER.decode(context);
		final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(b);
		try {
			final PrivateKey key = RSADecryptUtil.KEY_FACTORY.generatePrivate(spec);
			final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
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
