package kohgylw.kiftd.server.pojo;

/**
 *
 * <h2>搜索视图封装类</h2>
 * <p>
 * 该类继承自 FolderView，用于封装文件搜索结果的视图数据。
 * 包含搜索关键词（keyWorld）字段，用于在前端展示搜索条件和结果。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class SearchView extends FolderView {
	
	private String keyWorld;

	public String getKeyWorld() {
		return keyWorld;
	}

	public void setKeyWorld(String keyWorld) {
		this.keyWorld = keyWorld;
	}
	
}
