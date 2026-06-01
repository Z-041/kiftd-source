package kohgylw.kiftd.newcore.infrastructure.crypto;

public interface KeyService {

	String getPublicKey();

	String getPrivateKey();

	int getKeySize();

	String hashPassword(String password);

	boolean verifyPassword(String password, String storedHash);

	boolean isPasswordHashed(String storedHash);
}
