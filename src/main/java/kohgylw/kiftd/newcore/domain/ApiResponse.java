package kohgylw.kiftd.newcore.domain;

public class ApiResponse<T> {

	private final boolean success;
	private final String code;
	private final String message;
	private final T data;
	private final long timestamp;

	private ApiResponse(Builder<T> builder) {
		this.success = builder.success;
		this.code = builder.code;
		this.message = builder.message;
		this.data = builder.data;
		this.timestamp = builder.timestamp;
	}

	public static <T> ApiResponse<T> success() {
		return new Builder<T>()
				.success(true)
				.code(ResultCode.SUCCESS.getCode())
				.timestamp(System.currentTimeMillis())
				.build();
	}

	public static <T> ApiResponse<T> success(T data) {
		return new Builder<T>()
				.success(true)
				.code(ResultCode.SUCCESS.getCode())
				.data(data)
				.timestamp(System.currentTimeMillis())
				.build();
	}

	public static <T> ApiResponse<T> success(String code, T data) {
		return new Builder<T>()
				.success(true)
				.code(code)
				.data(data)
				.timestamp(System.currentTimeMillis())
				.build();
	}

	public static <T> ApiResponse<T> failure(String code, String message) {
		return new Builder<T>()
				.success(false)
				.code(code)
				.message(message)
				.timestamp(System.currentTimeMillis())
				.build();
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
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

	public T getData() {
		return data;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public static class Builder<T> {

		private boolean success;
		private String code;
		private String message;
		private T data;
		private long timestamp;

		private Builder() {
		}

		public Builder<T> success(boolean success) {
			this.success = success;
			return this;
		}

		public Builder<T> code(String code) {
			this.code = code;
			return this;
		}

		public Builder<T> message(String message) {
			this.message = message;
			return this;
		}

		public Builder<T> data(T data) {
			this.data = data;
			return this;
		}

		public Builder<T> timestamp(long timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public ApiResponse<T> build() {
			return new ApiResponse<>(this);
		}
	}
}
