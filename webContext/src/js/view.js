
// 获取实时文件夹视图
function showFolderView(fid, targetId) {
	// 判断是否正在进行另一个相同的请求，如果是则取消本次操作
	if (loadingFolderView) {
		return;
	}
	// 如果正在进行文件夹内容统计，则取消此请求
	if (folderCountRequest != undefined && folderCountRequest != null) {
		folderCountRequest.abort();
		folderCountRequest = null;
	}
	startLoading();
	if (remainingLoadingRequest) {
		remainingLoadingRequest.abort();
	}
	$.ajax({
		type: 'POST',
		dataType: 'text',
		data: {
			fid: fid
		},
		url: 'homeController/getFolderView.ajax',
		success: function(result) {
			endLoading();
			switch (result) {
				case "ERROR":
					// 获取错误直接弹出提示框并将相关内容填为提示信息
					doAlert();
					$("#tb").html("<span class='graytext'>获取失败，请尝试刷新</span>");
					$("#publishTime").html(
						"<span class='graytext'>获取失败，请尝试刷新</span>");
					$("#parentlistbox").html(
						"<span class='graytext'>获取失败，请尝试刷新</span>");
					break;
				case "NOT_FOUND":
				case "notAccess":
					// 对于各种不能访问的情况，要先将记忆路径重置再跳转至根路径下
					document.cookie = "folder_id=" + escape("root");
				case "mustLogin":
					// 如果服务器说必须登录，那么也跳转至根路径下（从而进入登录页面）
					window.location.href = "/";
					break;
				default:
					// 上述情况都不是，则返回的应该是文件夹视图数据，接下来对其进行解析
					folderView = JSON.parse(result);
					// 记录当前获取的文件夹视图的ID号，便于其他操作使用
					locationpath = folderView.folder.folderId;
					// 存储打开的文件夹路径至Cookie中，以便下次打开时直接显示
					document.cookie = "folder_id=" + escape(locationpath) + "; SameSite=Lax";
					// 记录上级目录ID，方便返回上一级
					parentpath = folderView.folder.folderParent;
					// 记录本文件夹的访问级别，便于在新建文件夹时判断应该从哪一个级别开始供用户选择
					constraintLevel = folderView.folder.folderConstraint;
					screenedFoldrView = null;
					// 备份一份原始的文件夹视图数据，同时也记录下原始的查询偏移量
					originFolderView = $.extend(true, {}, folderView);
					totalFoldersOffset = folderView.foldersOffset;
					totalFilesOffset = folderView.filesOffset;
					// 搜索输入框重置
					$("#sreachKeyWordIn").val("");
					// 各项基于文件夹视图返回数据的解析操作……
					showParentList(folderView);
					showAccountView(folderView);
					showPublishTime(folderView);
					$("#sortByFN").removeClass();
					$("#sortByCD").removeClass();
					$("#sortByFS").removeClass();
					$("#sortByCN").removeClass();
					$("#sortByOR").removeClass();
					showFolderTable(folderView);
					// 更新文件夹信息至信息模态框
					$("#fim_name").text(folderView.folder.folderName);
					$("#fim_creator").text(folderView.folder.folderCreator);
					$("#fim_folderCreationDate").text(
						folderView.folder.folderCreationDate);
					$("#fim_folderSize").text("--");
					$("#fim_statistics_total").text("");
					$("#fim_folderId").text(folderView.folder.folderId);
					updateTheFolderInfo();
					// 判断是否还需要加载后续数据
					if (folderView.foldersOffset > folderView.selectStep
						|| folderView.filesOffset > folderView.selectStep) {
						// 如果文件夹偏移量或文件偏移量大于查询步进长度，则说明一定还有后续数据需要加载，那么继续加载后续数据
						showLoadingRemaininngBox();
						loadingRemainingFolderView(targetId);
					} else {
						// 否则，说明文件夹视图加载完成，进行定位工作即可
						hiddenLoadingRemaininngBox();
						doFixedRow(targetId);
					}
					break;
			}
		},
		error: function(XMLHttpRequest, textStatus, errorThrown) {
			endLoading();
			doAlert();
			$("#tb").html("<span class='graytext'>获取失败，请尝试刷新</span>");
			$("#publishTime").html("<span class='graytext'>获取失败，请尝试刷新</span>");
			$("#parentlistbox")
				.html("<span class='graytext'>获取失败，请尝试刷新</span>");
		}
	});
}

// 开始文件视图加载动画
function startLoading() {
	loadingFolderView = true;
	$('#loadingModal').modal({
		backdrop: 'static',
		keyboard: false
	});
	$('#loadingModal').modal('show');
	$('#loadingModal').addClass("shown");
}

// 结束文件视图加载动画
function endLoading() {
	loadingFolderView = false;
	$('#loadingModal').modal('hide');
	$('#loadingModal').removeClass("shown");
}

// 开始登陆加载动画
// 显示当前文件夹的父级路径
function showParentList(folderView) {
	$("#parentFolderList").html("");
	var f = folderView.folder;
	if (folderView.parentList.length > 0) {
		$.each(folderView.parentList, function(n, val) {
			$("#parentFolderList").append(
				"<li><a href='#' onclick='event.preventDefault(); entryFolder("
				+ '"' + val.folderId + '"' + ")'>" + html2Escape(val.folderName)
				+ "</a></li>");
		});
	} else {
		$("#parentFolderList").html("<li class='disabled'><a>无</a></li>");
	}
	if (f.folderName.length > 6) {
		$("#currentFolderName").text(f.folderName.substr(0, 6) + "...");
	} else {
		$("#currentFolderName").text(f.folderName);
	}
	if (f.folderName == "ROOT") {
		$("#folderIconSpan").removeClass("glyphicon-folder-close");
		$("#folderIconSpan").removeClass("glyphicon-search");
		$("#folderIconSpan").addClass("glyphicon-home");
	} else if (folderView.keyWorld != null) {
		$("#folderIconSpan").removeClass("glyphicon-folder-close");
		$("#folderIconSpan").removeClass("glyphicon-home");
		$("#folderIconSpan").addClass("glyphicon-search");
	} else {
		$("#folderIconSpan").removeClass("glyphicon-home");
		$("#folderIconSpan").removeClass("glyphicon-search");
		$("#folderIconSpan").addClass("glyphicon-folder-close");
	}
}

// 显示用户视图，包括文件列表、登录信息、操作权限接口等
function showAccountView(folderView) {
	$("#tb,#tb2").html("");
	resetAccountAdminEntry();// 清空按钮区后重置入口渲染状态，刷新/切换文件夹时入口可重新渲染
	account = folderView.account;
	if (folderView.account != null) {
		// 说明已经登录，显示注销按钮
		$("#tb")
			.append(
				"<button class='btn btn-link rightbtn hidden-xs' data-toggle='modal' data-target='#logoutModal'>注销 ["
				+ html2Escape(folderView.account)
			+ "] <span class='glyphicon glyphicon-off' aria-hidden='true'></span></button>");
	$("#tb2")
		.append(
			"<button class='btn btn-link' data-toggle='modal' data-target='#logoutModal'>注销 ["
			+ html2Escape(folderView.account)
				+ "] <span class='glyphicon glyphicon-off' aria-hidden='true'></span></button>");
		if (folderView.allowChangePassword == 'true') {
			$("#tb")
				.append(
					" <button class='btn btn-link rightbtn hidden-xs' data-toggle='modal' data-target='#changePasswordModal'>修改密码 <span class='glyphicon glyphicon-edit' aria-hidden='true'></span></button>");
			$("#tb2")
				.append(
					" <button class='btn btn-link' data-toggle='modal' data-target='#changePasswordModal'>修改密码 <span class='glyphicon glyphicon-edit' aria-hidden='true'></span></button>");
		}
		// 超级管理员额外显示账户管理入口（服务器配置/存储等系统管理已收敛至桌面端 GUI）
		checkAndShowAccountAdminEntry();
	} else {
		// 说明用户未登录，显示登录按钮
		$("#tb")
			.append(
				"<button class='btn btn-link rightbtn hidden-xs' data-toggle='modal' data-target='#loginModal'>登入 <span class='glyphicon glyphicon-user' aria-hidden='true'></span></button>");
		$("#tb2")
			.append(
				"<button class='btn btn-link' data-toggle='modal' data-target='#loginModal'>登入 <span class='glyphicon glyphicon-user' aria-hidden='true'></span></button>");
		if (folderView.allowSignUp == 'true') {
			$("#tb")
				.append(
					" <button class='btn btn-link rightbtn hidden-xs' onclick='window.location.href = \"/prv/signup.html\"'>立即注册 <span class='glyphicon glyphicon-log-in' aria-hidden='true'></span></button>");
			$("#tb2")
				.append(
					" <button class='btn btn-link' onclick='window.location.href = \"prv/signup.html\"'>立即注册 <span class='glyphicon glyphicon-log-in' aria-hidden='true'></span></button>");
		}
	}
	var authList = folderView.authList;
	// 对操作菜单进行初始化，根据权限显示可操作的按钮（并非约束）。
	$("#fileListDropDown li").addClass("disabled");
	$("#fileListDropDown li a").attr("onclick", "");
	if (authList != null) {
		if (checkAuth(authList, "C")) {
			$("#createFolderButtonLi").removeClass("disabled");
			$("#createFolderButtonLi a")
				.attr("onclick", "showNewFolderModel()");
		}
		if (checkAuth(authList, "U")) {
			$("#uploadFileButtonLi").removeClass("disabled");
			$("#uploadFileButtonLi a").attr("onclick", "showUploadFileModel()");
			if (checkAuth(authList, "C") && isSupportWebkitdirectory()) {// 若浏览器支持文件夹选择，且具备新建文件夹权限，则允许进行文件夹上传
				$("#uploadFolderButtonLi").removeClass("disabled");
				$("#uploadFolderButtonLi a").attr("onclick",
					"showUploadFolderModel()");
			}
		}
		if (folderView.enableDownloadZip && checkAuth(authList, "L")) {
			$("#packageDownloadBox")
				.html(
					"<button class='btn btn-link navbar-btn' onclick='showDownloadAllCheckedModel()'><span class='glyphicon glyphicon-briefcase'></span> 打包下载</button>");
		} else {
			$("#packageDownloadBox").html("");
		}
		if (checkAuth(authList, "D")) {
			$("#deleteSeelectFileButtonLi").removeClass("disabled");
			$("#deleteSeelectFileButtonLi a").attr("onclick",
				"showDeleteAllCheckedModel()");
		}
		if (checkAuth(authList, "M")) {
			// 若具备M权限，则允许复制文件
			$("#copyFileButtonLi").removeClass("disabled");
			$("#stickFileButtonLi").removeClass("disabled");
			$("#copyFileButtonLi a").attr("onclick", "copyFile()");
			$("#stickFileButtonLi a").attr("onclick", "stickFile()");
			$("#copyFileButtonLi").removeClass("hidden");
			$("#copyFileButtonLi").addClass("show");
			if (checkAuth(authList, "D")) {
				// 若同时具备D权限，则允许剪切文件
				$("#cutFileButtonLi").removeClass("disabled");
				$("#cutFileButtonLi a").attr("onclick", "cutFile()");
				$("#cutFileButtonLi").removeClass("hidden");
				$("#cutFileButtonLi").addClass("show");
			}
			if (checkedMovefiles !== undefined && checkedMovefiles.size > 0) {
				// 如果移动目标列表已经有项目，则启用“粘贴”按钮
				if (checkedMovefiles.size < 100) {
					$("#stickFilesCount").text(
						"（" + checkedMovefiles.size + "）");
				} else {
					$("#stickFilesCount").text("（99+）");
				}
				$("#stickFileButtonLi").removeClass("hidden");
				$("#stickFileButtonLi").addClass("show");
			} else {
				// 如果移动目标列表没有项目，则隐藏“粘贴”按钮
				$("#stickFileButtonLi").removeClass("show");
				$("#stickFileButtonLi").addClass("hidden");
				$("#stickFilesCount").text("");
			}
		}
	}
}

// 检查权限列表
function checkAuth(authList, auth) {
	var k = false;
	$.each(authList, function(n, a) {
		if (a == auth) {
			k = true;
		}
	});
	return k;
}

// 显示视图更新时间
function showPublishTime(folderView) {
	$("#publishTime").html("");
	var pt = "";
	if (folderView.publishTime != null) {
		pt = folderView.publishTime;
	} else {
		pt = "--";
	}
	$("#publishTime").text(pt);
}

// 刷新文件夹视图
function refreshFolderView() {
	if (locationpath != null && locationpath.length > 0) {
		showFolderView(locationpath);
	} else {
		showFolderView('root');
	}
	subscribeNotice();// 刷新时也判断是否有新公告需要显示
}

// 返回上一级文件夹
function returnPF() {
	if (parentpath != null && parentpath != "null") {
		showFolderView(parentpath);
	} else {
		showFolderView('root');
	}
}

// 显示文件夹内容
function showFolderTable(folderView) {
	$("#foldertable").html("");
	if (parentpath != null && parentpath != "null") {
		$("#foldertable")
			.append(
				"<tr onclick='returnPF()'><td><button onclick='' class='btn btn-link btn-xs'>../</button></td><td class='hidden-xs'>--</td><td>--</td><td class='hidden-xs'>--</td><td>--</td></tr>");
	}
	var authList = folderView.authList;
	var aD = false;
	var aR = false;
	var aL = false;
	var aO = false;
	if (checkAuth(authList, "D")) {
		aD = true;
	}
	if (checkAuth(authList, "R")) {
		aR = true;
	}
	if (checkAuth(authList, "L")) {
		aL = true;
	}
	if (checkAuth(authList, "O")) {
		aO = true;
	}
	// 遍历并倒序显示文件夹列表
	for (var i1 = folderView.folderList.length; i1 > 0; i1--) {
		var f = folderView.folderList[i1 - 1];
		$("#foldertable").append(createNewFolderRow(f, aD, aR, aO));
	}
	// 遍历并倒序显示文件列表
	for (var i2 = folderView.fileList.length; i2 > 0; i2--) {
		var fi = folderView.fileList[i2 - 1];
		$("#foldertable").append(createFileRow(fi, aL, aD, aR, aO));
	}
}

// 根据一个文件对象生成对应的文件行的HTML内容
function createFileRow(fi, aL, aD, aR, aO) {
	fi.fileName = html2Escape(fi.fileName);
	// 文件名称
	var fileRow = "<tr id=" + fi.fileId + " onclick='checkfile(event," + '"'
		+ fi.fileId + '"' + ")' ondblclick='checkConsFile(event," + '"'
		+ fi.fileId + '"' + ")' id='" + fi.fileId
		+ "' class='filerow'><td>" + fi.fileName
		+ "</td><td class='hidden-xs'>" + fi.fileCreationDate + "</td>";
	// 文件大小
	fileRow = fileRow + "<td>" + formatFileSize(fi.fileSize) + "</td>";
	// 文件创建者（该值来自文件元数据，需转义防止存储型XSS）
	fileRow = fileRow + "<td class='hidden-xs'>" + html2Escape(fi.fileCreator) + "</td><td>";
	// 文件操作按钮
	if (aL) {
		fileRow = fileRow
			+ "<button onclick='showDownloadModel("
			+ '"'
			+ fi.fileId
			+ '","'
			+ replaceAllQuotationMarks(fi.fileName)
			+ '"'
			+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-cloud-download'></span> 下载</button>";
		// 对于各种特殊格式文件提供的预览和播放功能
		var suffix = getSuffix(fi.fileName);
		switch (suffix) {
			case "mp4":
				fileRow = fileRow
					+ "<button onclick='playVideo("
					+ '"'
					+ fi.fileId
					+ '"'
					+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-play'></span> 播放</button>";
				break;
			case "webm":
			case "mov":
			case "avi":
			case "wmv":
			case "mkv":
			case "flv":
				if (folderView.enableFFMPEG) {
					fileRow = fileRow
						+ "<button onclick='playVideo("
						+ '"'
						+ fi.fileId
						+ '"'
						+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-play'></span> 播放</button>";
				}
				break;
			case "pdf":
		case "docx":
		case "xlsx":
		case "pptx":
			fileRow = fileRow
				+ "<button onclick='openPreview("
				+ '"'
				+ fi.fileId
				+ '","'
				+ replaceAllQuotationMarks(fi.fileName)
				+ '"'
				+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-eye-open'></span> 预览</button>";
			break;
		case "jpg":
		case "jpeg":
		case "gif":
		case "png":
		case "bmp":
			break;
		default:
			break;
		}
	}
	if (aD) {
		fileRow = fileRow
			+ "<button onclick='showDeleteFileModel("
			+ '"'
			+ fi.fileId
			+ '","'
			+ replaceAllQuotationMarks(fi.fileName)
			+ '"'
			+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-remove'></span> 删除</button>";
	}
	if (aR) {
		fileRow = fileRow
			+ "<button onclick='showRenameFileModel("
			+ '"'
			+ fi.fileId
			+ '"'
			+ ","
			+ '"'
			+ replaceAllQuotationMarks(fi.fileName)
			+ '"'
			+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-wrench'></span> 重命名</button>";
	}
	if (aO) {
		fileRow = fileRow
			+ "<button onclick='showFolderView("
			+ '"'
			+ fi.fileParentFolder
			+ '","'
			+ fi.fileId
			+ '"'
			+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-sunglasses'></span> 定位</button>";
	}
	if (aL && folderView.showFileChain == 'true') {
		fileRow = fileRow
			+ "<button onclick='getFileChain("
			+ '"'
			+ fi.fileId
			+ '","'
			+ replaceAllQuotationMarks(fi.fileName)
			+ '"'
			+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-link'></span> 链接</button>";
	}
	if (!aR && !aD && !aL && !aO) {
		fileRow = fileRow + "--";
	}
	fileRow = fileRow + "</td></tr>";
	return fileRow;
}

// 将字符串形式的文件体积格式化为“数字+单位”的形式，内容符合标准html规范。
function formatFileSize(size) {
	var fileSizeToInt = parseInt(size);// 将文件体积（字符标识）转化为整型
	var formatSize;
	var formatUnit;
	if (fileSizeToInt < 1024) {
		// 文件体积小于1KB时
		formatSize = fileSizeToInt;
		formatUnit = "B";
	} else if (fileSizeToInt < 1048576) {
		// 文件体积介于1KB至1MB时
		formatSize = (fileSizeToInt / 1024);
		formatUnit = "KB";
	} else if (fileSizeToInt < 1073741824) {
		// 文件体积介于1MB至1GB时
		formatSize = (fileSizeToInt / 1048576);
		formatUnit = "MB";
	} else if (fileSizeToInt < 1099511627776) {
		// 文件体积介于1GB至1TB时
		formatSize = (fileSizeToInt / 1073741824);
		formatUnit = "GB";
	} else if (fileSizeToInt < 9007199254740991) {
		// 文件体积介于1TB至8192TB时
		formatSize = (fileSizeToInt / 1099511627776);
		formatUnit = "TB";
	} else {
		// 文件体积已经大得无法描述时
		return "&gt;=8192TB";
	}
	return formatSize.toFixed(1) + formatUnit;
}

// 根据一个文件夹对象生成对应的文件行的HTML内容
function createNewFolderRow(f, aD, aR, aO) {
	f.folderName = html2Escape(f.folderName);
	var folderRow = "<tr id='"
		+ f.folderId
		+ "' onclick='checkfile(event,"
		+ '"'
		+ f.folderId
		+ '"'
		+ ")' ondblclick='checkConsFile(event,"
		+ '"'
		+ f.folderId
		+ '"'
		+ ")' class='filerow' iskfolder='true' ><td><button onclick='entryFolder("
		+ '"' + f.folderId + '"' + ")' class='btn btn-link btn-xs'>/"
		+ f.folderName + "</button></td><td class='hidden-xs'>"
		+ f.folderCreationDate + "</td><td>--</td><td class='hidden-xs'>"
		+ html2Escape(f.folderCreator) + "</td><td>";
	if (aD) {
		folderRow = folderRow
			+ "<button onclick='showDeleteFolderModel("
			+ '"'
			+ f.folderId
			+ '","'
			+ replaceAllQuotationMarks(f.folderName)
			+ '"'
			+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-remove'></span> 删除</button>";
	}
	if (aR) {
		folderRow = folderRow
			+ "<button onclick='showRenameFolderModel("
			+ '"'
			+ f.folderId
			+ '","'
			+ replaceAllQuotationMarks(f.folderName)
			+ '",'
			+ f.folderConstraint
			+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-wrench'></span> 编辑</button>";
	}
	if (aO) {
		folderRow = folderRow
			+ "<button onclick='showFolderView("
			+ '"'
			+ f.folderParent
			+ '","'
			+ f.folderId
			+ '"'
			+ ")' class='btn btn-link btn-xs'><span class='glyphicon glyphicon-sunglasses'></span> 定位</button>";
	}
	if (!aR && !aD && !aO) {
		folderRow = folderRow + "--";
	}
	folderRow = folderRow + "</td></tr>";
	return folderRow;
}

function doSearchFile() {
	var keyworld = $("#sreachKeyWordIn").val();
	if (keyworld.length != 0) {
		// 如果用户在搜索字段中声明了全局搜索
		if (keyworld.startsWith("all:") || keyworld.startsWith("all：")) {
			selectInCompletePath(keyworld.substring(4));
		} else {
			startLoading();
			selectInThisPath(keyworld);// 否则，均在本级下搜索
			endLoading();
		}
	} else {
		if (folderView.keyWorld != null) {
			showFolderView(locationpath);
		} else {
			screenedFoldrView = null;
			showOriginFolderView();
		}
	}
}

// 在本级内搜索
function selectInThisPath(keyworld) {
	try {
		var upperCasekeyworld = keyworld.toUpperCase();
		screenedFoldrView = $.extend(true, {}, originFolderView);
		screenedFoldrView.folderList = [];
		screenedFoldrView.fileList = [];
		for (var i = 0, j = originFolderView.folderList.length; i < j; i++) {
			if (originFolderView.folderList[i].folderName.toUpperCase().indexOf(upperCasekeyworld) >= 0) {
				screenedFoldrView.folderList
					.push(originFolderView.folderList[i]);
			}
		}
		for (var i = 0, j = originFolderView.fileList.length; i < j; i++) {
			if (originFolderView.fileList[i].fileName.toUpperCase().indexOf(upperCasekeyworld) >= 0) {
				screenedFoldrView.fileList.push(originFolderView.fileList[i]);
			}
		}
		$("#sortByFN").removeClass();
		$("#sortByCD").removeClass();
		$("#sortByFS").removeClass();
		$("#sortByCN").removeClass();
		$("#sortByOR").removeClass();
		folderView = $.extend(true, {}, screenedFoldrView);
		showFolderTable(folderView);
	} catch (e) {
		kiftdAlert("错误：搜索关键字有误。请在特殊符号（例如“*”）前加上“\\”进行转义。");
	}
}

// 全路径查找
function selectInCompletePath(keyworld) {
	if (keyworld.length == 0) {
		showFolderView(locationpath);
		return;
	}
	// 如果正在进行文件夹内容统计，则取消此请求
	if (folderCountRequest != undefined && folderCountRequest != null) {
		folderCountRequest.abort();
		folderCountRequest = null;
	}
	startLoading();
	$.ajax({
		type: 'POST',
		dataType: 'text',
		data: {
			fid: locationpath,
			keyworld: keyworld
		},
		url: 'homeController/sreachInCompletePath.ajax',
		success: function(result) {
			endLoading();
			if (result == "ERROR") {
				doAlert();
				$("#tb").html("<span class='graytext'>获取失败，请尝试刷新</span>");
				$("#publishTime").html(
					"<span class='graytext'>获取失败，请尝试刷新</span>");
				$("#parentlistbox").html(
					"<span class='graytext'>获取失败，请尝试刷新</span>");
			} else if (result == "mustLogin") {
				window.location.href = "prv/login.html";
			} else if (result == "notAccess") {
				document.cookie = "folder_id=" + escape("root");
				window.location.href = "/";
			} else {
				folderView = JSON.parse(result);
				locationpath = folderView.folder.folderId;
				parentpath = folderView.folder.folderParent;
				constraintLevel = folderView.folder.folderConstraint;
				screenedFoldrView = null;
				$("#sreachKeyWordIn").val("all:" + folderView.keyWorld);
				showParentList(folderView);
				showAccountView(folderView);
				showPublishTime(folderView);
				originFolderView = $.extend(true, {}, folderView);
				$("#sortByFN").removeClass();
				$("#sortByCD").removeClass();
				$("#sortByFS").removeClass();
				$("#sortByCN").removeClass();
				$("#sortByOR").removeClass();
				showFolderTable(folderView);
				$("#fim_name").text(folderView.folder.folderName);
				$("#fim_creator").text("--");
				$("#fim_statistics_total").text("");
				$("#fim_folderCreationDate").text("--");
				$("#fim_folderSize").text("--");
				$("#fim_folderId").text("--");
				updateTheFolderInfo();
			}
		},
		error: function() {
			endLoading();
			doAlert();
			$("#tb").html("<span class='graytext'>获取失败，请尝试刷新</span>");
			$("#publishTime").html("<span class='graytext'>获取失败，请尝试刷新</span>");
			$("#parentlistbox")
				.html("<span class='graytext'>获取失败，请尝试刷新</span>");
		}
	});
}

// 返回顶部实现
function loadingRemainingFolderView(targetId) {
	// 判断是否正在执行另一个相同的请求，避免重复操作
	if (remainingLoadingRequest) {
		return;
	}
	// 计算新的查询偏移量
	var newfoldersOffset = 0;
	var newfilesOffset = 0;
	if ((folderView.foldersOffset - folderView.selectStep) > 0) {
		newfoldersOffset = folderView.foldersOffset - folderView.selectStep;
	}
	if ((folderView.filesOffset - folderView.selectStep) > 0) {
		newfilesOffset = folderView.filesOffset - folderView.selectStep;
	}
	if (newfoldersOffset <= 0 && newfilesOffset <= 0) {
		originFolderView = $.extend(true, {}, folderView);
		hiddenLoadingRemaininngBox();
		doFixedRow(targetId);
		return;
	}
	var loadingRemainingRate_folders = 1;
	var loadingRemainingRate_files = 1;
	if (totalFoldersOffset > 0) {
		loadingRemainingRate_folders = (totalFoldersOffset - newfoldersOffset)
			/ totalFoldersOffset;
	}
	if (totalFilesOffset > 0) {
		loadingRemainingRate_files = (totalFilesOffset - newfilesOffset)
			/ totalFilesOffset;
	}
	var loadingRemainingRate = (loadingRemainingRate_folders + loadingRemainingRate_files) / 2;
	$("#loadingrate").text(parseInt(loadingRemainingRate * 100) + "%");
	remainingLoadingRequest = $
		.ajax({
			url: 'homeController/getRemainingFolderView.ajax',
			data: {
				fid: locationpath,
				foldersOffset: newfoldersOffset,
				filesOffset: newfilesOffset
			},
			type: 'POST',
			dataType: 'text',
			success: function(result) {
				remainingLoadingRequest = null;
				switch (result) {
					case "ERROR":
						kiftdAlert("错误：无法加载剩余文件列表，文件数据可能未显示完全，请刷新重试！");
						hiddenLoadingRemaininngBox();
						doFixedRow();
						break;
					case "NOT_FOUND":
					case "notAccess":
						document.cookie = "folder_id=" + escape("root");// 归位记忆路径
					case "mustLogin":
						window.location.href = "/";
						break;
					default:
						folderView.foldersOffset = newfoldersOffset;
						folderView.filesOffset = newfilesOffset;
						var remainingFV = JSON.parse(result);
						updateFolderTable(remainingFV);
						updateTheFolderInfo();
						if (folderView.foldersOffset > 0
							|| folderView.filesOffset > 0) {
							loadingRemainingFolderView(targetId);
						} else {
							originFolderView = $.extend(true, {}, folderView);
							hiddenLoadingRemaininngBox();
							doFixedRow(targetId);
						}
						break;
				}
			},
			error: function(jqXHR, textStatus, errorThrown) {
				remainingLoadingRequest = null;
				hiddenLoadingRemaininngBox();
				if ('abort' != textStatus) {
					kiftdAlert("错误：无法连接服务器，文件列表加载被中断。请刷新重试！");
				}
			}
		});
}

// 定位指定文件所在行
function doFixedRow(targetId) {
	if (targetId && targetId.length > 0) {
		$("#" + targetId).addClass("info");
		$("html,body").animate({
			scrollTop: $("#" + targetId).offset().top - $(window).height() / 2
		}, 'slow');
	}
}

// 显示“正在加载文件列表”提示栏
function showLoadingRemaininngBox() {
	loadingComplete = false;
	$("#loadingremaininngbox").addClass("show");
	$("#loadingremaininngbox").removeClass("hidden");
	$("#searchbtn").prop('disabled', true);
}

// 隐藏“正在加载文件列表”提示栏
function hiddenLoadingRemaininngBox() {
	loadingComplete = true;
	$("#loadingremaininngbox").removeClass("show");
	$("#loadingremaininngbox").addClass("hidden");
	$("#searchbtn").prop('disabled', false);
}

// 将加载的后续文件夹视图数据更新至页面上显示
function updateFolderTable(remainingFV) {
	var authList = folderView.authList;
	var aD = false;
	var aR = false;
	var aL = false;
	var aO = false;
	if (checkAuth(authList, "D")) {
		aD = true;
	}
	if (checkAuth(authList, "R")) {
		aR = true;
	}
	if (checkAuth(authList, "L")) {
		aL = true;
	}
	if (checkAuth(authList, "O")) {
		aO = true;
	}
	if (remainingFV.folderList) {
		if (remainingFV.folderList.length > 0) {
			for (var i1 = remainingFV.folderList.length; i1 > 0; i1--) {
				var f = remainingFV.folderList[i1 - 1];
				if (!folderContains(folderView.folderList, f.folderId)) {
					folderView.folderList.unshift(f);
					$("[iskfolder=true]:last").after(
						createNewFolderRow(f, aD, aR, aO));
				}
			}
		}
	}
	if (remainingFV.fileList) {
		if (remainingFV.fileList.length > 0) {
			for (var i2 = remainingFV.fileList.length; i2 > 0; i2--) {
				var fi = remainingFV.fileList[i2 - 1];
				if (!fileContains(folderView.fileList, fi.fileId)) {
					folderView.fileList.unshift(fi);
					$("#foldertable").append(createFileRow(fi, aL, aD, aR, aO));
				}
			}
		}
	}
}

// 判断文件夹数组中是否存已在ID相同的某个文件夹
function folderContains(folderList, targetFolderId) {
	for (var i = folderList.length; i > 0; i--) {
		if (folderList[i - 1].folderId == targetFolderId) {
			return true;
		}
	}
	return false;
}

// 判断文件数组中是否存已在ID相同的某个文件
function fileContains(fileList, targetFileId) {
	for (var i = fileList.length; i > 0; i--) {
		if (fileList[i - 1].fileId == targetFileId) {
			return true;
		}
	}
	return false;
}

// 更新文件夹视图信息
function updateTheFolderInfo() {
	$("#fim_statistics").text(
		"本级包含 " + folderView.folderList.length + " 个文件夹， "
		+ folderView.fileList.length + " 个文件");
}

// 替换所有引号，将其进一步转义，主要用于传递带引号的文件名
function replaceAllQuotationMarks(txt) {
	return txt.replace(/\\/g, "\\\\").replace(/\"/g, "\\\"");
}

// 对所有可能进入html的字符串进行转义操作
function html2Escape(sHtml) {
	return sHtml.replace(/[<>&\'\"]/g, function(c) {
		return {
			'<': '&lt;',
			'>': '&gt;',
			'&': '&amp;',
			'\'': '&#39;',
			'"': '&quot;'
		}[c];
	});
}
