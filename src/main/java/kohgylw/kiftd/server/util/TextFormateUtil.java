package kohgylw.kiftd.server.util;

import java.util.regex.*;

/**
 *
 * <h2>文本格式校验工具类</h2>
 * <p>
 * 该工具类提供了文件夹名称和文件名的格式校验功能，
 * 以及字符串中是否包含转义符的检测功能。
 * 用于确保用户输入的文件/文件夹名称符合文件系统规范，
 * 避免包含非法字符（如 |、\\、/、*、<、>、\"、?、&、$、: 等）。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class TextFormateUtil
{
    private static TextFormateUtil tfu;
    private static final Pattern NAME_PATTERN = Pattern.compile("[|\\/*<>\"?&$:]+");
    
    public static TextFormateUtil instance() {
        return TextFormateUtil.tfu;
    }
    
    public boolean matcherFolderName(final String folderName) {
        final Matcher m = NAME_PATTERN.matcher(folderName);
        return !m.find();
    }
    
    public boolean matcherFileName(final String fileName) {
        final Matcher m = NAME_PATTERN.matcher(fileName);
        return !m.find();
    }
    
    /**
	 * 
	 * <h2>判断字符串中是否含有转义符</h2>
	 * <p>
	 * 该方法主要用于避免Gson在处理含有正斜杠的字符串时会出现“重复转义”的问题。
	 * 举例来说，如果转义字符串"{foo:\"\\bar\"}"，正常情况下，应解析出foo字段的值为“\bar”，
	 * 但实际上Gson会将字符串中的正斜杠也视为转义，结果就会导致解析异常。解决方法很简单，先用这个
	 * 方法判断一下字符串中是否含有转义符，然后再决定是解析即可。
	 * </p>
	 * @author 青阳龙野(kohgylw)
	 * @param in java.lang.String 输入字符串
	 * @return boolean 判断结果，若包含转义符正斜杠则返回true
	 */
	public boolean hasEscapes(String in) {
		return in.indexOf("\\") >= 0;
	}
    
    static {
        TextFormateUtil.tfu = new TextFormateUtil();
    }
}
