package kohgylw.kiftd.server.util;

import kohgylw.kiftd.server.util.ConfigurationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.http.HttpServletRequest;

class IpAddrGetterTest {

    private final IpAddrGetter getter = new IpAddrGetter();

    @BeforeAll
    static void initPrinter() {
        kohgylw.kiftd.printer.Printer.init(false);
    }

    @Test
    void testGetIpAddrFromRemoteAddr() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(false);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");

            String ip = getter.getIpAddr(request);
            assertEquals("192.168.1.100", ip);
        }
    }

    @Test
    void testGetIpAddrWithXForwardedFor() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(true);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 198.51.100.14");
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");

            String ip = getter.getIpAddr(request);
            assertEquals("203.0.113.195", ip);
        }
    }

    @Test
    void testGetIpAddrWithProxyClientIP() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(true);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Proxy-Client-IP")).thenReturn("10.0.0.100");
            // 仅当请求来源为受信代理（回环/内网）时才采信转发头
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            String ip = getter.getIpAddr(request);
            assertEquals("10.0.0.100", ip);
        }
    }

    @Test
    void testGetIpAddrXFFDisabledFallsBackToRemoteAddr() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(false);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            String ip = getter.getIpAddr(request);
            assertEquals("127.0.0.1", ip);
        }
    }

    @Test
    void testGetIpAddrWhenRemoteAddrIsNull() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(false);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn(null);

            String ip = getter.getIpAddr(request);
            assertEquals("获取失败", ip);
        }
    }

    @Test
    void testGetIpAddrWithUnknownHeader() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(true);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(request.getRemoteAddr()).thenReturn("172.16.0.1");

            String ip = getter.getIpAddr(request);
            assertEquals("172.16.0.1", ip);
        }
    }

    @Test
    void testGetIpAddrSkipsInvalidXffEntries() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(true);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn("not-an-ip, 203.0.113.195, 198.51.100.14");
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");

            String ip = getter.getIpAddr(request);
            assertEquals("203.0.113.195", ip);
        }
    }

    @Test
    void testGetIpAddrFallsBackWhenAllXffInvalid() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(true);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn("hack, another-fake");
            when(request.getRemoteAddr()).thenReturn("198.51.100.10");

            String ip = getter.getIpAddr(request);
            assertEquals("198.51.100.10", ip);
        }
    }

    @Test
    void testGetIpAddrInvalidRemoteAddrReturnsFailure() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(false);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("not-an-ip");

            String ip = getter.getIpAddr(request);
            assertEquals("获取失败", ip);
        }
    }

}