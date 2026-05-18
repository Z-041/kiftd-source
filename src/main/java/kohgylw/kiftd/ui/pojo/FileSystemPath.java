package kohgylw.kiftd.ui.pojo;

import java.io.File;

/**
 *
 * <h2>文件系统路径封装类</h2>
 * <p>
 * 该类用于封装文件系统存储路径的信息，包括路径类型（主文件系统/扩展存储区）、
 * 对应的File对象和索引编号。用于在设置窗口中展示和选择文件存储位置。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FileSystemPath {
	
	public static final String MAIN_FILE_SYSTEM_NAME="主文件系统";
	public static final String EXTEND_STORES_NAME="扩展存储区";
	
	private String type;
	private File path;
	private short index;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public short getIndex() {
		return index;
	}
	public void setIndex(short index) {
		this.index = index;
	}
	public File getPath() {
		return path;
	}
	public void setPath(File path) {
		this.path = path;
	}
	
}
