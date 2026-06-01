package kohgylw.kiftd.newcore.domain;

public class FileOperationContext {

	private final String account;
	private final String folderId;
	private final String fileId;
	private final String fileName;
	private final String clientIp;

	private FileOperationContext(Builder builder) {
		this.account = builder.account;
		this.folderId = builder.folderId;
		this.fileId = builder.fileId;
		this.fileName = builder.fileName;
		this.clientIp = builder.clientIp;
	}

	public String getAccount() {
		return account;
	}

	public String getFolderId() {
		return folderId;
	}

	public String getFileId() {
		return fileId;
	}

	public String getFileName() {
		return fileName;
	}

	public String getClientIp() {
		return clientIp;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String account;
		private String folderId;
		private String fileId;
		private String fileName;
		private String clientIp;

		public Builder account(String account) {
			this.account = account;
			return this;
		}

		public Builder folderId(String folderId) {
			this.folderId = folderId;
			return this;
		}

		public Builder fileId(String fileId) {
			this.fileId = fileId;
			return this;
		}

		public Builder fileName(String fileName) {
			this.fileName = fileName;
			return this;
		}

		public Builder clientIp(String clientIp) {
			this.clientIp = clientIp;
			return this;
		}

		public FileOperationContext build() {
			return new FileOperationContext(this);
		}
	}
}
