package kohgylw.kiftd.newcore.domain.vo;

import java.util.Objects;

public final class FileSize {

	private final long bytes;

	private FileSize(long bytes) {
		if (bytes < 0) {
			throw new IllegalArgumentException("文件大小不能为负数");
		}
		this.bytes = bytes;
	}

	public static FileSize ofBytes(long bytes) {
		return new FileSize(bytes);
	}

	public static FileSize ofKilobytes(long kilobytes) {
		return new FileSize(kilobytes * 1024);
	}

	public static FileSize ofMegabytes(long megabytes) {
		return new FileSize(megabytes * 1024 * 1024);
	}

	public static FileSize ofGigabytes(long gigabytes) {
		return new FileSize(gigabytes * 1024 * 1024 * 1024);
	}

	public long toBytes() {
		return bytes;
	}

	public double toKilobytes() {
		return bytes / 1024.0;
	}

	public double toMegabytes() {
		return bytes / (1024.0 * 1024);
	}

	public double toGigabytes() {
		return bytes / (1024.0 * 1024 * 1024);
	}

	public String toFormattedString() {
		if (bytes == 0) {
			return "0 B";
		}
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int unitIndex = 0;
		double size = bytes;
		while (size >= 1024 && unitIndex < units.length - 1) {
			size /= 1024;
			unitIndex++;
		}
		return String.format("%.2f %s", size, units[unitIndex]);
	}

	public FileSize add(FileSize other) {
		return new FileSize(this.bytes + other.bytes);
	}

	public FileSize subtract(FileSize other) {
		return new FileSize(Math.max(0, this.bytes - other.bytes));
	}

	public boolean isGreaterThan(FileSize other) {
		return this.bytes > other.bytes;
	}

	public boolean isLessThan(FileSize other) {
		return this.bytes < other.bytes;
	}

	public boolean isEqualTo(FileSize other) {
		return this.bytes == other.bytes;
	}

	public boolean isZero() {
		return bytes == 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FileSize fileSize = (FileSize) o;
		return bytes == fileSize.bytes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bytes);
	}

	@Override
	public String toString() {
		return toFormattedString();
	}
}
