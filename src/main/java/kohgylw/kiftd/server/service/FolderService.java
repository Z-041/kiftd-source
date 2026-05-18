package kohgylw.kiftd.server.service;

import jakarta.servlet.http.*;

/**
 *
 * <h2>文件夹服务接口</h2>
 * <p>
 * 该接口定义了与文件夹相关的业务操作，包括创建、删除、重命名文件夹，
 * 以及文件夹上传时的冲突处理和内容统计等功能。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface FolderService {

	/**
	 *
	 * <h2>创建新文件夹</h2>
	 * <p>在指定父文件夹下创建一个新的子文件夹。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含父文件夹 ID 和新文件夹名称
	 * @return String 创建结果，"SUCCESS" 表示成功
	 */
	String newFolder(final HttpServletRequest request);

	/**
	 *
	 * <h2>删除文件夹</h2>
	 * <p>删除指定文件夹及其全部内容（递归删除）。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含待删除文件夹 ID
	 * @return String 删除结果，"SUCCESS" 表示成功
	 */
	String deleteFolder(final HttpServletRequest request);

	/**
	 *
	 * <h2>重命名文件夹</h2>
	 * <p>将指定文件夹重命名为新名称。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含文件夹 ID 和新名称
	 * @return String 重命名结果，"SUCCESS" 表示成功
	 */
	String renameFolder(final HttpServletRequest request);

	/**
	 *
	 * <h2>按名称删除文件夹（覆盖场景）</h2>
	 * <p>
	 * 上传文件夹时若存在同名文件夹且用户选择覆盖，则先删除原同名文件夹以便后续上传。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String 删除结果
	 */
	String deleteFolderByName(final HttpServletRequest request);

	/**
	 *
	 * <h2>创建新名称文件夹（保留两者场景）</h2>
	 * <p>
	 * 上传文件夹时若存在同名文件夹且用户选择保留两者，则创建一个带编号的新文件夹名称。
	 * 返回结果包含 result（success/error）和 newName（新建文件夹名称）两个属性。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String 包含 result 和 newName 的 JSON 结果
	 */
	String createNewFolderByName(final HttpServletRequest request);

	/**
	 * 
	 * <h2>统计文件夹内容</h2>
	 * <p>统计指定文件夹内的子文件夹数量和文件数量，以 JSON 格式返回结果。</p>
	 *
	 * @param request HttpServletRequest  请求对象，包含目标文件夹ID
	 * @return String 统计信息的 JSON 字符串，请求不合法则返回 "ERROR"
	 */
	String getFolderCountResult(final HttpServletRequest request);
}