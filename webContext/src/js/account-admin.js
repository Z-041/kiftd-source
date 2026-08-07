// ==================== 账户管理（仅超级管理员可见） ====================
// 服务器配置、存储概览等系统级管理已迁移至桌面端 GUI，Web 端仅保留账户管理。
// 本模块由构建脚本（npm run build）与 home.js 按顺序合并后压缩为 home.min.js，
// 故仍使用全局作用域（函数/变量挂于 window），保证 HTML onclick 引用不受影响。

// 账户管理入口是否已渲染（每次刷新视图清空按钮区后由 resetAccountAdminEntry 重置）
var accountAdminEntryShown = false;
// 是否已查询过超管身份（登录态内仅查询一次，避免刷新/切换文件夹时重复请求）
var accountAdminChecked = false;
// 当前登录账户是否为超级管理员
var accountAdminEnabled = false;

// 重置入口渲染状态：视图刷新时按钮区会被清空，调用后允许入口重新渲染
function resetAccountAdminEntry() {
	accountAdminEntryShown = false;
}

// 查询当前登录账户是否为超级管理员，是则显示账户管理入口按钮
function checkAndShowAccountAdminEntry() {
	if (accountAdminChecked) {
		renderAccountAdminEntry();
		return;
	}
	$.ajax({
		type: "GET",
		dataType: "json",
		url: "homeController/getAccountInfo.ajax",
		success: function(info) {
			accountAdminChecked = true;
			if (info && info.superAdmin) {
				accountAdminEnabled = true;
				renderAccountAdminEntry();
			}
		},
		error: function() {
			// 获取失败不阻塞页面使用，仅不显示入口
		}
	});
}

// 渲染账户管理入口按钮（仅超管且本次尚未渲染时执行）
function renderAccountAdminEntry() {
	if (accountAdminEntryShown || !accountAdminEnabled) {
		return;
	}
	accountAdminEntryShown = true;
	$("#tb").append(
		" <button class='btn btn-link rightbtn hidden-xs' data-toggle='modal' data-target='#accountManageModal'>账户管理 <span class='glyphicon glyphicon-cog' aria-hidden='true'></span></button>");
	$("#tb2").append(
		" <button class='btn btn-link' data-toggle='modal' data-target='#accountManageModal'>账户管理 <span class='glyphicon glyphicon-cog' aria-hidden='true'></span></button>");
}

// 统一的账户管理 API 请求封装（/api/admin/* 由 ApiAuthFilter 鉴权，仅超级管理员可访问）
function accountAdminRequest(url, method, payload, onSuccess) {
	$.ajax({
		type: method,
		dataType: "json",
		url: url,
		contentType: "application/json;charset=UTF-8",
		data: payload ? JSON.stringify(payload) : undefined,
		success: function(resp) {
			if (resp && resp.success) {
				if (onSuccess) {
					onSuccess(resp.data);
				}
			} else {
				showAccountManageAlert((resp && resp.message) || "操作失败");
			}
		},
		error: function(xhr) {
			var msg = "请求失败，请检查网络或服务器状态";
			if (xhr.status === 401) {
				msg = "请先登录系统";
			} else if (xhr.status === 403) {
				msg = "当前账户无管理权限";
			}
			showAccountManageAlert(msg);
		}
	});
}

function showAccountManageAlert(txt) {
	$("#accountManageAlert").text(txt).show();
}

function hideAccountManageAlert() {
	$("#accountManageAlert").hide();
}

// 打开账户管理模态框时加载账户列表
$('#accountManageModal').on('shown.bs.modal', function() {
	hideAccountManageAlert();
	hideNewAccountForm();
	loadAccountList();
});

function loadAccountList() {
	$("#accountManageTbody").html("<tr><td colspan='7'>加载中...</td></tr>");
	accountAdminRequest("api/admin/accounts", "GET", null, function(accounts) {
		adminAccountList = accounts;
		if (!accounts || accounts.length === 0) {
			$("#accountManageTbody").html("<tr><td colspan='7'>暂无账户</td></tr>");
			return;
		}
		var html = "";
		for (var i = 0; i < accounts.length; i++) {
			var a = accounts[i];
			var authDesc = a.auth ? a.auth.split("").map(function(c) {
				return {
					'c': '建',
					'u': '传',
					'd': '删',
					'r': '改',
					'm': '移',
					'l': '下'
				}[c] || c;
			}).join(" ") : "无";
			var opBtns;
			if (a.superAdmin) {
				// 超管账户（仅 account.properties 中 privilege=S 配置，无内置超管）不可修改/删除
				opBtns = "<span class='text-muted'>系统管理员</span>";
			} else {
				opBtns = "<button type='button' class='btn btn-default btn-xs' onclick='resetPasswordByAdmin(\""
					+ html2Escape(a.account) + "\")'>重置密码</button> "
					+ "<button type='button' class='btn btn-default btn-xs' onclick='changeAuthByAdmin(\""
					+ html2Escape(a.account) + "\")'>权限</button> "
					+ "<button type='button' class='btn btn-danger btn-xs' onclick='deleteAccountByAdmin(\""
					+ html2Escape(a.account) + "\")'>删除</button>";
			}
			html += "<tr>"
				+ "<td>" + html2Escape(a.account) + "</td>"
				+ "<td>" + html2Escape(authDesc) + "</td>"
				+ "<td>" + html2Escape(a.group || "-") + "</td>"
				+ "<td>" + (a.superAdmin ? "是" : "否") + "</td>"
				+ "<td>" + formatAccountSize(a.uploadMaxSize) + "</td>"
				+ "<td>" + formatAccountRate(a.downloadMaxRate) + "</td>"
				+ "<td>" + opBtns + "</td>"
				+ "</tr>";
		}
		$("#accountManageTbody").html(html);
	});
}

function formatAccountSize(bytes) {
	if (bytes === null || bytes === undefined || bytes < 0) {
		return "无限制";
	}
	if (bytes === 0) {
		return "0 B";
	}
	var units = ["B", "KB", "MB", "GB", "TB"];
	var i = Math.floor(Math.log(bytes) / Math.log(1024));
	return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + " " + units[i];
}

function formatAccountRate(rate) {
	if (rate === null || rate === undefined || rate <= 0) {
		return "无限制";
	}
	return formatAccountSize(rate) + "/s";
}

function showNewAccountForm() {
	$("#newAccountFormBox").removeClass("hidden");
}

function hideNewAccountForm() {
	$("#newAccountFormBox").addClass("hidden");
}

function createAccountByAdmin() {
	var account = $("#naAccount").val().trim();
	var password = $("#naPassword").val();
	var auth = $("#naAuth").val().trim();
	if (!account || !password) {
		showAccountManageAlert("账户名与密码不能为空");
		return;
	}
	accountAdminRequest("api/admin/accounts", "POST", {
		account: account,
		password: password,
		auth: auth
	}, function() {
		$("#naAccount").val("");
		$("#naPassword").val("");
		$("#naAuth").val("");
		hideNewAccountForm();
		kiftdAlert("账户已创建");
		loadAccountList();
	});
}

// 当前账户管理操作的目标账户
var adminOpAccount = null;
// 最近一次加载的账户列表（供权限回显使用）
var adminAccountList = null;
// 权限字符与说明的映射（顺序即展示顺序）
var authCheckboxMap = [{
	ch: 'c',
	desc: '新建文件夹'
}, {
	ch: 'u',
	desc: '上传文件'
}, {
	ch: 'd',
	desc: '删除'
}, {
	ch: 'r',
	desc: '重命名/编辑'
}, {
	ch: 'm',
	desc: '移动/复制'
}, {
	ch: 'l',
	desc: '下载'
}];

// 重置密码：打开模态框
function resetPasswordByAdmin(account) {
	adminOpAccount = account;
	$("#resetPwdAdminName").text(account);
	$("#resetPwdAdminNewPwd").val("");
	$("#resetPwdAdminReNewPwd").val("");
	$("#resetPwdAdminAlert").hide();
	$("#resetPwdAdminModal").modal("show");
	setTimeout(function() {
		$("#resetPwdAdminNewPwd").focus();
	}, 300);
}

function confirmResetPasswordByAdmin() {
	var pwd = $("#resetPwdAdminNewPwd").val();
	var rePwd = $("#resetPwdAdminReNewPwd").val();
	if (!pwd) {
		showResetPwdAdminAlert("新密码不能为空");
		return;
	}
	if (pwd !== rePwd) {
		showResetPwdAdminAlert("两次输入的新密码不一致");
		return;
	}
	accountAdminRequest("api/admin/accounts/" + encodeURIComponent(adminOpAccount) + "/password", "PUT", {
		password: pwd
	}, function() {
		$("#resetPwdAdminModal").modal("hide");
		kiftdAlert("密码已重置");
	});
}

function showResetPwdAdminAlert(txt) {
	$("#resetPwdAdminAlert").text(txt).show();
}

// 权限修改：打开模态框并回显当前权限
function changeAuthByAdmin(account) {
	adminOpAccount = account;
	var currentAuth = "";
	if (adminAccountList) {
		for (var i = 0; i < adminAccountList.length; i++) {
			if (adminAccountList[i].account === account) {
				currentAuth = adminAccountList[i].auth || "";
				break;
			}
		}
	}
	$("#authAdminName").text(account);
	$("#authAdminAlert").hide();
	$("#authAdminCheckboxBox").html("");
	for (var i = 0; i < authCheckboxMap.length; i++) {
		var item = authCheckboxMap[i];
		var checked = currentAuth.indexOf(item.ch) >= 0 ? " checked" : "";
		$("#authAdminCheckboxBox").append(
			"<label class='checkbox-inline'><input type='checkbox' id='authAdminChk_"
			+ item.ch + "'" + checked + "> " + item.desc + " (" + item.ch + ")</label>");
	}
	$("#authAdminModal").modal("show");
}

function confirmChangeAuthByAdmin() {
	var auth = "";
	for (var i = 0; i < authCheckboxMap.length; i++) {
		if ($("#authAdminChk_" + authCheckboxMap[i].ch).prop("checked")) {
			auth += authCheckboxMap[i].ch;
		}
	}
	accountAdminRequest("api/admin/accounts/" + encodeURIComponent(adminOpAccount) + "/auth", "PUT", {
		auth: auth
	}, function() {
		$("#authAdminModal").modal("hide");
		kiftdAlert("权限已更新");
		loadAccountList();
	});
}

// 删除账户：打开确认模态框
function deleteAccountByAdmin(account) {
	adminOpAccount = account;
	$("#deleteAccountConfirmName").text(account);
	$("#deleteAccountConfirmModal").modal("show");
}

function confirmDeleteAccountByAdmin() {
	accountAdminRequest("api/admin/accounts/" + encodeURIComponent(adminOpAccount), "DELETE", null, function() {
		$("#deleteAccountConfirmModal").modal("hide");
		kiftdAlert("账户已删除");
		loadAccountList();
	});
}
