package kohgylw.kiftd.newcore.domain;

import java.util.List;

public class FolderViewResult {

	private final String json;
	private final List<String> authList;
	private final String account;
	private final boolean allowChangePassword;
	private final boolean allowSignUp;

	private FolderViewResult(Builder builder) {
		this.json = builder.json;
		this.authList = builder.authList;
		this.account = builder.account;
		this.allowChangePassword = builder.allowChangePassword;
		this.allowSignUp = builder.allowSignUp;
	}

	public String getJson() {
		return json;
	}

	public List<String> getAuthList() {
		return authList;
	}

	public String getAccount() {
		return account;
	}

	public boolean isAllowChangePassword() {
		return allowChangePassword;
	}

	public boolean isAllowSignUp() {
		return allowSignUp;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String json;
		private List<String> authList;
		private String account;
		private boolean allowChangePassword;
		private boolean allowSignUp;

		public Builder json(String json) {
			this.json = json;
			return this;
		}

		public Builder authList(List<String> authList) {
			this.authList = authList;
			return this;
		}

		public Builder account(String account) {
			this.account = account;
			return this;
		}

		public Builder allowChangePassword(boolean allowChangePassword) {
			this.allowChangePassword = allowChangePassword;
			return this;
		}

		public Builder allowSignUp(boolean allowSignUp) {
			this.allowSignUp = allowSignUp;
			return this;
		}

		public FolderViewResult build() {
			return new FolderViewResult(this);
		}
	}
}
