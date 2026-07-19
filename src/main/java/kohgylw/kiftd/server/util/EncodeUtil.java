package kohgylw.kiftd.server.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class EncodeUtil {

	private EncodeUtil() {
	}

	public static String getFileNameByUTF8(String name) {
		return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
	}

}
