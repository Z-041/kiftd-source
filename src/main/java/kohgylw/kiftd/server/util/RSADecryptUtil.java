package kohgylw.kiftd.server.util;

import java.util.*;
import java.nio.charset.*;
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
	private static Base64.Decoder decoder;
	private static KeyFactory kf;
	private static Cipher c;

	public static String dncryption(final String context, final String privateKey) {
		final byte[] b = RSADecryptUtil.decoder.decode(privateKey);
		final byte[] s = RSADecryptUtil.decoder.decode(context.getBytes(StandardCharsets.UTF_8));
		final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(b);
		try {
			final PrivateKey key = RSADecryptUtil.kf.generatePrivate(spec);
			RSADecryptUtil.c.init(2, key);
			final byte[] f = RSADecryptUtil.c.doFinal(s);
			return new String(f);
		} catch (Exception e) {
			Printer.instance.print(e.getMessage());
			Printer.instance.print("错误：RSA解密失败。");
		}
		return null;
	}

	static {
		RSADecryptUtil.decoder = Base64.getDecoder();
		try {
			RSADecryptUtil.kf = KeyFactory.getInstance("RSA");
			RSADecryptUtil.c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e2) {
			e2.printStackTrace();
		}
	}
}
