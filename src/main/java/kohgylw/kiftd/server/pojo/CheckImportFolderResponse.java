package kohgylw.kiftd.server.pojo;

/**
 *
 * <h2>导入文件夹检查响应封装类</h2>
 * <p>
 * 该类用于封装检查导入文件夹操作的响应结果，包含操作结果标识和最大文件大小限制，
 * 供前端判断导入操作是否可行并获取相应限制信息。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class CheckImportFolderResponse {
	
	private String result;
	private String maxSize;
	
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public String getMaxSize() {
		return maxSize;
	}
	public void setMaxSize(String maxSize) {
		this.maxSize = maxSize;
	}
	
	

}
