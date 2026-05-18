package kohgylw.kiftd.server.controller;

import org.springframework.stereotype.*;
import jakarta.annotation.*;

import kohgylw.kiftd.server.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

import jakarta.servlet.http.*;

/**
 *
 * <h2>主控制器</h2>
 * <p>
 * 该控制器用于处理 kiftd 主页（home.html）的所有 AJAX 请求，包括账户登录/注销、文件夹浏览/操作、
 * 文件上传/下载/操作、视频播放、图片预览等功能的请求分发。每个方法对应一个特定的 RESTful 端点。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 2.0
 */
@Controller
@RequestMapping({ "/homeController" })
public class HomeController {

	private static final String CHARSET_BY_AJAX = "text/html; charset=utf-8";

	@Resource
	private ServerInfoService si;
	@Resource
	private AccountService as;
	@Resource
	private FolderViewService fvs;
	@Resource
	private FolderService fs;
	@Resource
	private FileService fis;
	@Resource
	private PlayVideoService pvs;
	@Resource
	private ShowPictureService sps;
	@Resource
	private FileChainService fcs;

	/**
	 *
	 * <h2>获取服务器操作系统名称</h2>
	 * <p>返回当前 kiftd 服务器所运行的操作系统名称。</p>
	 *
	 * @return String 操作系统名称，例如 "Windows 10" 或 "Linux"
	 */
	@RequestMapping({ "/getServerOS.ajax" })
	@ResponseBody
	public String getServerOS() {
		return this.si.getOSName();
	}

	/**
	 *
	 * <h2>获取 RSA 公钥</h2>
	 * <p>返回用于密码加密传输的 RSA 公钥字符串，登录时前端使用该公钥加密密码。</p>
	 *
	 * @return String RSA 公钥的 Base64 编码字符串
	 */
	@RequestMapping(value = { "/getPublicKey.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getPublicKey() {
		return this.as.getPublicKey();
	}

	/**
	 *
	 * <h2>执行登录</h2>
	 * <p>校验用户提交的登录凭据（包含 RSA 加密后的密码），并在通过后创建会话。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含登录账号和加密密码参数
	 * @param session HttpSession 会话对象，用于存储登录状态
	 * @return String 登录结果，"SUCCESS" 表示成功，其他值表示失败原因
	 */
	@RequestMapping({ "/doLogin.ajax" })
	@ResponseBody
	public String doLogin(final HttpServletRequest request, final HttpSession session) {
		return this.as.checkLoginRequest(request, session);
	}

	/**
	 *
	 * <h2>获取新验证码</h2>
	 * <p>生成一个新的图片验证码并写入响应流，同时将验证码文本存入当前会话以便后续校验。</p>
	 *
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象，用于输出验证码图片流
	 * @param session  HttpSession 会话对象，用于存储验证码文本
	 */
	@RequestMapping({ "/getNewVerCode.do" })
	public void getNewVerCode(final HttpServletRequest request, final HttpServletResponse response,
			final HttpSession session) {
		as.getNewLoginVerCode(request, response, session);
	}

	/**
	 *
	 * <h2>修改密码</h2>
	 * <p>处理用户提交的密码修改请求，包含旧密码验证和新密码设置。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含旧密码、新密码等参数
	 * @return String 修改结果，"SUCCESS" 表示成功，其他值表示失败原因
	 */
	@RequestMapping(value = { "/doChangePassword.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String doChangePassword(final HttpServletRequest request) {
		return as.changePassword(request);
	}

	/**
	 *
	 * <h2>获取文件夹视图</h2>
	 * <p>根据文件夹 ID 返回该文件夹的 JSON 视图，包含其中的子文件夹和文件列表。</p>
	 *
	 * @param fid     String 目标文件夹 ID
	 * @param session HttpSession 当前会话，用于权限校验
	 * @param request HttpServletRequest 请求对象
	 * @return String 文件夹视图的 JSON 字符串
	 */
	@RequestMapping(value = { "/getFolderView.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getFolderView(final String fid, final HttpSession session, final HttpServletRequest request) {
		return fvs.getFolderViewToJson(fid, session, request);
	}

	/**
	 *
	 * <h2>获取剩余文件夹视图</h2>
	 * <p>返回当前文件夹之外的剩余文件夹列表 JSON 视图，用于实现文件夹导航。</p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String 剩余文件夹视图的 JSON 字符串
	 */
	@RequestMapping(value = { "/getRemainingFolderView.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getRemainingFolderView(final HttpServletRequest request) {
		return fvs.getRemainingFolderViewToJson(request);
	}

	/**
	 *
	 * <h2>注销登录</h2>
	 * <p>使当前会话失效，清除登录状态。</p>
	 *
	 * @param session HttpSession 待注销的会话对象
	 * @return String 固定返回 "SUCCESS"
	 */
	@RequestMapping({ "/doLogout.ajax" })
	public @ResponseBody String doLogout(final HttpSession session) {
		this.as.logout(session);
		return "SUCCESS";
	}

	/**
	 *
	 * <h2>创建新文件夹</h2>
	 * <p>在指定父文件夹下创建一个新的子文件夹。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含父文件夹 ID 和新文件夹名称
	 * @return String 创建结果，"SUCCESS" 表示成功
	 */
	@RequestMapping({ "/newFolder.ajax" })
	@ResponseBody
	public String newFolder(final HttpServletRequest request) {
		return this.fs.newFolder(request);
	}

	/**
	 *
	 * <h2>删除文件夹</h2>
	 * <p>删除指定文件夹及其全部内容（递归删除）。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含待删除文件夹 ID
	 * @return String 删除结果，"SUCCESS" 表示成功
	 */
	@RequestMapping({ "/deleteFolder.ajax" })
	@ResponseBody
	public String deleteFolder(final HttpServletRequest request) {
		return this.fs.deleteFolder(request);
	}

	/**
	 *
	 * <h2>重命名文件夹</h2>
	 * <p>将指定文件夹重命名为新名称。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含文件夹 ID 和新名称
	 * @return String 重命名结果，"SUCCESS" 表示成功
	 */
	@RequestMapping({ "/renameFolder.ajax" })
	@ResponseBody
	public String renameFolder(final HttpServletRequest request) {
		return this.fs.renameFolder(request);
	}

	/**
	 *
	 * <h2>上传文件</h2>
	 * <p>处理单文件上传请求，将文件块写入存储区并记录文件信息。</p>
	 *
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象
	 * @param file     MultipartFile 上传的文件块
	 * @return String 上传结果，"SUCCESS" 表示成功
	 */
	@RequestMapping(value = { "/douploadFile.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String douploadFile(final HttpServletRequest request, final HttpServletResponse response,
			final MultipartFile file) {
		return this.fis.doUploadFile(request, response, file);
	}

	/**
	 *
	 * <h2>检查上传文件</h2>
	 * <p>在正式上传前检查文件是否已存在以及是否存在命名冲突。</p>
	 *
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象
	 * @return String 检查结果，包含冲突信息或 "SUCCESS"
	 */
	@RequestMapping(value = { "/checkUploadFile.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String checkUploadFile(final HttpServletRequest request, final HttpServletResponse response) {
		return this.fis.checkUploadFile(request, response);
	}

	/**
	 *
	 * <h2>上传文件夹前置检查</h2>
	 * <p>检查待上传的文件夹是否存在同名冲突，返回检查结果供前端决策。</p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String 检查结果，包含是否存在同名文件夹等信息
	 */
	@RequestMapping(value = { "/checkImportFolder.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String checkImportFolder(final HttpServletRequest request) {
		return this.fis.checkImportFolder(request);
	}

	/**
	 *
	 * <h2>执行文件夹上传</h2>
	 * <p>将前端提交的文件夹结构及文件写入存储区。</p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @param file    MultipartFile 文件夹对应的文件数据
	 * @return String 上传结果，"SUCCESS" 表示成功
	 */
	@RequestMapping(value = { "/doImportFolder.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String doImportFolder(final HttpServletRequest request, final MultipartFile file) {
		return fis.doImportFolder(request, file);
	}

	/**
	 *
	 * <h2>按名称删除文件夹（文件夹覆盖场景）</h2>
	 * <p>上传文件夹时若存在同名文件夹且用户选择覆盖，则先执行此方法删除原同名文件夹。</p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String 删除结果
	 */
	@RequestMapping(value = { "/deleteFolderByName.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String deleteFolderByName(final HttpServletRequest request) {
		return fs.deleteFolderByName(request);
	}

	/**
	 *
	 * <h2>按名称创建新文件夹（保留两者场景）</h2>
	 * <p>上传文件夹时若存在同名文件夹且用户选择保留两者，则创建一个新的文件夹名称用于上传。</p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String 包含新文件夹名称的结果
	 */
	@RequestMapping(value = { "/createNewFolderByName.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String createNewFolderByName(final HttpServletRequest request) {
		return fs.createNewFolderByName(request);
	}

	/**
	 *
	 * <h2>删除文件</h2>
	 * <p>删除指定 ID 的文件记录及其存储块。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含文件 ID
	 * @return String 删除结果，"SUCCESS" 表示成功
	 */
	@RequestMapping({ "/deleteFile.ajax" })
	@ResponseBody
	public String deleteFile(final HttpServletRequest request) {
		return this.fis.deleteFile(request);
	}

	/**
	 *
	 * <h2>下载文件</h2>
	 * <p>将指定文件的数据流写入响应，实现文件下载功能。</p>
	 *
	 * @param request  HttpServletRequest 请求对象，包含文件 ID
	 * @param response HttpServletResponse 响应对象，用于输出文件流
	 */
	@RequestMapping({ "/downloadFile.do" })
	public void downloadFile(final HttpServletRequest request, final HttpServletResponse response) {
		this.fis.doDownloadFile(request, response);
	}

	/**
	 *
	 * <h2>重命名文件</h2>
	 * <p>将指定文件重命名为新名称。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含文件 ID 和新名称
	 * @return String 重命名结果，"SUCCESS" 表示成功
	 */
	@RequestMapping({ "/renameFile.ajax" })
	@ResponseBody
	public String renameFile(final HttpServletRequest request) {
		return this.fis.doRenameFile(request);
	}

	/**
	 *
	 * <h2>获取视频播放信息</h2>
	 * <p>返回指定视频文件的播放信息 JSON（包含文件名、大小、是否需要转码等）。</p>
	 *
	 * @param request  HttpServletRequest 请求对象，包含视频文件 ID
	 * @param response HttpServletResponse 响应对象
	 * @return String 视频播放信息的 JSON 字符串
	 */
	@RequestMapping(value = { "/playVideo.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String playVideo(final HttpServletRequest request, final HttpServletResponse response) {
		return this.pvs.getPlayVideoJson(request);
	}

	/**
	 *
	 * <h2>预览图片请求</h2>
	 * <p>
	 * 该方法用于处理预览图片请求。配合 Viewer.js 插件，返回指定格式的 JSON 数据供前端图片查看器使用。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象，包含目标文件 ID
	 * @return String 预览图片的 JSON 信息
	 */
	@RequestMapping(value = { "/getPrePicture.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getPrePicture(final HttpServletRequest request) {
		return this.sps.getPreviewPictureJson(request);
	}

	/**
	 *
	 * <h2>获取压缩的预览图片</h2>
	 * <p>
	 * 该方法用于预览较大图片时获取其压缩版本以加快预览速度，压缩等级根据图片原始尺寸自动决定。
	 * </p>
	 *
	 * @param request  HttpServletRequest 请求对象，其中应包含 fileId 指定预览图片的文件块 ID
	 * @param response HttpServletResponse 响应对象，用于写出压缩后的图片数据流
	 */
	@RequestMapping({ "/showCondensedPicture.do" })
	public void showCondensedPicture(final HttpServletRequest request, final HttpServletResponse response) {
		sps.getCondensedPicture(request, response);
	}

	/**
	 *
	 * <h2>批量删除文件</h2>
	 * <p>一次性删除多个指定的文件。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含待删除文件的 ID 列表
	 * @return String 删除结果
	 */
	@RequestMapping({ "/deleteCheckedFiles.ajax" })
	@ResponseBody
	public String deleteCheckedFiles(final HttpServletRequest request) {
		return this.fis.deleteCheckedFiles(request);
	}

	/**
	 *
	 * <h2>获取打包剩余时间</h2>
	 * <p>返回当前正在进行的打包操作的预计剩余时间（秒），用于前端显示进度。</p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String 剩余时间的描述字符串
	 */
	@RequestMapping({ "/getPackTime.ajax" })
	@ResponseBody
	public String getPackTime(final HttpServletRequest request) {
		return this.fis.getPackTime(request);
	}

	/**
	 *
	 * <h2>启动多文件打包下载</h2>
	 * <p>将多个选中文件加入打包队列，返回打包就绪状态。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含待下载文件的 ID 列表
	 * @return String 打包准备结果
	 */
	@RequestMapping({ "/downloadCheckedFiles.ajax" })
	@ResponseBody
	public String downloadCheckedFiles(final HttpServletRequest request) {
		return this.fis.downloadCheckedFiles(request);
	}

	/**
	 *
	 * <h2>下载打包后的 ZIP 文件</h2>
	 * <p>将之前打包好的压缩文件写入响应流供用户下载。</p>
	 *
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象，用于输出 ZIP 文件流
	 * @throws Exception 写入过程中可能发生的 I/O 异常
	 */
	@RequestMapping({ "/downloadCheckedFilesZip.do" })
	public void downloadCheckedFilesZip(final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {
		this.fis.downloadCheckedFilesZip(request, response);
	}

	/**
	 *
	 * <h2>移动文件操作前置确认</h2>
	 * <p>
	 * 该逻辑用于在执行移动或复制前确认目标文件夹是否合法以及是否会产生文件名冲突。
	 * </p>
	 *
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @return java.lang.String 判断结果，详情请见具体实现
	 */
	@RequestMapping(value = { "/confirmMoveFiles.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String confirmMoveFiles(final HttpServletRequest request) {
		return fis.confirmMoveFiles(request);
	}

	/**
	 *
	 * <h2>执行移动文件操作</h2>
	 * <p>
	 * 该逻辑用于正式执行移动或复制操作，在调用之前应先执行判断操作。
	 * </p>
	 *
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @return java.lang.String 执行结果，详情请见具体实现
	 */
	@RequestMapping({ "/moveCheckedFiles.ajax" })
	@ResponseBody
	public String moveCheckedFiles(final HttpServletRequest request) {
		return fis.doMoveFiles(request);
	}

	/**
	 *
	 * <h2>执行全局查询</h2>
	 * <p>
	 * 该逻辑用于进行全局搜索，将会迭代搜索目标文件夹及其全部子文件夹以查找符合关键字的结果，并返回单独的搜索结果视图。
	 * </p>
	 *
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @return java.lang.String 搜索结果，详情请见具体实现
	 */
	@RequestMapping(value = { "/sreachInCompletePath.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String sreachInCompletePath(final HttpServletRequest request) {
		return fvs.getSreachViewToJson(request);
	}

	/**
	 *
	 * <h2>应答机制</h2>
	 * <p>
	 * 该机制旨在防止某些长耗时操作可能导致 Session 失效的问题（例如上传、视频播放等），方便用户持续操作。
	 * </p>
	 *
	 * @return String 固定返回 "pong" 或空字符串
	 */
	@RequestMapping(value = { "/ping.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String pong(final HttpServletRequest request) {
		return as.doPong(request);
	}

	/**
	 *
	 * <h2>查询是否允许自由注册</h2>
	 * <p>询问服务器是否开启了自由注册新账户功能。</p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String "true" 表示允许注册，"false" 表示不允许
	 */
	@RequestMapping(value = { "/askForAllowSignUpOrNot.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String askForAllowSignUpOrNot(final HttpServletRequest request) {
		return as.isAllowSignUp();
	}

	/**
	 *
	 * <h2>处理注册新账户请求</h2>
	 * <p>处理用户提交的新账户注册请求，创建新账户。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含注册所需的账户信息
	 * @return String 注册结果，"SUCCESS" 表示成功
	 */
	@RequestMapping(value = { "/doSigUp.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String doSigUp(final HttpServletRequest request) {
		return as.doSignUp(request);
	}

	/**
	 *
	 * <h2>获取永久资源链接密钥</h2>
	 * <p>获取指定文件的外部资源链接校验密钥（ckey），用于生成永久资源分享链接。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含文件 ID
	 * @return String 该文件的外部链接校验密钥
	 */
	@RequestMapping(value = { "/getFileChainKey.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String getFileChainKey(final HttpServletRequest request) {
		return fcs.getChainKeyByFid(request);
	}

	/**
	 *
	 * <h2>统计文件夹内容</h2>
	 * <p>对指定文件夹的内容进行统计，返回子文件夹数量和文件数量等信息。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含目标文件夹 ID
	 * @return String 统计结果的 JSON 字符串
	 */
	@RequestMapping(value = { "/countFolderContent.ajax" }, produces = { CHARSET_BY_AJAX })
	@ResponseBody
	public String countFolderContent(final HttpServletRequest request) {
		return fs.getFolderCountResult(request);
	}
}
