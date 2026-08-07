package kohgylw.kiftd.newcore.service;

import jakarta.servlet.http.HttpServletRequest;

public interface FolderService {

	String newFolder(HttpServletRequest request);

	String deleteFolder(HttpServletRequest request);

	String renameFolder(HttpServletRequest request);

	String deleteFolderByName(HttpServletRequest request);

	String createNewFolderByName(HttpServletRequest request);

	String getFolderCountResult(HttpServletRequest request);

	/**
	 * 
	 * <h2>删除指定文件夹及其全部子树内容（共享编排入口）</h2>
	 * <p>
	 * 执行完整的权限校验、数据库删除与日志记录。供 deleteFolder、deleteFolderByName
	 * 以及批量删除等场景复用，避免各处重复实现同一删除编排。
	 * </p>
	 * 
	 * @param folderId 待删除文件夹ID，为null、空串或"root"时视为参数非法
	 * @param account  当前账户（可为null，表示匿名访问）
	 * @param request  请求对象（用于记录删除日志）
	 * @return int 返回 0 表示删除成功或文件夹不存在（幂等成功）；1 表示无权限；
	 *         2 表示删除失败（参数非法或数据库删除未生效）
	 */
	int deleteFolderChecked(String folderId, String account, HttpServletRequest request);
}
