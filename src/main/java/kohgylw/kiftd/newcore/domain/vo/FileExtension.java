package kohgylw.kiftd.newcore.domain.vo;

import java.util.Objects;
import java.util.Set;

public final class FileExtension {

	private static final Set<String> IMAGE_EXTENSIONS = Set.of(
			"jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico"
	);

	private static final Set<String> VIDEO_EXTENSIONS = Set.of(
			"mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp"
	);

	private static final Set<String> AUDIO_EXTENSIONS = Set.of(
			"mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "ape"
	);

	private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
			"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
			"txt", "md", "rtf", "odt", "ods", "odp", "csv"
	);

	private static final Set<String> ARCHIVE_EXTENSIONS = Set.of(
			"zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz"
	);

	private static final Set<String> CODE_EXTENSIONS = Set.of(
			"java", "py", "js", "ts", "go", "rs", "c", "cpp", "h",
			"html", "css", "scss", "less", "json", "xml", "yml", "yaml",
			"sh", "bat", "ps1", "sql"
	);

	private final String extension;

	private FileExtension(String extension) {
		if (extension == null) {
			this.extension = "";
		} else {
			this.extension = extension.toLowerCase().trim();
		}
	}

	public static FileExtension of(String fileName) {
		if (fileName == null || fileName.isEmpty() || !fileName.contains(".")) {
			return new FileExtension("");
		}
		int lastDotIndex = fileName.lastIndexOf(".");
		if (lastDotIndex == fileName.length() - 1) {
			return new FileExtension("");
		}
		return new FileExtension(fileName.substring(lastDotIndex + 1));
	}

	public String getValue() {
		return extension;
	}

	public boolean isEmpty() {
		return extension.isEmpty();
	}

	public boolean isImage() {
		return IMAGE_EXTENSIONS.contains(extension);
	}

	public boolean isVideo() {
		return VIDEO_EXTENSIONS.contains(extension);
	}

	public boolean isAudio() {
		return AUDIO_EXTENSIONS.contains(extension);
	}

	public boolean isDocument() {
		return DOCUMENT_EXTENSIONS.contains(extension);
	}

	public boolean isArchive() {
		return ARCHIVE_EXTENSIONS.contains(extension);
	}

	public boolean isCode() {
		return CODE_EXTENSIONS.contains(extension);
	}

	public boolean isMedia() {
		return isImage() || isVideo() || isAudio();
	}

	public String getFileType() {
		if (isImage()) return "image";
		if (isVideo()) return "video";
		if (isAudio()) return "audio";
		if (isDocument()) return "document";
		if (isArchive()) return "archive";
		if (isCode()) return "code";
		return "other";
	}

	public boolean equalsIgnoreCase(String other) {
		return extension.equalsIgnoreCase(other != null ? other : "");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FileExtension that = (FileExtension) o;
		return extension.equals(that.extension);
	}

	@Override
	public int hashCode() {
		return Objects.hash(extension);
	}

	@Override
	public String toString() {
		return extension;
	}
}
