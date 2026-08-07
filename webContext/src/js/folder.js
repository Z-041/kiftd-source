var folderTypes = ['公开的', '仅小组', '仅创建者'];// 文件夹约束条件（由小至大）

// 显示新建文件夹模态框
function showNewFolderModel() {
	$('#newFolderModal').modal('show');
}

// 修改新建文件夹约束等级
function changeNewFolderType(type) {
	$("#newfoldertype").text(folderTypes[type]);
	$("#foldername").attr("folderConstraintLevel", type + "");
}

// 创建新的文件夹
function createfolder() {
	var fn = $("#foldername").val();
	var fc = $("#foldername").attr("folderConstraintLevel");
	var reg = new RegExp("[\/\|\\\\\*\\<\\>\\?\\:\\&\\$" + '"' + "]+", "g");
	if (fn.length == 0) {
		showFolderAlert("提示：文件夹名称不能为空。");
	} else if (fn.length > 128) {
		showFolderAlert("提示：文件夹名称太长。");
	} else if (!reg.test(fn) && fn.indexOf(".") != 0) {
		$("#folderalert").removeClass("alert");
		$("#folderalert").removeClass("alert-danger");
		$("#foldernamebox").removeClass("has-error");
		$("#folderalert").text("");
		$.ajax({
			type: "POST",
			dataType: "text",
			data: {
				parentId: locationpath,
				folderName: fn,
				folderConstraint: fc
			},
			url: "homeController/newFolder.ajax",
			success: function(result) {
				if (result == "mustLogin") {
					window.location.href = "prv/login.html";
				} else {
					switch (result) {
						case "noAuthorized":
							showFolderAlert("提示：您的操作未被授权，创建文件夹失败。");
							break;
						case "errorParameter":
							showFolderAlert("提示：参数不正确，创建文件夹失败。");
							break;
						case "cannotCreateFolder":
							showFolderAlert("提示：出现意外错误，可能未能创建文件夹。");
							break;
						case "nameOccupied":
							showFolderAlert("提示：该名称已被占用，请选取其他名称。");
							break;
						case "foldersTotalOutOfLimit":
							showFolderAlert("提示：该文件夹内存储的文件夹数量已达上限，无法在其中创建更多文件夹。");
							break;
						case "createFolderSuccess":
							$('#newFolderModal').modal('hide');
							showFolderView(locationpath);
							break;
						default:
							showFolderAlert("提示：出现意外错误，可能未能创建文件夹。");
							break;
					}
				}
			},
			error: function() {
				showFolderAlert("提示：出现意外错误，可能未能创建文件夹");
			}
		});
	} else {
		showFolderAlert("提示：文件夹名中不应含有：引号 / \\ * | < > & $ : ? 且不能以“.”开头。");
	}
}

// 显示新建文件夹状态提示
function showFolderAlert(txt) {
	$("#folderalert").addClass("alert");
	$("#folderalert").addClass("alert-danger");
	$("#foldernamebox").addClass("has-error");
	$("#folderalert").text(txt);
}

// 进入某一文件夹
function entryFolder(folderId) {
	showFolderView(folderId);
}

// 显示删除文件夹模态框
function showDeleteFolderModel(folderId, folderName) {
	$('#deleteFolderBox')
		.html(
			"<button id='dmbutton' type='button' class='btn btn-danger' onclick='deleteFolder("
			+ '"' + folderId + '"' + ")'>删除</button>");
	$("#dmbutton").prop('disabled', false);
	$("#cancelDeleteFolderBtn").prop('disabled', false);
	$('#deleteFolderMessage').text(
		"提示：确定要彻底删除文件夹：[" + folderName + "]及其全部内容么？该操作不可恢复");
	$('#deleteFolderModal').modal('toggle');
}

// 执行删除文件夹
function deleteFolder(folderId) {
	$("#dmbutton").prop('disabled', true);
	$("#cancelDeleteFolderBtn").prop('disabled', true);
	$('#deleteFolderMessage').text("提示：正在删除，请稍候...");
	$.ajax({
		type: "POST",
		dataType: "text",
		data: {
			folderId: folderId
		},
		url: "homeController/deleteFolder.ajax",
		success: function(result) {
			if (result == "mustLogin") {
				window.location.href = "prv/login.html";
			} else {
				if (result == "noAuthorized") {
					$('#deleteFolderMessage').text("提示：您的操作未被授权，删除文件夹失败");
					$("#dmbutton").prop('disabled', false);
					$("#cancelDeleteFolderBtn").prop('disabled', true);
				} else if (result == "errorParameter") {
					$('#deleteFolderMessage').text("提示：参数不正确，删除文件夹失败");
					$("#dmbutton").prop('disabled', false);
					$("#cancelDeleteFolderBtn").prop('disabled', true);
				} else if (result == "cannotDeleteFolder") {
					$('#deleteFolderMessage').text("提示：出现意外错误，可能未能删除文件夹");
					$("#dmbutton").prop('disabled', false);
					$("#cancelDeleteFolderBtn").prop('disabled', true);
				} else if (result == "deleteFolderSuccess") {
					$('#deleteFolderModal').modal('hide');
					showFolderView(locationpath);
				} else {
					$('#deleteFolderMessage').text("提示：出现意外错误，可能未能删除文件夹");
					$("#dmbutton").prop('disabled', false);
					$("#cancelDeleteFolderBtn").prop('disabled', true);
				}
			}
		},
		error: function() {
			$('#deleteFolderMessage').text("提示：出现意外错误，可能未能删除文件夹");
			$("#dmbutton").prop('disabled', false);
			$("#cancelDeleteFolderBtn").prop('disabled', true);
		}
	});
}

// 显示重命名文件夹模态框
function showRenameFolderModel(folderId, folderName, type) {
	$("#renameFolderBox").html(
		"<button type='button' class='btn btn-primary' onclick='renameFolder("
		+ '"' + folderId + '"' + ")'>修改</button>");
	$("#newfoldername").val(folderName);
	changeEditFolderType(type);
	$("#renameFolderModal").modal('show');
}

// 修改编辑文件夹的约束等级
function changeEditFolderType(type) {
	$("#editfoldertype").text(folderTypes[type]);
	$("#newfoldername").attr("folderConstraintLevel", type + "");
}

// 执行重命名文件夹
function renameFolder(folderId) {
	var newName = $("#newfoldername").val();
	var fc = $("#newfoldername").attr("folderConstraintLevel");
	var reg = new RegExp("[\/\|\\\\\*\\<\\>\\?\\:\\&\\$" + '"' + "]+", "g");
	if (newName.length == 0) {
		showRFolderAlert("提示：文件夹名称不能为空。");
	} else if (newName.length > 128) {
		showRFolderAlert("提示：文件夹名称太长。");
	} else if (!reg.test(newName) && newName.indexOf(".") != 0) {
		$("#newfolderalert").removeClass("alert");
		$("#newfolderalert").removeClass("alert-danger");
		$("#folderrenamebox").removeClass("has-error");
		$("#newfolderalert").text("");
		$.ajax({
			type: "POST",
			dataType: "text",
			data: {
				folderId: folderId,
				newName: newName,
				folderConstraint: fc
			},
			url: "homeController/renameFolder.ajax",
			success: function(result) {
				if (result == "mustLogin") {
					window.location.href = "prv/login.html";
				} else {
					if (result == "noAuthorized") {
						showRFolderAlert("提示：您的操作未被授权，编辑失败。");
					} else if (result == "errorParameter") {
						showRFolderAlert("提示：参数不正确，编辑失败，请刷新后重试。");
					} else if (result == "nameOccupied") {
						showRFolderAlert("提示：该名称已被占用，请选取其他名称。");
					} else if (result == "renameFolderSuccess") {
						$('#renameFolderModal').modal('hide');
						showFolderView(locationpath);
					} else {
						showRFolderAlert("提示：出现意外错误，可能未能编辑文件夹，请刷新后重试。");
					}
				}
			},
			error: function() {
				showRFolderAlert("提示：出现意外错误，可能未能编辑文件夹，请刷新后重试。");
			}
		});
	} else {
		showRFolderAlert("提示：文件夹名中不应含有：引号 / \\ * | < > & $ : ? 且不能以“.”开头。");
	}
}

// 显示重命名文件夹状态提示
function showRFolderAlert(txt) {
	$("#editfolderalert").addClass("alert");
	$("#editfolderalert").addClass("alert-danger");
	$("#folderrenamebox").addClass("has-error");
	$("#editfolderalert").text(txt);
}
