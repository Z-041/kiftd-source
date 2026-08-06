package kohgylw.kiftd.server.util;

import java.util.regex.Pattern;

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
    
}
