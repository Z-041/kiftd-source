package kohgylw.kiftd.newcore.service.impl;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.newcore.domain.OperationResult;
import kohgylw.kiftd.newcore.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceAdapter implements AuthService {

	@Resource
	private kohgylw.kiftd.server.service.AccountService legacyAccountService;

	@Override
	public OperationResult login(HttpServletRequest request, HttpSession session) {
		String result = legacyAccountService.checkLoginRequest(request, session);
		if ("permitlogin".equals(result)) {
			return OperationResult.success("permitlogin");
		}
		return OperationResult.failure(result);
	}

	@Override
	public void logout(HttpSession session) {
		legacyAccountService.logout(session);
	}

	@Override
	public String getPublicKeyJson() {
		return legacyAccountService.getPublicKey();
	}

	@Override
	public void getVerificationCode(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		legacyAccountService.getNewLoginVerCode(request, response, session);
	}

	@Override
	public OperationResult changePassword(HttpServletRequest request) {
		String result = legacyAccountService.changePassword(request);
		if ("success".equals(result)) {
			return OperationResult.success("success");
		}
		return OperationResult.failure(result);
	}

	@Override
	public String doPong(HttpServletRequest request) {
		return legacyAccountService.doPong(request);
	}

	@Override
	public boolean isAllowSignUp() {
		return "true".equals(legacyAccountService.isAllowSignUp());
	}

	@Override
	public OperationResult signUp(HttpServletRequest request) {
		String result = legacyAccountService.doSignUp(request);
		if ("success".equals(result)) {
			return OperationResult.success("success");
		}
		return OperationResult.failure(result);
	}
}
