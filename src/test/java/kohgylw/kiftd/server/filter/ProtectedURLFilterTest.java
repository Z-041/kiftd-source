package kohgylw.kiftd.server.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.newcore.config.ConfigurationManager;

@ExtendWith(MockitoExtension.class)
class ProtectedURLFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private HttpSession session;

    @Test
    void testForbiddenPageDirectAccess_redirectsToHome() throws Exception {
        when(request.getServletPath()).thenReturn("/prv/forbidden.html");

        ProtectedURLFilter filter = new ProtectedURLFilter();
        filter.doFilter(request, response, chain);

        verify(response, times(1)).sendRedirect("/home.html");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void testErrorPageDirectAccess_redirectsToHome() throws Exception {
        when(request.getServletPath()).thenReturn("/prv/error.html");

        ProtectedURLFilter filter = new ProtectedURLFilter();
        filter.doFilter(request, response, chain);

        verify(response, times(1)).sendRedirect("/home.html");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void testLoginPageWithoutSession_passesThrough() throws Exception {
        when(request.getServletPath()).thenReturn("/prv/login.html");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("ACCOUNT")).thenReturn(null);

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.foundAccount(null)).thenReturn(false);

            ProtectedURLFilter filter = new ProtectedURLFilter();
            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    void testLoginPageWithValidSession_redirectsToHome() throws Exception {
        when(request.getServletPath()).thenReturn("/prv/login.html");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("ACCOUNT")).thenReturn("admin");

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.foundAccount("admin")).thenReturn(true);

            ProtectedURLFilter filter = new ProtectedURLFilter();
            filter.doFilter(request, response, chain);

            verify(response, times(1)).sendRedirect("/home.html");
            verify(chain, never()).doFilter(request, response);
        }
    }

    @Test
    void testSignupPage_passesThrough() throws Exception {
        when(request.getServletPath()).thenReturn("/prv/signup.html");

        ProtectedURLFilter filter = new ProtectedURLFilter();
        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

}