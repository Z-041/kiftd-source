package kohgylw.kiftd.newcore.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.newcore.service.FileService;
import kohgylw.kiftd.newcore.service.FolderService;
import kohgylw.kiftd.newcore.service.FolderViewService;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;
    @Mock
    private FolderService folderService;
    @Mock
    private FolderViewService folderViewService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private MultipartFile file;

    private FileController fileController;

    @BeforeEach
    void setUp() {
        fileController = new FileController(fileService, folderService, folderViewService);
    }

    @Test
    void testDouploadFile_Success() {
        when(fileService.doUploadFile(any(HttpServletRequest.class), any(HttpServletResponse.class), any(MultipartFile.class)))
                .thenReturn("SUCCESS");

        String result = fileController.douploadFile(request, response, file, session);

        assertEquals("SUCCESS", result);
        verify(fileService, times(1)).doUploadFile(request, response, file);
    }

    @Test
    void testDouploadFile_Failure() {
        when(fileService.doUploadFile(any(HttpServletRequest.class), any(HttpServletResponse.class), any(MultipartFile.class)))
                .thenReturn("FAILURE");

        String result = fileController.douploadFile(request, response, file, session);

        assertEquals("FAILURE", result);
        verify(fileService, times(1)).doUploadFile(request, response, file);
    }

    @Test
    void testCheckUploadFile_Success() {
        when(fileService.checkUploadFile(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenReturn("canupload");

        String result = fileController.checkUploadFile(request, response, session);

        assertEquals("canupload", result);
        verify(fileService, times(1)).checkUploadFile(request, response);
    }

    @Test
    void testCheckUploadFile_Full() {
        when(fileService.checkUploadFile(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenReturn("outoflimit");

        String result = fileController.checkUploadFile(request, response, session);

        assertEquals("outoflimit", result);
        verify(fileService, times(1)).checkUploadFile(request, response);
    }

    @Test
    void testCheckImportFolder_Success() {
        when(fileService.checkImportFolder(any(HttpServletRequest.class))).thenReturn("canimport");

        String result = fileController.checkImportFolder(request, session);

        assertEquals("canimport", result);
        verify(fileService, times(1)).checkImportFolder(request);
    }

    @Test
    void testCheckImportFolder_Failure() {
        when(fileService.checkImportFolder(any(HttpServletRequest.class))).thenReturn("cannotimport");

        String result = fileController.checkImportFolder(request, session);

        assertEquals("cannotimport", result);
        verify(fileService, times(1)).checkImportFolder(request);
    }

    @Test
    void testDoImportFolder_Success() {
        when(fileService.doImportFolder(any(HttpServletRequest.class), any(MultipartFile.class)))
                .thenReturn("SUCCESS");

        String result = fileController.doImportFolder(request, file, session);

        assertEquals("SUCCESS", result);
        verify(fileService, times(1)).doImportFolder(request, file);
    }

    @Test
    void testDoImportFolder_Failure() {
        when(fileService.doImportFolder(any(HttpServletRequest.class), any(MultipartFile.class)))
                .thenReturn("FAILURE");

        String result = fileController.doImportFolder(request, file, session);

        assertEquals("FAILURE", result);
        verify(fileService, times(1)).doImportFolder(request, file);
    }

    @Test
    void testDeleteFolderByName_Success() {
        when(folderService.deleteFolderByName(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = fileController.deleteFolderByName(request, session);

        assertEquals("SUCCESS", result);
        verify(folderService, times(1)).deleteFolderByName(request);
    }

    @Test
    void testDeleteFolderByName_Failure() {
        when(folderService.deleteFolderByName(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = fileController.deleteFolderByName(request, session);

        assertEquals("FAILURE", result);
        verify(folderService, times(1)).deleteFolderByName(request);
    }

    @Test
    void testCreateNewFolderByName_Success() {
        when(folderService.createNewFolderByName(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = fileController.createNewFolderByName(request, session);

        assertEquals("SUCCESS", result);
        verify(folderService, times(1)).createNewFolderByName(request);
    }

    @Test
    void testCreateNewFolderByName_Failure() {
        when(folderService.createNewFolderByName(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = fileController.createNewFolderByName(request, session);

        assertEquals("FAILURE", result);
        verify(folderService, times(1)).createNewFolderByName(request);
    }

    @Test
    void testDeleteFile_Success() {
        when(fileService.deleteFile(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = fileController.deleteFile(request, session);

        assertEquals("SUCCESS", result);
        verify(fileService, times(1)).deleteFile(request);
    }

    @Test
    void testDeleteFile_Failure() {
        when(fileService.deleteFile(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = fileController.deleteFile(request, session);

        assertEquals("FAILURE", result);
        verify(fileService, times(1)).deleteFile(request);
    }

    @Test
    void testDownloadFile_Success() {
        doNothing().when(fileService).doDownloadFile(any(HttpServletRequest.class), any(HttpServletResponse.class));

        assertDoesNotThrow(() -> {
            fileController.downloadFile(request, response, session);
        });
        verify(fileService, times(1)).doDownloadFile(request, response);
    }

    @Test
    void testDownloadFile_Exception() {
        doThrow(new RuntimeException("download error")).when(fileService)
                .doDownloadFile(any(HttpServletRequest.class), any(HttpServletResponse.class));

        assertThrows(RuntimeException.class, () -> {
            fileController.downloadFile(request, response, session);
        });
        verify(fileService, times(1)).doDownloadFile(request, response);
    }

    @Test
    void testRenameFile_Success() {
        when(fileService.doRenameFile(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = fileController.renameFile(request, session);

        assertEquals("SUCCESS", result);
        verify(fileService, times(1)).doRenameFile(request);
    }

    @Test
    void testRenameFile_Failure() {
        when(fileService.doRenameFile(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = fileController.renameFile(request, session);

        assertEquals("FAILURE", result);
        verify(fileService, times(1)).doRenameFile(request);
    }

    @Test
    void testDeleteCheckedFiles_Success() {
        when(fileService.deleteCheckedFiles(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = fileController.deleteCheckedFiles(request, session);

        assertEquals("SUCCESS", result);
        verify(fileService, times(1)).deleteCheckedFiles(request);
    }

    @Test
    void testDeleteCheckedFiles_Failure() {
        when(fileService.deleteCheckedFiles(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = fileController.deleteCheckedFiles(request, session);

        assertEquals("FAILURE", result);
        verify(fileService, times(1)).deleteCheckedFiles(request);
    }

    @Test
    void testGetPackTime_Success() {
        when(fileService.getPackTime(any(HttpServletRequest.class))).thenReturn("{\"time\":123456}");

        String result = fileController.getPackTime(request, session);

        assertEquals("{\"time\":123456}", result);
        verify(fileService, times(1)).getPackTime(request);
    }

    @Test
    void testGetPackTime_Empty() {
        when(fileService.getPackTime(any(HttpServletRequest.class))).thenReturn("");

        String result = fileController.getPackTime(request, session);

        assertEquals("", result);
        verify(fileService, times(1)).getPackTime(request);
    }

    @Test
    void testDownloadCheckedFiles_Success() {
        when(fileService.downloadCheckedFiles(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = fileController.downloadCheckedFiles(request, session);

        assertEquals("SUCCESS", result);
        verify(fileService, times(1)).downloadCheckedFiles(request);
    }

    @Test
    void testDownloadCheckedFiles_Failure() {
        when(fileService.downloadCheckedFiles(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = fileController.downloadCheckedFiles(request, session);

        assertEquals("FAILURE", result);
        verify(fileService, times(1)).downloadCheckedFiles(request);
    }

    @Test
    void testDownloadCheckedFilesZip_Success() throws Exception {
        doNothing().when(fileService).downloadCheckedFilesZip(any(HttpServletRequest.class), any(HttpServletResponse.class));

        assertDoesNotThrow(() -> {
            fileController.downloadCheckedFilesZip(request, response, session);
        });
        verify(fileService, times(1)).downloadCheckedFilesZip(request, response);
    }

    @Test
    void testDownloadCheckedFilesZip_Exception() throws Exception {
        doThrow(new Exception("zip error")).when(fileService)
                .downloadCheckedFilesZip(any(HttpServletRequest.class), any(HttpServletResponse.class));

        assertThrows(Exception.class, () -> {
            fileController.downloadCheckedFilesZip(request, response, session);
        });
        verify(fileService, times(1)).downloadCheckedFilesZip(request, response);
    }

    @Test
    void testConfirmMoveFiles_Success() {
        when(fileService.confirmMoveFiles(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = fileController.confirmMoveFiles(request, session);

        assertEquals("SUCCESS", result);
        verify(fileService, times(1)).confirmMoveFiles(request);
    }

    @Test
    void testConfirmMoveFiles_Failure() {
        when(fileService.confirmMoveFiles(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = fileController.confirmMoveFiles(request, session);

        assertEquals("FAILURE", result);
        verify(fileService, times(1)).confirmMoveFiles(request);
    }

    @Test
    void testMoveCheckedFiles_Success() {
        when(fileService.doMoveFiles(any(HttpServletRequest.class))).thenReturn("SUCCESS");

        String result = fileController.moveCheckedFiles(request, session);

        assertEquals("SUCCESS", result);
        verify(fileService, times(1)).doMoveFiles(request);
    }

    @Test
    void testMoveCheckedFiles_Failure() {
        when(fileService.doMoveFiles(any(HttpServletRequest.class))).thenReturn("FAILURE");

        String result = fileController.moveCheckedFiles(request, session);

        assertEquals("FAILURE", result);
        verify(fileService, times(1)).doMoveFiles(request);
    }

    @Test
    void testSreachInCompletePath_Success() {
        when(folderViewService.getSearchViewJson(any(HttpServletRequest.class))).thenReturn("{\"results\":[]}");

        String result = fileController.sreachInCompletePath(request, session);

        assertEquals("{\"results\":[]}", result);
        verify(folderViewService, times(1)).getSearchViewJson(request);
    }

    @Test
    void testSreachInCompletePath_WithResults() {
        String searchResult = "{\"results\":[{\"name\":\"file1.txt\"},{\"name\":\"file2.txt\"}]}";
        when(folderViewService.getSearchViewJson(any(HttpServletRequest.class))).thenReturn(searchResult);

        String result = fileController.sreachInCompletePath(request, session);

        assertEquals(searchResult, result);
        verify(folderViewService, times(1)).getSearchViewJson(request);
    }
}
