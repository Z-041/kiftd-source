package kohgylw.kiftd.util.file_system_manager.pojo;

/**
 *
 * <h2>文件系统管理器文件夹树节点</h2>
 * <p>
 * 该类继承自 server.model.Folder，用于在文件管理器的UI树形菜单中显示文件夹节点。
 * 重写了toString()方法，返回文件夹名称作为节点的显示文本。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FolderTreeNode extends kohgylw.kiftd.server.model.Folder{
	
	@Override
	public String toString() {
		return getFolderName();
	}

}
