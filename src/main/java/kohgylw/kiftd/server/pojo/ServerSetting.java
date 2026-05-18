package kohgylw.kiftd.server.pojo;

import java.util.List;

import kohgylw.kiftd.server.enumeration.*;

/**
 *
 * <h2>服务器设置封装类</h2>
 * <p>
 * 该类用于封装kiftd服务器的全部配置信息，包括登录要求、验证码等级、
 * 缓冲区大小、日志级别、端口号、文件存储路径、永久资源链接开关、
 * 修改密码功能和扩展存储路径列表等。这些设置从配置文件中读取，
 * 并通过ConfigureReader加载后供系统各模块使用。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class ServerSetting {
	private boolean mustLogin;
	private VCLevel vc;
	private int buffSize;
	private LogLevel log;
	private int port;
	private String fsPath;
	private boolean fileChain;
	private boolean changePassword;
	private List<ExtendStores> extendStores;

	public boolean isMustLogin() {
		return this.mustLogin;
	}

	public void setMustLogin(final boolean mustLogin) {
		this.mustLogin = mustLogin;
	}

	public int getBuffSize() {
		return this.buffSize;
	}

	public void setBuffSize(final int buffSize) {
		this.buffSize = buffSize;
	}

	public LogLevel getLog() {
		return this.log;
	}

	public void setLog(final LogLevel log) {
		this.log = log;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public String getFsPath() {
		return this.fsPath;
	}

	public void setFsPath(final String fsPath) {
		this.fsPath = fsPath;
	}

	public VCLevel getVc() {
		return this.vc;
	}

	public void setVc(VCLevel vc) {
		this.vc = vc;
	}

	public List<ExtendStores> getExtendStores() {
		return extendStores;
	}

	public void setExtendStores(List<ExtendStores> extendStores) {
		this.extendStores = extendStores;
	}

	public boolean isOpenFileChain() {
		return fileChain;
	}

	public void setFileChain(boolean fileChain) {
		this.fileChain = fileChain;
	}

	public boolean isAllowChangePassword() {
		return changePassword;
	}

	public void setChangePassword(boolean changePassword) {
		this.changePassword = changePassword;
	}
}
