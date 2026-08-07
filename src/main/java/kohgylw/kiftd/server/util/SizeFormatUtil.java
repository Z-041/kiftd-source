package kohgylw.kiftd.server.util;

import java.text.DecimalFormat;

public final class SizeFormatUtil {

	private static final long KB = 1024L;
	private static final long MB = 1048576L;
	private static final long GB = 1073741824L;
	private static final long TB = 1099511627776L;

	private SizeFormatUtil() {
	}

	public static String formatFileSize(long size) {
		return formatFileSize(size, null);
	}

	public static String formatFileSize(String size) {
		try {
			return formatFileSize(Long.parseLong(size));
		} catch (NumberFormatException e) {
			return size;
		}
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
		return new DecimalFormat("#.#").format(convertSize) + " " + unit;
	}

	public static long parseSizeString(String in) {
		if (in == null || in.length() <= 0) {
			return -1L;
		}
		try {
			return parseSizeWithUnit(in, false);
		} catch (IllegalArgumentException ignored) {
			return 0L;
		}
	}

	public static long parseRateString(String in) {
		if (in == null || in.length() <= 0) {
			return -1L;
		}
		try {
			return parseSizeWithUnit(in, true);
		} catch (IllegalArgumentException ignored) {
			return 0L;
		}
	}

	private static long parseSizeWithUnit(String in, boolean defaultIsKb) {
		String trimmed = in.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("empty size string");
		}
		if (trimmed.length() <= 1) {
			long base;
			try {
				base = Long.parseLong(trimmed);
			} catch (NumberFormatException e) {
				// NumberFormatException 是 RuntimeException，若直接抛出会绕过调用方的
				// IllegalArgumentException 捕获逻辑，这里统一转为 IllegalArgumentException
				throw new IllegalArgumentException("invalid size string: " + in, e);
			}
			return defaultIsKb ? base * KB : base;
		}
		String value;
		String unit;
		char last = Character.toLowerCase(trimmed.charAt(trimmed.length() - 1));
		if (last == 'b') {
			// 形如 "2B"、"2KB"、"2MB"、"2GB"、"2TB"
			if (trimmed.length() == 2) {
				unit = "b";
				value = trimmed.substring(0, 1);
			} else {
				unit = String.valueOf(Character.toLowerCase(trimmed.charAt(trimmed.length() - 2)));
				value = trimmed.substring(0, trimmed.length() - 2);
			}
		} else {
			// 形如 "2"、"2K"、"2M"、"2G"、"2T"
			unit = String.valueOf(last);
			value = trimmed.substring(0, trimmed.length() - 1);
		}
		long base;
		try {
			base = Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid size string: " + in, e);
		}
		switch (unit) {
		case "k":
			return base * KB;
		case "m":
			return base * MB;
		case "g":
			return base * GB;
		case "t":
			return base * TB;
		case "b":
			return base;
		default:
			return defaultIsKb ? base * KB : base;
		}
	}
}
