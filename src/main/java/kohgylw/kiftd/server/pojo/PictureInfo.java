package kohgylw.kiftd.server.pojo;

/**
 *
 * <h2>图片信息封装类</h2>
 * <p>
 * 该类用于封装单张图片的信息，包括文件名和访问URL，
 * 供图片预览功能使用，用于在前端展示图片列表。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class PictureInfo {
	
	private String fileName;
	private String url;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

}
