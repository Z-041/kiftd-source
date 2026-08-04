package kohgylw.kiftd.server.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.server.util.ConfigurationManager;

@ExtendWith(MockitoExtension.class)
class MastLoginFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private HttpSession session;

    @Test
    void testMustLoginDisabled_allRequestsPass() throws Exception {
        MastLoginFilter filter = new MastLoginFilter();
        when(request.getSession()).thenReturn(session);
        when(request.getServletPath()).thenReturn("/homeController/anyAction.do");

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.mustLogin()).thenReturn(false);

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
        }
    }

    @Test
    void testLoginPagePassesWithoutAuth() throws Exception {
        MastLoginFilter filter = new MastLoginFilter();
        when(request.getSession()).thenReturn(session);
        when(request.getServletPath()).thenReturn("/prv/login.html");

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
        }
    }

    @Test
    void testExternalLinksControllerPassesWithoutAuth() throws Exception {
        MastLoginFilter filter = new MastLoginFilter();
        when(request.getSession()).thenReturn(session);
        when(request.getServletPath()).thenReturn("/externalLinksController/view.do");

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
        }
    }

    @Test
    void testProtectedPageWithoutSession_redirectsToLogin() throws Exception {
        MastLoginFilter filter = new MastLoginFilter();
        when(request.getSession()).thenReturn(session);
        when(request.getServletPath()).thenReturn("/homeController/listFile.do");
        when(session.getAttribute("ACCOUNT")).thenReturn(null);

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.mustLogin()).thenReturn(true);

            filter.doFilter(request, response, chain);

            verify(chain, never()).doFilter(request, response);
            verify(response, times(1)).sendRedirect("/prv/login.html");
        }
    }

    @Test
    void testProtectedPageWithInvalidAccount_redirectsToLogin() throws Exception {
        MastLoginFilter filter = new MastLoginFilter();
        when(request.getSession()).thenReturn(session);
        when(request.getServletPath()).thenReturn("/homeController/listFile.do");
        when(session.getAttribute("ACCOUNT")).thenReturn("unknownUser");

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.mustLogin()).thenReturn(true);
            when(reader.foundAccount("unknownUser")).thenReturn(false);

            filter.doFilter(request, response, chain);

            verify(chain, never()).doFilter(request, response);
            verify(response, times(1)).sendRedirect("/prv/login.html");
        }
    }

    @Test
    void testProtectedPageWithValidAccount_passesThrough() throws Exception {
        MastLoginFilter filter = new MastLoginFilter();
        when(request.getSession()).thenReturn(session);
        when(request.getServletPath()).thenReturn("/homeController/listFile.do");
        when(session.getAttribute("ACCOUNT")).thenReturn("admin");

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.mustLogin()).thenReturn(true);
            when(reader.foundAccount("admin")).thenReturn(true);

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
        }
    }

    @Test
    void testAjaxRequestWithoutSession_returnsMustLogin() throws Exception {
        MastLoginFilter filter = new MastLoginFilter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(request.getSession()).thenReturn(session);
        when(request.getServletPath()).thenReturn("/homeController/renameFile.ajax");
        when(session.getAttribute("ACCOUNT")).thenReturn(null);
        when(response.getWriter()).thenReturn(pw);

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.mustLogin()).thenReturn(true);

            filter.doFilter(request, response, chain);

            verify(chain, never()).doFilter(request, response);
            pw.flush();
            assertTrue(sw.toString().contains("mustLogin"),
                    "AJAX request without login should return 'mustLogin'");
        }
    }

    @Test
    void testAjaxLoginRequestPassesWithoutAuth() throws Exception {
        MastLoginFilter filter = new MastLoginFilter();
        when(request.getSession()).thenReturn(session);
        when(request.getServletPath()).thenReturn("/homeController/doLogin.ajax");

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.mustLogin()).thenReturn(true);

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
        }
    }

    @Test
    void testStaticResourceRedirectsToLoginWhenMustLogin() throws Exception {
        MastLoginFilter filter = new MastLoginFilter();
        when(request.getSession()).thenReturn(session);
        when(request.getServletPath()).thenReturn("/css/style.css");

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.mustLogin()).thenReturn(true);

            filter.doFilter(request, response, chain);

            verify(response, times(1)).sendRedirect("/prv/login.html");
        }
    }

}