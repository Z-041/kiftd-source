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
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.IpAddrGetter;

@ExtendWith(MockitoExtension.class)
class IPFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private IpAddrGetter ipGetter;

    @Test
    void testIpRuleDisabledAllRequestsPassThrough() throws Exception {
        IPFilter filter = new IPFilter();
        java.lang.reflect.Field idgField = IPFilter.class.getDeclaredField("idg");
        idgField.setAccessible(true);
        idgField.set(filter, ipGetter);

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.enableIPRule()).thenReturn(false);

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(response, never()).sendError(anyInt());
        }
    }

    @Test
    void testAllowedIpPassesThrough() throws Exception {
        IPFilter filter = new IPFilter();
        java.lang.reflect.Field idgField = IPFilter.class.getDeclaredField("idg");
        idgField.setAccessible(true);
        idgField.set(filter, ipGetter);

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.enableIPRule()).thenReturn(true);
            when(ipGetter.getIpAddr(request)).thenReturn("192.168.1.100");
            when(reader.filterAccessIP("192.168.1.100")).thenReturn(true);

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(response, never()).sendError(anyInt());
        }
    }

    @Test
    void testBlockedIpReturns403() throws Exception {
        IPFilter filter = new IPFilter();
        java.lang.reflect.Field idgField = IPFilter.class.getDeclaredField("idg");
        idgField.setAccessible(true);
        idgField.set(filter, ipGetter);

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.enableIPRule()).thenReturn(true);
            when(ipGetter.getIpAddr(request)).thenReturn("10.0.0.5");
            when(reader.filterAccessIP("10.0.0.5")).thenReturn(false);

            filter.doFilter(request, response, chain);

            verify(chain, never()).doFilter(request, response);
            verify(response, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Test
    void testBlockedIpDoesNotSendOtherErrorCodes() throws Exception {
        IPFilter filter = new IPFilter();
        java.lang.reflect.Field idgField = IPFilter.class.getDeclaredField("idg");
        idgField.setAccessible(true);
        idgField.set(filter, ipGetter);

        try (var cr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager reader = mock(ConfigurationManager.class);
            cr.when(ConfigurationManager::instance).thenReturn(reader);
            when(reader.enableIPRule()).thenReturn(true);
            when(ipGetter.getIpAddr(request)).thenReturn("192.168.0.99");
            when(reader.filterAccessIP("192.168.0.99")).thenReturn(false);

            filter.doFilter(request, response, chain);

            verify(response, times(1)).sendError(403);
            verify(response, never()).sendError(401);
            verify(response, never()).sendError(500);
        }
    }

}