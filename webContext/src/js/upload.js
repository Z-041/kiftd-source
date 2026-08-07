// 显示上传文件模态框
function showUploadFileModel() {
	$("#uploadFileAlert").hide();
	$("#uploadFileAlert").text("");
	if (isUpLoading == false) {
		$("#filepath").prop("disabled", false);
		$("#uploadfile").val("");
		$("#filepath").val("");
		$("#pros").width("0%");
		$("#pros").attr('aria-valuenow', '0');
		$("#umbutton").prop('disabled', false);
		$("#filecount").text("");
		$("#uploadstatus").html("");
		$("#selectcount").text("");
		$("#selectFileUpLoadModelAsAll").prop("checked", false);
		$("#selectFileUpLoadModelAlert").hide();
	}
	$('#uploadFileModal').modal('show');
}

// 点击文本框触发input:file选择文件动作
function checkpath() {
	$('#uploadfile').click();
}

// 获取选中文件
function getInputUpload() {
	fs = $("#uploadfile").get(0).files;
	showfilepath();
}

// 文件选中后自动回填文件路径
function showfilepath() {
	var filename = "";
	for (var i = 0; i < fs.length; i++) {
		filename = filename + fs[i].name;
		if (i < (fs.length - 1)) {
			filename = filename + "、";
		}
	}
	if (fs.length <= 1) {
		$("#selectcount").text("");
	} else {
		$("#selectcount").text("（共" + fs.length + "个）");
	}
	$("#filepath").val(filename);
}

// 检查文件是否能够上传
function checkUploadFile() {
	if (isUpLoading == false && isImporting == false) {
		if (fs != null && fs.length > 0) {
			$("#filepath").attr("disabled", "disabled");
			$("#umbutton").prop('disabled', true);
			isUpLoading = true;
			repeModelList = null;
			$("#uploadFileAlert").hide();
			$("#uploadFileAlert").text("");
			var filenames = new Array();
			var maxSize = 0;
			var maxFileIndex = 0;
			for (var i = 0; i < fs.length; i++) {
				filenames[i] = fs[i].name;
				if (fs[i].size > maxSize) {
					maxSize = fs[i].size;
					maxFileIndex = i;
				}
			}
			var namelist = JSON.stringify(filenames);
			uploadTargetFolder = locationpath;// 记录上传目标

			$
				.ajax({
					type: "POST",
					dataType: "text",
					data: {
						folderId: uploadTargetFolder,
						namelist: namelist,
						maxSize: maxSize,
						maxFileIndex: maxFileIndex
					},
					url: "homeController/checkUploadFile.ajax",
					success: function(result) {
						if (result == "mustLogin") {
							window.location.href = "prv/login.html";
						} else {
							switch (result) {
								case "errorParameter":
									showUploadFileAlert("提示：参数不正确，无法开始上传");
									break;
								case "noAuthorized":
									showUploadFileAlert("提示：您的操作未被授权，无法开始上传");
									break;
								case "filesTotalOutOfLimit":
									showUploadFileAlert("提示：该文件夹内存储的文件数量已达上限，无法在其中上传更多文件。您可以尝试将其上传至其他文件夹内。");
									break;
								default:
									var resp = JSON.parse(result);
									if (resp.checkResult == "fileTooLarge") {
										showUploadFileAlert("提示：文件["
											+ resp.overSizeFile
											+ "]的体积超过最大限制（"
											+ resp.maxUploadFileSize
											+ "），无法开始上传");
									} else if (resp.checkResult == "hasExistsNames") {
										repeList = resp.pereFileNameList;
										repeIndex = 0;
										selectFileUpLoadModelStart();
									} else if (resp.checkResult == "permitUpload") {
										doupload(1);
									} else {
										showUploadFileAlert("提示：出现意外错误，无法开始上传");
									}
									break;
							}
						}
					},
					error: function() {
						showUploadFileAlert("提示：出现意外错误，无法开始上传");
					}
				});
		} else {
			showUploadFileAlert("提示：您未选择任何文件，无法开始上传");
		}
	} else {
		showUploadFileAlert("提示：另一项上传文件或文件夹的任务尚未完成，无法开始上传");
	}
}

var repeList;// 这个是重复文件名的列表，型如['xxx','ooo',...]
var repeIndex;// 当前设定上传模式的文件序号
var repeModelList;// 这个是对每一个重复文件选取的上传模式，型如{'xxx':'skip','ooo':'both',...}

// 针对同名文件，选择上传的模式：跳过（skip）、覆盖（cover）和保留两者（both）
function selectFileUpLoadModelStart() {
	var authList = originFolderView.authList;
	if (checkAuth(authList, "D")) {
		$("#uploadcoverbtn").show();
	} else {
		$("#uploadcoverbtn").hide();
	}
	$("#selectFileUpLoadModelAlert").show();
	$("#repeFileName").text(repeList[repeIndex]);
}

// 设定重名文件的处理方法
function selectFileUpLoadModelEnd(t) {
	if (repeModelList == null) {
		repeModelList = {};
	}
	repeModelList[$("#repeFileName").text()] = t;
	$("#selectFileUpLoadModelAlert").hide();
	if ($('#selectFileUpLoadModelAsAll').prop('checked')) {
		for (var i = repeIndex; i < repeList.length; i++) {
			repeModelList[repeList[i]] = t;
		}
		doupload(1);
	} else {
		repeIndex++;
		if (repeIndex < repeList.length) {
			selectFileUpLoadModelStart();
		} else {
			doupload(1);
		}
	}
}

// 执行文件上传并实现上传进度显示
function doupload(count) {
	var fcount = fs.length;
	$("#pros").width("0%");// 先将进度条置0
	$("#pros").attr('aria-valuenow', "0");
	var uploadfile = fs[count - 1];// 获取要上传的文件
	if (uploadfile != null) {
		var fname = uploadfile.name;
		if (fcount > 1) {
			$("#filecount").text("（" + count + "/" + fcount + "）");// 显示当前进度
		}
		$("#uploadstatus").prepend(
			"<p>" + html2Escape(fname) + " <span id='uls_" + count
			+ "' class='text-info'>[正在上传...]</span></p>");
		xhr = new XMLHttpRequest();// 这东西类似于servlet里面的request

		var fd = new FormData();// 用于封装文件数据的对象
		fd.append("file", uploadfile);// 将文件对象添加到FormData对象中，字段名为uploadfile
		fd.append("fname", fname);
		fd.append("folderId", uploadTargetFolder);
		if (repeModelList != null && repeModelList[fname] != null) {
			if (repeModelList[fname] == 'skip') {
				$("#uls_" + count).html("<span class='text-success'>[已完成]</span>");
				if (count < fcount) {
					doupload(count + 1);
					return;
				} else {
					isUpLoading = false;
					$("#filepath").prop("disabled", false);
					$("#uploadfile").val("");
					$("#filepath").val("");
					$("#pros").width("0%");
					$("#pros").attr('aria-valuenow', "0");
					$("#umbutton").prop('disabled', false);
					$("#filecount").text("");
					$("#uploadstatus").text("");
					$("#selectcount").text("");
					$('#uploadFileModal').modal('hide');
					showFolderView(locationpath);
					return;
				}
			}
			fd.append("repeType", repeModelList[fname]);
		}
		xhr.open("POST", "homeController/douploadFile.ajax", true);// 上传目标

		xhr.upload.addEventListener("progress", uploadProgress, false);// 这个是对上传进度的监听
		// 上面的三个参数分别是：事件名（指定名称）、回调函数、是否冒泡（一般是false即可）

		xhr.send(fd);// 上传FormData对象

		if (pingInt == null) {
			pingInt = setInterval(function() { ping(); }, 60000);// 上传中开始计时应答
		}

		// 上传结束后执行的回调函数
		xhr.onloadend = function() {
			// 停止应答计时
			if (pingInt != null) {
				window.clearInterval(pingInt);
				pingInt = null;
			}
			if (xhr.status === 200) {
				// 上传成功
				var result = xhr.responseText;
				if (result == "uploadsuccess") {
					$("#uls_" + count).html("<span class='text-success'>[已完成]</span>");
					if (count < fcount) {
						doupload(count + 1);
					} else {
						isUpLoading = false;
						$("#filepath").prop("disabled", false);
						$("#uploadfile").val("");
						$("#filepath").val("");
						$("#pros").width("0%");
						$("#pros").attr('aria-valuenow', "0");
						$("#umbutton").prop('disabled', false);
						$("#filecount").text("");
						$("#uploadstatus").text("");
						$("#selectcount").text("");
						$('#uploadFileModal').modal('hide');
						showFolderView(locationpath);
					}
				} else if (result == "uploaderror") {
					showUploadFileAlert("提示：出现意外错误，文件：[" + fname
						+ "]上传失败，上传被中断。");
					$("#uls_" + count).html("<span class='text-danger'>[失败]</span>");
				} else if (result == 'filesTotalOutOfLimit') {
					showUploadFileAlert("提示：该文件夹内存储的文件数量已达上限，文件：[" + fname
						+ "]上传失败。您可以尝试将其上传至其他文件夹内。");
					$("#uls_" + count).html("<span class='text-danger'>[失败]</span>");
				} else {
					showUploadFileAlert("提示：出现意外错误，文件：[" + fname
						+ "]上传失败，上传被中断。");
					$("#uls_" + count).html("<span class='text-danger'>[失败]</span>");
				}
			} else {
				showUploadFileAlert("提示：出现意外错误，文件：[" + fname + "]上传失败，上传被中断。");
				$("#uls_" + count).html("<span class='text-danger'>[失败]</span>");
			}
		};
	} else {
		showUploadFileAlert("提示：要上传的文件不存在。");
		$("#uploadstatus").prepend(
			"<p>未找到要上传的文件 <span id='uls_" + count + "' class='text-danger'>[失败]</span></p>");
	}
}

// 显示上传文件进度
function uploadProgress(evt) {
	if (evt.lengthComputable) {
		// evt.loaded：文件上传的大小 evt.total：文件总的大小
		var percentComplete = Math.round((evt.loaded) * 100 / evt.total);
		// 加载进度条，同时显示信息
		$("#pros").width(percentComplete + "%");
		$("#pros").attr('aria-valuenow', "" + percentComplete);
	}
}

// 显示上传文件错误提示
function showUploadFileAlert(txt) {
	isUpLoading = false;
	$("#filepath").prop("disabled", false);
	$("#uploadFileAlert").show();
	$("#uploadFileAlert").text(txt);
	$("#umbutton").prop('disabled', false);
}

// 取消上传文件
function abortUpload() {
	if (isUpLoading) {
		isUpLoading = false;
		if (xhr != null) {
			xhr.abort();
		}
	}
	$('#uploadFileModal').modal('hide');
	showFolderView(locationpath);
}

function goBackToTop() {
	$('html,body').animate({
		scrollTop: 0
	}, 'slow');
}

var getDownloadFileId;// 下载链接的文件ID
var getDownloadFileName;// 下载链接的文件名（便于下载工具识别）

// 获取某一文件的下载链接
function getDownloadURL() {
	$
		.ajax({
			url: 'externalLinksController/getDownloadKey.ajax',
			type: 'POST',
			dataType: 'text',
			data: {
				fId: getDownloadFileId
			},
			success: function(result) {
				// 获取链接
				var dlurl = window.location.protocol
					+ "//"
					+ window.location.host
					+ "/externalLinksController/downloadFileByKey/"
					+ encodeURIComponent(getDownloadFileName.replace(
						/\\/g, "_")) + "?dkey=" + result;
				// 显示链接内容
				$("#downloadHrefBox").html(
					"<a href='" + dlurl + "'>" + dlurl + "</a>");
			},
			error: function() {
				$("#downloadHrefBox")
					.html(
						"<span class='text-muted'>获取失败，请检查网络状态或<a href='#' onclick='event.preventDefault(); getDownloadURL()'>点此</a>重新获取。</span>");
			}
		});
}

// 防止长耗时待机时会话超时的应答器，每分钟应答一次
function ping() {
	$.ajax({
		url: "homeController/ping.ajax",
		type: "POST",
		dataType: "text",
		data: {},
		success: function(result) {
			if (result != 'pong') {
				if (pingInt != null) {
					window.clearInterval(pingInt);
					pingInt = null;
				}
			}
		},
		error: function() {
			if (pingInt != null) {
				window.clearInterval(pingInt);
				pingInt = null;
			}
		}
	});
}

// 判断浏览器是否支持webkitdirectory属性且不为ios系统（判断是否能进行文件夹上传）
function isSupportWebkitdirectory() {
	var testWebkitdirectory = document.createElement("input");
	if ("webkitdirectory" in testWebkitdirectory
		&& !(/(iPhone|iPad|iPod|iOS)/i.test(navigator.userAgent))) {
		return true;
	} else {
		return false;
	}
};
function showUploadFolderModel() {
	$("#importFolderAlert").hide();
	$("#importFolderAlert").text("");
	if (isImporting == false) {// 如果未进行上传，则还原上传文件夹的基本状态
		$("#folderpath").val("");
		$("#importfolder").val("");
		$("#importpros").width("0%");
		$("#importpros").attr('aria-valuenow', '0');
		$("#importstatus").html("");
		$("#folderpath").prop("disabled", false);
		$("#importFolderLevelBtn").prop("disabled", false);
		$("#importcount").text("");
		$("#importbutton").prop('disabled', false);
		$("#importfoldertypelist").html("");
		$("#selectFolderImportModelAlert").hide();
		if (account != null) {
			$("#folderpath")
				.attr("folderConstraintLevel", constraintLevel + "");
			$("#importfoldertype").text(folderTypes[constraintLevel]);
			for (var i = constraintLevel; i < folderTypes.length; i++) {
				$("#importfoldertypelist").append(
					"<li><a onclick='changeImportFolderType(" + i + ")'>"
					+ folderTypes[i] + "</a></li>");
			}
		} else {
			$("#importfoldertypelist").append(
				"<li><a onclick='changeImportFolderType(0)'>"
				+ folderTypes[0] + "</a></li>");
		}
	}
	$("#importFolderModal").modal('show');
}

// 点击上传路径文本框时弹出文件夹选择窗口
function checkimportpath() {
	$('#importfolder').click();
}

// 用户选择文件夹后回填路径
function getInputImport() {
	ifs = $("#importfolder")[0].files;
	if (ifs.length > 0) {
		importFolderName = ifs[0].webkitRelativePath.substring(0,
			ifs[0].webkitRelativePath.indexOf("/"));
		$("#folderpath").val(importFolderName);
	}
}

// 检查文件夹是否能够上传
function checkImportFolder() {
	if (isUpLoading == false && isImporting == false) {
		if (ifs != null && ifs.length > 0) {// 必须选中文件
			$("#folderpath").prop("disabled", true);
			$("#importFolderLevelBtn").prop("disabled", true);
			$("#importbutton").prop('disabled', true);
			$("#importFolderAlert").hide();
			$("#importFolderAlert").text("");
			isImporting = true;
			var maxSize = 0;
			var maxFileIndex = 0;
			// 找出最大体积的文件以便服务器进行效验
			for (var i = 0; i < ifs.length; i++) {
				if (ifs[i].size > maxSize) {
					maxSize = ifs[i].size;
					maxFileIndex = i;
				}
			}
			uploadTargetFolder = locationpath;// 记录上传目标
			// 发送合法性检查请求
			$
				.ajax({
					url: 'homeController/checkImportFolder.ajax',
					type: 'POST',
					dataType: 'text',
					data: {
						folderName: importFolderName,
						maxSize: maxSize,
						folderId: uploadTargetFolder
					},
					success: function(result) {
						var resJson = JSON.parse(result);
						switch (resJson.result) {
							case 'noAuthorized':
								showImportFolderAlert("提示：您的操作未被授权，无法开始上传");
								break;
							case 'errorParameter':
								showImportFolderAlert("提示：参数不正确，无法开始上传。必须选择一个文件夹");
								break;
							case 'mustLogin':
								window.location.href = "prv/login.html";
								break;
							case 'fileOverSize':
								showImportFolderAlert("提示：文件["
									+ ifs[maxFileIndex].webkitRelativePath
									+ "]的体积超过最大限制（" + resJson.maxSize
									+ "），无法开始上传");
								break;
							case 'foldersTotalOutOfLimit':
								showImportFolderAlert("提示：该文件夹内存储的文件夹数量已达上限，无法在其中上传更多文件夹。您可以尝试将其上传至其他文件夹内。");
								break;
							case 'repeatFolder_Both':
								$("#repeFolderName").text(importFolderName);
								$("#importcoverbtn").hide();
								$("#selectFolderImportModelAlert").show();
								break;
							case 'repeatFolder_coverOrBoth':
								$("#repeFolderName").text(importFolderName);
								$("#importcoverbtn").show();
								$("#selectFolderImportModelAlert").show();
								break;
							case 'permitUpload':
								iteratorImport(0);// 直接允许上传
								break;
							default:
								showImportFolderAlert("提示：出现意外错误，无法开始上传");
								break;
						}
					},
					error: function() {
						showImportFolderAlert("提示：出现意外错误，无法开始上传");
					}
				});
		} else {
			showImportFolderAlert("提示：您未选择任何文件夹，无法开始上传");
		}
	} else {
		showImportFolderAlert("提示：另一项上传文件或文件夹的任务尚未完成，无法开始上传");
	}
}

// 显示上传文件夹错误提示
function showImportFolderAlert(txt) {
	isImporting = false;
	$("#folderpath").prop("disabled", false);
	$("#importFolderLevelBtn").prop("disabled", false);
	$("#importFolderAlert").show();
	$("#importFolderAlert").text(txt);
	$("#importbutton").prop('disabled', false);
}

// 显示上传文件夹进度
function importProgress(evt) {
	if (evt.lengthComputable) {
		// evt.loaded：文件上传的大小 evt.total：文件总的大小
		var percentComplete = Math.round((evt.loaded) * 100 / evt.total);
		// 加载进度条，同时显示信息
		$("#importpros").width(percentComplete + "%");
		$("#importpros").attr('aria-valuenow', "" + percentComplete);
	}
}

// 覆盖并上传文件夹
function importAndCover() {
	$("#selectFolderImportModelAlert").hide();
	$.ajax({
		url: 'homeController/deleteFolderByName.ajax',
		type: 'POST',
		data: {
			parentId: uploadTargetFolder,
			folderName: importFolderName
		},
		dataType: 'text',
		success: function(result) {
			if (result == 'deleteSuccess') {
				iteratorImport(0);// 若覆盖成功，则开始上传
			} else {
				showImportFolderAlert("提示：无法覆盖原文件夹，上传失败");
			}
		},
		error: function() {
			showImportFolderAlert("提示：无法覆盖原文件夹，上传失败");
		}
	});
}

// 保留两者并上传文件夹
function importAndBoth() {
	$("#selectFolderImportModelAlert").hide();
	var fc = $("#folderpath").attr("folderConstraintLevel");// 文件夹访问级别
	$
		.ajax({
			url: 'homeController/createNewFolderByName.ajax',
			type: 'POST',
			data: {
				parentId: uploadTargetFolder,
				folderName: importFolderName,
				folderConstraint: fc
			},
			dataType: 'text',
			success: function(result) {
				var resJson = JSON.parse(result);
				if (resJson.result == 'success') {
					iteratorImport(0, resJson.newName);// 若新建成功，则使用新文件夹名称开始上传
				} else if (resJson.result == 'foldersTotalOutOfLimit') {
					showImportFolderAlert("提示：该文件夹内存储的文件夹数量已达上限，无法上传同名文件夹并保留两者。您可以尝试将其上传至其他文件夹内。");
				} else {
					showImportFolderAlert("提示：生成新文件夹名称失败，无法开始上传");
				}
			},
			error: function() {
				showImportFolderAlert("提示：生成新文件夹名称失败，无法开始上传");
			}
		});
}

// 迭代上传文件夹内的文件（直接上传）
function iteratorImport(i, newFolderName) {
	$("#importpros").width("0%");// 先将进度条置0
	$("#importpros").attr('aria-valuenow', "0");
	var uploadfile = ifs[i];// 获取要上传的文件
	var fcount = ifs.length;
	var fc = $("#folderpath").attr("folderConstraintLevel");// 文件夹访问级别
	if (uploadfile != null) {
		var fname = uploadfile.webkitRelativePath;
		if (fcount > 1) {
			$("#importcount").text("（" + (i + 1) + "/" + fcount + "）");// 显示当前进度
		}
		$("#importstatus").prepend(
			"<p>" + html2Escape(fname) + "<span id='ils_" + i
			+ "'>[正在上传...]</span></p>");
		xhr = new XMLHttpRequest();// 这东西类似于servlet里面的request

		var fd = new FormData();// 用于封装文件数据的对象

		fd.append("file", uploadfile);// 将文件对象添加到FormData对象中，字段名为uploadfile
		fd.append("folderId", uploadTargetFolder);
		fd.append("folderConstraint", fc);
		fd.append("originalFileName", fname);
		if (!!newFolderName) {
			fd.append("newFolderName", newFolderName);
		}
		xhr.open("POST", "homeController/doImportFolder.ajax", true);// 上传目标

		xhr.upload.addEventListener("progress", importProgress, false);// 这个是对上传进度的监听
		// 上面的三个参数分别是：事件名（指定名称）、回调函数、是否冒泡（一般是false即可）

		xhr.send(fd);// 上传FormData对象

		if (pingInt == null) {
			pingInt = setInterval(function() { ping(); }, 60000);// 上传中开始计时应答
		}

		// 上传结束后执行的回调函数
		xhr.onloadend = function() {
			// 停止应答计时
			if (pingInt != null) {
				window.clearInterval(pingInt);
				pingInt = null;
			}
			if (xhr.status === 200) {
				// TODO 上传成功
				var result = xhr.responseText;
				if (result == "uploadsuccess") {
					$("#ils_" + i).html("<span class='text-success'>[已完成]</span>");
					var ni = i + 1;
					if (ni < fcount) {
						iteratorImport(ni, newFolderName);
					} else {
						isImporting = false;
						$("#folderpath").prop("disabled", false);
						$("#importFolderLevelBtn").prop("disabled", false);
						$("#importfolder").val("");
						$("#folderpath").val("");
						$("#importpros").width("0%");
						$("#importpros").attr('aria-valuenow', "0");
						$("#importbutton").prop('disabled', false);
						$("#importcount").text("");
						$("#importstatus").text("");
						$('#importFolderModal').modal('hide');
						showFolderView(locationpath);
					}
				} else if (result == "uploaderror") {
					showImportFolderAlert("提示：出现意外错误，文件：[" + fname
						+ "]上传失败，上传被中断。");
					$("#ils_" + i).html("<span class='text-danger'>[失败]</span>");
				} else if (result == "foldersTotalOutOfLimit") {
					showImportFolderAlert("提示：该文件夹内存储的文件夹数量已达上限，文件：[" + fname
						+ "]上传失败，上传被中断。");
					$("#ils_" + i).html("<span class='text-danger'>[失败]</span>");
				} else if (result == "filesTotalOutOfLimit") {
					showImportFolderAlert("提示：该文件夹内存储的文件数量已达上限，文件：[" + fname
						+ "]上传失败，上传被中断。");
					$("#ils_" + i).html("<span class='text-danger'>[失败]</span>");
				} else {
					showImportFolderAlert("提示：出现意外错误，文件：[" + fname
						+ "]上传失败，上传被中断。");
					$("#ils_" + i).html("<span class='text-danger'>[失败]</span>");
				}
			} else {
				showImportFolderAlert("提示：出现意外错误，文件：[" + fname + "]上传失败，上传被中断。");
				$("#ils_" + i).html("<span class='text-danger'>[失败]</span>");
			}
		};
	} else {
		showImportFolderAlert("提示：要上传的文件不存在。");
		$("#importstatus").prepend(
			"<p>未找到要上传的文件 <span id='ils_" + i + "' class='text-danger'>[失败]</span></p>");
	}
}

// 取消文件夹上传
function abortImport() {
	if (isImporting) {
		isImporting = false;
		if (xhr != null) {
			xhr.abort();
		}
	}
	$('#importFolderModal').modal('hide');
	showFolderView(locationpath);
}

// 修改上传文件夹约束等级
function changeImportFolderType(type) {
	$("#importfoldertype").text(folderTypes[type]);
	$("#folderpath").attr("folderConstraintLevel", type + "");
}
