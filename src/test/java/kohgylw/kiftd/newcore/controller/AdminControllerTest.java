package kohgylw.kiftd.newcore.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import kohgylw.kiftd.newcore.service.SystemService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private SystemService systemService;
    @Mock
    private HttpServletRequest request;

    private AdminController adminController;

    @BeforeEach
    void setUp() {
        adminController = new AdminController(systemService);
    }

    @Test
    void testGetServerOS_Windows() {
        when(systemService.getOSName()).thenReturn("Windows 10");

        String result = adminController.getServerOS();

        assertEquals("Windows 10", result);
        verify(systemService, times(1)).getOSName();
    }

    @Test
    void testGetServerOS_Linux() {
        when(systemService.getOSName()).thenReturn("Linux");

        String result = adminController.getServerOS();

        assertEquals("Linux", result);
        verify(systemService, times(1)).getOSName();
    }

    @Test
    void testGetServerOS_Mac() {
        when(systemService.getOSName()).thenReturn("Mac OS X");

        String result = adminController.getServerOS();

        assertEquals("Mac OS X", result);
        verify(systemService, times(1)).getOSName();
    }

    @Test
    void testGetServerOS_Empty() {
        when(systemService.getOSName()).thenReturn("");

        String result = adminController.getServerOS();

        assertEquals("", result);
        verify(systemService, times(1)).getOSName();
    }

    @Test
    void testGetFileChainKey_Success() {
        String expectedKey = "{\"key\":\"testChainKey123\"}";
        when(systemService.getFileChainKey(any(HttpServletRequest.class))).thenReturn(expectedKey);

        String result = adminController.getFileChainKey(request);

        assertEquals(expectedKey, result);
        verify(systemService, times(1)).getFileChainKey(request);
    }

    @Test
    void testGetFileChainKey_Empty() {
        when(systemService.getFileChainKey(any(HttpServletRequest.class))).thenReturn("");

        String result = adminController.getFileChainKey(request);

        assertEquals("", result);
        verify(systemService, times(1)).getFileChainKey(request);
    }

    @Test
    void testGetFileChainKey_Error() {
        String errorJson = "{\"error\":\"permission denied\"}";
        when(systemService.getFileChainKey(any(HttpServletRequest.class))).thenReturn(errorJson);

        String result = adminController.getFileChainKey(request);

        assertEquals(errorJson, result);
        verify(systemService, times(1)).getFileChainKey(request);
    }
}
