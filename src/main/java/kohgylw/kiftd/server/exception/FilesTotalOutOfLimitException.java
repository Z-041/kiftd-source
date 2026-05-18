package kohgylw.kiftd.server.exception;

/**
 *
 * <h2>文件数量超出限制异常</h2>
 * <p>
 * 当在单个文件夹下上传的文件数量超过系统配置的最大限制时抛出此异常。
 * 用于防止用户在单个文件夹下上传过多文件，保障文件系统的性能和安全。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FilesTotalOutOfLimitException extends Exception{

	private static final long serialVersionUID = -7163748586450753154L;

}
