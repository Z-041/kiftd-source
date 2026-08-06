package kohgylw.kiftd.newcore.domain;

/**
 *
 * <h2>AJAX 通道协议码常量</h2>
 * <p>
 * 该常量类集中定义旧式 AJAX 通道（"/homeController/*.ajax"）与前端约定的响应协议码。
 * 此前这些协议码以字符串字面量散落在各 Service 实现中，容易因拼写错误导致前端分支失效；
 * 统一收敛后，服务层与控制器只需引用常量即可，保证线上协议稳定且单一来源可维护。
 * 注意：各常量的字符串值属于前后端线上契约，修改前必须同步调整前端。
 * </p>
 *
 * @author 技术债治理迭代
 * @version 1.0
 */
public final class AjaxProtocol {

	// ---- 通用协议码 ----
	/** 操作成功 */
	public static final String SUCCESS = "SUCCESS";
	/** 通用失败 */
	public static final String ERROR = "ERROR";
	/** 资源不存在 */
	public static final String NOT_FOUND = "NOT_FOUND";
	/** 无访问权限 */
	public static final String NOT_ACCESS = "notAccess";
	/** 未授权（需登录） */
	public static final String NO_AUTHORIZED = "noAuthorized";
	/** 参数错误 */
	public static final String ERROR_PARAMETER = "errorParameter";

	// ---- 文件夹相关协议码 ----
	/** 文件夹数量超出单目录上限 */
	public static final String FOLDERS_TOTAL_OUT_OF_LIMIT = "foldersTotalOutOfLimit";
	/** 名称已被占用 */
	public static final String NAME_OCCUPIED = "nameOccupied";
	/** 创建文件夹成功 */
	public static final String CREATE_FOLDER_SUCCESS = "createFolderSuccess";
	/** 无法创建文件夹 */
	public static final String CANNOT_CREATE_FOLDER = "cannotCreateFolder";
	/** 删除文件夹成功 */
	public static final String DELETE_FOLDER_SUCCESS = "deleteFolderSuccess";
	/** 无法删除文件夹 */
	public static final String CANNOT_DELETE_FOLDER = "cannotDeleteFolder";
	/** 重命名文件夹成功 */
	public static final String RENAME_FOLDER_SUCCESS = "renameFolderSuccess";
	/** 删除出错（文件夹非空等） */
	public static final String DELETE_ERROR = "deleteError";
	/** 删除成功（通用） */
	public static final String DELETE_SUCCESS = "deleteSuccess";

	// ---- 文件相关协议码 ----
	/** 文件数量超出单目录上限 */
	public static final String FILES_TOTAL_OUT_OF_LIMIT = "filesTotalOutOfLimit";
	/** 删除文件成功 */
	public static final String DELETE_FILE_SUCCESS = "deleteFileSuccess";
	/** 无法删除文件 */
	public static final String CANNOT_DELETE_FILE = "cannotDeleteFile";
	/** 重命名文件成功 */
	public static final String RENAME_FILE_SUCCESS = "renameFileSuccess";
	/** 无法移动文件（含目标重名/越权等） */
	public static final String CANNOT_MOVE_FILES = "cannotMoveFiles";

	private AjaxProtocol() {
	}
}
