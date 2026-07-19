package kohgylw.kiftd.newcore.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@TableName("FOLDER")
public class FolderEntity {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@TableId("folder_id")
	private String folderId;

	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private String folderName;

	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private LocalDateTime creationDate;

	@TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private String creator;

	@TableField(value = "folderParent", insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private String parentFolderId;

	@TableField(value = "folderConstraint", insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
	private int constraintLevel;

	public FolderEntity() {
	}

	private FolderEntity(Builder builder) {
		this.folderId = builder.folderId;
		this.folderName = builder.folderName;
		this.creationDate = builder.creationDate;
		this.creator = builder.creator;
		this.parentFolderId = builder.parentFolderId;
		this.constraintLevel = builder.constraintLevel;
	}

	public String getFolderId() {
		return folderId;
	}

	public void setFolderId(String folderId) {
		this.folderId = folderId;
	}

	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
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

	public String getParentFolderId() {
		return parentFolderId;
	}

	public void setParentFolderId(String parentFolderId) {
		this.parentFolderId = parentFolderId;
	}

	public int getConstraintLevel() {
		return constraintLevel;
	}

	public void setConstraintLevel(int constraintLevel) {
		this.constraintLevel = constraintLevel;
	}

	public String getFormattedCreationDate() {
		return creationDate != null ? creationDate.format(DATE_FORMATTER) : "";
	}

	public boolean isRoot() {
		return parentFolderId == null || parentFolderId.isEmpty() || "root".equals(parentFolderId);
	}

	public boolean isOwnedBy(String account) {
		return creator != null && creator.equals(account);
	}

	public boolean hasConstraint() {
		return constraintLevel > 0;
	}

	public FolderEntity rename(String newName) {
		return FolderEntity.builder()
				.folderId(this.folderId)
				.folderName(newName)
				.creationDate(this.creationDate)
				.creator(this.creator)
				.parentFolderId(this.parentFolderId)
				.constraintLevel(this.constraintLevel)
				.build();
	}

	public FolderEntity moveTo(String targetFolderId) {
		return FolderEntity.builder()
				.folderId(this.folderId)
				.folderName(this.folderName)
				.creationDate(this.creationDate)
				.creator(this.creator)
				.parentFolderId(targetFolderId)
				.constraintLevel(this.constraintLevel)
				.build();
	}

	public FolderEntity withConstraint(int level) {
		return FolderEntity.builder()
				.folderId(this.folderId)
				.folderName(this.folderName)
				.creationDate(this.creationDate)
				.creator(this.creator)
				.parentFolderId(this.parentFolderId)
				.constraintLevel(level)
				.build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String folderId;
		private String folderName;
		private LocalDateTime creationDate;
		private String creator;
		private String parentFolderId;
		private int constraintLevel;

		public Builder folderId(String folderId) {
			this.folderId = folderId;
			return this;
		}

		public Builder folderName(String folderName) {
			this.folderName = folderName;
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

		public Builder parentFolderId(String parentFolderId) {
			this.parentFolderId = parentFolderId;
			return this;
		}

		public Builder constraintLevel(int constraintLevel) {
			this.constraintLevel = constraintLevel;
			return this;
		}

		public FolderEntity build() {
			return new FolderEntity(this);
		}
	}
}
