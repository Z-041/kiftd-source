package kohgylw.kiftd.server.pojo;

import java.io.File;

/**
 *
 * <h2>扩展存储路径封装类</h2>
 * <p>
 * 该类用于封装扩展存储路径的配置信息，包括存储索引号和对应的文件路径。
 * 用于支持将文件存储到多个不同的磁盘或目录中，实现分布式存储。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class ExtendStores {
	
	private short index;
	private File path;
	
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
