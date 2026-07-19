package kohgylw.kiftd.util;

import java.text.DecimalFormat;

public final class SizeFormatUtil {

	private static final long KB = 1024L;
	private static final long MB = 1048576L;
	private static final long GB = 1073741824L;
	private static final long TB = 1099511627776L;

	private static final DecimalFormat FORMAT = new DecimalFormat("#.#");

	private SizeFormatUtil() {
	}

	public static String formatFileSize(long size) {
		return formatFileSize(size, null);
	}

	public static String formatFileSize(long size, String invalidMessage) {
		if (size <= 0 && invalidMessage != null) {
			return invalidMessage;
		}
		double convertSize;
		String unit;
		if (size < KB) {
			convertSize = (double) size;
			unit = "B";
		} else if (size < MB) {
			convertSize = ((double) size / (double) KB);
			unit = "KB";
		} else if (size < GB) {
			convertSize = ((double) size / (double) MB);
			unit = "MB";
		} else if (size < TB) {
			convertSize = ((double) size / (double) GB);
			unit = "GB";
		} else {
			convertSize = ((double) size / (double) TB);
			unit = "TB";
		}
		return FORMAT.format(convertSize) + " " + unit;
	}

	public static String formatFileSize(String sizeStr) {
		if (sizeStr == null || sizeStr.isEmpty()) {
			return "0 B";
		}
		try {
			return formatFileSize(Long.parseLong(sizeStr));
		} catch (NumberFormatException e) {
			return sizeStr;
		}
	}

	public static long parseSizeString(String in) {
		if (in == null || in.length() <= 0) {
			return -1L;
		}
		try {
			return parseSizeWithUnit(in, false);
		} catch (Exception ignored) {
			return 0L;
		}
	}

	public static long parseRateString(String in) {
		if (in == null || in.length() <= 0) {
			return -1L;
		}
		try {
			return parseSizeWithUnit(in, true);
		} catch (Exception ignored) {
			return 0L;
		}
	}

	private static long parseSizeWithUnit(String in, boolean defaultIsKb) {
		if (in.length() <= 1) {
			long base = Long.parseLong(in.trim());
			return defaultIsKb ? base * KB : base;
		}
		String value;
		String unit;
		int len = in.length();
		if (len > 2 && in.toLowerCase().charAt(len - 1) == 'b') {
			unit = in.substring(len - 2, len - 1).toLowerCase();
			value = in.substring(0, len - 2).trim();
		} else {
			unit = in.substring(len - 1).toLowerCase();
			value = in.substring(0, len - 1).trim();
		}
		switch (unit) {
		case "k":
			return Long.parseLong(value) * KB;
		case "m":
			return Long.parseLong(value) * MB;
		case "g":
			return Long.parseLong(value) * GB;
		default:
			long base = Long.parseLong(in.trim());
			return defaultIsKb ? base * KB : base;
		}
	}
}
