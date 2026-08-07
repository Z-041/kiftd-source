package kohgylw.kiftd.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.enumeration.LogLevel;
import kohgylw.kiftd.server.enumeration.VCLevel;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.pojo.ExtendStores;
import kohgylw.kiftd.server.pojo.ServerSetting;

public class ConfigurationManager {

	public static final int INVALID_DOWNLOAD_ZIP_SETTING = 17;
	public static final int INVALID_PORT = 1;
	public static final int INVALID_LOG = 2;
	public static final int INVALID_FILE_SYSTEM_PATH = 3;
	public static final int INVALID_BUFFER_SIZE = 4;
	public static final int CANT_CREATE_FILE_BLOCK_PATH = 5;
	public static final int CANT_CREATE_FILE_NODE_PATH = 6;
	public static final int CANT_CREATE_TF_PATH = 7;
	public static final int CANT_CONNECT_DB = 8;
	public static final int HTTPS_SETTING_ERROR = 9;
	public static final int INVALID_VC = 10;
	public static final int INVALID_CHANGE_PASSWORD_SETTING = 11;
	public static final int INVALID_FILE_CHAIN_SETTING = 12;
	public static final int INVALID_IP_XFF_SETTING = 13;
	public static final int INVALID_FFMPEG_SETTING = 14;
	public static final int INVALID_MUST_LOGIN_SETTING = 15;
	public static final int INVALID_RECYCLE_BIN_PATH = 16;
	public static final int LEGAL_PROPERTIES = 0;

	private static final int MAX_EXTENDSTORES_NUM = 255;
	private static final String DEFAULT_IMPORT_ACCOUNT = "SYS_IN";
	private static final String[] SYS_ACCOUNTS = { DEFAULT_IMPORT_ACCOUNT, "Anonymous", "匿名用户" };

	private static volatile ConfigurationManager instance;

	private KiftdProperties serverp;
	private KiftdProperties accountp;
	private int status;
	private String basePath;
	private String confDir;
	private String fileSystemPath;
	private String fileBlockPath;
	private String fileNodePath;
	private String tempFilePath;
	private String mustLogin;
	private int port;
	private String log;
	private String vc;
	private String rawFSPath;
	private List<ExtendStores> extendStores;
	private int bufferSize;
	private String dbURL;
	private String dbDriver;
	private String dbUser;
	private String dbPwd;
	private boolean allowChangePassword;
	private boolean openFileChain;
	private boolean allowSignUp;
	private String signUpAuth;
	private String signUpGroup;
	private String timeZone;
	private boolean openHttps;
	private String httpsKeyFile;
	private String httpsKeyType;
	private String httpsKeyPass;
	private int httpsPort;
	private Set<String> ipRoster;
	private boolean ipAllowOrBanned;
	private boolean enableIPRule;
	private boolean ipXFFAnalysis;
	private boolean enableFFMPEG;
	private boolean enableDownloadByZip;
	private String recycleBinPath;
	private String defaultFileSystemPath;

	private static volatile Thread accountUpdateDaemon;

	protected ConfigurationManager() {
		this.status = -1;
		this.basePath = System.getProperty("user.dir");
		String classPath = System.getProperty("java.class.path");
		if (classPath != null && classPath.indexOf(File.pathSeparator) < 0) {
			File f = new File(classPath);
			classPath = f.getAbsolutePath();
			if (classPath.endsWith(".jar")) {
				String jarDir = classPath.substring(0, classPath.lastIndexOf(File.separator));
				File confInJarDir = new File(jarDir, "conf");
				if (confInJarDir.isDirectory()) {
					this.basePath = jarDir;
				}
			}
		}
		if (!new File(this.basePath, "conf").isDirectory()) {
			try {
				java.net.URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
				String locPath = new File(location.toURI()).getPath();
				File jarOrClassesDir = new File(locPath);
				if (!jarOrClassesDir.isDirectory()) {
					jarOrClassesDir = jarOrClassesDir.getParentFile();
				} else if (locPath.endsWith(File.separator + "classes") || locPath.endsWith("/classes")) {
					jarOrClassesDir = jarOrClassesDir.getParentFile();
				}
				if (jarOrClassesDir != null) {
					File confInJarDir = new File(jarOrClassesDir, "conf");
					if (confInJarDir.isDirectory()) {
						this.basePath = jarOrClassesDir.getAbsolutePath();
					}
				}
			} catch (Exception ignored) {
				// 探测 jar/classes 内 conf 目录失败时忽略：后续逻辑会回退到 user.dir
			}
		}
		if (!new File(this.basePath, "webContext").isDirectory()) {
			String userDir = System.getProperty("user.dir");
			if (new File(userDir, "webContext").isDirectory()) {
				this.basePath = userDir;
			}
		}
		this.defaultFileSystemPath = this.basePath + File.separator + "filesystem" + File.separator;
		this.confDir = this.basePath + File.separator + "conf" + File.separator;
		this.serverp = new KiftdProperties();
		this.accountp = new KiftdProperties();
		this.extendStores = new ArrayList<>();
		this.ipRoster = new TreeSet<>();

		final File serverProp = new File(this.confDir + "server.properties");
		if (!serverProp.isFile()) {
			Printer.instance.print("服务器配置文件不存在，需要初始化服务器配置。");
			createDefaultServerPropertiesFile();
		}
		final File accountProp = new File(this.confDir + "account.properties");
		if (!accountProp.isFile()) {
			Printer.instance.print("用户账户配置文件不存在，需要初始化账户配置。");
			createDefaultAccountPropertiesFile();
		}
		try {
			Printer.instance.print("正在载入配置文件...");
			try (final FileInputStream serverIn = new FileInputStream(serverProp);
					final FileInputStream accountIn = new FileInputStream(accountProp)) {
				this.serverp.load(serverIn);
				this.accountp.load(accountIn);
			}
			initIPRules();
			initSignUpRules();
			Printer.instance.print("配置文件载入完毕。正在检查配置...");
			this.status = this.validate();
			if (this.status == LEGAL_PROPERTIES) {
				Printer.instance.print("准备就绪。");
				startAccountUpdateListener();
			}
			instance = this;
		} catch (Exception e) {
			Printer.instance.print("错误：无法加载一个或多个配置文件（位于" + this.confDir + "路径下），请尝试删除旧的配置文件并重新启动本应用或查看安装路径的权限（必须可读写）。");
			Printer.instance.print("详细错误：" + e.getMessage());
		}
	}

	public static ConfigurationManager instance() {
		if (instance == null) {
			synchronized (ConfigurationManager.class) {
				if (instance == null) {
					instance = new ConfigurationManager();
				}
			}
		}
		return instance;
	}

	public int getStatus() {
		return this.status;
	}

	public void revalidate() {
		this.status = validate();
	}

	// ==================== Account Management ====================

	public boolean foundAccount(final String account) {
		if (account == null) {
			return false;
		}
		for (String sysAccount : SYS_ACCOUNTS) {
			if (sysAccount.equals(account)) {
				return true;
			}
		}
		if (getImportAccount().equals(account)) {
			return true;
		}
		synchronized (accountp) {
			final String accountPwd = this.accountp.getProperty(account + ".pwd");
			return accountPwd != null && accountPwd.length() > 0;
		}
	}

	/**
	 * 判断是否为系统保留账户名（内部导入账户 SYS_IN、Anonymous、匿名用户及配置的导入账户）。
	 * 保留名不允许被注册或创建为普通账户，避免与系统账户语义混淆。
	 */
	public boolean isSystemAccount(final String account) {
		if (account == null) {
			return false;
		}
		for (String sysAccount : SYS_ACCOUNTS) {
			if (sysAccount.equals(account)) {
				return true;
			}
		}
		return getImportAccount().equals(account);
	}

	public boolean checkAccountPwd(final String account, final String pwd) {
		final String apwd = this.accountp.getProperty(account + ".pwd");
		if (apwd == null) {
			return false;
		}
		return PasswordUtil.verifyPassword(pwd, apwd);
	}

	public boolean authorized(final String account, final AccountAuth auth, List<String> folders) {
		if (hasSuperAuth(account)) {
			return true;
		}
		if (account != null && account.length() > 0) {
			final StringBuilder auths = new StringBuilder();
			synchronized (accountp) {
				for (String id : folders) {
					String addedAuth = accountp.getProperty(account + ".auth." + id);
					if (addedAuth != null) {
						auths.append(addedAuth);
					}
				}
				final String accauth = this.accountp.getProperty(account + ".auth");
				final String overall = this.accountp.getProperty("authOverall");
				if (accauth != null) {
					auths.append(accauth);
				}
				if (overall != null) {
					auths.append(overall);
				}
			}
			return checkAuthChar(auths.toString(), auth);
		} else {
			return checkOverallAuth(auth);
		}
	}

	private boolean checkAuthChar(String auths, AccountAuth auth) {
		switch (auth) {
		case CREATE_NEW_FOLDER:
			return auths.indexOf("c") >= 0;
		case UPLOAD_FILES:
			return auths.indexOf("u") >= 0;
		case DELETE_FILE_OR_FOLDER:
			return auths.indexOf("d") >= 0;
		case RENAME_FILE_OR_FOLDER:
			return auths.indexOf("r") >= 0;
		case DOWNLOAD_FILES:
			return auths.indexOf("l") >= 0;
		case MOVE_FILES:
			return auths.indexOf("m") >= 0;
		default:
			return false;
		}
	}

	private boolean checkOverallAuth(AccountAuth auth) {
		final String overall = this.accountp.getProperty("authOverall");
		if (overall == null) {
			return false;
		}
		return checkAuthChar(overall, auth);
	}

	public boolean authorized(AccountAuth auth, String account, String folderId, FolderUtil folderUtil,
			FolderMapper folderMapper) {
		if (!authorized(account, auth, folderUtil.getAllFoldersId(folderId))) {
			return false;
		}
		Folder folder = folderMapper.selectById(folderId);
		if (folder == null) {
			return false;
		}
		return accessFolder(folder, account);
	}

	public boolean accessFolder(Folder f, String account) {
		if (f == null) {
			return false;
		}
		if (hasSuperAuth(account)) {
			return true;
		}
		int cl = f.getFolderConstraint();
		if (cl == 0) {
			return true;
		} else {
			if (account != null) {
				if (cl == 1) {
					if (f.getFolderCreator().equals(account)) {
						return true;
					}
					String vGroup = accountp.getProperty(account + ".group");
					String cGroup = accountp.getProperty(f.getFolderCreator() + ".group");
					if (vGroup != null && cGroup != null) {
						if ("*".equals(vGroup) || "*".equals(cGroup)) {
							return true;
						}
						String[] vgs = vGroup.split(";");
						String[] cgs = cGroup.split(";");
						for (String vs : vgs) {
							for (String cs : cgs) {
								if (vs.equals(cs)) {
									return true;
								}
							}
						}
					}
				}
				if (cl == 2) {
					return f.getFolderCreator().equals(account);
				}
			}
			return false;
		}
	}

	private boolean hasSuperAuth(String account) {
		if (account != null) {
			// 内置 admin 账户即为系统管理员；同时支持部署者在 account.properties 中
			// 显式配置“账户名.privilege=S”将其他账户提升为超级管理员。
			return "admin".equals(account) || "S".equals(accountp.getProperty(account + ".privilege"));
		}
		return false;
	}

	/**
	 * 原子写入配置文件：先写临时文件再原子替换目标文件，
	 * 避免写入中断（进程崩溃、磁盘满等）留下半写文件损坏配置。
	 */
	private void storePropertiesAtomically(String fileName, KiftdProperties props, String header) throws IOException {
		File target = new File(this.confDir + fileName);
		File tmp = new File(this.confDir + fileName + ".tmp");
		try (FileOutputStream out = new FileOutputStream(tmp)) {
			props.store(out, header);
		}
		try {
			Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public boolean changePassword(String account, String newPassword) throws Exception {
		if (account != null && newPassword != null) {
			if (accountp.getProperty(account + ".pwd") != null) {
				synchronized (accountp) {
					// 以 PBKDF2 哈希形式存储，禁止明文落盘；verifyPassword 兼容历史明文，可平滑迁移
					accountp.setProperty(account + ".pwd", PasswordUtil.hashPassword(newPassword));
					storePropertiesAtomically("account.properties", accountp, null);
					return true;
				}
			}
		}
		return false;
	}

	public boolean createNewAccount(String newAccount, String newPassword) throws Exception {
		if (newAccount != null && newPassword != null) {
			if (accountp.getProperty(newAccount + ".pwd") == null) {
				synchronized (accountp) {
					// 以 PBKDF2 哈希形式存储，禁止明文落盘
					accountp.setProperty(newAccount + ".pwd", PasswordUtil.hashPassword(newPassword));
					if (signUpAuth != null) {
						accountp.setProperty(newAccount + ".auth", signUpAuth);
					}
					if (signUpGroup != null) {
						accountp.setProperty(newAccount + ".group", signUpGroup);
					}
					storePropertiesAtomically("account.properties", accountp, null);
					return true;
				}
			}
		}
		return false;
	}

	public List<String> getAllAddedAuthFoldersId() {
		return accountp.stringPropertieNames().stream()
				.map(config -> {
					int index = config.lastIndexOf(".auth.");
					return index >= 0 ? config.substring(index + 6) : null;
				})
				.filter(id -> id != null)
				.collect(Collectors.toList());
	}

	public List<String> getAllAccounts() {
		synchronized (accountp) {
			return accountp.stringPropertieNames().stream()
					.filter(config -> config.endsWith(".pwd"))
					.map(config -> config.substring(0, config.length() - 4))
					.filter(account -> !account.isEmpty())
					.sorted(String::compareToIgnoreCase)
					.collect(Collectors.toList());
		}
	}

	public String getAccountAuth(String account) {
		if (account == null) {
			return null;
		}
		synchronized (accountp) {
			return accountp.getProperty(account + ".auth");
		}
	}

	public String getAccountGroup(String account) {
		if (account == null) {
			return null;
		}
		synchronized (accountp) {
			return accountp.getProperty(account + ".group");
		}
	}

	public boolean isSuperAdmin(String account) {
		return hasSuperAuth(account);
	}

	public boolean resetPassword(String account, String newPassword) throws Exception {
		if (account == null || newPassword == null || newPassword.isEmpty()) {
			return false;
		}
		synchronized (accountp) {
			if (accountp.getProperty(account + ".pwd") == null) {
				return false;
			}
			// 以 PBKDF2 哈希形式存储，禁止明文落盘；verifyPassword 兼容历史明文，可平滑迁移
			accountp.setProperty(account + ".pwd", PasswordUtil.hashPassword(newPassword));
			storePropertiesAtomically("account.properties", accountp, null);
			return true;
		}
	}

	public boolean deleteAccount(String account) throws Exception {
		if (account == null || account.isEmpty()) {
			return false;
		}
		synchronized (accountp) {
			if (accountp.getProperty(account + ".pwd") == null) {
				return false;
			}
			String prefix = account + ".";
			List<String> keysToRemove = accountp.stringPropertieNames().stream()
					.filter(config -> config.startsWith(prefix))
					.collect(Collectors.toList());
			for (String key : keysToRemove) {
				accountp.removeProperty(key);
			}
			try (FileOutputStream out = new FileOutputStream(this.confDir + "account.properties")) {
				accountp.store(out, null);
				return true;
			}
		}
	}

	public boolean updateAccountAuth(String account, String auth) throws Exception {
		if (account == null || auth == null) {
			return false;
		}
		synchronized (accountp) {
			if (accountp.getProperty(account + ".pwd") == null) {
				return false;
			}
			accountp.setProperty(account + ".auth", auth);
			try (FileOutputStream out = new FileOutputStream(this.confDir + "account.properties")) {
				accountp.store(out, null);
				return true;
			}
		}
	}

	public boolean removeAddedAuthByFolderId(List<String> fIds) {
		if (fIds == null || fIds.size() == 0) {
			return false;
		}
		Set<String> configs = accountp.stringPropertieNames();
		List<String> invalidConfigs = new ArrayList<>();
		for (String fId : fIds) {
			for (String config : configs) {
				if (config.endsWith(".auth." + fId)) {
					invalidConfigs.add(config);
				}
			}
		}
		synchronized (accountp) {
			for (String config : invalidConfigs) {
				accountp.removeProperty(config);
			}
			try {
				storePropertiesAtomically("account.properties", accountp, null);
				return true;
			} catch (IOException e) {
				Printer.instance.print("错误：更新账户配置文件时出现错误，请立即检查账户配置文件。");
				return false;
			}
		}
	}

	public long getUploadFileSize(String account) {
		String defaultMaxSizeP = accountp.getProperty("defaultMaxSize");
		if (account == null) {
			return SizeFormatUtil.parseSizeString(defaultMaxSizeP);
		} else {
			String accountMaxSizeP = accountp.getProperty(account + ".maxSize");
			return accountMaxSizeP == null ? SizeFormatUtil.parseSizeString(defaultMaxSizeP)
					: SizeFormatUtil.parseSizeString(accountMaxSizeP);
		}
	}

	public long getDownloadMaxRate(String account) {
		String defaultMaxRateP = accountp.getProperty("defaultMaxRate");
		if (account == null) {
			return SizeFormatUtil.parseRateString(defaultMaxRateP);
		} else {
			String accountMaxRateP = accountp.getProperty(account + ".maxRate");
			return accountMaxRateP == null ? SizeFormatUtil.parseRateString(defaultMaxRateP)
					: SizeFormatUtil.parseRateString(accountMaxRateP);
		}
	}

	public String getImportAccount() {
		String importAccount = accountp.getProperty("import.account");
		return (importAccount != null && importAccount.length() > 0) ? importAccount : DEFAULT_IMPORT_ACCOUNT;
	}

	// ==================== Getters ====================

	public int getPort() {
		return this.port;
	}

	public boolean mustLogin() {
		return this.mustLogin != null && this.mustLogin.equals("N");
	}

	public String getFileSystemPath() {
		return this.fileSystemPath;
	}

	public String getFileBlockPath() {
		return this.fileBlockPath;
	}

	public String getFileNodePath() {
		return this.fileNodePath;
	}

	public String getTemporaryfilePath() {
		return this.tempFilePath;
	}

	public String getBasePath() {
		return this.basePath;
	}

	public String getPath() {
		return this.basePath;
	}

	public int getBuffSize() {
		return this.bufferSize;
	}

	public LogLevel getLogLevel() {
		if (this.log == null) {
			this.log = "";
		}
		switch (this.log) {
		case "N":
			return LogLevel.None;
		case "R":
			return LogLevel.Runtime_Exception;
		case "E":
			return LogLevel.Event;
		default:
			return null;
		}
	}

	public boolean inspectLogLevel(final LogLevel l) {
		int o = 0;
		int m = 0;
		if (l == null) {
			return false;
		}
		switch (l) {
		case None:
			m = 0;
			break;
		case Runtime_Exception:
			m = 1;
			break;
		case Event:
			m = 2;
			break;
		default:
			m = 0;
			break;
		}
		if (this.log == null) {
			this.log = "";
		}
		switch (this.log) {
		case "N":
			o = 0;
			break;
		case "R":
			o = 1;
			break;
		case "E":
			o = 2;
			break;
		default:
			o = 1;
			break;
		}
		return o >= m;
	}

	public VCLevel getVCLevel() {
		if (this.vc == null) {
			this.vc = "";
		}
		switch (this.vc) {
		case "STANDARD":
			return VCLevel.Standard;
		case "SIMP":
			return VCLevel.Simplified;
		case "CLOSE":
			return VCLevel.Close;
		default:
			return null;
		}
	}

	public List<ExtendStores> getExtendStores() {
		return extendStores;
	}

	public int getMaxExtendstoresNum() {
		return MAX_EXTENDSTORES_NUM;
	}

	public boolean isAllowChangePassword() {
		return allowChangePassword;
	}

	public boolean isOpenFileChain() {
		return openFileChain;
	}

	public boolean isAllowSignUp() {
		return allowSignUp;
	}

	public boolean isIpXFFAnalysis() {
		return ipXFFAnalysis;
	}

	public boolean isEnableFFMPEG() {
		return enableFFMPEG;
	}

	public boolean isEnableDownloadByZip() {
		return enableDownloadByZip;
	}

	public boolean isHttpsEnabled() {
		return openHttps;
	}

	public boolean openHttps() {
		return openHttps;
	}

	public String getHttpsKeyType() {
		return httpsKeyType;
	}

	public String getHttpsKeyFile() {
		return httpsKeyFile;
	}

	public String getHttpsKeyPass() {
		return httpsKeyPass;
	}

	public int getHttpsPort() {
		return httpsPort;
	}

	public String getFileNodePathURL() {
		return dbURL;
	}

	public String getFileNodePathDriver() {
		return dbDriver;
	}

	public String getFileNodePathUserName() {
		return dbUser;
	}

	public String getFileNodePathPassWord() {
		return dbPwd;
	}

	public boolean useMySQL() {
		return serverp == null ? false : "true".equals(serverp.getProperty("mysql.enable"));
	}

	public String getCorsAllowedOrigins() {
		return serverp == null ? "" : serverp.getProperty("cors.allowedOrigins", "");
	}

	public String getHttpsRedirectHost() {
		return serverp == null ? null : serverp.getProperty("https.redirect.host");
	}

	public String getRecycleBinPath() {
		return recycleBinPath;
	}

	public boolean enableIPRule() {
		return enableIPRule;
	}

	public boolean filterAccessIP(String ipAddr) {
		return enableIPRule ? ipAllowOrBanned ^ ipRoster.contains(ipAddr) : false;
	}

	// ==================== Server Settings Update ====================

	public boolean doUpdate(final ServerSetting ss) {
		if (ss != null) {
			Printer.instance.print("正在更新服务器配置...");
			this.serverp.setProperty("mustLogin", ss.isMustLogin() ? "N" : "O");
			this.serverp.setProperty("buff.size", ss.getBuffSize() + "");
			this.serverp.setProperty("password.change", ss.isAllowChangePassword() ? "Y" : "N");
			this.serverp.setProperty("openFileChain", ss.isOpenFileChain() ? "OPEN" : "CLOSE");
			String loglevelCode = "E";
			switch (ss.getLog()) {
			case Event:
				loglevelCode = "E";
				break;
			case Runtime_Exception:
				loglevelCode = "R";
				break;
			case None:
				loglevelCode = "N";
				break;
			}
			this.serverp.setProperty("log", loglevelCode);
			switch (ss.getVc()) {
			case Standard:
				this.serverp.setProperty("VC.level", "STANDARD");
				break;
			case Close:
				this.serverp.setProperty("VC.level", "CLOSE");
				break;
			case Simplified:
				this.serverp.setProperty("VC.level", "SIMP");
				break;
			}
			this.serverp.setProperty("port", ss.getPort() + "");
			this.serverp.setProperty("FS.path",
					(ss.getFsPath() + File.separator).equals(this.defaultFileSystemPath) ? "DEFAULT"
							: ss.getFsPath());
			for (short i = 1; i < MAX_EXTENDSTORES_NUM; i++) {
				this.serverp.removeProperty("FS.extend." + i);
			}
			for (ExtendStores es : ss.getExtendStores()) {
				this.serverp.setProperty("FS.extend." + es.getIndex(), es.getPath().getAbsolutePath());
			}
			if (this.validate() == 0) {
				try {
					storePropertiesAtomically("server.properties", this.serverp, null);
					Printer.instance.print("配置更新完毕，准备就绪。");
					return true;
				} catch (IOException e) {
					Printer.instance.print("错误：更新设置失败，无法存入设置文件。");
				}
			}
		}
		return false;
	}

	// ==================== Init form values ====================

	public String getInitPort() {
		if (this.serverp != null && serverp.getProperty("port") != null) {
			return serverp.getProperty("port");
		}
		return "8080";
	}

	public String getInitBuffSize() {
		if (this.serverp != null && serverp.getProperty("buff.size") != null) {
			return serverp.getProperty("buff.size");
		}
		return "1048576";
	}

	public String getInitFileSystemPath() {
		if (this.serverp != null && serverp.getProperty("FS.path") != null) {
			return serverp.getProperty("FS.path").equals("DEFAULT") ? defaultFileSystemPath
					: serverp.getProperty("FS.path");
		}
		return defaultFileSystemPath;
	}

	public LogLevel getInitLogLevel() {
		if (serverp != null && serverp.getProperty("log") != null) {
			switch (serverp.getProperty("log")) {
			case "N":
				return LogLevel.None;
			case "R":
				return LogLevel.Runtime_Exception;
			case "E":
				return LogLevel.Event;
			default:
				return LogLevel.Event;
			}
		}
		return LogLevel.Event;
	}

	public VCLevel getInitVCLevel() {
		if (serverp != null && serverp.getProperty("VC.level") != null) {
			switch (serverp.getProperty("VC.level")) {
			case "STANDARD":
				return VCLevel.Standard;
			case "SIMP":
				return VCLevel.Simplified;
			case "CLOSE":
				return VCLevel.Close;
			default:
				return VCLevel.Standard;
			}
		}
		return VCLevel.Standard;
	}

	// ==================== Internal ====================

	private int validate() {
		Printer.instance.print("正在检查服务器配置...");
		final String pMustLogin = this.serverp.getProperty("mustLogin");
		if (pMustLogin == null) {
			Printer.instance.print("警告：未找到是否必须登录配置，将采用默认值（O）。");
			this.mustLogin = "O";
		} else {
			if (!"N".equals(pMustLogin) && !"O".equals(pMustLogin)) {
				Printer.instance.print("错误：必须登入功能配置不正确（只能设置为\"O\"或\"N\"），请重新检查。");
				return INVALID_MUST_LOGIN_SETTING;
			}
			this.mustLogin = pMustLogin;
		}
		final String ports = this.serverp.getProperty("port");
		if (ports == null) {
			Printer.instance.print("警告：未找到端口配置，将采用默认值（8080）。");
			this.port = 8080;
		} else {
			try {
				this.port = Integer.parseInt(ports);
				if (this.port <= 0 || this.port > 65535) {
					Printer.instance.print("错误：端口号配置不正确，必须使用1-65535之间的整数。");
					return INVALID_PORT;
				}
			} catch (NumberFormatException e) {
				Printer.instance.print("错误：端口号配置不正确，必须使用1-65535之间的整数。");
				return INVALID_PORT;
			}
		}
		final String logs = this.serverp.getProperty("log");
		if (logs == null) {
			Printer.instance.print("警告：未找到日志等级配置，将采用默认值（E）。");
			this.log = "E";
		} else {
			if (!logs.equals("N") && !logs.equals("R") && !logs.equals("E")) {
				Printer.instance.print("错误：日志等级配置不正确（只能设置为\"N\"、\"R\"或\"E\"），请重新检查。");
				return INVALID_LOG;
			}
			this.log = logs;
		}
		final String vcl = this.serverp.getProperty("VC.level");
		if (vcl == null) {
			Printer.instance.print("警告：未找到登录验证码配置，将采用默认值（STANDARD）。");
			this.vc = "STANDARD";
		} else {
			switch (vcl) {
			case "STANDARD":
			case "SIMP":
			case "CLOSE":
				this.vc = vcl;
				break;
			default:
				Printer.instance.print("错误：登录验证码配置不正确（只能设置为\"STANDARD\"、\"SIMP\"或\"CLOSE\"），请重新检查。");
				return INVALID_VC;
			}
		}
		final String changePassword = this.serverp.getProperty("password.change");
		if (changePassword == null) {
			Printer.instance.print("警告：未找到用户修改密码功能配置，将采用默认值（禁用）。");
			this.allowChangePassword = false;
		} else {
			switch (changePassword) {
			case "Y":
				this.allowChangePassword = true;
				break;
			case "N":
				this.allowChangePassword = false;
				break;
			default:
				Printer.instance.print("错误：用户修改账户密码功能配置不正确（只能设置为\"Y\"或\"N\"），请重新检查。");
				return INVALID_CHANGE_PASSWORD_SETTING;
			}
		}
		final String fileChain = this.serverp.getProperty("openFileChain");
		if (fileChain == null) {
			Printer.instance.print("警告：未找到永久资源链接功能配置，将采用默认值（禁用）。");
			this.openFileChain = false;
		} else {
			switch (fileChain) {
			case "OPEN":
				this.openFileChain = true;
				break;
			case "CLOSE":
				this.openFileChain = false;
				break;
			default:
				Printer.instance.print("错误：永久资源链接功能配置不正确（只能设置为\"OPEN\"或\"CLOSE\"），请重新检查。");
				return INVALID_FILE_CHAIN_SETTING;
			}
		}
		final String bufferSizes = this.serverp.getProperty("buff.size");
		if (bufferSizes == null) {
			Printer.instance.print("警告：未找到缓冲大小配置，将采用默认值（1048576）。");
			this.bufferSize = 1048576;
		} else {
			try {
				this.bufferSize = Integer.parseInt(bufferSizes);
				if (this.bufferSize <= 0) {
					Printer.instance.print("错误：缓冲区大小设置无效。");
					return INVALID_BUFFER_SIZE;
				}
			} catch (NumberFormatException e2) {
				Printer.instance.print("错误：缓冲区大小设置无效。");
				return INVALID_BUFFER_SIZE;
			}
		}
		this.rawFSPath = this.serverp.getProperty("FS.path");
		if (this.rawFSPath == null) {
			Printer.instance.print("警告：未找到主文件系统路径配置，将采用默认值。");
			this.fileSystemPath = this.defaultFileSystemPath;
		} else if (this.rawFSPath.equals("DEFAULT")) {
			this.fileSystemPath = this.defaultFileSystemPath;
		} else {
			this.fileSystemPath = this.rawFSPath.replace("\\:", ":").replace("\\\\", "\\");
		}
		if (!fileSystemPath.endsWith(File.separator)) {
			fileSystemPath = fileSystemPath + File.separator;
		}
		extendStores.clear();
		for (short i = 1; i < MAX_EXTENDSTORES_NUM + 1; i++) {
			if (serverp.getProperty("FS.extend." + i) != null) {
				ExtendStores es = new ExtendStores();
				es.setPath(new File(
							serverp.getProperty("FS.extend." + i).replace("\\:", ":").replace("\\\\", "\\")));
				es.setIndex(i);
				extendStores.add(es);
			}
		}
		final File fsFile = new File(this.fileSystemPath);
		if (!fsFile.isDirectory() || !fsFile.canRead() || !fsFile.canWrite()) {
			Printer.instance.print("错误：文件系统路径[" + this.fileSystemPath + "]无效，该路径必须指向一个具备读写权限的文件夹。");
			return INVALID_FILE_SYSTEM_PATH;
		}
		for (ExtendStores es : extendStores) {
			if (!es.getPath().isDirectory() || !es.getPath().canRead() || !es.getPath().canWrite()) {
				Printer.instance.print(
						"错误：扩展存储区路径[" + es.getPath().getAbsolutePath() + "]无效，该路径必须指向一个具备读写权限的文件夹。");
				return INVALID_FILE_SYSTEM_PATH;
			}
		}
		for (int i = 0; i < extendStores.size() - 1; i++) {
			for (int j = i + 1; j < extendStores.size(); j++) {
				if (extendStores.get(i).getPath().equals(extendStores.get(j).getPath())) {
					Printer.instance.print("错误：扩展存储区路径[" + extendStores.get(j).getPath().getAbsolutePath()
							+ "]无效，该路径已被其他扩展存储区占用。");
					return INVALID_FILE_SYSTEM_PATH;
				}
			}
		}
		this.fileBlockPath = this.fileSystemPath + "fileblocks" + File.separator;
		final File fbFile = new File(this.fileBlockPath);
		if (!fbFile.isDirectory() && !fbFile.mkdirs()) {
			Printer.instance.print("错误：无法创建文件块存放区[" + this.fileBlockPath + "]。");
			return CANT_CREATE_FILE_BLOCK_PATH;
		}
		this.fileNodePath = this.fileSystemPath + "filenodes" + File.separator;
		final File fnFile = new File(this.fileNodePath);
		if (!fnFile.isDirectory() && !fnFile.mkdirs()) {
			Printer.instance.print("错误：无法创建文件节点存放区[" + this.fileNodePath + "]。");
			return CANT_CREATE_FILE_NODE_PATH;
		}
		this.tempFilePath = this.fileSystemPath + "temporaryfiles" + File.separator;
		final File tpFile = new File(tempFilePath);
		if (!tpFile.isDirectory() && !tpFile.mkdirs()) {
			Printer.instance.print("错误：无法创建临时文件存放区[" + this.tempFilePath + "]。");
			return CANT_CREATE_TF_PATH;
		}
		if ("true".equals(serverp.getProperty("mysql.enable"))) {
			dbDriver = "com.mysql.cj.jdbc.Driver";
			String url = serverp.getProperty("mysql.url", "127.0.0.1/kift");
			int slashIndex = url.indexOf("/");
			if (slashIndex <= 0 || url.substring(slashIndex).length() == 1) {
				Printer.instance.print("错误：自定义数据库的URL中必须指定数据库名称。");
				return CANT_CONNECT_DB;
			}
			dbURL = "jdbc:mysql://" + url + "?useUnicode=true&characterEncoding=utf8";
			dbUser = serverp.getProperty("mysql.user", "root");
			dbPwd = serverp.getProperty("mysql.password", "");
			timeZone = serverp.getProperty("mysql.timezone");
			if (timeZone != null) {
				dbURL = dbURL + "&serverTimezone=" + timeZone;
			}
			try {
				Class.forName(dbDriver).getDeclaredConstructor().newInstance();
				Connection testConn = DriverManager.getConnection(dbURL, dbUser, dbPwd);
				testConn.close();
			} catch (ReflectiveOperationException | SQLException e) {
				Printer.instance.print("错误：无法连接至自定义数据库：" + dbURL + "（user=" + dbUser
						+ "），请确重新配置MySQL数据库相关项。");
				return CANT_CONNECT_DB;
			}
		} else {
			dbDriver = "org.h2.Driver";
			dbURL = "jdbc:h2:file:" + fileNodePath + "kift";
			dbUser = "root";
			// 默认口令用于兼容历史数据；如需修改，可在 server.properties 中配置 db.pwd 覆盖
			dbPwd = serverp.getProperty("db.pwd", "301537gY");
		}
		String enableHttps = serverp.getProperty("https.enable");
		if (enableHttps != null) {
			if ("true".equals(enableHttps)) {
				File keyFile = new File(basePath, "https.p12");
				if (keyFile.isFile()) {
					httpsKeyType = "PKCS12";
				} else {
					keyFile = new File(basePath, "https.jks");
					if (keyFile.isFile()) {
						httpsKeyType = "JKS";
					} else {
						Printer.instance.print(
								"错误：无法启用https支持，因为kiftd未能找到https证书文件。您必须在应用主目录内放置PKCS12（必须命名为https.p12）或JKS（必须命名为https.jks）证书。");
						return HTTPS_SETTING_ERROR;
					}
				}
				httpsKeyFile = keyFile.getAbsolutePath();
				httpsKeyPass = serverp.getProperty("https.keypass", "");
				String httpsports = serverp.getProperty("https.port");
				if (httpsports == null) {
					Printer.instance.print("警告：未找到https端口配置，将采用默认值（443）。");
					httpsPort = 443;
				} else {
					try {
						this.httpsPort = Integer.parseInt(httpsports);
						if (httpsPort <= 0 || httpsPort > 65535) {
							Printer.instance.print("错误：无法启用https支持，https访问端口号配置不正确。");
							return HTTPS_SETTING_ERROR;
						}
					} catch (NumberFormatException e) {
						Printer.instance.print("错误：无法启用https支持，https访问端口号配置不正确。");
						return HTTPS_SETTING_ERROR;
					}
				}
				openHttps = true;
			} else if (!"false".equals(enableHttps)) {
				Printer.instance.print("错误：https支持功能的启用项配置不正确（只能设置为\"true\"或\"false\"），请重新检查。");
				return HTTPS_SETTING_ERROR;
			}
		}
		String xffConf = serverp.getProperty("IP.xff");
		if (xffConf != null) {
			switch (xffConf) {
			case "disable":
				ipXFFAnalysis = false;
				break;
			case "enable":
				ipXFFAnalysis = true;
				break;
			default:
				Printer.instance.print("错误：IP地址xff解析配置不正确（只能设置为\"disable\"或\"enable\"），请重新检查。");
				return INVALID_IP_XFF_SETTING;
			}
		} else {
			// 默认关闭 XFF 解析，防止客户端伪造 X-Forwarded-For 头绕过 IP 访问规则；
			// 仅在明确部署于可信反向代理之后时，才应配置 IP.xff=enable
			ipXFFAnalysis = false;
		}
		String ffmpegConf = serverp.getProperty("video.ffmpeg");
		if (ffmpegConf != null) {
			switch (ffmpegConf) {
			case "disable":
				enableFFMPEG = false;
				break;
			case "enable":
				enableFFMPEG = true;
				break;
			default:
				Printer.instance.print("错误：视频播放功能的在线解码配置不正确（只能设置为\"disable\"或\"enable\"），请重新检查。");
				return INVALID_FFMPEG_SETTING;
			}
		} else {
			enableFFMPEG = true;
		}
		String downloadZipConf = serverp.getProperty("download.zip");
		if (downloadZipConf != null) {
			switch (downloadZipConf) {
			case "disable":
				enableDownloadByZip = false;
				break;
			case "enable":
				enableDownloadByZip = true;
				break;
			default:
				Printer.instance.print("错误：\"打包下载\"功能的配置不正确（只能设置为\"disable\"或\"enable\"），请重新检查。");
				return INVALID_DOWNLOAD_ZIP_SETTING;
			}
		} else {
			enableDownloadByZip = true;
		}
		String recycleBinPathProp = this.serverp.getProperty("recyclebin");
		if (recycleBinPathProp != null && !recycleBinPathProp.isEmpty()) {
			recycleBinPathProp = recycleBinPathProp.replace("\\:", ":").replace("\\\\", "\\");
			if (!recycleBinPathProp.endsWith(File.separator)) {
				recycleBinPathProp = recycleBinPathProp + File.separator;
			}
			File recycleBin = new File(recycleBinPathProp);
			if (!recycleBin.isDirectory() || !recycleBin.canWrite() || !recycleBin.canRead()) {
				Printer.instance.print("错误：删除留档功能的配置不正确，必须是一个可读写的文件夹。");
				return INVALID_RECYCLE_BIN_PATH;
			}
			this.recycleBinPath = recycleBin.getAbsolutePath();
		}
		Printer.instance.print("检查完毕。");
		return LEGAL_PROPERTIES;
	}

	private void initIPRules() {
		ipRoster.clear();
		String ipRosterSetting = accountp.getProperty("IP.allow");
		if (ipRosterSetting != null && ipRosterSetting.length() > 0) {
			ipAllowOrBanned = false;
			enableIPRule = true;
			ipRoster.addAll(Arrays.asList(ipRosterSetting.split(";")));
			return;
		}
		ipRosterSetting = accountp.getProperty("IP.banned");
		if (ipRosterSetting != null && ipRosterSetting.length() > 0) {
			ipAllowOrBanned = true;
			enableIPRule = true;
			ipRoster.addAll(Arrays.asList(ipRosterSetting.split(";")));
			return;
		}
		enableIPRule = false;
	}

	private void initSignUpRules() {
		final String signUpAuth = this.accountp.getProperty("authSignup");
		final String signUpGroup = this.accountp.getProperty("groupSignup");
		if (signUpAuth != null || signUpGroup != null) {
			this.allowSignUp = true;
			this.signUpAuth = signUpAuth;
			this.signUpGroup = signUpGroup;
		} else {
			this.allowSignUp = false;
		}
	}

	public void startAccountUpdateListener() {
		if (accountUpdateDaemon == null) {
			synchronized (ConfigurationManager.class) {
				if (accountUpdateDaemon == null) {
					Path confPath = Paths.get(confDir);
					accountUpdateDaemon = new Thread(() -> {
						WatchService ws = null;
						try {
							ws = confPath.getFileSystem().newWatchService();
							confPath.register(ws, StandardWatchEventKinds.ENTRY_MODIFY,
									StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE);
							while (true) {
								WatchKey wk = ws.take();
								List<WatchEvent<?>> es = wk.pollEvents();
								for (WatchEvent<?> we : es) {
									if ("account.properties".equals(we.context().toString())) {
										Printer.instance.print("正在更新账户配置信息...");
										final File accountProp = new File(this.confDir + "account.properties");
										if (accountProp.isFile() && accountProp.canRead()) {
											try (final FileInputStream accountPropIn = new FileInputStream(accountProp)) {
												synchronized (accountp) {
													this.accountp.load(accountPropIn);
												}
												initIPRules();
												initSignUpRules();
												Printer.instance.print("账户配置更新完成，已加载最新配置。");
											}
										} else {
											accountp.clear();
											Printer.instance.print("警告：账户配置文件已被删除或无法读取，账户信息已清空。");
										}
									}
								}
								if (!wk.reset()) {
									break;
								}
							}
						} catch (Exception e) {
							Printer.instance.print(
									"错误：用户配置文件更改监听失败，该功能已失效，kiftd无法实时更新用户配置（可尝试重启程序以恢复该功能）。");
						} finally {
							if (ws != null) {
								try {
									ws.close();
								} catch (IOException ignored) {
									// 关闭监听 WatchService 失败无需处理：功能失效已有外层异常提示
								}
							}
						}
					});
					accountUpdateDaemon.setDaemon(true);
					accountUpdateDaemon.start();
				}
			}
		}
	}

	public void createDefaultServerPropertiesFile() {
		Printer.instance.print("正在生成初始服务器配置文件（" + this.confDir + "server.properties）...");
		final java.util.Properties dsp = new java.util.Properties();
		dsp.setProperty("mustLogin", "O");
		dsp.setProperty("port", "8080");
		dsp.setProperty("log", "E");
		dsp.setProperty("VC.level", "STANDARD");
		dsp.setProperty("FS.path", "DEFAULT");
		dsp.setProperty("buff.size", "1048576");
		dsp.setProperty("password.change", "N");
		dsp.setProperty("openFileChain", "CLOSE");
		try (FileOutputStream fos = new FileOutputStream(this.confDir + "server.properties")) {
			dsp.store(fos, "<This is the default kiftd server setting file. >");
			Printer.instance.print("初始服务器配置文件生成完毕。");
		} catch (IOException e) {
			Printer.instance.print("错误：无法生成初始服务器配置文件，存储路径不存在或写入失败。");
		}
	}

	private void createDefaultAccountPropertiesFile() {
		Printer.instance.print("正在生成初始账户配置文件（" + this.confDir + "account.properties）...");
		final java.util.Properties dap = new java.util.Properties();
		// 默认密码同样以 PBKDF2 哈希存储，避免配置文件明文泄露
		dap.setProperty("admin.pwd", PasswordUtil.hashPassword("000000"));
		dap.setProperty("admin.auth", "cudrm");
		dap.setProperty("authOverall", "l");
		try (FileOutputStream accountSettingOut = new FileOutputStream(this.confDir + "account.properties")) {
			dap.store(accountSettingOut, "<This is the default kiftd account setting file. >");
			Printer.instance.print("初始账户配置文件生成完毕。");
			Printer.instance.print("警告：已创建默认管理员账户 admin，初始密码为 000000，请登录后立即修改！");
		} catch (IOException e) {
			Printer.instance.print("错误：无法生成初始账户配置文件，存储路径不存在或写入失败。");
		}
	}
}
