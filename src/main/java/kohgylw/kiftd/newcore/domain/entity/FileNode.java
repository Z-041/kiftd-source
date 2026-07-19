package kohgylw.kiftd.newcore.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@TableName("FILE")
public class FileNode {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@TableId("file_id")
	private String fileId;

	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private String fileName;

	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private Long fileSize;

	@TableField(value = "fileParentFolder", insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private String parentFolderId;

	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private LocalDateTime creationDate;

	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private String creator;

	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private String filePath;

	public FileNode() {
	}

	private FileNode(Builder builder) {
		this.fileId = builder.fileId;
		this.fileName = builder.fileName;
		this.fileSize = builder.fileSize;
		this.parentFolderId = builder.parentFolderId;
		this.creationDate = builder.creationDate;
		this.creator = builder.creator;
		this.filePath = builder.filePath;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public String getParentFolderId() {
		return parentFolderId;
	}

	public void setParentFolderId(String parentFolderId) {
		this.parentFolderId = parentFolderId;
	}

	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDateTime creationDate) {
		this.creationDate = creationDate;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFormattedCreationDate() {
		return creationDate != null ? creationDate.format(DATE_FORMATTER) : "";
	}

	public String getFormattedFileSize() {
		if (fileSize == null || fileSize == 0) {
			return "0 B";
		}
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int unitIndex = 0;
		double size = fileSize;
		while (size >= 1024 && unitIndex < units.length - 1) {
			size /= 1024;
			unitIndex++;
		}
		return String.format("%.2f %s", size, units[unitIndex]);
	}

	public String getFileExtension() {
		if (fileName == null || !fileName.contains(".")) {
			return "";
		}
		return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
	}

	public boolean isImage() {
		String ext = getFileExtension();
		return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif")
				|| ext.equals("bmp") || ext.equals("webp");
	}

	public boolean isVideo() {
		String ext = getFileExtension();
		return ext.equals("mp4") || ext.equals("avi") || ext.equals("mkv") || ext.equals("mov")
				|| ext.equals("wmv") || ext.equals("flv") || ext.equals("webm");
	}

	public boolean isAudio() {
		String ext = getFileExtension();
		return ext.equals("mp3") || ext.equals("wav") || ext.equals("flac") || ext.equals("aac")
				|| ext.equals("ogg") || ext.equals("wma");
	}

	public boolean isDocument() {
		String ext = getFileExtension();
		return ext.equals("pdf") || ext.equals("doc") || ext.equals("docx") || ext.equals("xls")
				|| ext.equals("xlsx") || ext.equals("ppt") || ext.equals("pptx") || ext.equals("txt")
				|| ext.equals("md");
	}

	public boolean isArchive() {
		String ext = getFileExtension();
		return ext.equals("zip") || ext.equals("rar") || ext.equals("7z") || ext.equals("tar")
				|| ext.equals("gz");
	}

	public String getFileType() {
		if (isImage()) return "image";
		if (isVideo()) return "video";
		if (isAudio()) return "audio";
		if (isDocument()) return "document";
		if (isArchive()) return "archive";
		return "other";
	}

	public boolean isOwnedBy(String account) {
		return creator != null && creator.equals(account);
	}

	public FileNode rename(String newName) {
		return FileNode.builder()
				.fileId(this.fileId)
				.fileName(newName)
				.fileSize(this.fileSize)
				.parentFolderId(this.parentFolderId)
				.creationDate(this.creationDate)
				.creator(this.creator)
				.filePath(this.filePath)
				.build();
	}

	public FileNode moveTo(String targetFolderId) {
		return FileNode.builder()
				.fileId(this.fileId)
				.fileName(this.fileName)
				.fileSize(this.fileSize)
				.parentFolderId(targetFolderId)
				.creationDate(this.creationDate)
				.creator(this.creator)
				.filePath(this.filePath)
				.build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String fileId;
		private String fileName;
		private Long fileSize;
		private String parentFolderId;
		private LocalDateTime creationDate;
		private String creator;
		private String filePath;

		public Builder fileId(String fileId) {
			this.fileId = fileId;
			return this;
		}

		public Builder fileName(String fileName) {
			this.fileName = fileName;
			return this;
		}

		public Builder fileSize(Long fileSize) {
			this.fileSize = fileSize;
			return this;
		}

		public Builder parentFolderId(String parentFolderId) {
			this.parentFolderId = parentFolderId;
			return this;
		}

		public Builder creationDate(LocalDateTime creationDate) {
			this.creationDate = creationDate;
			return this;
		}

		public Builder creator(String creator) {
			this.creator = creator;
			return this;
		}

		public Builder filePath(String filePath) {
			this.filePath = filePath;
			return this;
		}

		public FileNode build() {
			return new FileNode(this);
		}
	}
}
