package kohgylw.kiftd.newcore.infrastructure.crypto.impl;

import jakarta.annotation.Resource;
import kohgylw.kiftd.newcore.infrastructure.crypto.CryptoService;
import kohgylw.kiftd.server.util.AESCipher;
import kohgylw.kiftd.server.util.RSADecryptUtil;
import org.springframework.stereotype.Service;

@Service
public class CryptoServiceAdapter implements CryptoService {

	@Resource
	private AESCipher aesCipher;

	@Override
	public String encrypt(String base64Key, String content) throws Exception {
		return aesCipher.encrypt(base64Key, content);
	}

	@Override
	public String decrypt(String base64Key, String ciphertext) throws Exception {
		return aesCipher.decrypt(base64Key, ciphertext);
	}

	@Override
	public String generateRandomAesKey() throws Exception {
		return aesCipher.generateRandomKey();
	}

	@Override
	public String rsaDecrypt(String context, String privateKey) {
		return RSADecryptUtil.dncryption(context, privateKey);
	}
}
