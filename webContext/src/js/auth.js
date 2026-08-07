function startLogin() {
	$("#accountid").prop('disabled', true);
	$("#accountpwd").prop('disabled', true);
	$("#dologinButton").prop('disabled', true);
	$("#vercode").prop('disabled', true);
}

// 结束登陆加载动画
function finishLogin() {
	$("#accountid").prop('disabled', false);
	$("#accountpwd").prop('disabled', false);
	$("#dologinButton").prop('disabled', false);
	$("#vercode").prop('disabled', false);
}

// 登录操作
function dologin() {
	var accountId = $("#accountid").val();
	var accountPwd = $("#accountpwd").val();
	var check = "y";
	if (accountId.length == 0) {
		$("#accountidbox").addClass("has-error");
		check = "n";
	} else {
		$("#accountidbox").removeClass("has-error");
	}
	if (accountPwd.length == 0) {
		$("#accountpwdbox").addClass("has-error");
		check = "n";
	} else {
		$("#accountpwdbox").removeClass("has-error");
	}
	if (check == "y") {
		startLogin();
		// 加密认证-获取公钥并将请求加密发送给服务器，避免中途被窃取
		$.ajax({
			url: 'homeController/getPublicKey.ajax',
			type: 'POST',
			data: {},
			dataType: 'text',
			success: function(result) {
				var publicKeyInfo = JSON.parse(result);
				var date = new Date();// 这个是客户浏览器上的当前时间
				var loginInfo = JSON.stringify({
			accountId: accountId,
			accountPwd: accountPwd,
			time: publicKeyInfo.time
		});
				var encrypt = new JSEncrypt();// 加密插件对象
				encrypt.setPublicKey(publicKeyInfo.publicKey);// 设置公钥
				var encrypted = encrypt.encrypt(loginInfo);// 进行加密
				sendLoginInfo(encrypted);
			},
			error: function() {
				finishLogin();
				$("#alertbox").addClass("alert");
				$("#alertbox").addClass("alert-danger");
				$("#alertbox").text("提示：登录请求失败，请检查网络或服务器运行状态");
			}
		});
	}
}

// 发送加密文本
function sendLoginInfo(encrypted) {
	$
		.ajax({
			type: "POST",
			dataType: "text",
			url: "homeController/doLogin.ajax",
			data: {
				encrypted: encrypted,
				vercode: $("#vercode").val()
			},
			success: function(result) {
				finishLogin();
				$("#alertbox").removeClass("alert");
				$("#alertbox").removeClass("alert-danger");
				$("#alertbox").text("");
				$("#vercodebox").html("");
				$("#vercodebox").removeClass("show");
				$("#vercodebox").addClass("hidden");
				switch (result) {
					case "permitlogin":
						$("#accountidbox").removeClass("has-error");
						$("#accountpwdbox").removeClass("has-error");
						$('#loginModal').modal('hide');
						showFolderView(locationpath);
						break;
					case "accountnotfound":
						$("#accountidbox").addClass("has-error");
						$("#accountpwdbox").removeClass("has-error");
						$("#alertbox").addClass("alert");
						$("#alertbox").addClass("alert-danger");
						$("#alertbox").text("提示：登录失败，账户不存在或未设置");
						break;
					case "accountpwderror":
						$("#accountpwdbox").addClass("has-error");
						$("#accountidbox").removeClass("has-error");
						$("#alertbox").addClass("alert");
						$("#alertbox").addClass("alert-danger");
						$("#alertbox").text("提示：登录失败，密码错误或未设置");
						break;
					case "needsubmitvercode":
						$("#vercodebox")
							.html(
								"<label id='vercodetitle' class='col-sm-7'><img id='showvercode' class='vercodeimg' alt='点击获取验证码' src='homeController/getNewVerCode.do?s="
								+ (new Date()).getTime()
								+ "' onclick='getNewVerCode()'></label><div class='col-sm-5'><input type='text' class='form-control' id='vercode' placeholder='验证码……'></div>");
						$("#vercodebox").removeClass("hidden");
						$("#vercodebox").addClass("show");
						break;
					case "error":
						$("#alertbox").addClass("alert");
						$("#alertbox").addClass("alert-danger");
						$("#alertbox").text(
							"提示：登录失败，登录请求无法通过加密效验（可能是请求耗时过长导致的）");
						break;
					default:
						$("#alertbox").addClass("alert");
						$("#alertbox").addClass("alert-danger");
						$("#alertbox").text("提示：无法登录，未知错误");
						break;
				}
			},
			error: function() {
				finishLogin();
				$("#alertbox").addClass("alert");
				$("#alertbox").addClass("alert-danger");
				$("#alertbox").text("提示：登录请求失败，请检查网络或服务器运行状态");
			}
		});
}

// 获取一个新的验证码
function getNewVerCode() {
	$("#showvercode").attr("src",
		"homeController/getNewVerCode.do?s=" + (new Date()).getTime());
}

// 注销操作
function dologout() {
	$('#logoutModal').modal('hide');
	$.ajax({
		url: 'homeController/doLogout.ajax',
		type: 'POST',
		data: {},
		dataType: 'text',
		success: function(result) {
			if (result == "SUCCESS") {
				// 登出后重置账户管理入口的全部缓存，以便重新登录时重新判断超管身份
				accountAdminChecked = false;
				accountAdminEnabled = false;
				accountAdminEntryShown = false;
				showFolderView(locationpath);
			}
		},
		error: function() {
			doAlert();
		}
	});
}

function doChangePassword() {
	// 还原提示状态
	$(
		"#changepassword_oldepwdbox,#changepassword_newpwdbox,#changepassword_reqnewpwdbox")
		.removeClass("has-error");
	$("#changepasswordalertbox").hide();
	var change_oldPassword = $("#changepassword_oldpwd").val();
	var change_newPassword = $("#changepassword_newpwd").val();
	var change_reqNewPassword = $("#changepassword_reqnewpwd").val();
	// 输入非空检查
	if (change_oldPassword.length == 0) {
		$("#changepassword_oldepwdbox").addClass("has-error");
		$("#changepassword_oldpwd").focus();
		return;
	}
	if (change_newPassword.length == 0) {
		$("#changepassword_newpwdbox").addClass("has-error");
		$("#changepassword_newpwd").focus();
		return;
	}
	if (change_reqNewPassword.length == 0) {
		$("#changepassword_reqnewpwdbox").addClass("has-error");
		$("#changepassword_reqnewpwd").focus();
		return;
	}
	// 确认密码检查
	isChangingPassword = true;
	$(
		"#changepassword_oldpwd,#changepassword_newpwd,#changepassword_reqnewpwd,#changePasswordButton,#changepassword_vercode")
		.prop('disabled', true);
	if (change_newPassword + "" != change_reqNewPassword + "") {
		showChangePasswordAlert("提示：两次输入的新密码不一致，请检查确认");
		$("#changepassword_newpwdbox").addClass("has-error");
		$("#changepassword_reqnewpwdbox").addClass("has-error");
		return;
	}
	// 以加密方式发送修改密码请求
	$.ajax({
		url: 'homeController/getPublicKey.ajax',
		type: 'POST',
		data: {},
		dataType: 'text',
		success: function(result) {
			// 获取公钥
			var changepwd_publicKeyInfo = JSON.parse(result);
			// 生成JSON对象格式的信息
			var changePasswordInfo = JSON.stringify({
			oldPwd: change_oldPassword,
			newPwd: change_newPassword,
			time: changepwd_publicKeyInfo.time
		});
			var encrypt = new JSEncrypt();// 加密插件对象
			encrypt.setPublicKey(changepwd_publicKeyInfo.publicKey);// 设置公钥
			var encrypted = encrypt.encrypt(changePasswordInfo);// 进行加密
			sendChangePasswordInfo(encrypted);
		},
		error: function() {
			showChangePasswordAlert("提示：密码修改失败，请检查网络链接或服务器运行状态");
		}
	});
}

// 将加密数据发送至服务器并显示操作结果
function sendChangePasswordInfo(encrypted) {
	$
		.ajax({
			type: "POST",
			dataType: "text",
			url: "homeController/doChangePassword.ajax",
			data: {
				encrypted: encrypted,
				vercode: $("#changepassword_vercode").val()
			},
			success: function(result) {
				$("#changepassword_vccodebox").hide();
				isChangingPassword = false;
				switch (result) {
					case "success":
						$('#changePasswordModal').modal('hide');
						break;
					case "mustlogin":
						showChangePasswordAlert("提示：登录已失效或尚未登录账户，请刷新并登陆账户");
						break;
					case "illegal":
						showChangePasswordAlert("提示：用户修改密码功能已被禁用，请求被拒绝");
						break;
					case "oldpwderror":
						showChangePasswordAlert("提示：旧密码输入错误，请求被拒绝");
						$("#changepassword_oldepwdbox").addClass("has-error");
						break;
					case "needsubmitvercode":
						$(
							"#changepassword_oldpwd,#changepassword_newpwd,#changepassword_reqnewpwd,#changePasswordButton")
							.prop('disabled', false);
						$("#changepassword_vccodebox")
							.html(
								"<label id='changepassword_vercodetitle' class='col-sm-5'><img id='changepassword_showvercode' class='vercodeimg' alt='点击获取验证码' src='homeController/getNewVerCode.do?s="
								+ (new Date()).getTime()
								+ "' onclick='changePasswordGetNewVerCode()'></label><div class='col-sm-7'><input type='text' class='form-control' id='changepassword_vercode' placeholder='验证码……'></div>");
						$("#changepassword_vccodebox").show();
						isChangingPassword = false;
						break;
					case "invalidnewpwd":
						showChangePasswordAlert("提示：密码修改失败，新密码不合法。新密码的长度需为3-32个字符，且仅支持ISO-8859-1中的字符（推荐使用英文字母、英文符号及阿拉伯数字）。");
						break;
					case "error":
						showChangePasswordAlert("提示：密码修改失败，修改请求无法通过加密效验（可能是请求耗时过长导致的）");
						break;
					case "cannotchangepwd":
						showChangePasswordAlert("提示：密码修改失败，发生意外错误，请稍后重试或联系管理员");
						break;
					default:
						showChangePasswordAlert("提示：密码修改失败，发生未知错误");
						break;
				}
			},
			error: function() {
				showChangePasswordAlert("提示：密码修改失败，请检查网络链接或服务器运行状态");
			}
		});
}

// 显示修改密码错误提示
function showChangePasswordAlert(txt) {
	isChangingPassword = false;
	$(
		"#changepassword_oldpwd,#changepassword_newpwd,#changepassword_reqnewpwd,#changePasswordButton,#changepassword_vercode")
		.prop('disabled', false);
	$("#changepasswordalertbox").show();
	$("#changepasswordalertbox").text(txt);
}

// （修改密码版本的）获取一个新的验证码
function changePasswordGetNewVerCode() {
	$("#changepassword_showvercode").attr("src",
		"homeController/getNewVerCode.do?s=" + (new Date()).getTime());
}
