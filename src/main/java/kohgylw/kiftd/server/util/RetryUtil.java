package kohgylw.kiftd.server.util;

import kohgylw.kiftd.printer.Printer;

public class RetryUtil {

	private static final int DEFAULT_MAX_RETRIES = 3;
	private static final long DEFAULT_INITIAL_DELAY_MS = 100;
	private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

	public interface RetryableOperation<T> {
		T execute() throws Exception;
	}

	public static <T> T executeWithRetry(RetryableOperation<T> operation, String operationName) {
		return executeWithRetry(operation, operationName, DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY_MS,
				DEFAULT_BACKOFF_MULTIPLIER);
	}

	public static <T> T executeWithRetry(RetryableOperation<T> operation, String operationName, int maxRetries,
			long initialDelayMs, double backoffMultiplier) {
		if (maxRetries <= 0) {
			maxRetries = DEFAULT_MAX_RETRIES;
		}
		if (initialDelayMs <= 0) {
			initialDelayMs = DEFAULT_INITIAL_DELAY_MS;
		}
		if (backoffMultiplier <= 1.0) {
			backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
		}

		Exception lastException = null;
		long delay = initialDelayMs;

		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				return operation.execute();
			} catch (Exception e) {
				lastException = e;
				if (attempt < maxRetries) {
					Printer.instance.print("[重试] " + operationName + " 第 " + (attempt + 1) + " 次失败，" + delay + "ms 后进行第 "
							+ (attempt + 2) + " 次重试。原因: " + e.getMessage());
					try {
						Thread.sleep(delay);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						Printer.instance.print("[重试] " + operationName + " 重试等待被中断。");
						break;
					}
					delay = (long) (delay * backoffMultiplier);
				}
			}
		}

		Printer.instance.print("[重试] " + operationName + " 经过 " + maxRetries + " 次重试后仍然失败。最终错误: "
				+ (lastException != null ? lastException.getMessage() : "未知错误"));
		return null;
	}

	public static boolean executeWithRetry(Runnable operation, String operationName) {
		return executeWithRetry(() -> {
			operation.run();
			return true;
		}, operationName) != null;
	}
}
