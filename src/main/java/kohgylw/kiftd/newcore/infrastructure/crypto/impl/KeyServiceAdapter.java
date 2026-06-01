package kohgylw.kiftd.newcore.infrastructure.crypto.impl;

import jakarta.annotation.Resource;
import kohgylw.kiftd.newcore.infrastructure.crypto.KeyService;
import kohgylw.kiftd.server.util.PasswordUtil;
import kohgylw.kiftd.server.util.RSAKeyUtil;
import org.springframework.stereotype.Service;

@Service
public class KeyServiceAdapter implements KeyService {

	@Resource
	private RSAKeyUtil rsaKeyUtil;

	@Override
	public String getPublicKey() {
		return rsaKeyUtil.getPublicKey();
	}

	@Override
	public String getPrivateKey() {
		return rsaKeyUtil.getPrivateKey();
	}

	@Override
	public int getKeySize() {
		return rsaKeyUtil.getKeySize();
	}

	@Override
	public String hashPassword(String password) {
		return PasswordUtil.hashPassword(password);
	}

	@Override
	public boolean verifyPassword(String password, String storedHash) {
		return PasswordUtil.verifyPassword(password, storedHash);
	}

	@Override
	public boolean isPasswordHashed(String storedHash) {
		return PasswordUtil.isPasswordHashed(storedHash);
	}
}
