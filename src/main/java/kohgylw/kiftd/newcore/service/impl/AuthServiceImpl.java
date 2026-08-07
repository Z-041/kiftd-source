package kohgylw.kiftd.newcore.service.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import com.google.gson.Gson;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import kohgylw.kiftd.newcore.domain.OperationResult;
import kohgylw.kiftd.newcore.service.AuthService;
import kohgylw.kiftd.server.enumeration.VCLevel;
import kohgylw.kiftd.server.pojo.ChangePasswordInfoPojo;
import kohgylw.kiftd.server.pojo.LoginInfoPojo;
import kohgylw.kiftd.server.pojo.PublicKeyInfo;
import kohgylw.kiftd.server.pojo.SignUpInfoPojo;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.RSADecryptUtil;
import kohgylw.kiftd.server.util.RSAKeyUtil;
import kohgylw.kiftd.server.util.VerificationCode;
import kohgylw.kiftd.server.util.VerificationCodeFactory;


@Service
@Primary
public class AuthServiceImpl implements AuthService {

	private final RSAKeyUtil ku;
	private final LogUtil lu;
	private final Gson gson;

	private static final long TIME_OUT = 30000L;
	private static final java.nio.charset.CharsetEncoder ISO_8859_1_ENCODER = StandardCharsets.ISO_8859_1.newEncoder();
	// 账户名白名单：仅允许字母、数字、下划线、点号与连字符（3-32位），
	// 排除空白、控制字符及配置文件/HTML 特殊字符，防止配置注入与存储型XSS
	private static final Pattern ACCOUNT_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.\\-]{3,32}$");
	// 密码白名单：允许任意非空白字符（3-32位），排除空白与控制字符
	private static final Pattern PASSWORD_PATTERN = Pattern.compile("^\\S{3,32}$");

	private VerificationCodeFactory vcf;

	private static final Set<String> focusAccount = new HashSet<>();
	// 登录失败限速：以 IP+账户 为维度统计连续失败次数，达到阈值后锁定一段时间，防止暴力破解
	private static final int MAX_LOGIN_FAILURES = 5;
	private static final long LOGIN_LOCK_DURATION_MS = 15 * 60 * 1000L;
	private final Map<String, LoginFailRecord> loginFailRecords = new ConcurrentHashMap<>();
	private final IpAddrGetter ipAddrGetter;

	// 登录失败计数记录
	private static class LoginFailRecord {
		volatile int count;
		volatile long lockUntil;
		volatile long lastFailTime;
	}

	public AuthServiceImpl(RSAKeyUtil ku, LogUtil lu, Gson gson, IpAddrGetter ipAddrGetter) {
		this.ku = ku;
		this.lu = lu;
		this.gson = gson;
		this.ipAddrGetter = ipAddrGetter;
		VCLevel vcLevel = ConfigurationManager.instance().getVCLevel();
		if (!vcLevel.equals(VCLevel.Close)) {
			int line = 0;
			int oval = 0;
			switch (vcLevel) {
			case Standard:
				line = 6;
				oval = 2;
				break;
			case Simplified:
				line = 1;
				oval = 0;
				break;
			default:
				break;
			}
			vcf = new VerificationCodeFactory(45, line, oval, 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm',
					'n', 'p', 'q', 'r', 's', 't', 'w', 'x', 'y', 'z', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
					'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'W', 'X', 'Y', 'Z');
		}
	}

	@Override
	public OperationResult login(HttpServletRequest request, HttpSession session) {
		final String encrypted = request.getParameter("encrypted");
		try {
			final String loginInfoStr = RSADecryptUtil.dncryption(encrypted, ku.getPrivateKey());
			if (loginInfoStr == null) {
				// 缺少密文参数或解密失败（如伪造/损坏的密文），直接判定登录失败
				return OperationResult.failure("error");
			}
			final LoginInfoPojo info = gson.fromJson(loginInfoStr.replace("\\", "\\\\"), LoginInfoPojo.class);
			if (System.currentTimeMillis() - Long.parseLong(info.getTime()) > TIME_OUT) {
				return OperationResult.failure("error");
			}
			final String accountId = info.getAccountId();
			// 登录失败限速：以 IP+账户 为维度统计连续失败次数，超过阈值后锁定一段时间
			final String failKey = ipAddrGetter.getIpAddr(request) + "|" + accountId;
			final long now = System.currentTimeMillis();
			// 惰性清理已过期的锁定记录，避免缓存无限增长
			loginFailRecords.values().removeIf(v -> v.lockUntil > 0 && v.lockUntil + LOGIN_LOCK_DURATION_MS < now);
			LoginFailRecord record = loginFailRecords.computeIfAbsent(failKey, k -> new LoginFailRecord());
			if (record.lockUntil > now) {
				return OperationResult.failure("attemptslimit");
			}
			final boolean accountExists = ConfigurationManager.instance().foundAccount(accountId);
			final boolean passwordCorrect = ConfigurationManager.instance().checkAccountPwd(accountId,
					info.getAccountPwd());
			if (!accountExists) {
				recordLoginFail(record);
				return OperationResult.failure("accountnotfound");
			}
			if (!ConfigurationManager.instance().getVCLevel().equals(VCLevel.Close)) {
				synchronized (focusAccount) {
					if (focusAccount.contains(accountId)) {
						String reqVerCode = request.getParameter("vercode");
						String trueVerCode = (String) session.getAttribute("VERCODE");
						session.removeAttribute("VERCODE");
						if (reqVerCode == null || trueVerCode == null
								|| !trueVerCode.equals(reqVerCode.toLowerCase())) {
							return OperationResult.failure("needsubmitvercode");
						}
					}
				}
			}
			if (passwordCorrect) {
				// 密码以明文存储于 account.properties（纯文件控制），登录成功后不再自动升级为哈希
				session.invalidate();
				HttpSession newSession = request.getSession(true);
				newSession.setAttribute("ACCOUNT", (Object) accountId);
				if (!ConfigurationManager.instance().getVCLevel().equals(VCLevel.Close)) {
					synchronized (focusAccount) {
						focusAccount.remove(accountId);
					}
				}
				loginFailRecords.remove(failKey);
				return OperationResult.success("permitlogin");
			}
			synchronized (focusAccount) {
				if (!ConfigurationManager.instance().getVCLevel().equals(VCLevel.Close)) {
					focusAccount.add(accountId);
				}
			}
			recordLoginFail(record);
			return OperationResult.failure("accountpwderror");
		} catch (Exception e) {
			lu.writeException(e);
			return OperationResult.failure("error");
		}
	}

	/**
	 * 记录一次登录失败；当连续失败次数达到阈值时锁定该 IP+账户 组合一段时长。
	 */
	private void recordLoginFail(LoginFailRecord record) {
		synchronized (record) {
			record.lastFailTime = System.currentTimeMillis();
			if (++record.count >= MAX_LOGIN_FAILURES) {
				record.lockUntil = System.currentTimeMillis() + LOGIN_LOCK_DURATION_MS;
				record.count = 0;
			}
		}
	}

	@Override
	public void logout(HttpSession session) {
		session.invalidate();
	}

	@Override
	public String getPublicKeyJson() {
		PublicKeyInfo pki = new PublicKeyInfo();
		pki.setPublicKey(ku.getPublicKey());
		pki.setTime(System.currentTimeMillis());
		return gson.toJson(pki);
	}

	@Override
	public void getVerificationCode(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		try {
			if (ConfigurationManager.instance().getVCLevel().equals(VCLevel.Close)) {
				response.sendError(404);
			} else {
				VerificationCode vc = vcf.next(4);
				session.setAttribute("VERCODE", vc.getCode());
				response.setContentType("image/png");
				try (OutputStream out = response.getOutputStream()) {
					vc.saveTo(out);
					out.flush();
				}
			}
		} catch (IOException e) {
			try {
				response.sendError(500);
			} catch (IOException e1) {
				this.lu.writeException(e1);
			}
		}
	}

	@Override
	public OperationResult changePassword(HttpServletRequest request) {
		if (!ConfigurationManager.instance().isAllowChangePassword()) {
			return OperationResult.failure("illegal");
		}
		HttpSession session = request.getSession();
		final String account = (String) session.getAttribute("ACCOUNT");
		if (account == null) {
			return OperationResult.failure("mustlogin");
		}
		final String encrypted = request.getParameter("encrypted");
		try {
			final String changePasswordInfoStr = RSADecryptUtil.dncryption(encrypted, ku.getPrivateKey());
			final ChangePasswordInfoPojo info = gson.fromJson(changePasswordInfoStr.replace("\\", "\\\\"),
					ChangePasswordInfoPojo.class);
			if (System.currentTimeMillis() - Long.parseLong(info.getTime()) > TIME_OUT) {
				return OperationResult.failure("error");
			}
			if (!ConfigurationManager.instance().getVCLevel().equals(VCLevel.Close)) {
				synchronized (focusAccount) {
					if (focusAccount.contains(account)) {
						String reqVerCode = request.getParameter("vercode");
						String trueVerCode = (String) session.getAttribute("VERCODE");
						session.removeAttribute("VERCODE");
						if (reqVerCode == null || trueVerCode == null
								|| !trueVerCode.equals(reqVerCode.toLowerCase())) {
							return OperationResult.failure("needsubmitvercode");
						}
					}
				}
			}
			if (ConfigurationManager.instance().checkAccountPwd(account, info.getOldPwd())) {
				if (!ConfigurationManager.instance().getVCLevel().equals(VCLevel.Close)) {
					synchronized (focusAccount) {
						focusAccount.remove(account);
					}
				}
				String newPassword = info.getNewPwd();
				if (newPassword != null && newPassword.length() >= 3 && newPassword.length() <= 32
						&& ISO_8859_1_ENCODER.canEncode(newPassword)) {
					if (ConfigurationManager.instance().changePassword(account, newPassword)) {
						lu.writeChangePasswordEvent(request, account);
						return OperationResult.success("success");
					}
				}
				return OperationResult.failure("invalidnewpwd");
			} else {
				synchronized (focusAccount) {
					if (!ConfigurationManager.instance().getVCLevel().equals(VCLevel.Close)) {
						focusAccount.add(account);
					}
				}
				return OperationResult.failure("oldpwderror");
			}
		} catch (Exception e) {
			lu.writeException(e);
			return OperationResult.failure("cannotchangepwd");
		}
	}

	@Override
	public String doPong(HttpServletRequest request) {
		if (request.getSession().getAttribute("ACCOUNT") != null) {
			return "pong";
		}
		return "";
	}

	@Override
	public boolean isAllowSignUp() {
		return ConfigurationManager.instance().isAllowSignUp();
	}

	@Override
	public OperationResult signUp(HttpServletRequest request) {
		if (!ConfigurationManager.instance().isAllowSignUp()) {
			return OperationResult.failure("illegal");
		}
		HttpSession session = request.getSession();
		if (session.getAttribute("ACCOUNT") != null) {
			return OperationResult.failure("mustlogout");
		}
		String reqVerCode = request.getParameter("vercode");
		if (!ConfigurationManager.instance().getVCLevel().equals(VCLevel.Close)) {
			String trueVerCode = (String) session.getAttribute("VERCODE");
			session.removeAttribute("VERCODE");
			if (reqVerCode == null || trueVerCode == null || !trueVerCode.equals(reqVerCode.toLowerCase())) {
				return OperationResult.failure("needvercode");
			}
		}
		final String encrypted = request.getParameter("encrypted");
		try {
			final String signUpInfoStr = RSADecryptUtil.dncryption(encrypted, ku.getPrivateKey());
			final SignUpInfoPojo info = gson.fromJson(signUpInfoStr.replace("\\", "\\\\"),
					SignUpInfoPojo.class);
			if (System.currentTimeMillis() - Long.parseLong(info.getTime()) > TIME_OUT) {
				return OperationResult.failure("error");
			}
			if (ConfigurationManager.instance().foundAccount(info.getAccount())) {
				return OperationResult.failure("accountexists");
			}
			String account = info.getAccount();
			String password = info.getPwd();
			// 账户名须符合白名单（字母、数字、下划线、点号、连字符），密码须为非空白可打印字符
			if (account == null || !ACCOUNT_NAME_PATTERN.matcher(account).matches()
					|| !ISO_8859_1_ENCODER.canEncode(account)) {
				return OperationResult.failure("invalidaccount");
			}
			if (password == null || !PASSWORD_PATTERN.matcher(password).matches()
					|| !ISO_8859_1_ENCODER.canEncode(password)) {
				return OperationResult.failure("invalidpwd");
			}
			if (ConfigurationManager.instance().createNewAccount(account, password)) {
				lu.writeSignUpEvent(request, account);
				session.invalidate();
				HttpSession newSession = request.getSession(true);
				newSession.setAttribute("ACCOUNT", account);
				return OperationResult.success("success");
			} else {
				return OperationResult.failure("cannotsignup");
			}
		} catch (Exception e) {
			lu.writeException(e);
			return OperationResult.failure("cannotsignup");
		}
	}
}
