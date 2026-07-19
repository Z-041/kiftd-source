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
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.newcore.service.FolderService;
import kohgylw.kiftd.newcore.service.FolderViewService;

@ExtendWith(MockitoExtension.class)
class FolderControllerTest {

    @Mock
    private FolderViewService folderViewService;
    @Mock
    private FolderService folderService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    private FolderController folderController;

    @BeforeEach
    void setUp() {
        folderController = new FolderController(folderViewService, folderService);
    }

    @Test
    void testGetFolderView_Success() {
        String folderId = "folder123";
        String expectedJson = "{\"folderId\":\"folder123\",\"files\":[]}";
        when(folderViewService.getFolderViewJson(eq(folderId), any(HttpSession.class), any(HttpServletRequest.class)))
                .thenReturn(expectedJson);

        String result = folderController.getFolderView(folderId, session, request);

        assertEquals(expectedJson, result);
        verify(folderViewService, times(1)).getFolderViewJson(folderId, session, request);
    }

    @Test
    void testGetFolderView_EmptyFolderId() {
        String folderId = "";
        String expectedJson = "{\"folderId\":\"\",\"files\":[]}";
        when(folderViewService.getFolderViewJson(eq(folderId), any(HttpSession.class), any(HttpServletRequest.class)))
                .thenReturn(expectedJson);

        String result = folderController.getFolderView(folderId, session, request);

        assertEquals(expectedJson, result);
        verify(folderViewService, times(1)).getFolderViewJson(folderId, session, request);
    }

    @Test
    void testGetFolderView_NullFolderId() {
        String folderId = null;
        String expectedJson = "{\"error\":\"invalid folder id\"}";
        when(folderViewService.getFolderViewJson(isNull(), any(HttpSession.class), any(HttpServletRequest.class)))
                .thenReturn(expectedJson);

        String result = folderController.getFolderView(folderId, session, request);

        assertEquals(expectedJson, result);
        verify(folderViewService, times(1)).getFolderViewJson(null, session, request);
    }

    @Test
    void testGetFolderView_WithFiles() {
        String folderId = "folder456";
        String expectedJson = "{\"folderId\":\"folder456\",\"files\":[{\"name\":\"file1.txt\"},{\"name\":\"file2.txt\"}]}";
        when(folderViewService.getFolderViewJson(eq(folderId), any(HttpSession.class), any(HttpServletRequest.class)))
                .thenReturn(expectedJson);

        String result = folderController.getFolderView(folderId, session, request);

        assertEquals(expectedJson, result);
        verify(folderViewService, times(1)).getFolderViewJson(folderId, session, request);
    }

    @Test
    void testGetRemainingFolderView_Success() {
        String expectedJson = "{\"remaining\":100}";
        when(folderViewService.getRemainingFolderViewJson(any(HttpServletRequest.class)))
                .thenReturn(expectedJson);

        String result = folderController.getRemainingFolderView(request);

        assertEquals(expectedJson, result);
        verify(folderViewService, times(1)).getRemainingFolderViewJson(request);
    }

    @Test
    void testGetRemainingFolderView_Empty() {
        when(folderViewService.getRemainingFolderViewJson(any(HttpServletRequest.class)))
                .thenReturn("");

        String result = folderController.getRemainingFolderView(request);

        assertEquals("", result);
        verify(folderViewService, times(1)).getRemainingFolderViewJson(request);
    }

    @Test
    void testNewFolder_Success() {
        when(folderService.newFolder(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = folderController.newFolder(request);

        assertEquals("SUCCESS", result);
        verify(folderService, times(1)).newFolder(request);
    }

    @Test
    void testNewFolder_Failure() {
        when(folderService.newFolder(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = folderController.newFolder(request);

        assertEquals("FAILURE", result);
        verify(folderService, times(1)).newFolder(request);
    }

    @Test
    void testNewFolder_AlreadyExists() {
        when(folderService.newFolder(any(HttpServletRequest.class))).thenReturn("folderexists");

        String result = folderController.newFolder(request);

        assertEquals("folderexists", result);
        verify(folderService, times(1)).newFolder(request);
    }

    @Test
    void testDeleteFolder_Success() {
        when(folderService.deleteFolder(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = folderController.deleteFolder(request);

        assertEquals("SUCCESS", result);
        verify(folderService, times(1)).deleteFolder(request);
    }

    @Test
    void testDeleteFolder_Failure() {
        when(folderService.deleteFolder(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = folderController.deleteFolder(request);

        assertEquals("FAILURE", result);
        verify(folderService, times(1)).deleteFolder(request);
    }

    @Test
    void testDeleteFolder_NotFound() {
        when(folderService.deleteFolder(any(HttpServletRequest.class))).thenReturn("notfound");

        String result = folderController.deleteFolder(request);

        assertEquals("notfound", result);
        verify(folderService, times(1)).deleteFolder(request);
    }

    @Test
    void testRenameFolder_Success() {
        when(folderService.renameFolder(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = folderController.renameFolder(request);

        assertEquals("SUCCESS", result);
        verify(folderService, times(1)).renameFolder(request);
    }

    @Test
    void testRenameFolder_Failure() {
        when(folderService.renameFolder(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = folderController.renameFolder(request);

        assertEquals("FAILURE", result);
        verify(folderService, times(1)).renameFolder(request);
    }

    @Test
    void testRenameFolder_NameExists() {
        when(folderService.renameFolder(any(HttpServletRequest.class))).thenReturn("nameexists");

        String result = folderController.renameFolder(request);

        assertEquals("nameexists", result);
        verify(folderService, times(1)).renameFolder(request);
    }

    @Test
    void testCountFolderContent_Success() {
        String expectedJson = "{\"fileCount\":10,\"folderCount\":5}";
        when(folderService.getFolderCountResult(any(HttpServletRequest.class))).thenReturn(expectedJson);

        String result = folderController.countFolderContent(request);

        assertEquals(expectedJson, result);
        verify(folderService, times(1)).getFolderCountResult(request);
    }

    @Test
    void testCountFolderContent_Empty() {
        String expectedJson = "{\"fileCount\":0,\"folderCount\":0}";
        when(folderService.getFolderCountResult(any(HttpServletRequest.class))).thenReturn(expectedJson);

        String result = folderController.countFolderContent(request);

        assertEquals(expectedJson, result);
        verify(folderService, times(1)).getFolderCountResult(request);
    }
}
