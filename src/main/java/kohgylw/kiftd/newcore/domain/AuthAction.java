package kohgylw.kiftd.newcore.domain;

public enum AuthAction {

	CREATE_FOLDER("c", "创建文件夹"),
	UPLOAD_FILES("u", "上传文件"),
	DELETE("d", "删除"),
	RENAME("r", "重命名"),
	DOWNLOAD("l", "下载"),
	MOVE("m", "移动");

	private final String code;
	private final String description;

	AuthAction(String code, String description) {
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public static AuthAction fromCode(String code) {
		for (AuthAction action : values()) {
			if (action.code.equals(code)) {
				return action;
			}
		}
		return null;
	}
}
