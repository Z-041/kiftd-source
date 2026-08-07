// 显示下载文件模态框
function showDownloadModel(fileId, fileName) {
	$("#downloadFileName").text("提示：您确认要下载文件：[" + fileName + "]么？");
	$("#downloadHrefBox").html("<span class='text-muted'>正在生成...</span>");
	getDownloadFileId = fileId;
	getDownloadFileName = fileName;
	$("#downloadFileBox")
		.html(
			"<button id='dlmbutton' type='button' class='btn btn-primary' onclick='dodownload("
			+ '"' + fileId + '"' + ")'>开始下载</button>");
	$("#dlmbutton").prop('disabled', false);
	$("#downloadModal").modal('show');
}

// 执行下载操作
function dodownload(fileId) {
	$("#dlmbutton").prop('disabled', true);
	$("#downloadFileName").text("提示：准备开始下载，请稍候...");
	var t = setTimeout(function() {$('#downloadModal').modal('hide');}, 800);
	window.location.href = "homeController/downloadFile.do?fileId=" + fileId;
}

// 显示删除文件模态框
function showDeleteFileModel(fileId, fileName) {
	$('#deleteFileBox')
		.html(
			"<button id='dfmbutton' type='button' class='btn btn-danger' onclick='deleteFile("
			+ '"' + fileId + '"' + ")'>删除</button>");
	$("#dfmbutton").prop('disabled', false);
	$("#cancelDeleteFileBox").prop('disabled', false);
	$('#deleteFileMessage').text("提示：确定要彻底删除文件：[" + fileName + "]么？该操作不可恢复");
	$('#deleteFileModal').modal('toggle');
}

// 执行删除文件操作
function deleteFile(fileId) {
	$("#dfmbutton").prop('disabled', true);
	$("#cancelDeleteFileBox").prop('disabled', true);
	$('#deleteFileMessage').text("提示：正在删除，请稍候...");
	$.ajax({
		type: "POST",
		dataType: "text",
		data: {
			fileId: fileId
		},
		url: "homeController/deleteFile.ajax",
		success: function(result) {
			if (result == "mustLogin") {
				window.location.href = "prv/login.html";
			} else {
				if (result == "noAuthorized") {
					$('#deleteFileMessage').text("提示：您的操作未被授权，删除失败");
					$("#dfmbutton").prop('disabled', false);
					$("#cancelDeleteFileBox").prop('disabled', false);
				} else if (result == "errorParameter") {
					$('#deleteFileMessage').text("提示：参数不正确，删除失败");
					$("#dfmbutton").prop('disabled', false);
					$("#cancelDeleteFileBox").prop('disabled', false);
				} else if (result == "cannotDeleteFile") {
					$('#deleteFileMessage').text("提示：出现意外错误，可能未能删除文件");
					$("#dfmbutton").prop('disabled', false);
					$("#cancelDeleteFileBox").prop('disabled', false);
				} else if (result == "deleteFileSuccess") {
					$('#deleteFileModal').modal('hide');
					showFolderView(locationpath);
				} else {
					$('#deleteFileMessage').text("提示：出现意外错误，可能未能删除文件");
					$("#dfmbutton").prop('disabled', false);
					$("#cancelDeleteFileBox").prop('disabled', false);
				}
			}
		},
		error: function() {
			$('#deleteFileMessage').text("提示：出现意外错误，可能未能删除文件");
			$("#dfmbutton").prop('disabled', false);
			$("#cancelDeleteFileBox").prop('disabled', false);
		}
	});
}

// 显示重命名文件模态框
function showRenameFileModel(fileId, fileName) {
	$("#newFileNamealert").removeClass("alert");
	$("#newFileNamealert").removeClass("alert-danger");
	$("#filerenamebox").removeClass("has-error");
	$("#newFileNamealert").text("");
	$("#renameFileBox").html(
		"<button type='button' class='btn btn-primary' onclick='renameFile("
		+ '"' + fileId + '"' + ")'>修改</button>");
	$("#newfilename").val(fileName);
	$("#renameFileModal").modal('toggle');
}

// 修改文件名
function renameFile(fileId) {
	var reg = new RegExp("[\/\|\\\\\*\\<\\>\\?\\:\\&\\$" + '"' + "]+", "g");
	var newFileName = $("#newfilename").val();
	if (newFileName.length > 0) {
		if (newFileName.length < 128) {
			if (!reg.test(newFileName) && newFileName.indexOf(".") != 0) {
				$.ajax({
					type: "POST",
					dataType: "text",
					data: {
						fileId: fileId,
						newFileName: newFileName
					},
					url: "homeController/renameFile.ajax",
					success: function(result) {
						if (result == "mustLogin") {
							window.location.href = "prv/login.html";
						} else {
							if (result == "cannotRenameFile") {
								showRFileAlert("提示：出现意外错误，可能未能重命名文件，请刷新后重试。");
							} else if (result == "renameFileSuccess") {
								$('#renameFileModal').modal('hide');
								showFolderView(locationpath);
							} else if (result == "errorParameter") {
								showRFileAlert("提示：参数错误，重命名失败，请刷新后重试。");
							} else if (result == "nameOccupied") {
								showRFileAlert("提示：该名称已被占用，请选取其他名称。");
							} else if (result == "noAuthorized") {
								showRFileAlert("提示：您的操作未被授权，重命名失败，请刷新后重试。");
							} else {
								showRFileAlert("提示：出现意外错误，可能未能重命名文件，请刷新后重试。");
							}
						}
					},
					error: function() {
						showRFileAlert("提示：出现意外错误，可能未能重命名文件。");
					}
				});
			} else {
				showRFileAlert("提示：文件名中不应含有：引号 / \\ * | < > & $ : ? 且不能以“.”开头。");
			}
		} else {
			showRFileAlert("提示：文件名称太长。");
		}
	} else {
		showRFileAlert("提示：文件名不能为空。");
	}
}

// 显示重命名文件状态提示
function showRFileAlert(txt) {
	$("#newFileNamealert").addClass("alert");
	$("#newFileNamealert").addClass("alert-danger");
	$("#filerenamebox").addClass("has-error");
	$("#newFileNamealert").text(txt);
}

// 获取文件名的后缀名，以小写形式输出
function getSuffix(filename) {
	var index1 = filename.lastIndexOf(".");
	var index2 = filename.length;
	var suffix = filename.substring(index1 + 1, index2);
	return suffix.toLowerCase();
}

// 播放指定格式的视频
function playVideo(fileId) {
	window.open("quickview/video.html?fileId=" + fileId);
}

// 预览Office/PDF文档
function openPreview(fileId, fileName) {
	window.open("quickview/preview.html?fileId=" + fileId + "&name=" + encodeURIComponent(fileName));
}

// 兼容Chrome、IE、FF的Shift判定
function isShift(event) {
	var e = window.event || event;
	if (e.shiftKey) {
		return true;
	} else {
		return false;
	}
}

// 选中某一行文件，如果使用Shift点击则为多选
function checkfile(event, fileId) {
	if (!isShift(event)) {
		$(".filerow").removeClass("info");
		$("#" + fileId).addClass("info");
	} else {
		if ($("#" + fileId).hasClass("info")) {
			$("#" + fileId).removeClass("info");
		} else {
			$("#" + fileId).addClass("info");
		}
	}
}

// 连续选中若干行文件：Shift+双击，选中规则为：前有选前，后有选后，全有也选后。
function checkConsFile(event, fileId) {
	if (isShift(event)) {
		var endRow = $("#" + fileId);
		var endRowIndex = endRow.index();
		var startRowIndex = $('.filerow.info:last').index();
		if (startRowIndex != -1) {
			if (startRowIndex < endRowIndex) {
				while (endRow[0] && !endRow.hasClass("info")) {
					endRow.addClass("info");
					endRow = endRow.prev();
				}
			} else {
				while (endRow[0] && !endRow.hasClass("info")) {
					endRow.addClass("info");
					endRow = endRow.next();
				}
			}
		}
	}
}

// 用于获取全部选中的文件夹和文件ID，该function返回一个对象。
// 该对象中，filesId属性为文件ID，foldersId属性为文件夹ID。两个属性均为JSON数组形式的字符串，可直接发送至服务器。此外还有size，包含了元素的总数。
function getCheckedFilesAndFolders() {
	var filesAndFolders = new Object();
	filesAndFolders.size = 0;
	var filesId = new Array();
	var foldersId = new Array();
	var checkedfiles = $(".info").get();
	for (var i = 0; i < checkedfiles.length; i++) {
		if (checkedfiles[i].getAttribute("iskfolder") == "true") {
			foldersId.push(checkedfiles[i].id);
		} else {
			filesId.push(checkedfiles[i].id);
		}
		filesAndFolders.size++;
	}
	filesAndFolders.filesId = JSON.stringify(filesId);
	filesAndFolders.foldersId = JSON.stringify(foldersId);
	return filesAndFolders;
}

// 切换全部文件行的选中或非选中
function checkallfile() {
	if ($(".filerow.info").length == $(".filerow").length) {
		$(".filerow").removeClass("info");
	} else {
		$(".filerow").addClass("info");
	}
}

// 显示打包下载模态框
function showDownloadAllCheckedModel() {
	if (!folderView.enableDownloadZip) {
		return;
	}
	$("#downloadAllCheckedBox").html("");
	$("#downloadAllCheckedLoad").text("");
	var faf = getCheckedFilesAndFolders();
	if (faf.size == 0) {
		$("#downloadAllCheckedName").html(checkFilesTip);
	} else {
		$("#downloadAllCheckedName").text("提示：您确认要打包并下载这" + faf.size + "项么？");
		$("#downloadAllCheckedBox")
			.html(
				"<button id='dclmbutton' type='button' class='btn btn-primary' onclick='downloadAllChecked()'>开始下载</button>");
		$("#dclmbutton").prop('disabled', false);
	}
	$("#cancelDownloadAllCheckedBtn").prop('disabled', false);
	$("#downloadAllCheckedModal").modal('toggle');
}

// 下载选中的所有文件
function downloadAllChecked() {
	$("#dclmbutton").prop('disabled', true);
	$("#cancelDownloadAllCheckedBtn").prop('disabled', true);
	var faf = getCheckedFilesAndFolders();
	$("#downloadAllCheckedName").text(
		"提示：服务器正在对选中资源进行压缩（共" + faf.size
		+ "项），这可能需要一些时间（文件越大耗时越长），压缩完成将自动开始下载。");
	// 计算预计耗时
	$.ajax({
		url: 'homeController/getPackTime.ajax',
		type: 'POST',
		data: {
			strIdList: faf.filesId,
			strFidList: faf.foldersId
		},
		dataType: 'text',
		success: function(result) {
			if (result != "0") {
				var count = 0;
				$("#downloadAllCheckedLoad").text(
					"已耗时：" + count + "秒（预计耗时：" + result + "）");
				zipTimer = setInterval(function() {
					count++;
					$("#downloadAllCheckedLoad").text(
						"已耗时：" + count + "秒（预计耗时：" + result + "）");
				}, 1000);
			} else {
				var count = 0;
				$("#downloadAllCheckedLoad").text("已耗时：" + count + "秒");
				zipTimer = setInterval(function() {
					count++;
					$("#downloadAllCheckedLoad").text("已耗时：" + count + "秒");
				}, 1000);
			}
		},
		error: function() {
			$("#downloadAllCheckedLoad").text("（无法获取预计耗时）");
		}
	});
	// 同时发送压缩下载请求
	$.ajax({
		type: "POST",
		url: "homeController/downloadCheckedFiles.ajax",
		data: {
			strIdList: faf.filesId,
			strFidList: faf.foldersId
		},
		dataType: "text",
		success: function(result) {
			if (zipTimer != null) {
				window.clearInterval(zipTimer);
			}
			if (result == "ERROR") {
				$("#downloadAllCheckedName")
					.text("提示：压缩过程出错。无法完成压缩，请重试或告知管理员。");
				$("#dclmbutton").prop('disabled', false);
				$("#cancelDownloadAllCheckedBtn").prop('disabled', false);
			} else {
				$("#downloadAllCheckedLoad").text("");
				$("#downloadAllCheckedName").text("提示：压缩完成！准备开始下载...");
				var t = setTimeout(
					"$('#downloadAllCheckedModal').modal('hide');", 800);
				// POST提交全部下载请求
				var temp = document.createElement("form");
				temp.action = 'homeController/downloadCheckedFilesZip.do';
				temp.method = "post";
				temp.style.display = "none";
				var sl = document.createElement("input");
				sl.name = 'zipId';
				sl.value = result;
				temp.appendChild(sl);
				document.body.appendChild(temp);
				temp.submit();
			}
		},
		error: function() {
			$("#downloadAllCheckedName").text("提示：请求失败。无法完成压缩，请重试或告知管理员。");
			$("#dclmbutton").prop('disabled', false);
			$("#cancelDownloadAllCheckedBtn").prop('disabled', false);
		}
	});
}

// 删除选中的所有文件
function showDeleteAllCheckedModel() {
	$('#deleteFileBox').html("");
	var faf = getCheckedFilesAndFolders();
	$("#dfmbutton").prop('disabled', false);
	$("#cancelDeleteFileBox").prop('disabled', false);
	if (faf.size == 0) {
		$('#deleteFileMessage').html(checkFilesTip);
	} else {
		$('#deleteFileBox')
			.html(
				"<button id='dfmbutton' type='button' class='btn btn-danger' onclick='deleteAllChecked()'>全部删除</button>");
		$('#deleteFileMessage').text("提示：确定要彻底删除这" + faf.size + "项么？该操作不可恢复！");
	}
	$('#deleteFileModal').modal('toggle');
}

// 删除选中的所有文件
function deleteAllChecked() {
	// 提交全部删除请求
	var faf = getCheckedFilesAndFolders();
	$("#dfmbutton").prop('disabled', true);
	$("#cancelDeleteFileBox").prop('disabled', true);
	$('#deleteFileMessage').text("提示：正在删除，请稍候...");
	$.ajax({
		type: "POST",
		dataType: "text",
		data: {
			strIdList: faf.filesId,
			strFidList: faf.foldersId
		},
		url: "homeController/deleteCheckedFiles.ajax",
		success: function(result) {
			if (result == "mustLogin") {
				window.location.href = "prv/login.html";
			} else {
				if (result == "noAuthorized") {
					$('#deleteFileMessage').text("提示：您的操作未被授权，删除失败");
					$("#dfmbutton").prop('disabled', false);
					$("#cancelDeleteFileBox").prop('disabled', false);
				} else if (result == "errorParameter") {
					$('#deleteFileMessage').text("提示：参数不正确，未能全部删除文件");
					$("#dfmbutton").prop('disabled', false);
					$("#cancelDeleteFileBox").prop('disabled', false);
				} else if (result == "cannotDeleteFile") {
					$('#deleteFileMessage').text("提示：出现意外错误，可能未能删除全部文件");
					$("#dfmbutton").prop('disabled', false);
					$("#cancelDeleteFileBox").prop('disabled', false);
				} else if (result == "deleteFileSuccess") {
					$('#deleteFileModal').modal('hide');
					showFolderView(locationpath);
				} else {
					$('#deleteFileMessage').text("提示：出现意外错误，可能未能删除全部文件");
					$("#dfmbutton").prop('disabled', false);
					$("#cancelDeleteFileBox").prop('disabled', false);
				}
			}
		},
		error: function() {
			$('#deleteFileMessage').text("提示：出现意外错误，可能未能删除全部文件");
			$("#dfmbutton").prop('disabled', false);
			$("#cancelDeleteFileBox").prop('disabled', false);
		}
	});
}

// 按文件名排序
function sortbyfn() {
	if (!loadingComplete) {
		return;
	}
	if ($("#sortByCD,#sortByFS,#sortByCN,#sortByOR").hasClass(
		"glyphicon glyphicon-hourglass")) {
		return;
	}
	$("#sortByCD").removeClass();
	$("#sortByFS").removeClass();
	$("#sortByCN").removeClass();
	$("#sortByOR").removeClass();
	var order = 1;
	if ($("#sortByFN").hasClass('glyphicon-triangle-bottom')) {
		order = -1;
	}
	$("#sortByFN").removeClass();
	$("#sortByFN").addClass("glyphicon glyphicon-hourglass");
	// 另开一个计时器进行排序操作，避免因卡死导致加载动画无法显示
	setTimeout(function() {
		folderView.fileList.sort(function(v1, v2) {
			return order * v2.fileName.localeCompare(v1.fileName, "zh");
		});
		folderView.folderList.sort(function(v1, v2) {
			return order * v2.folderName.localeCompare(v1.folderName, "zh");
		});
		showFolderTable(folderView);
		$("#sortByFN").removeClass();
		if (order == -1) {
			$("#sortByFN").addClass("glyphicon glyphicon-triangle-up");
		} else {
			$("#sortByFN").addClass("glyphicon glyphicon-triangle-bottom");
		}
	}, 0);
}

// 按创建日期排序
function sortbycd() {
	if (!loadingComplete) {
		return;
	}
	if ($("#sortByFN,#sortByFS,#sortByCN,#sortByOR").hasClass(
		"glyphicon glyphicon-hourglass")) {
		return;
	}
	$("#sortByFN").removeClass();
	$("#sortByFS").removeClass();
	$("#sortByCN").removeClass();
	$("#sortByOR").removeClass();
	var order = 1;
	if ($("#sortByCD").hasClass('glyphicon-triangle-bottom')) {
		order = -1;
	}
	$("#sortByCD").removeClass();
	$("#sortByCD").addClass("glyphicon glyphicon-hourglass");
	setTimeout(function() {
		folderView.fileList.sort(function(v1, v2) {
			var v1DateStr = v1.fileCreationDate.replace("年", "-").replace("月",
				"-").replace("日", "");
			var v2DateStr = v2.fileCreationDate.replace("年", "-").replace("月",
				"-").replace("日", "");
			var res = ((new Date(Date.parse(v1DateStr)).getTime()) - (new Date(
				Date.parse(v2DateStr)).getTime()));
			return order * res;
		});
		folderView.folderList.sort(function(v1, v2) {
			var v1DateStr = v1.folderCreationDate.replace("年", "-").replace(
				"月", "-").replace("日", "");
			var v2DateStr = v2.folderCreationDate.replace("年", "-").replace(
				"月", "-").replace("日", "");
			var res = ((new Date(Date.parse(v1DateStr)).getTime()) - (new Date(
				Date.parse(v2DateStr)).getTime()));
			return order * res;
		});
		showFolderTable(folderView);
		$("#sortByCD").removeClass();
		if (order == -1) {
			$("#sortByCD").addClass("glyphicon glyphicon-triangle-up");
		} else {
			$("#sortByCD").addClass("glyphicon glyphicon-triangle-bottom");
		}
	}, 0);
}

// 按文件大小排序
function sortbyfs() {
	if (!loadingComplete) {
		return;
	}
	if ($("#sortByFN,#sortByCD,#sortByCN,#sortByOR").hasClass(
		"glyphicon glyphicon-hourglass")) {
		return;
	}
	$("#sortByFN").removeClass();
	$("#sortByCD").removeClass();
	$("#sortByCN").removeClass();
	$("#sortByOR").removeClass();
	var order = 1;
	if ($("#sortByFS").hasClass("glyphicon-triangle-bottom")) {
		order = -1;
	}
	$("#sortByFS").removeClass();
	$("#sortByFS").addClass("glyphicon glyphicon-hourglass");
	setTimeout(function() {
		folderView.fileList.sort(function(v1, v2) {
			return order * (v1.fileSize - v2.fileSize);
		});
		showFolderTable(folderView);
		$("#sortByFS").removeClass();
		if (order == -1) {
			$("#sortByFS").addClass("glyphicon glyphicon-triangle-up");
		} else {
			$("#sortByFS").addClass("glyphicon glyphicon-triangle-bottom");
		}
	}, 0);
}

// 按创建者排序
function sortbycn() {
	if (!loadingComplete) {
		return;
	}
	if ($("#sortByFN,#sortByCD,#sortByFS,#sortByOR").hasClass(
		"glyphicon glyphicon-hourglass")) {
		return;
	}
	$("#sortByFN").removeClass();
	$("#sortByCD").removeClass();
	$("#sortByFS").removeClass();
	$("#sortByOR").removeClass();
	var order = 1;
	if ($("#sortByCN").hasClass('glyphicon-triangle-bottom')) {
		order = -1;
	}
	$("#sortByCN").removeClass();
	$("#sortByCN").addClass("glyphicon glyphicon-hourglass");
	setTimeout(function() {
		folderView.fileList.sort(function(v1, v2) {
			return order * v2.fileCreator.localeCompare(v1.fileCreator, "zh");
		});
		folderView.folderList.sort(function(v1, v2) {
			return order
				* v2.folderCreator.localeCompare(v1.folderCreator, "zh");
		});
		showFolderTable(folderView);
		$("#sortByCN").removeClass();
		if (order == -1) {
			$("#sortByCN").addClass("glyphicon glyphicon-triangle-up");
		} else {
			$("#sortByCN").addClass("glyphicon glyphicon-triangle-bottom");
		}
	}, 0);
}

// 显示原始的顺序
function showOriginFolderView() {
	if (!loadingComplete) {
		return;
	}
	if ($("#sortByFN,#sortByCD,#sortByFS,#sortByCN").hasClass(
		"glyphicon glyphicon-hourglass")) {
		return;
	}
	$("#sortByFN").removeClass();
	$("#sortByCD").removeClass();
	$("#sortByFS").removeClass();
	$("#sortByCN").removeClass();
	$("#sortByOR").addClass("glyphicon glyphicon-hourglass");
	setTimeout(function() {
		if (screenedFoldrView != null) {
			folderView = $.extend(true, {}, screenedFoldrView);
		} else {
			folderView = $.extend(true, {}, originFolderView);
		}
		showFolderTable(folderView);
		$("#sortByOR").removeClass();
	}, 0);
}

// 执行“剪切”操作
function cutFile() {
	var checkedfiles = getCheckedFilesAndFolders();
	if (checkedfiles == undefined || checkedfiles.size == 0) {
		// 如果未选中任何文件，则提示用户要先选
		$('#moveFilesMessage').html(checkFilesTip);
		$("#selectFileMoveModelAsAll").prop("checked", false);
		$("#selectFileMoveModelAlert").hide();
		$("#dmvfbutton").remove();
		$('#moveFilesModal').modal('show');
	} else {
		// 否则，显示“粘贴”按钮
		checkedMovefiles = checkedfiles;
		if (checkedMovefiles.size < 100) {
			$("#stickFilesCount").text("（" + checkedMovefiles.size + "）");
		} else {
			$("#stickFilesCount").text("（99+）");
		}
		$("#stickFileButtonLi").removeClass("hidden");
		$("#stickFileButtonLi").addClass("show");
		isCopy = false;
	}
}

// 执行“复制”操作
function copyFile() {
	var checkedfiles = getCheckedFilesAndFolders();
	if (checkedfiles == undefined || checkedfiles.size == 0) {
		// 如果未选中任何文件，则提示用户要先选
		$('#moveFilesMessage').html(checkFilesTip);
		$("#selectFileMoveModelAsAll").prop("checked", false);
		$("#selectFileMoveModelAlert").hide();
		$("#dmvfbutton").remove();
		$('#moveFilesModal').modal('show');
	} else {
		// 否则，显示“粘贴”按钮
		checkedMovefiles = checkedfiles;
		if (checkedMovefiles.size < 100) {
			$("#stickFilesCount").text("（" + checkedMovefiles.size + "）");
		} else {
			$("#stickFilesCount").text("（99+）");
		}
		$("#stickFileButtonLi").removeClass("hidden");
		$("#stickFileButtonLi").addClass("show");
		isCopy = true;
	}
}

// 执行“粘贴”操作
function stickFile() {
	if (checkedMovefiles !== undefined && checkedMovefiles.size > 0) {
		if (isCopy) {
			$('#moveFilesMessage').text(
				"提示：确定将这" + checkedMovefiles.size + "项复制到当前位置么？");
			$('#moveFilesBox')
				.html(
					"<button id='dmvfbutton' type='button' class='btn btn-primary' onclick='doMoveFiles()'>全部复制</button>");
		} else {
			$('#moveFilesMessage').text(
				"提示：确定将这" + checkedMovefiles.size + "项移动到当前位置么？");
			$('#moveFilesBox')
				.html(
					"<button id='dmvfbutton' type='button' class='btn btn-danger' onclick='doMoveFiles()'>全部移动</button>");
		}
		$("#selectFileMoveModelAsAll").prop("checked", false);
		$("#cancelMoveFilesBtn").prop('disabled', false);
		$("#selectFileMoveModelAlert").hide();
		$('#moveFilesModal').modal('show');
	}
}

// 先行确认文件移动操作
function doMoveFiles() {
	$("#dmvfbutton").prop('disabled', true);
	$("#cancelMoveFilesBtn").prop('disabled', true);
	var method = "MOVE";
	if (isCopy) {
		$('#moveFilesMessage').text("提示：正在复制，请稍候...");
		method = "COPY";
	} else {
		$('#moveFilesMessage').text("提示：正在移动，请稍候...");
	}
	// 确认移动目标位置
	$
		.ajax({
			type: "POST",
			dataType: "text",
			data: {
				strIdList: checkedMovefiles.filesId,
				strFidList: checkedMovefiles.foldersId,
				locationpath: locationpath,
				method: method
			},
			url: "homeController/confirmMoveFiles.ajax",
			success: function(result) {
				if (result == "mustLogin") {
					window.location.href = "prv/login.html";
				} else {
					switch (result) {
						case "noAuthorized":
							$('#moveFilesMessage').text("提示：您的操作未被授权，操作失败");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
						case "errorParameter":
							$('#moveFilesMessage').text(
								"提示：参数不正确，无法完成此操作，请刷新后重试");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
						case "cannotMoveFiles":
							$('#moveFilesMessage').text(
								"提示：出现意外错误，可能未能完成此操作，请刷新后重试");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
						case "filesTotalOutOfLimit":
							$('#moveFilesMessage').text(
								"提示：该文件夹内存储的文件数量已达上限，无法添加更多文件");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
						case "foldersTotalOutOfLimit":
							$('#moveFilesMessage').text(
								"提示：该文件夹内存储的文件夹数量已达上限，无法添加更多文件夹");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
						case "confirmMoveFiles":
							strMoveOptMap = {};
							sendMoveFilesReq();
							break;
						default:
							if (result.startsWith("duplicationFileName:")) {
								repeMap = JSON.parse(result.substring(20));
								repeIndex = 0;
								strMoveOptMap = {};
								mRepeSize = repeMap.repeFolders.length
									+ repeMap.repeNodes.length;
								if (repeMap.repeFolders.length > 0) {
									$("#mrepeFileName")
										.text(
											repeMap.repeFolders[repeIndex].folderName);
								} else {
									$("#mrepeFileName")
										.text(
											repeMap.repeNodes[repeIndex].fileName);
								}
								var authList = originFolderView.authList;
								if (checkAuth(authList, "D")) {
									$("#movecoverbtn").show();
								} else {
									$("#movecoverbtn").hide();
								}
								$("#selectFileMoveModelAlert").show();
							} else if (result
								.startsWith("CANT_MOVE_TO_INSIDE:")) {
								$('#moveFilesMessage').text(
									"错误：不能将一个文件夹移动到其自身内部："
									+ result.substring(20));
							} else {
								$('#moveFilesMessage').text(
									"提示：出现意外错误，可能未能完成此操作，请刷新后重试");
								$("#dmvfbutton").prop('disabled', false);
								$("#cancelMoveFilesBtn")
									.prop('disabled', false);
							}
							break;
					}
				}
			},
			error: function() {
				$('#moveFilesMessage').text("提示：出现意外错误，可能未能完成此操作，请刷新后重试");
				$("#dmvfbutton").prop('disabled', false);
				$("#cancelMoveFilesBtn").prop('disabled', false);
			}
		});
}

// 移动或复制——对冲突的文件进行依次询问
function selectFileMoveModel(t) {
	if ($("#selectFileMoveModelAsAll").prop("checked")) {
		while (repeIndex < mRepeSize) {
			if (repeIndex < repeMap.repeFolders.length) {
				strMoveOptMap[repeMap.repeFolders[repeIndex].folderId] = t;
			} else {
				strMoveOptMap[repeMap.repeNodes[repeIndex
					- repeMap.repeFolders.length].fileId] = t;
			}
			repeIndex++;
		}
		$("#selectFileMoveModelAlert").hide();
		sendMoveFilesReq();
	}
	if (repeIndex < repeMap.repeFolders.length) {
		strMoveOptMap[repeMap.repeFolders[repeIndex].folderId] = t;
	} else {
		strMoveOptMap[repeMap.repeNodes[repeIndex - repeMap.repeFolders.length].fileId] = t;
	}
	repeIndex++;
	if (repeIndex < mRepeSize) {
		if (repeIndex < repeMap.repeFolders.length) {
			$("#mrepeFileName").text(repeMap.repeFolders[repeIndex].folderName);
		} else {
			$("#mrepeFileName")
				.text(
					repeMap.repeNodes[repeIndex
						- repeMap.repeFolders.length].fileName);
		}
	} else {
		$("#selectFileMoveModelAlert").hide();
		sendMoveFilesReq();
	}
}

// 正式执行移动或复制文件操作
function sendMoveFilesReq() {
	// 取到对冲突文件的操作列表
	var strOptMap = JSON.stringify(strMoveOptMap);
	// 取到操作类型，是移动还是复制
	var method = "MOVE";
	if (isCopy) {
		method = "COPY";
	}
	$
		.ajax({
			type: "POST",
			dataType: "text",
			data: {
				strIdList: checkedMovefiles.filesId,
				strFidList: checkedMovefiles.foldersId,
				strOptMap: strOptMap,
				locationpath: locationpath,
				method: method
			},
			url: "homeController/moveCheckedFiles.ajax",
			success: function(result) {
				if (result == "mustLogin") {
					window.location.href = "prv/login.html";
				} else {
					switch (result) {
						case "noAuthorized":
							$('#moveFilesMessage').text("提示：您的操作未被授权，操作失败");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
						case "errorParameter":
							$('#moveFilesMessage').text(
								"提示：参数不正确，无法完成此操作，请刷新后重试");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
						case "filesTotalOutOfLimit":
							$('#moveFilesMessage').text(
								"提示：该文件夹内存储的文件数量已达上限，无法添加更多文件");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
						case "foldersTotalOutOfLimit":
							$('#moveFilesMessage').text(
								"提示：该文件夹内存储的文件夹数量已达上限，无法添加更多文件夹");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
						case "cannotMoveFiles":
							$('#moveFilesMessage').text(
								"提示：出现意外错误，可能未能完成此操作，请刷新后重试");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
						case "moveFilesSuccess":
							checkedMovefiles = undefined;
							$('#moveFilesModal').modal('hide');
							$("#cancelMoveFilesBtn").prop('disabled', false);
							showFolderView(locationpath);
							break;
						default:
							$('#moveFilesMessage').text(
								"提示：出现意外错误，可能未能完成此操作，请刷新后重试");
							$("#dmvfbutton").prop('disabled', false);
							$("#cancelMoveFilesBtn").prop('disabled', false);
							break;
					}
				}
			},
			error: function() {
				$('#moveFilesMessage').text("提示：出现意外错误，可能未能完成此操作，请刷新后重试");
				$("#dmvfbutton").prop('disabled', false);
				$("#cancelMoveFilesBtn").prop('disabled', false);
			}
		});
}

var screenedFoldrView;// 经过排序的文件视图
