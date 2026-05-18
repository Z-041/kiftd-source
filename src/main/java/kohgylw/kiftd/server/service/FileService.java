package kohgylw.kiftd.server.service;

import org.springframework.web.multipart.*;
import jakarta.servlet.http.*;

/**
 *
 * <h2>文件服务接口</h2>
 * <p>
 * 该接口定义了与文件相关的所有业务操作，包括文件上传下载、重命名删除、
 * 批量打包下载、文件移动与复制等功能。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface FileService {

	/**
	 *
	 * <h2>检查上传文件</h2>
	 * <p>在正式上传前检查文件是否存在命名冲突，以及是否满足上传条件。</p>
	 *
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象
	 * @return String 检查结果，包含冲突信息或 "SUCCESS"
	 */
	String checkUploadFile(final HttpServletRequest request, final HttpServletResponse response);

	/**
	 *
	 * <h2>执行文件上传</h2>
	 * <p>将上传的文件块写入存储区，创建文件记录。</p>
	 *
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象
	 * @param file     MultipartFile 上传的文件数据
	 * @return String 上传结果，"SUCCESS" 表示成功
	 */
	String doUploadFile(final HttpServletRequest request, final HttpServletResponse response,
			final MultipartFile file);

	/**
	 *
	 * <h2>删除文件</h2>
	 * <p>删除指定的文件记录及其对应的存储块数据。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含文件 ID
	 * @return String 删除结果，"SUCCESS" 表示成功
	 */
	String deleteFile(final HttpServletRequest request);

	/**
	 *
	 * <h2>下载文件</h2>
	 * <p>将指定文件的数据流写入响应，支持断点续传。</p>
	 *
	 * @param request  HttpServletRequest 请求对象，包含文件 ID
	 * @param response HttpServletResponse 响应对象，用于输出文件流
	 */
	void doDownloadFile(final HttpServletRequest request, final HttpServletResponse response);

	/**
	 *
	 * <h2>重命名文件</h2>
	 * <p>将指定文件重命名为新名称。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含文件 ID 和新名称
	 * @return String 重命名结果，"SUCCESS" 表示成功
	 */
	String doRenameFile(final HttpServletRequest request);

	/**
	 *
	 * <h2>批量删除文件</h2>
	 * <p>一次性删除多个指定文件。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含待删除文件的 ID 列表
	 * @return String 批量删除结果
	 */
	String deleteCheckedFiles(final HttpServletRequest request);

	/**
	 *
	 * <h2>获取打包剩余时间</h2>
	 * <p>返回当前打包操作的预计剩余时间（秒），用于前端进度显示。</p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String 剩余时间的描述字符串
	 */
	String getPackTime(final HttpServletRequest request);

	/**
	 *
	 * <h2>启动多文件打包下载</h2>
	 * <p>将多个选中的文件加入打包队列，准备压缩打包。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含待打包文件的 ID 列表
	 * @return String 打包准备结果
	 */
	String downloadCheckedFiles(final HttpServletRequest request);

	/**
	 *
	 * <h2>下载打包后的 ZIP 文件</h2>
	 * <p>将之前打包好的压缩文件写入响应流供用户下载。</p>
	 *
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象，用于输出 ZIP 文件流
	 * @throws Exception 写入过程中可能发生的 I/O 异常
	 */
	void downloadCheckedFilesZip(final HttpServletRequest request, final HttpServletResponse response)
			throws Exception;

	/**
	 *
	 * <h2>移动文件前置判断</h2>
	 * <p>
	 * 验证将要执行的移动（或复制）操作是否合法，应在正式执行前调用。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象，应包含：
	 *        <ul>
	 *        <li>strIdList - 涉及的文件 ID 数组（JSON 格式）</li>
	 *        <li>strFidList - 涉及的文件夹 ID 数组（JSON 格式）</li>
	 *        <li>locationpath - 目标文件夹 ID</li>
	 *        <li>method - "COPY" 表示复制模式，否则为移动模式</li>
	 *        </ul>
	 * @return String 判断结果：
	 *         <ul>
	 *         <li>confirmMoveFiles - 允许进行移动操作</li>
	 *         <li>duplicationFileName:{JSON} - 允许移动但存在文件名冲突</li>
	 *         <li>noAuthorized - 无操作权限</li>
	 *         <li>errorParameter - 传入参数有误</li>
	 *         <li>filesTotalOutOfLimit / foldersTotalOutOfLimit - 容量超限</li>
	 *         <li>CANT_MOVE_TO_INSIDE:{文件夹名} - 不能移到自己内部</li>
	 *         </ul>
	 */
	String confirmMoveFiles(final HttpServletRequest request);

	/**
	 *
	 * <h2>执行移动或复制文件操作</h2>
	 * <p>正式执行文件或文件夹的移动（或复制）操作，调用前应先进行前置判断。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含操作参数及冲突处理策略
	 * @return String 执行结果：
	 *         <ul>
	 *         <li>moveFilesSuccess - 执行成功</li>
	 *         <li>noAuthorized - 无操作权限</li>
	 *         <li>errorParameter - 传入参数有误</li>
	 *         <li>filesTotalOutOfLimit / foldersTotalOutOfLimit - 容量超限</li>
	 *         <li>cannotMoveFiles - 操作失败</li>
	 *         </ul>
	 */
	String doMoveFiles(final HttpServletRequest request);

	/**
	 *
	 * <h2>上传文件夹前置检查</h2>
	 * <p>
	 * 验证上传文件夹的合法性，包括权限、是否重名、文件是否超限等。
	 * 根据检查结果告知前端应进行的下一步操作。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String 检查结果的 JSON 对象，包含：
	 *         <ul>
	 *         <li>noAuthorized - 权限不合法</li>
	 *         <li>errorParameter - 参数不合法</li>
	 *         <li>fileOverSize - 文件体积超限</li>
	 *         <li>coverOrBoth - 存在同名文件夹，可选择"覆盖"或"保留两者"</li>
	 *         <li>onlyBoth - 存在同名文件夹，仅能"保留两者"</li>
	 *         <li>permitUpload - 允许直接上传</li>
	 *         </ul>
	 */
	String checkImportFolder(final HttpServletRequest request);

	/**
	 *
	 * <h2>执行文件夹上传</h2>
	 * <p>将前端提交的文件夹数据写入存储区，创建对应的文件夹结构和文件记录。</p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @param file    MultipartFile 上传的文件夹文件数据
	 * @return String 上传结果
	 */
	String doImportFolder(final HttpServletRequest request, final MultipartFile file);
}