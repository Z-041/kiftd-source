package kohgylw.kiftd.newcore.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.newcore.service.AuthService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping({ "/homeController" })
public class AuthController {

	private static final String CHARSET_BY_AJAX = "text/html; charset=utf-8";

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@RequestMapping(value = { "/getPublicKey.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getPublicKey() {
		return this.authService.getPublicKeyJson();
	}

	@RequestMapping({ "/doLogin.ajax" })
	@ResponseBody
	public String doLogin(final HttpServletRequest request, final HttpSession session) {
		return this.authService.login(request, session).getCode();
	}

	@RequestMapping({ "/getNewVerCode.do" })
	public void getNewVerCode(final HttpServletRequest request, final HttpServletResponse response,
			final HttpSession session) {
		authService.getVerificationCode(request, response, session);
	}

	@RequestMapping({ "/doLogout.ajax" })
	@ResponseBody
	public String doLogout(final HttpSession session) {
		this.authService.logout(session);
		return "SUCCESS";
	}

	@RequestMapping(value = { "/doChangePassword.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String doChangePassword(final HttpServletRequest request) {
		return authService.changePassword(request).getCode();
	}

	@RequestMapping(value = { "/ping.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String pong(final HttpServletRequest request) {
		return authService.doPong(request);
	}

	@RequestMapping(value = { "/askForAllowSignUpOrNot.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String askForAllowSignUpOrNot(final HttpServletRequest request) {
		return authService.isAllowSignUp() ? "true" : "false";
	}

	@RequestMapping(value = { "/doSigUp.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String doSigUp(final HttpServletRequest request) {
		return authService.signUp(request).getCode();
	}
}
