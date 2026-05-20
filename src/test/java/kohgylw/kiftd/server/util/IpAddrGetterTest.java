package kohgylw.kiftd.server.util;

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
        try (MockedStatic<ConfigureReader> mockedCr = mockStatic(ConfigureReader.class)) {
            ConfigureReader mockReader = mock(ConfigureReader.class);
            mockedCr.when(ConfigureReader::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(false);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");

            String ip = getter.getIpAddr(request);
            assertEquals("192.168.1.100", ip);
        }
    }

    @Test
    void testGetIpAddrWithXForwardedFor() {
        try (MockedStatic<ConfigureReader> mockedCr = mockStatic(ConfigureReader.class)) {
            ConfigureReader mockReader = mock(ConfigureReader.class);
            mockedCr.when(ConfigureReader::instance).thenReturn(mockReader);
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
        try (MockedStatic<ConfigureReader> mockedCr = mockStatic(ConfigureReader.class)) {
            ConfigureReader mockReader = mock(ConfigureReader.class);
            mockedCr.when(ConfigureReader::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(true);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Proxy-Client-IP")).thenReturn("10.0.0.100");

            String ip = getter.getIpAddr(request);
            assertEquals("10.0.0.100", ip);
        }
    }

    @Test
    void testGetIpAddrXFFDisabledFallsBackToRemoteAddr() {
        try (MockedStatic<ConfigureReader> mockedCr = mockStatic(ConfigureReader.class)) {
            ConfigureReader mockReader = mock(ConfigureReader.class);
            mockedCr.when(ConfigureReader::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(false);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            String ip = getter.getIpAddr(request);
            assertEquals("127.0.0.1", ip);
        }
    }

    @Test
    void testGetIpAddrWhenRemoteAddrIsNull() {
        try (MockedStatic<ConfigureReader> mockedCr = mockStatic(ConfigureReader.class)) {
            ConfigureReader mockReader = mock(ConfigureReader.class);
            mockedCr.when(ConfigureReader::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(false);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn(null);

            String ip = getter.getIpAddr(request);
            assertEquals("获取失败", ip);
        }
    }

    @Test
    void testGetIpAddrWithUnknownHeader() {
        try (MockedStatic<ConfigureReader> mockedCr = mockStatic(ConfigureReader.class)) {
            ConfigureReader mockReader = mock(ConfigureReader.class);
            mockedCr.when(ConfigureReader::instance).thenReturn(mockReader);
            when(mockReader.isIpXFFAnalysis()).thenReturn(true);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(request.getRemoteAddr()).thenReturn("172.16.0.1");

            String ip = getter.getIpAddr(request);
            assertEquals("172.16.0.1", ip);
        }
    }

}