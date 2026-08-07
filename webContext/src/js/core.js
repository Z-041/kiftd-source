/**
 * home.js kiftd主页面操作定义 by 青阳龙野 该文件为home.html的操作定义文件，包含了kiftd页面上的主要操作实现。
 */

// 所用val名称列表，注意不要冲突，其中一些参数能够设定界面行为。
var locationpath = "root";// 记录当前文件路径
var parentpath = "null";// 记录当前文件路径的父级目录
var zipTimer;// 打包下载计时器
var folderView;// 返回的文件系统视图对象
var originFolderView;// 保存原始的文件视图对象
var fs;// 选中的要上传的文件列表
var ifs;// 选中的要上传的文件夹内的文件列表
var checkedMovefiles;// 移动文件的存储列表
var isCopy;// 移动文件是否为复制模式，如果是，则“复制”，否则“剪切”
var repeMap;// 移动文件时，保存用户对每个冲突项目的处理操作
var strMoveOptMap;// 移动（或复制）操作导致文件名冲突的项目
var mRepeSize;// 移动（或复制）操作导致文件名冲突的项目数
var constraintLevel;// 当前文件夹限制等级
var account;// 用户账户
var isUpLoading = false;// 是否正在执行上传操作
var isImporting = false;// 是否正在执行上传文件夹操作
var uploadTargetFolder;// 执行上传操作时的目标文件夹（单独保存以避免用户切换文件夹）
var isChangingPassword = false;// 是否正在执行修改密码操作
var importFolderName;// 上传文件夹时保存文件夹名称
var xhr;// 文件或文件夹上传请求对象
var checkFilesTip = "提示：您还未选择任何文件，请先选中一些文件后再执行本操作：<br /><br /><kbd>单击</kbd>：选中某一文件<br /><br /><kbd><kbd>Shift</kbd>+<kbd>单击</kbd></kbd>：选中多个文件<br /><br /><kbd><kbd>Shift</kbd>+<kbd>双击</kbd></kbd>：选中连续的文件<br /><br /><kbd><kbd>Shitf</kbd>+<kbd>A</kbd></kbd>：选中/取消选中所有文件";// 选取文件提示
var winHeight;// 窗口高度
var pingInt;// 定时应答器的定时装置
var noticeInited = false;// 公告信息的md5标识
var loadingComplete;// 判断文件夹视图是否加载完成
var totalFoldersOffset;// 记录文件夹原始的查询偏移量，便于计算加载进度
var totalFilesOffset;// 记录文件原始的查询偏移量，便于计算加载进度
var remainingLoadingRequest;// 后续数据加载请求的索引，便于中途取消
var loadingFolderView;// 是否正在加载文件夹视图的判断，避免重复操作
var folderCountRequest;// 文件夹内容统计请求对象，该对象可用于判断当前是否在进行文件夹内容的统计，同时可用于取消该操作

// 界面功能方法定义
// 页面初始化
$(function() {
	window.onresize = function() {
		changeFilesTableStyle();
		updateWinHeight();
	}
	changeFilesTableStyle();
	getServerOS();// 得到服务器操作系统信息
	subscribeNotice();// 加载公告MD5，以判断是否需要显示最新公告
	// 查询是否存在记忆路径，若有，则直接显示记忆路径的内容，否则显示ROOT根路径
	var arr = document.cookie.match(new RegExp("(^| )folder_id=([^;]*)(;|$)"));
	if (arr != null) {
		showFolderView(unescape(arr[2]));// 显示记忆路径页面视图
	} else {
		showFolderView("root");// 显示根节点页面视图
	}
	// 点击空白处取消选中文件（已尝试兼容火狐，请期待用户反馈，如不好使再改）
	$(document).click(function(e) {
		var filetable = $("#filetable")[0];
		var srcElement = e.srcElement;
		if (!srcElement) {
			srcElement = e.target;
		}
		if (srcElement !== filetable && !$.contains(filetable, e.target)) {
			$(".filerow").removeClass("info");
		}
	});
	// 关闭打包下载模态框自动停止计时
	$('#downloadAllCheckedModal').on('hidden.bs.modal', function(e) {
		if (zipTimer != null) {
			window.clearInterval(zipTimer);
		}
	});
	// 关闭登陆模态框自动清空输入数据
	$('#loginModal').on('hidden.bs.modal', function() {
		if (!$("#dologinButton").prop('disabled')) {
			$("#accountid").val('');
			$("#accountpwd").val('');
		}
		$("#accountidbox").removeClass("has-error");
		$("#accountpwdbox").removeClass("has-error");
		$("#alertbox").removeClass("alert");
		$("#alertbox").removeClass("alert-danger");
		$("#alertbox").text("");
		$("#vercodebox").html("");
		$("#vercodebox").removeClass("show");
		$("#vercodebox").addClass("hidden");
	});
	// 各个模态框的打开判定及回车响应功能。该功能仅对“首选”的按钮有效，对其他按钮无效，以避免用户误操作。
	$('.modal').on('shown.bs.modal', function() {
		$(this).addClass("shown");
	});
	$('.modal').on('hidden.bs.modal', function() {
		$(this).removeClass("shown");
	});
	$("body").keypress(function(e) {
		var keyCode = e.keyCode ? e.keyCode : e.which ? e.which : e.charCode;
		if (keyCode == 13) {
			if ("sreachKeyWordIn" === document.activeElement.id) {
				doSearchFile();
			} else {
				var g = $(".shown .btn-primary");
				if (g.get(0) != null && g.prop("disabled") == false) {
					g.click();
				}
			}
			return false;
		}
	});
	// 开启登陆模态框自动聚焦账户输入框
	$('#loginModal').on('shown.bs.modal', function() {
		$("#accountid").focus();
	});
	// 开启新建文件夹框自动初始化状态
	$('#newFolderModal')
		.on(
			'show.bs.modal',
			function() {
				$("#folderalert").removeClass("alert");
				$("#folderalert").removeClass("alert-danger");
				$("#foldernamebox").removeClass("has-error");
				$("#folderalert").text("");
				$("#foldername").val("");
				$("#foldertypelist").html("");
				if (account != null) {
					$("#foldername").attr("folderConstraintLevel",
						constraintLevel + "");
					$("#newfoldertype").text(
						folderTypes[constraintLevel]);
					for (var i = constraintLevel; i < folderTypes.length; i++) {
						$("#foldertypelist").append(
							"<li><a onclick='changeNewFolderType("
							+ i + ")'>" + folderTypes[i]
							+ "</a></li>");
					}
				} else {
					$("#foldertypelist").append(
						"<li><a onclick='changeNewFolderType(0)'>"
						+ folderTypes[0] + "</a></li>");
				}
			});
	// 开启新建文件夹模态框自动聚焦文件名输入框
	$('#newFolderModal').on('shown.bs.modal', function() {
		$("#foldername").focus();
	});
	// 关闭上传模态框时自动提示如何查看上传进度
	$('#uploadFileModal,#importFolderModal').on('hidden.bs.modal', function(e) {
		if (isUpLoading || isImporting) {
			$('#operationMenuBox').attr("data-placement", "top");
			$('#operationMenuBox').attr("data-trigger", "focus");
			$('#operationMenuBox').attr("data-title", "上传中");
			$('#operationMenuBox').attr("data-content", "您可以重新打开上传窗口查看上传进度。");
			$('#operationMenuBox').popover();
			$('#operationMenuBox').popover('show');
			// 2秒后消失提示框
			var closeUploadTips = setTimeout(function() {
				$('#operationMenuBox').attr("data-title", "");
				$('#operationMenuBox').attr("data-content", "");
				$('#operationMenuBox').popover('destroy');
			}, 2000);
		}
	});
	// 开启编辑文件夹框自动初始化状态
	$('#renameFolderModal')
		.on(
			'show.bs.modal',
			function(e) {
				$("#editfolderalert").removeClass("alert");
				$("#editfolderalert").removeClass("alert-danger");
				$("#folderrenamebox").removeClass("has-error");
				$("#editfolderalert").text("");
				$("#editfoldertypelist").html("");
				if (account != null) {
					for (var i = constraintLevel; i < folderTypes.length; i++) {
						$("#editfoldertypelist").append(
							"<li><a onclick='changeEditFolderType("
							+ i + ")'>" + folderTypes[i]
							+ "</a></li>");
					}
				} else {
					$("#editfoldertypelist").append(
						"<li><a onclick='changeEditFolderType(0)'>"
						+ folderTypes[0] + "</a></li>");
				}
			});
	// 响应拖动上传文件
	document.ondragover = function(e) {
		if (e.preventDefault) {
			e.preventDefault();
			e.stopPropagation();
		} else {
			window.event.cancelBubble = true;
			window.event.returnValue = false;
		}
	}
	document.ondrop = function(e) {
		if (e.preventDefault) {
			e.preventDefault();
			e.stopPropagation();
		} else {
			window.event.cancelBubble = true;
			window.event.returnValue = false;
		}
		if (folderView.authList != null) {
			if (checkAuth(folderView.authList, "U")) {// 如果有上传权限且未进行其他上传
				if (isUpLoading || isImporting) {
					kiftdAlert("提示：您正在执行另一项上传任务，请在上传完成后再试。");
				} else {
					if (!(window.ActiveXObject || "ActiveXObject" in window)) {// 判断是否为IE
						var dt;
						if (e.dataTransfer != null) {
							dt = e.dataTransfer; // 获取到拖入上传的文件对象
						} else {
							dt = window.event.dataTransfer;
						}
						var testFile = true;
						if (dt.items !== undefined) {
							for (var i = 0; i < dt.items.length; i++) {
								var item = dt.items[i];
								if (item.kind === "file"
									&& item.webkitGetAsEntry().isFile) {

								} else {
									testFile = false;
								}
							}
						} else {
							for (var i = 0; i < dt.files.length; i++) {
								var dropFile = dt.files[i];
								if (dropFile.type) {

								} else {
									try {
										var fileReader = new FileReader();
										fileReader.readAsDataURL(dropFile
											.slice(0, 10));
										fileReader.addEventListener('load',
											function(e) {

											}, false);
										fileReader.addEventListener('error',
											function(e) {
												testFile = false;
											}, false);
									} catch (e) {
										testFile = false;
									}
								}
							}
						}
						if (testFile) {
							fs = e.dataTransfer.files; // 获取到拖入上传的文件对象
							showUploadFileModel();
							showfilepath();
							checkUploadFile();
						} else {
							kiftdAlert("提示：您拖入的文件中包含了一个或多个文件夹，无法进行上传。");
						}
					} else {
						kiftdAlert("提示：IE浏览器不支持拖拽上传。您可以使用现代浏览器或将浏览模式切换为“极速模式”来体验该功能。");
					}
				}
			} else {
				kiftdAlert("提示：您不具备上传权限，无法上传文件。");
			}
		} else {
			kiftdAlert("提示：您不具备上传权限，无法上传文件。");
		}
	}
	// 各种快捷键绑定
	$(document).keypress(
		function(e) {
			if ($('.modal.shown').length == 0
				|| ($('.modal.shown').length == 1 && $('.modal.shown')
					.attr('id') == 'loadingModal')) {
				var keyCode = e.keyCode ? e.keyCode : e.which ? e.which
					: e.charCode;
				if (isShift(e)
					&& document.activeElement.id != "sreachKeyWordIn") {// 在按住shift的情况下……
					switch (keyCode) {
						case 65:// shift+a 全选
							checkallfile();
							break;
						case 78:// shift+n 新建文件夹
							$('#createFolderButtonLi a').click();
							break;
						case 85:// shift+u 上传文件
							$('#uploadFileButtonLi a').click();
							break;
						case 68:// shift+d 删除
							$('#deleteSeelectFileButtonLi a').click();
							break;
						case 70:// shift+f 上传文件夹
							$('#uploadFolderButtonLi a').click();
							break;
						case 67:// shift+c 复制
							if (checkedMovefiles == undefined
								|| checkedMovefiles.size == 0) {
								$('#copyFileButtonLi a').click();
							}
							break;
						case 88:// shift+x 剪切
							if (checkedMovefiles == undefined
								|| checkedMovefiles.size == 0) {
								$('#cutFileButtonLi a').click();
							}
							break;
						case 86:// shift+v 粘贴
							if (checkedMovefiles !== undefined
								&& checkedMovefiles.size > 0) {
								$('#stickFileButtonLi a').click();
							}
							break;
						default:
							return true;
					}
					return false;
				}
			}
		});
	// 关闭移动提示框自动取消移动
	$('#moveFilesModal').on('hidden.bs.modal', function(e) {
		if (checkedMovefiles == undefined || checkedMovefiles.size == 0) {
			checkedMovefiles = undefined;
			$("#copyFileButtonLi").removeClass("hidden");
			$("#copyFileButtonLi").addClass("show");
			$("#cutFileButtonLi").removeClass("hidden");
			$("#cutFileButtonLi").addClass("show");
			$("#stickFileButtonLi").removeClass("show");
			$("#stickFileButtonLi").addClass("hidden");
			$("#stickFilesCount").text("");
			$('#moveFilesBox').html("");
		}
	});
	$('#downloadModal').on('hidden.bs.modal', function(e) {
		$('#downloadURLCollapse').collapse('hide');
	});
	// 获取窗口高度
	updateWinHeight();
	// 根据屏幕下拉程度自动显示、隐藏“返回顶部”按钮
	$(window).scroll(function() {
		if ($(this).scrollTop() > 2 * winHeight) {
			$('#gobacktotopbox').removeClass("hidden");
		} else {
			$('#gobacktotopbox').addClass("hidden");
		}
	});

	// 打开查看下载链接时，向后台生成/获取下载链接
	$('#downloadURLCollapse').on('shown.bs.collapse', function() {
		getDownloadURL();
	});

	// 开启修改密码模态框时初始化状态
	$('#changePasswordModal')
		.on(
			'show.bs.modal',
			function(e) {
				if (!isChangingPassword) {
					$(
						"#changepassword_oldpwd,#changepassword_newpwd,#changepassword_reqnewpwd,#changePasswordButton,#changepassword_vercode")
						.prop('disabled', false);
					$(
						"#changepassword_oldepwdbox,#changepassword_newpwdbox,#changepassword_reqnewpwdbox")
						.removeClass("has-error");
					$(
						"#changepassword_oldpwd,#changepassword_newpwd,#changepassword_reqnewpwd")
						.val("");
					$(
						"#changepasswordalertbox,#changepassword_vccodebox")
						.hide();
				}
			});
	// 并自动聚焦旧密码输入框
	$('#changePasswordModal').on('shown.bs.modal', function(e) {
		if (!isChangingPassword) {
			$("#changepassword_oldpwd").focus();
		}
	});
	// 开启公告信息模态框前自动判断是否已经勾选“30天不再显示”
	$('#noticeModal').on(
		'show.bs.modal',
		function(e) {
			var cookieMd530 = document.cookie.match(new RegExp(
				"(^| )notice_md5_30=([^;]*)(;|$)"));
			if (cookieMd530) {
				$("#dontShowSomeNoticeAt30Day").prop("checked", true);
			} else {
				$("#dontShowSomeNoticeAt30Day").prop("checked", false);
			}
		});
	// 关闭公告信息模态框后根据是否已经勾选“30天不再显示”设置cookie
	$('#noticeModal').on(
		'hidden.bs.modal',
		function(e) {
			var noticed = new Date();
			if ($("#dontShowSomeNoticeAt30Day").prop("checked")) {
				noticed.setTime(noticed.getTime()
					+ (30 * 24 * 60 * 60 * 1000));
				var cookieMd5 = document.cookie.match(new RegExp(
					"(^| )notice_md5=([^;]*)(;|$)"));
				if (cookieMd5) {
					document.cookie = "notice_md5_30="
						+ escape(unescape(cookieMd5[2])) + ";expires="
						+ noticed.toUTCString();
				} else {
					cookieMd5 = document.cookie.match(new RegExp(
						"(^| )notice_md5_30=([^;]*)(;|$)"));
					if (cookieMd5) {
						document.cookie = "notice_md5_30="
							+ escape(unescape(cookieMd5[2]))
							+ ";expires=" + noticed.toUTCString();
					}
				}
			} else {
				noticed.setTime(0);
				var cookieMd530 = document.cookie.match(new RegExp(
					"(^| )notice_md5_30=([^;]*)(;|$)"));
				if (cookieMd530) {
					document.cookie = "notice_md5_30=0;expires="
						+ noticed.toUTCString();
				}
			}
		});
	// 打开文件夹详情模态框时自动请求文件夹统计数据
	$('#folderInfoModal').on('shown.bs.modal', function(e) {
		// 是否正在执行请求？
		if (folderCountRequest == undefined || folderCountRequest == null) {
			// 该文件夹视图是否是一个搜索视图？
			if (!checkAuth(folderView.authList, "O") && folderView.keyWorld == undefined) {
				// 向服务器发送文件夹内容统计请求
				$("#fim_folderSize").text("计算中...");
				$("#fim_statistics_total").text("（计算中...）");
				$.ajax({
					url: 'homeController/countFolderContent.ajax',
					type: 'POST',
					data: {
						folderId: locationpath
					},
					dataType: 'text',
					success: function(result) {
						if (result == "ERROR") {
							// 如果发生错误，则显示获取失败
							$("#fim_folderSize").text("获取失败");
							$("#fim_statistics_total").text("（获取失败）");
						} else {
							// 获取成功，则解析其中的信息并显示
							var folderCountResult = JSON.parse(result);
							$("#fim_folderSize").text(formatFileSize(folderCountResult.totalSize));
							$("#fim_statistics_total").html("（总计包含" + folderCountResult.folderNum + "个文件夹，" + folderCountResult.fileNum + "个文件）");
						}
						// 最后请求完成，清空请求对象
						folderCountRequest = null;
					},
					error: function() {
						$("#fim_folderSize").text("获取失败");
						$("#fim_statistics_total").text("（获取失败）");
						folderCountRequest = null;
					}
				});
			}
		}
	});
});

// 更新页面高度
function updateWinHeight() {
	if (window.innerHeight) {
		winHeight = window.innerHeight;
	} else if ((document.body) && (document.body.clientHeight)) {
		winHeight = document.body.clientHeight;
	}
}

// 根据屏幕大小增删表格显示内容
function changeFilesTableStyle() {
	var win = $(window).width();
	if (win < 768) {
		$('#filetableheadera').addClass('filetableheaderstyle');
		$('#filetableheadera').attr('data-toggle', 'collapse');
		$('#filetableheadera').attr('data-target', '#filetableoptmenu');
		$('#mdropdownicon').html('（点击展开/折叠菜单）');
	} else {
		$('#filetableheadera').removeClass('filetableheaderstyle');
		$('#filetableheadera').attr('data-toggle', 'modal');
		$('#filetableheadera').attr('data-target', '#folderInfoModal');
		$('#mdropdownicon').html('');
	}
}

// 统一的提示模态框（替代原生 alert，样式与页面现有模态框一致）
function kiftdAlert(msg) {
	$("#kiftdAlertText").text(msg);
	$("#kiftdAlertModal").modal("show");
}

// 全局请求失败提示
function doAlert() {
	kiftdAlert("错误：无法连接到kiftd服务器，请检查您的网络连接或查看服务器运行状态。");
}

// 获取服务器操作系统
function getServerOS() {
	$.ajax({
		type: "POST",
		dataType: "text",
		data: {},
		url: "homeController/getServerOS.ajax",
		success: function(result) {
			if (result == "mustLogin") {
				window.location.href = "prv/login.html";
				return;
			}
			$("#serverOS").text(result);
		},
		error: function() {
			$("#serverOS").html("<a onclick='getServerOS()'>获取失败，点击重试</a>");
		}
	});
}
