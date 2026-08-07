package kohgylw.kiftd.newcore.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.gson.Gson;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kohgylw.kiftd.newcore.domain.AjaxProtocol;
import kohgylw.kiftd.newcore.service.AuthService;
import kohgylw.kiftd.server.util.ConfigurationManager;


@Controller
@RequestMapping({ "/homeController" })
public class AuthController {

	private static final String CHARSET_BY_AJAX = "text/html; charset=utf-8";
	private static final String JSON_BY_AJAX = "application/json; charset=utf-8";
	private static final String SESSION_ACCOUNT_ATTR = "ACCOUNT";

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@RequestMapping(value = { "/getPublicKey.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getPublicKey() {
		return this.authService.getPublicKeyJson();
	}

	@PostMapping({ "/doLogin.ajax" })
	@ResponseBody
	public String doLogin(final HttpServletRequest request, final HttpSession session) {
		return this.authService.login(request, session).getCode();
	}

	@RequestMapping({ "/getNewVerCode.do" })
	public void getNewVerCode(final HttpServletRequest request, final HttpServletResponse response,
			final HttpSession session) {
		authService.getVerificationCode(request, response, session);
	}

	@PostMapping({ "/doLogout.ajax" })
	@ResponseBody
	public String doLogout(final HttpSession session) {
		this.authService.logout(session);
		return AjaxProtocol.SUCCESS;
	}

	@PostMapping(value = { "/doChangePassword.ajax" }, produces = { CHARSET_BY_AJAX })
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

	@PostMapping(value = { "/doSigUp.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String doSigUp(final HttpServletRequest request) {
		return authService.signUp(request).getCode();
	}

	/**
	 * 返回当前登录账户信息（账户名 + 是否超级管理员），供前端渲染账户管理入口。
	 * 未登录时返回 {"account":null,"superAdmin":false}。
	 */
	@RequestMapping(value = { "/getAccountInfo.ajax" }, produces = { JSON_BY_AJAX })
	@ResponseBody
	public String getAccountInfo(final HttpSession session) {
		String account = session == null ? null : (String) session.getAttribute(SESSION_ACCOUNT_ATTR);
		Map<String, Object> info = new HashMap<>();
		info.put("account", account);
		info.put("superAdmin", account != null && ConfigurationManager.instance().isSuperAdmin(account));
		return new Gson().toJson(info);
	}
}
