package kohgylw.kiftd.newcore.infrastructure.logging;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import kohgylw.kiftd.printer.Printer;

/**
 * API 请求性能监控过滤器
 * 仅对 /api/ 路径下的请求进行耗时统计
 */
public class ApiPerformanceFilter implements Filter {

	private static final String START_TIME_ATTR = "api_start_time";
	private static final long SLOW_REQUEST_THRESHOLD_MS = 1000;
	private static final int MAX_ENDPOINT_ENTRIES = 100;

	private static final AtomicLong totalRequestCount = new AtomicLong(0);
	private static final AtomicLong slowRequestCount = new AtomicLong(0);
	private static final AtomicLong totalResponseTime = new AtomicLong(0);
	private static final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
	private static final AtomicLong maxResponseTime = new AtomicLong(0);
	private static final Map<String, AtomicLong> endpointRequestCount = new ConcurrentHashMap<>();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		long startTime = System.currentTimeMillis();
		request.setAttribute(START_TIME_ATTR, startTime);

		try {
			chain.doFilter(request, response);
		} finally {
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			String uri = httpRequest.getRequestURI();
			String method = httpRequest.getMethod();
			String endpoint = method + " " + uri;

			totalRequestCount.incrementAndGet();
			totalResponseTime.addAndGet(duration);

			updateMinResponseTime(duration);
			updateMaxResponseTime(duration);

			AtomicLong counter = endpointRequestCount.computeIfAbsent(endpoint, k -> {
				// 端点数量达到上限后不再新增统计条目，仅更新已有条目计数
				if (endpointRequestCount.size() >= MAX_ENDPOINT_ENTRIES) {
					return null;
				}
				return new AtomicLong(0);
			});
			// 条目数已达上限时不新增（counter 为 null），避免对 null 自增触发 NPE
			if (counter != null) {
				counter.incrementAndGet();
			}

			if (duration > SLOW_REQUEST_THRESHOLD_MS) {
				slowRequestCount.incrementAndGet();
				Printer.instance.print(String.format("[慢请求警告] %s %s - 耗时 %dms", method, uri, duration));
			}
		}
	}

	private void updateMinResponseTime(long duration) {
		long currentMin;
		do {
			currentMin = minResponseTime.get();
			if (duration >= currentMin) {
				break;
			}
		} while (!minResponseTime.compareAndSet(currentMin, duration));
	}

	private void updateMaxResponseTime(long duration) {
		long currentMax;
		do {
			currentMax = maxResponseTime.get();
			if (duration <= currentMax) {
				break;
			}
		} while (!maxResponseTime.compareAndSet(currentMax, duration));
	}

	public static long getTotalRequestCount() {
		return totalRequestCount.get();
	}

	public static long getSlowRequestCount() {
		return slowRequestCount.get();
	}

	public static double getAverageResponseTime() {
		long count = totalRequestCount.get();
		if (count == 0) {
			return 0.0;
		}
		return (double) totalResponseTime.get() / count;
	}

	public static long getMinResponseTime() {
		long min = minResponseTime.get();
		return min == Long.MAX_VALUE ? 0 : min;
	}

	public static long getMaxResponseTime() {
		return maxResponseTime.get();
	}

	public static Map<String, AtomicLong> getEndpointRequestCount() {
		return endpointRequestCount;
	}
}
