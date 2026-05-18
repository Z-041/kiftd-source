package kohgylw.kiftd.server.service;

import jakarta.servlet.http.*;

/**
 * 
 * <h2>文件夹视图服务接口</h2>
 * <p>
 * 文件夹视图是用于页面上显示的一个文件夹信息封装，包含内容列表、可用权限和状态信息。
 * 该服务层接口用于查询、拼装、处理文件夹视图的相关请求，是 kiftd 核心功能的主要组成之一。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface FolderViewService {

	/**
	 * 
	 * <h2>根据主键获取文件夹视图</h2>
	 * <p>根据文件夹 ID 返回该文件夹的完整视图，封装为 JSON 格式供前端页面显示。</p>
	 *
	 * @param fid     String 目标文件夹 ID
	 * @param session HttpSession 当前用户会话，用于权限判断
	 * @param request HttpServletRequest 请求对象
	 * @return String 文件夹视图的 JSON 字符串，格式参考 FolderView POJO 类
	 */
	String getFolderViewToJson(final String fid, final HttpSession session, final HttpServletRequest request);

	/**
	 * 
	 * <h2>全路径搜索查询</h2>
	 * <p>
	 * 根据文件夹 ID 和关键字生成全路径搜索结果视图，结构类似于普通文件夹视图，
	 * 但额外包含搜索结果相关的属性。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象，包含 fid（文件夹 ID）和 keyword（搜索关键字）
	 * @return String 搜索结果视图的 JSON 字符串，格式参考 SreachView POJO 类
	 */
	String getSreachViewToJson(final HttpServletRequest request);

	/**
	 * 
	 * <h2>获取文件夹的后续视图</h2>
	 * <p>
	 * 获取指定文件夹下的分段文件数据列表，确保页面能加载完整的文件夹数据（如大量文件分批加载）。
	 * 可视作 getFolderViewToJson 方法的后续补充操作。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String 后续文件夹视图的 JSON 字符串，格式参考 RemainingFolderView POJO 类
	 */
	String getRemainingFolderViewToJson(final HttpServletRequest request);
}