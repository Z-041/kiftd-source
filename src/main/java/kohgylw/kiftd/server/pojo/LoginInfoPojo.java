package kohgylw.kiftd.server.pojo;

/**
 *
 * <h2>登录信息封装类</h2>
 * <p>
 * 该类用于封装用户登录时提交的凭据信息，包括账号ID、密码和时间戳。
 * 这些信息由前端使用RSA公钥加密后传输，服务端解密后使用。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class LoginInfoPojo
{
    private String accountId;
    private String accountPwd;
    private String time;
    
    public String getAccountId() {
        return this.accountId;
    }
    
    public void setAccountId(final String accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountPwd() {
        return this.accountPwd;
    }
    
    public void setAccountPwd(final String accountPwd) {
        this.accountPwd = accountPwd;
    }
    
    public String getTime() {
        return this.time;
    }
    
    public void setTime(final String time) {
        this.time = time;
    }
}
