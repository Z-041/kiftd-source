package kohgylw.kiftd.newcore.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kohgylw.kiftd.newcore.domain.ApiResponse;
import kohgylw.kiftd.newcore.domain.ResultCode;
import kohgylw.kiftd.server.util.ConfigurationManager;

/**
 *
 * <h2>账户管理 API 控制器</h2>
 * <p>
 * 面向 Web 端账户自助中心的 JSON 端点，覆盖账户列表、新建、删除、权限修改与密码重置。
 * 服务器配置、存储概览等系统级管理已收敛至桌面端 GUI（MC 图形界面），不再暴露于 Web。
 * 管理员认证由 ApiAuthFilter 统一执行（/api/* 强制 session + 超级管理员），
 * 本控制器不内嵌鉴权逻辑。
 * </p>
 *
 * @author 技术债治理迭代
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin")
public class AccountAdminController {

	private static final String AUTH_CHARS = "cudrml";
	private static final String SESSION_ACCOUNT_ATTR = "ACCOUNT";
	// 与注册通道（AuthServiceImpl）保持一致：账户名 3-32 位字母/数字/下划线/点/连字符，密码 3-32 位非空白
	private static final Pattern ACCOUNT_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.\\-]{3,32}$");
	private static final Pattern PASSWORD_PATTERN = Pattern.compile("^\\S{3,32}$");
	// 权限字符串由 c/u/d/r/m/l 构成，去重后最多 6 位
	private static final int MAX_AUTH_LENGTH = 6;

	private final ConfigurationManager configurationManager;

	public AccountAdminController(ConfigurationManager configurationManager) {
		this.configurationManager = configurationManager;
	}

	@GetMapping("/accounts")
	public ApiResponse<List<Map<String, Object>>> listAccounts() {
		List<Map<String, Object>> accounts = new ArrayList<>();
		for (String account : configurationManager.getAllAccounts()) {
			Map<String, Object> item = new HashMap<>();
			item.put("account", account);
			item.put("auth", configurationManager.getAccountAuth(account));
			item.put("group", configurationManager.getAccountGroup(account));
			item.put("superAdmin", configurationManager.isSuperAdmin(account));
			item.put("uploadMaxSize", configurationManager.getUploadFileSize(account));
			item.put("downloadMaxRate", configurationManager.getDownloadMaxRate(account));
			accounts.add(item);
		}
		return ApiResponse.success(accounts);
	}

	@PostMapping("/accounts")
	public ApiResponse<Void> createAccount(@RequestBody AccountCreateRequest request) {
		if (request.account() == null || request.password() == null) {
			return ApiResponse.failure(ResultCode.BAD_REQUEST.getCode(), "账户名与密码不能为空");
		}
		String account = request.account().trim();
		String password = request.password();
		if (!ACCOUNT_NAME_PATTERN.matcher(account).matches()) {
			return ApiResponse.failure(ResultCode.BAD_REQUEST.getCode(),
					"账户名须为 3-32 位字母/数字/下划线/点/连字符");
		}
		if (configurationManager.isSystemAccount(account)) {
			return ApiResponse.failure(ResultCode.BAD_REQUEST.getCode(), "该账户名为系统保留名称，不可创建");
		}
		if (!PASSWORD_PATTERN.matcher(password).matches()) {
			return ApiResponse.failure(ResultCode.BAD_REQUEST.getCode(), "密码须为 3-32 位非空白字符");
		}
		String auth = request.auth() == null ? "" : request.auth();
		if (!isValidAuth(auth)) {
			return ApiResponse.failure(ResultCode.BAD_REQUEST.getCode(), "权限只能包含 c/u/d/r/m/l 字符且不超过 6 位");
		}
		try {
			if (!configurationManager.createNewAccount(account, password)) {
				return ApiResponse.failure(ResultCode.USER_ALREADY_EXISTS.getCode(), "账户已存在");
			}
			if (!auth.isEmpty()) {
				configurationManager.updateAccountAuth(account, auth);
			}
			return ApiResponse.success();
		} catch (Exception e) {
			return ApiResponse.failure(ResultCode.ERROR.getCode(), "创建账户失败");
		}
	}

	@DeleteMapping("/accounts/{account}")
	public ApiResponse<Void> deleteAccount(@PathVariable String account, HttpServletRequest request) {
		// 超级管理员账户（仅 account.properties 中 privilege=S 配置的账户，无内置超管）
		// 一律不可删除，防止部署了多个超管时互相删除导致失去管理能力。
		if (configurationManager.isSuperAdmin(account)) {
			return ApiResponse.failure(ResultCode.FORBIDDEN.getCode(), "超级管理员账户不可删除");
		}
		String currentAccount = currentAccount(request);
		if (account.equals(currentAccount)) {
			return ApiResponse.failure(ResultCode.FORBIDDEN.getCode(), "不能删除当前登录的账户");
		}
		try {
			if (!configurationManager.deleteAccount(account)) {
				return ApiResponse.failure(ResultCode.USER_NOT_FOUND.getCode(), "账户不存在或删除失败");
			}
			return ApiResponse.success();
		} catch (Exception e) {
			return ApiResponse.failure(ResultCode.ERROR.getCode(), "删除账户失败");
		}
	}

	@PutMapping("/accounts/{account}/password")
	public ApiResponse<Void> resetPassword(@PathVariable String account, @RequestBody PasswordRequest request,
			HttpServletRequest httpRequest) {
		if (request.password() == null || !PASSWORD_PATTERN.matcher(request.password()).matches()) {
			return ApiResponse.failure(ResultCode.BAD_REQUEST.getCode(), "新密码须为 3-32 位非空白字符");
		}
		// 超管账户（仅 privilege=S 配置的账户，无内置超管）的密码仅本人可重置，
		// 防止任一超管劫持其他超管账户。
		if (configurationManager.isSuperAdmin(account) && !account.equals(currentAccount(httpRequest))) {
			return ApiResponse.failure(ResultCode.FORBIDDEN.getCode(), "超级管理员账户密码仅本人可重置");
		}
		try {
			if (!configurationManager.resetPassword(account, request.password())) {
				return ApiResponse.failure(ResultCode.USER_NOT_FOUND.getCode(), "账户不存在或重置失败");
			}
			return ApiResponse.success();
		} catch (Exception e) {
			return ApiResponse.failure(ResultCode.ERROR.getCode(), "重置密码失败");
		}
	}

	@PutMapping("/accounts/{account}/auth")
	public ApiResponse<Void> updateAuth(@PathVariable String account, @RequestBody AuthRequest request) {
		String auth = request.auth() == null ? "" : request.auth();
		if (!isValidAuth(auth)) {
			return ApiResponse.failure(ResultCode.BAD_REQUEST.getCode(), "权限只能包含 c/u/d/r/m/l 字符且不超过 6 位");
		}
		// 超管账户的权限修改会被 isSuperAdmin 一律放行，修改其权限无实际意义且易造成混淆，直接禁止。
		if (configurationManager.isSuperAdmin(account)) {
			return ApiResponse.failure(ResultCode.FORBIDDEN.getCode(), "超级管理员账户权限不可修改");
		}
		try {
			if (!configurationManager.updateAccountAuth(account, auth)) {
				return ApiResponse.failure(ResultCode.USER_NOT_FOUND.getCode(), "账户不存在或修改失败");
			}
			return ApiResponse.success();
		} catch (Exception e) {
			return ApiResponse.failure(ResultCode.ERROR.getCode(), "修改权限失败");
		}
	}

	private boolean isValidAuth(String auth) {
		if (auth.length() > MAX_AUTH_LENGTH) {
			return false;
		}
		for (int i = 0; i < auth.length(); i++) {
			if (AUTH_CHARS.indexOf(auth.charAt(i)) < 0) {
				return false;
			}
		}
		return true;
	}

	private String currentAccount(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session == null ? null : (String) session.getAttribute(SESSION_ACCOUNT_ATTR);
	}

	// ==================== 请求体 DTO ====================

	public record AccountCreateRequest(String account, String password, String auth) {
	}

	public record PasswordRequest(String password) {
	}

	public record AuthRequest(String auth) {
	}
}
