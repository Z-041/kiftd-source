package kohgylw.kiftd.server.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import kohgylw.kiftd.printer.Printer;

public class CircuitBreaker {

	public enum State {
		CLOSED, OPEN, HALF_OPEN
	}

	private final String name;
	private final int failureThreshold;
	private final long recoveryTimeoutMs;
	private final int halfOpenSuccessThreshold;

	private final AtomicInteger failureCount = new AtomicInteger(0);
	private final AtomicInteger successCount = new AtomicInteger(0);
	private volatile State state = State.CLOSED;
	private final AtomicLong lastFailureTime = new AtomicLong(0);

	public CircuitBreaker(String name, int failureThreshold, long recoveryTimeoutMs, int halfOpenSuccessThreshold) {
		this.name = name;
		this.failureThreshold = failureThreshold > 0 ? failureThreshold : 5;
		this.recoveryTimeoutMs = recoveryTimeoutMs > 0 ? recoveryTimeoutMs : 30000;
		this.halfOpenSuccessThreshold = halfOpenSuccessThreshold > 0 ? halfOpenSuccessThreshold : 2;
	}

	public CircuitBreaker(String name, int failureThreshold, long recoveryTimeoutMs) {
		this(name, failureThreshold, recoveryTimeoutMs, 2);
	}

	public synchronized boolean isRequestAllowed() {
		if (state == State.CLOSED) {
			return true;
		}

		if (state == State.OPEN) {
			long elapsed = System.currentTimeMillis() - lastFailureTime.get();
			if (elapsed >= recoveryTimeoutMs) {
				state = State.HALF_OPEN;
				successCount.set(0);
				Printer.instance.print("[熔断器] " + name + " 进入半开状态，尝试恢复服务。");
				return true;
			}
			return false;
		}

		return true;
	}

	public synchronized void recordSuccess() {
		if (state == State.HALF_OPEN) {
			int successes = successCount.incrementAndGet();
			if (successes >= halfOpenSuccessThreshold) {
				state = State.CLOSED;
				failureCount.set(0);
				successCount.set(0);
				Printer.instance.print("[熔断器] " + name + " 服务已恢复，熔断器关闭。");
			}
		} else {
			failureCount.set(0);
		}
	}

	public synchronized void recordFailure(Exception e) {
		lastFailureTime.set(System.currentTimeMillis());

		if (state == State.HALF_OPEN) {
			state = State.OPEN;
			successCount.set(0);
			failureCount.set(failureThreshold);
			Printer.instance.print("[熔断器] " + name + " 半开状态下失败，重新打开熔断器。错误: " + e.getMessage());
			return;
		}

		int failures = failureCount.incrementAndGet();
		if (failures >= failureThreshold && state == State.CLOSED) {
			state = State.OPEN;
			Printer.instance.print("[熔断器] " + name + " 连续失败 " + failures + " 次，熔断器已打开。错误: " + e.getMessage());
		}
	}

	public State getState() {
		return state;
	}

	public int getFailureCount() {
		return failureCount.get();
	}

	public String getName() {
		return name;
	}

	public void reset() {
		state = State.CLOSED;
		failureCount.set(0);
		successCount.set(0);
		lastFailureTime.set(0);
	}

	public interface CircuitBreakerOperation<T> {
		T execute() throws Exception;
	}

	public <T> T execute(CircuitBreakerOperation<T> operation, String operationName) throws Exception {
		if (!isRequestAllowed()) {
			throw new RuntimeException("[熔断器] " + name + " 已打开，快速失败: " + operationName);
		}

		try {
			T result = operation.execute();
			recordSuccess();
			return result;
		} catch (Exception e) {
			recordFailure(e);
			throw e;
		}
	}
}
