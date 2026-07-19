package kohgylw.kiftd.newcore.domain.entity;

public class Account {

	private String accountName;
	private String passwordHash;
	private String auth;
	private String group;
	private boolean isSuperAdmin;
	private Long maxUploadSize;
	private Long maxDownloadRate;

	public Account() {
	}

	private Account(Builder builder) {
		this.accountName = builder.accountName;
		this.passwordHash = builder.passwordHash;
		this.auth = builder.auth;
		this.group = builder.group;
		this.isSuperAdmin = builder.isSuperAdmin;
		this.maxUploadSize = builder.maxUploadSize;
		this.maxDownloadRate = builder.maxDownloadRate;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public boolean isSuperAdmin() {
		return isSuperAdmin;
	}

	public void setSuperAdmin(boolean superAdmin) {
		isSuperAdmin = superAdmin;
	}

	public Long getMaxUploadSize() {
		return maxUploadSize;
	}

	public void setMaxUploadSize(Long maxUploadSize) {
		this.maxUploadSize = maxUploadSize;
	}

	public Long getMaxDownloadRate() {
		return maxDownloadRate;
	}

	public void setMaxDownloadRate(Long maxDownloadRate) {
		this.maxDownloadRate = maxDownloadRate;
	}

	public boolean hasPermission(String permission) {
		if (isSuperAdmin) {
			return true;
		}
		if (auth == null || auth.isEmpty()) {
			return false;
		}
		return auth.contains(permission);
	}

	public boolean isInGroup(String groupName) {
		if (group == null || groupName == null) {
			return false;
		}
		return group.equals(groupName);
	}

	public boolean hasUploadPermission() {
		return hasPermission("up");
	}

	public boolean hasDeletePermission() {
		return hasPermission("del");
	}

	public boolean hasDownloadPermission() {
		return hasPermission("dl");
	}

	public boolean hasNewFolderPermission() {
		return hasPermission("mkf");
	}

	public boolean canUploadFile(long fileSize) {
		if (!hasUploadPermission()) {
			return false;
		}
		if (maxUploadSize == null || maxUploadSize <= 0) {
			return true;
		}
		return fileSize <= maxUploadSize;
	}

	public Account withPasswordHash(String newPasswordHash) {
		return Account.builder()
				.accountName(this.accountName)
				.passwordHash(newPasswordHash)
				.auth(this.auth)
				.group(this.group)
				.isSuperAdmin(this.isSuperAdmin)
				.maxUploadSize(this.maxUploadSize)
				.maxDownloadRate(this.maxDownloadRate)
				.build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String accountName;
		private String passwordHash;
		private String auth;
		private String group;
		private boolean isSuperAdmin;
		private Long maxUploadSize;
		private Long maxDownloadRate;

		public Builder accountName(String accountName) {
			this.accountName = accountName;
			return this;
		}

		public Builder passwordHash(String passwordHash) {
			this.passwordHash = passwordHash;
			return this;
		}

		public Builder auth(String auth) {
			this.auth = auth;
			return this;
		}

		public Builder group(String group) {
			this.group = group;
			return this;
		}

		public Builder isSuperAdmin(boolean isSuperAdmin) {
			this.isSuperAdmin = isSuperAdmin;
			return this;
		}

		public Builder maxUploadSize(Long maxUploadSize) {
			this.maxUploadSize = maxUploadSize;
			return this;
		}

		public Builder maxDownloadRate(Long maxDownloadRate) {
			this.maxDownloadRate = maxDownloadRate;
			return this;
		}

		public Account build() {
			return new Account(this);
		}
	}
}
