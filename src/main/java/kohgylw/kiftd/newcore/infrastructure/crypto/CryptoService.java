package kohgylw.kiftd.newcore.infrastructure.crypto;

public interface CryptoService {

	String encrypt(String base64Key, String content) throws Exception;

	String decrypt(String base64Key, String ciphertext) throws Exception;

	String generateRandomAesKey() throws Exception;

	String rsaDecrypt(String context, String privateKey);
}
