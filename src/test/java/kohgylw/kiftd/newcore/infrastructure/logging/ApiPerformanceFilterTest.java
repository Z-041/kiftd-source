package kohgylw.kiftd.newcore.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import kohgylw.kiftd.printer.Printer;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiPerformanceFilterTest {

	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private FilterChain chain;

	private ApiPerformanceFilter filter;

	@BeforeEach
	void setUp() {
		Printer.instance = mock(Printer.class);
		filter = new ApiPerformanceFilter();
		ApiPerformanceFilter.getEndpointRequestCount().clear();
	}

	@AfterEach
	void tearDown() {
		Printer.instance = null;
		ApiPerformanceFilter.getEndpointRequestCount().clear();
	}

	@Test
	void testDoFilter_RecordsStatsForEndpoint() throws Exception {
		long totalBefore = ApiPerformanceFilter.getTotalRequestCount();
		when(request.getRequestURI()).thenReturn("/api/system/info");
		when(request.getMethod()).thenReturn("GET");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		assertEquals(totalBefore + 1, ApiPerformanceFilter.getTotalRequestCount());
		assertEquals(1L, ApiPerformanceFilter.getEndpointRequestCount().get("GET /api/system/info").get());
		assertTrue(ApiPerformanceFilter.getAverageResponseTime() >= 0);
		assertTrue(ApiPerformanceFilter.getMinResponseTime() >= 0);
		assertTrue(ApiPerformanceFilter.getMaxResponseTime() >= 0);
	}

	@Test
	void testDoFilter_ExceptionPropagated_StatsStillRecorded() throws Exception {
		long totalBefore = ApiPerformanceFilter.getTotalRequestCount();
		when(request.getRequestURI()).thenReturn("/api/system/boom");
		when(request.getMethod()).thenReturn("POST");
		doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);

		assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, chain));

		assertEquals(totalBefore + 1, ApiPerformanceFilter.getTotalRequestCount());
		assertTrue(ApiPerformanceFilter.getEndpointRequestCount().containsKey("POST /api/system/boom"));
	}

	@Test
	void testDoFilter_SlowRequest_IncrementsSlowCount() throws Exception {
		long slowBefore = ApiPerformanceFilter.getSlowRequestCount();
		when(request.getRequestURI()).thenReturn("/api/slow");
		when(request.getMethod()).thenReturn("GET");
		doAnswer(invocation -> {
			Thread.sleep(1050);
			return null;
		}).when(chain).doFilter(request, response);

		filter.doFilter(request, response, chain);

		assertEquals(slowBefore + 1, ApiPerformanceFilter.getSlowRequestCount());
	}

	@Test
	void testDoFilter_EndpointCountReachesLimit_NoNpeAndNoNewEntries() throws Exception {
		// 填满 MAX_ENDPOINT_ENTRIES(100) 个端点
		for (int i = 0; i < 100; i++) {
			when(request.getRequestURI()).thenReturn("/api/endpoint/" + i);
			when(request.getMethod()).thenReturn("GET");
			filter.doFilter(request, response, chain);
		}
		assertEquals(100, ApiPerformanceFilter.getEndpointRequestCount().size());

		// 第 101 个新端点不再新增条目且不抛 NPE（EXC-004 回归验证）
		when(request.getRequestURI()).thenReturn("/api/endpoint/overflow");
		filter.doFilter(request, response, chain);

		assertEquals(100, ApiPerformanceFilter.getEndpointRequestCount().size());
		assertFalse(ApiPerformanceFilter.getEndpointRequestCount().containsKey("GET /api/endpoint/overflow"));
	}
}
