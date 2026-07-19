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
public final class TextFormateUtil {
    private static final TextFormateUtil INSTANCE = new TextFormateUtil();
    // 非法字符：控制字符以及 Windows/Unix 路径与 shell 危险字符
    private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[\\x00-\\x1f\\x7f|\\\\/*<>\"?&$:]+");
    // Windows 保留设备名（不区分大小写），后接点号或字符串结尾均视为保留名
    private static final Pattern RESERVED_NAME_PATTERN = Pattern
            .compile("(?i)^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\.|$)");
    private static final int MAX_NAME_LENGTH = 255;

    private TextFormateUtil() {
    }

    public static TextFormateUtil instance() {
        return INSTANCE;
    }

    public boolean matcherFolderName(final String folderName) {
        return isValidName(folderName);
    }

    public boolean matcherFileName(final String fileName) {
        return isValidName(fileName);
    }

    private boolean isValidName(final String name) {
        if (name == null) {
            return false;
        }
        final int len = name.length();
        if (len == 0 || len > MAX_NAME_LENGTH) {
            return false;
        }
        if (ILLEGAL_CHAR_PATTERN.matcher(name).find()) {
            return false;
        }
        // 首尾空格或点号在 Windows 与部分文件系统会导致异常或安全问题
        final char first = name.charAt(0);
        final char last = name.charAt(len - 1);
        if (first == ' ' || first == '.' || last == ' ' || last == '.') {
            return false;
        }
        // Windows 保留设备名
        if (RESERVED_NAME_PATTERN.matcher(name).find()) {
            return false;
        }
        return true;
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
}
