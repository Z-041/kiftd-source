package kohgylw.kiftd.server.enumeration;

/**
 *
 * <h2>账户权限枚举</h2>
 * <p>
 * 该枚举定义了kiftd系统中账户可以拥有的操作权限类型：
 * CREATE_NEW_FOLDER（创建新文件夹）、
 * UPLOAD_FILES（上传文件）、
 * DELETE_FILE_OR_FOLDER（删除文件或文件夹）、
 * RENAME_FILE_OR_FOLDER（重命名文件或文件夹）、
 * DOWNLOAD_FILES（下载文件）、
 * MOVE_FILES（移动文件）。
 * 每种权限可单独赋予或收回，用于实现精细化的访问控制。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public enum AccountAuth
{
    CREATE_NEW_FOLDER, 
    UPLOAD_FILES, 
    DELETE_FILE_OR_FOLDER, 
    RENAME_FILE_OR_FOLDER, 
    DOWNLOAD_FILES,
	MOVE_FILES;
}
