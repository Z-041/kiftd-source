package kohgylw.kiftd.server.pojo;

/**
 *
 * <h2>修改密码信息封装类</h2>
 * <p>
 * 该类用于封装用户修改密码时提交的凭据信息，包括旧密码RSA密文、
 * 新密码RSA密文和时间戳密文和时间戳，供服务端解密后校验密码修改请求的合法性。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class ChangePasswordInfoPojo
{
    private String oldPwd;
    private String newPwd;
    private String time;
	public String getOldPwd() {
		return oldPwd;
	}
	public void setOldPwd(String oldPwd) {
		this.oldPwd = oldPwd;
	}
	public String getNewPwd() {
		return newPwd;
	}
	public void setNewPwd(String newPwd) {
		this.newPwd = newPwd;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
    
   
}
