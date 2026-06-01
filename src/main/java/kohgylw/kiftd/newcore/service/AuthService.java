package kohgylw.kiftd.newcore.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.newcore.domain.OperationResult;

public interface AuthService {

	OperationResult login(HttpServletRequest request, HttpSession session);

	void logout(HttpSession session);

	String getPublicKeyJson();

	void getVerificationCode(HttpServletRequest request, HttpServletResponse response, HttpSession session);

	OperationResult changePassword(HttpServletRequest request);

	String doPong(HttpServletRequest request);

	boolean isAllowSignUp();

	OperationResult signUp(HttpServletRequest request);
}
