function getFileChain(fileId, fileName) {
	$("#fileChainTextarea").text("正在获取……");
	$("#copyChainBtn").prop('disabled', true);
	$('#fileChainModal').modal('show');
	$.ajax({
		type: "POST",
		dataType: "text",
		url: "homeController/getFileChainKey.ajax",
		data: {
			fid: fileId
		},
		success: function(result) {
			switch (result) {
				case "ERROR":
					$("#fileChainTextarea").text("提示：获取失败，请刷新页面或稍后再试。");
					break;
				case "mustlogin":
					window.location.href = "prv/login.html";
					break;
				default:
					$("#fileChainTextarea").text(
						window.location.protocol
						+ "//"
						+ window.location.host
						+ "/externalLinksController/chain/"
						+ encodeURIComponent(fileName.replace(/\\/g,
							"_")) + "?ckey="
						+ encodeURIComponent(result));
					$("#copyChainBtn").prop('disabled', false);
					break;
			}
		},
		error: function() {
			$("#fileChainTextarea").text("提示：获取失败，无法连接服务器。");
		}
	});
}

// 复制链接内容
function copyFileChain() {
	let node = document.getElementById('fileChainTextarea');
	node.select();
	if (navigator.clipboard) {
		navigator.clipboard.writeText(node.value).catch(function() {});
	}
}

// 显示公告模态框
function showNoticeModal() {
	$('#noticeModal').modal('show');
}

// 加载公告内容并初始化公告模态框
function initNoticeModal() {
	$("#noticeModalBody").load(
		"resourceController/getNoticeContext.do?dms=" + Date.now(),
		function() {
			$('#noticeModalBody img').css("max-width", "100%");
			if (winHeight >= 300) {
				$('#noticeModalBody').css("max-height",
					(winHeight - 180) + "px");
			} else {
				$('#noticeModalBody').css("max-height", "300px");
			}
			noticeInited = true;
			showNoticeModal();
			showNoticeBtn();
		});
}

// 打开主页时自动订阅未阅读过的公告信息并显示，如果该公告已经阅读过则不会显示。
function subscribeNotice() {
	$.ajax({
		url: 'resourceController/getNoticeMD5.ajax',
		data: {},
		type: 'POST',
		dataType: 'text',
		success: function(result) {
			if (result != "") {
				var cookieMd5 = document.cookie.match(new RegExp(
					"(^| )notice_md5=([^;]*)(;|$)"));
				if (cookieMd5) {
					if (result == unescape(cookieMd5[2])) {
						showNoticeBtn();
						return;
					}
				} else {
					cookieMd5 = document.cookie.match(new RegExp(
						"(^| )notice_md5_30=([^;]*)(;|$)"));
					if (cookieMd5) {
						if (result == unescape(cookieMd5[2])) {
							showNoticeBtn();
							return;
						}
					}
				}
				initNoticeModal();
				document.cookie = "notice_md5=" + escape(result);
			}
		},
		error: function() {
			kiftdAlert("错误：无法从服务器获取公告信息，请尝试刷新页面。");
		}
	});
}

// 显示“公告”浮动按钮，方便用户手动打开公告
function showNoticeBtn() {
	$("#shownoticebox").removeClass("hidden");
	$("#shownoticebox").addClass("show");
}

// 手动显示公告
function showNotice() {
	if (noticeInited) {
		showNoticeModal();
	} else {
		initNoticeModal();
	}
}
