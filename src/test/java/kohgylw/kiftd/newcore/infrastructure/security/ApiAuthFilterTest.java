package kohgylw.kiftd.newcore.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.server.util.ConfigurationManager;

@ExtendWith(MockitoExtension.class)
class ApiAuthFilterTest {

	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private FilterChain chain;
	@Mock
	private HttpSession session;

	private StringWriter captureResponseWriter() throws Exception {
		StringWriter sw = new StringWriter();
		when(response.getWriter()).thenReturn(new PrintWriter(sw));
		return sw;
	}

	@Test
	void testNoSession_returns401Json() throws Exception {
		ApiAuthFilter filter = new ApiAuthFilter();
		when(request.getSession(false)).thenReturn(null);
		StringWriter sw = captureResponseWriter();

		filter.doFilter(request, response, chain);

		verify(chain, never()).doFilter(request, response);
		verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		sw.flush();
		assertTrue(sw.toString().contains("\"code\":\"UNAUTHORIZED\""));
	}

	@Test
	void testLoggedInNonAdmin_returns403Json() throws Exception {
		ApiAuthFilter filter = new ApiAuthFilter();
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute("ACCOUNT")).thenReturn("normalUser");
		StringWriter sw = captureResponseWriter();

		try (var cr = mockStatic(ConfigurationManager.class)) {
			ConfigurationManager reader = mock(ConfigurationManager.class);
			cr.when(ConfigurationManager::instance).thenReturn(reader);
			when(reader.isSuperAdmin("normalUser")).thenReturn(false);

			filter.doFilter(request, response, chain);
		}

		verify(chain, never()).doFilter(request, response);
		verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
		sw.flush();
		assertTrue(sw.toString().contains("\"code\":\"FORBIDDEN\""));
	}

	@Test
	void testAdmin_passesThrough() throws Exception {
		ApiAuthFilter filter = new ApiAuthFilter();
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute("ACCOUNT")).thenReturn("admin");

		try (var cr = mockStatic(ConfigurationManager.class)) {
			ConfigurationManager reader = mock(ConfigurationManager.class);
			cr.when(ConfigurationManager::instance).thenReturn(reader);
			when(reader.isSuperAdmin("admin")).thenReturn(true);

			filter.doFilter(request, response, chain);
		}

		verify(chain, times(1)).doFilter(request, response);
	}
}
