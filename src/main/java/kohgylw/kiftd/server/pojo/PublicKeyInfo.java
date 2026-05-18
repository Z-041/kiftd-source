package kohgylw.kiftd.server.pojo;

/**
 *
 * <h2>公钥信息封装类</h2>
 * <p>
 * 该类用于封装RSA公钥及其生成时间戳，供前端获取公钥时使用。
 * 包含公钥字符串（Base64编码）和生成时间，用于前端加密登录密码。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class PublicKeyInfo
{
    private String publicKey;
    private long time;
    
    public String getPublicKey() {
        return this.publicKey;
    }
    
    public void setPublicKey(final String publicKey) {
        this.publicKey = publicKey;
    }
    
    public long getTime() {
        return this.time;
    }
    
    public void setTime(final long time) {
        this.time = time;
    }
}
