package kohgylw.kiftd.newcore.domain;

public class OperationResult {

	private final boolean success;
	private final String code;
	private final String message;

	private OperationResult(boolean success, String code, String message) {
		this.success = success;
		this.code = code;
		this.message = message;
	}

	public static OperationResult success() {
		return new OperationResult(true, "SUCCESS", null);
	}

	public static OperationResult success(String code) {
		return new OperationResult(true, code, null);
	}

	public static OperationResult failure(String code) {
		return new OperationResult(false, code, null);
	}

	public static OperationResult failure(String code, String message) {
		return new OperationResult(false, code, message);
	}

	public boolean isSuccess() {
		return success;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return (success ? "SUCCESS:" : "FAILURE:") + code
				+ (message != null ? " - " + message : "");
	}
}
