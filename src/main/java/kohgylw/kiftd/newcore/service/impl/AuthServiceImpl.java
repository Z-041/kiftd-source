package kohgylw.kiftd.newcore.service.impl;

import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.newcore.domain.OperationResult;
import kohgylw.kiftd.newcore.service.AuthService;
import kohgylw.kiftd.server.util.RSAKeyUtil;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.VerificationCodeFactory;
import kohgylw.kiftd.server.util.VerificationCode;
import kohgylw.kiftd.server.util.RSADecryptUtil;
import kohgylw.kiftd.server.enumeration.VCLevel;
import kohgylw.kiftd.server.pojo.LoginInfoPojo;
import kohgylw.kiftd.server.pojo.ChangePasswordInfoPojo;
import kohgylw.kiftd.server.pojo.SignUpInfoPojo;
import kohgylw.kiftd.server.pojo.PublicKeyInfo;

import com.google.gson.Gson;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Service
@Primary
public class AuthServiceImpl implements AuthService {

	private final RSAKeyUtil ku;
	private final LogUtil lu;
	private final Gson gson;

	private static final long TIME_OUT = 30000L;
	private static final java.nio.charset.CharsetEncoder ISO_8859_1_ENCODER = StandardCharsets.ISO_8859_1.newEncoder();

	private VerificationCodeFactory vcf;

	private static final Set<String> focusAccount = new HashSet<>();

	public AuthServiceImpl(RSAKeyUtil ku, LogUtil lu, Gson gson) {
		this.ku = ku;
		this.lu = lu;
		this.gson = gson;
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
			final LoginInfoPojo info = gson.fromJson(loginInfoStr.replace("\\", "\\\\"), LoginInfoPojo.class);
			if (System.currentTimeMillis() - Long.parseLong(info.getTime()) > TIME_OUT) {
				return OperationResult.failure("error");
			}
			final String accountId = info.getAccountId();
			final boolean accountExists = ConfigurationManager.instance().foundAccount(accountId);
			final boolean passwordCorrect = ConfigurationManager.instance().checkAccountPwd(accountId,
					info.getAccountPwd());
			if (!accountExists) {
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
				ConfigurationManager.instance().upgradePasswordHashIfNeeded(accountId, info.getAccountPwd());
				session.invalidate();
				HttpSession newSession = request.getSession(true);
				newSession.setAttribute("ACCOUNT", (Object) accountId);
				if (!ConfigurationManager.instance().getVCLevel().equals(VCLevel.Close)) {
					synchronized (focusAccount) {
						focusAccount.remove(accountId);
					}
				}
				return OperationResult.success("permitlogin");
			}
			synchronized (focusAccount) {
				if (!ConfigurationManager.instance().getVCLevel().equals(VCLevel.Close)) {
					focusAccount.add(accountId);
				}
			}
			return OperationResult.failure("accountpwderror");
		} catch (Exception e) {
			lu.writeException(e);
			return OperationResult.failure("error");
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
		} else {
			return "";
		}
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
			if (account != null && account.length() >= 3 && account.length() <= 32
					&& ISO_8859_1_ENCODER.canEncode(account)) {
				if (account.indexOf("=") < 0 && account.indexOf(":") < 0 && account.indexOf("#") != 0) {
					if (password != null && password.length() >= 3 && password.length() <= 32
							&& ISO_8859_1_ENCODER.canEncode(password)) {
						if (ConfigurationManager.instance().createNewAccount(account, password)) {
							lu.writeSignUpEvent(request, account);
							session.invalidate();
							HttpSession newSession = request.getSession(true);
							newSession.setAttribute("ACCOUNT", account);
							return OperationResult.success("success");
						} else {
							return OperationResult.failure("cannotsignup");
						}
					} else {
						return OperationResult.failure("invalidpwd");
					}
				} else {
					return OperationResult.failure("illegalaccount");
				}
			} else {
				return OperationResult.failure("invalidaccount");
			}
		} catch (Exception e) {
			lu.writeException(e);
			return OperationResult.failure("cannotsignup");
		}
	}
}
