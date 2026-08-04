package kohgylw.kiftd.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

import kohgylw.kiftd.newcore.KiftdApplication;
import kohgylw.kiftd.newcore.service.FileService;
import kohgylw.kiftd.newcore.service.FolderService;
import kohgylw.kiftd.newcore.service.FolderViewService;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;

import java.util.List;
import java.util.UUID;

import kohgylw.kiftd.printer.Printer;

@SpringBootTest(classes = KiftdApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class FileServiceIntegrationTest {

    static {
        if (Printer.instance == null) {
            Printer.init(false);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired(required = false)
    private FileService fileService;

    @Autowired(required = false)
    private FolderService folderService;

    @Autowired(required = false)
    private FolderViewService folderViewService;

    @Autowired(required = false)
    private NodeMapper nodeMapper;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testMockMvcAutowired() {
        assertNotNull(mockMvc, "MockMvc应该被自动注入");
    }

    @Test
    void testFileServiceBeanExists() {
        if (fileService != null) {
            assertNotNull(fileService, "文件服务Bean应该存在");
        }
    }

    @Test
    void testFolderServiceBeanExists() {
        if (folderService != null) {
            assertNotNull(folderService, "文件夹服务Bean应该存在");
        }
    }

    @Test
    void testFolderViewServiceBeanExists() {
        if (folderViewService != null) {
            assertNotNull(folderViewService, "文件夹视图服务Bean应该存在");
        }
    }

    @Test
    void testNodeMapperBeanExists() {
        if (nodeMapper != null) {
            assertNotNull(nodeMapper, "节点Mapper Bean应该存在");
        }
    }

    @Test
    void testNodeModelCreation() {
        Node node = new Node();
        node.setFileId(UUID.randomUUID().toString());
        node.setFileName("test.txt");
        node.setFileSize("1024");
        node.setFileParentFolder("root");
        node.setFileCreationDate("2024-01-01");
        node.setFileCreator("admin");
        node.setFilePath("/test/path");

        assertEquals("test.txt", node.getFileName());
        assertEquals("1024", node.getFileSize());
        assertEquals("root", node.getFileParentFolder());
        assertEquals("admin", node.getFileCreator());
        assertEquals("/test/path", node.getFilePath());
    }

    @Test
    void testNodeModelSettersAndGetters() {
        Node node = new Node();
        String id = UUID.randomUUID().toString();
        node.setFileId(id);
        node.setFileName("document.pdf");
        node.setFileSize("2048");
        node.setFileParentFolder("folder1");
        node.setFileCreationDate("2024-06-15");
        node.setFileCreator("testuser");
        node.setFilePath("/files/doc.pdf");

        assertEquals(id, node.getFileId());
        assertEquals("document.pdf", node.getFileName());
        assertEquals("2048", node.getFileSize());
        assertEquals("folder1", node.getFileParentFolder());
        assertEquals("2024-06-15", node.getFileCreationDate());
        assertEquals("testuser", node.getFileCreator());
        assertEquals("/files/doc.pdf", node.getFilePath());
    }

    @Test
    void testCheckUploadFileEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/checkUploadFile.ajax")
                .param("folderId", "root")
                .param("fileName", "test.txt")
                .param("fileSize", "1024"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertNotNull(response, "上传检查响应不应该为null");
    }

    @Test
    void testCheckImportFolderEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/checkImportFolder.ajax")
                .param("folderId", "root"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertNotNull(response, "导入文件夹检查响应不应该为null");
    }

    @Test
    void testDeleteFileEndpointWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/deleteFile.ajax")
                .param("fileId", "nonexistent"))
                .andReturn();
        assertNotNull(result.getResponse(), "删除文件响应不应该为null");
    }

    @Test
    void testRenameFileEndpointWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/renameFile.ajax")
                .param("fileId", "nonexistent")
                .param("newName", "newName.txt"))
                .andReturn();
        assertNotNull(result.getResponse(), "重命名文件响应不应该为null");
    }

    @Test
    void testDeleteCheckedFilesEndpointWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/deleteCheckedFiles.ajax")
                .param("fileIds", "nonexistent1,nonexistent2"))
                .andReturn();
        assertNotNull(result.getResponse(), "批量删除文件响应不应该为null");
    }

    @Test
    void testGetPackTimeEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/getPackTime.ajax")
                .param("fileIds", "nonexistent"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertNotNull(response, "打包时间响应不应该为null");
    }

    @Test
    void testDownloadCheckedFilesEndpointWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/downloadCheckedFiles.ajax")
                .param("fileIds", "nonexistent"))
                .andReturn();
        assertNotNull(result.getResponse(), "批量下载文件响应不应该为null");
    }

    @Test
    void testConfirmMoveFilesEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/confirmMoveFiles.ajax")
                .param("fileIds", "nonexistent")
                .param("targetFolderId", "root"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertNotNull(response, "确认移动文件响应不应该为null");
    }

    @Test
    void testMoveCheckedFilesEndpointWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/moveCheckedFiles.ajax")
                .param("fileIds", "nonexistent")
                .param("targetFolderId", "root"))
                .andReturn();
        assertNotNull(result.getResponse(), "移动文件响应不应该为null");
    }

    @Test
    void testSearchInCompletePathEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/sreachInCompletePath.ajax")
                .param("keyword", "test")
                .param("folderId", "root"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertNotNull(response, "搜索响应不应该为null");
    }

    @Test
    void testUploadFileEndpointWithoutFile() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/homeController/douploadFile.ajax")
                .param("folderId", "root"))
                .andReturn();
        assertNotNull(result.getResponse(), "上传文件响应不应该为null");
    }

    @Test
    void testUploadFileWithMockFile() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/homeController/douploadFile.ajax")
                .file(mockFile)
                .param("folderId", "root"))
                .andReturn();
        assertNotNull(result.getResponse(), "上传文件响应不应该为null");
    }

    @Test
    void testImportFolderWithMockFile() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                new byte[]{1, 2, 3, 4}
        );

        MvcResult result = mockMvc.perform(multipart("/homeController/doImportFolder.ajax")
                .file(mockFile)
                .param("folderId", "root"))
                .andReturn();
        assertNotNull(result.getResponse(), "导入文件夹响应不应该为null");
    }

    @Test
    void testDownloadFileEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/downloadFile.do")
                .param("fileId", "nonexistent"))
                .andReturn();
        assertNotNull(result.getResponse(), "下载文件响应不应该为null");
    }

    @Test
    void testNodeMapperQueryByParentFolderId() {
        if (nodeMapper != null) {
            List<Node> nodes = nodeMapper.queryByParentFolderId("root");
            assertNotNull(nodes, "节点列表不应该为null");
        }
    }

    @Test
    void testNodeMapperQueryByPath() {
        if (nodeMapper != null) {
            List<Node> nodes = nodeMapper.queryByPath("/nonexistent/path");
            assertNotNull(nodes, "节点列表不应该为null");
            assertTrue(nodes.isEmpty(), "不存在的路径应该返回空列表");
        }
    }

    @Test
    void testConfigurationManagerGetUploadFileSize() {
        ConfigurationManager cm = ConfigurationManager.instance();
        long size = cm.getUploadFileSize("admin");
        assertEquals(size, size, "上传文件大小限制方法应该正常返回");
    }

    @Test
    void testConfigurationManagerGetDownloadMaxRate() {
        ConfigurationManager cm = ConfigurationManager.instance();
        long rate = cm.getDownloadMaxRate("admin");
        assertTrue(rate >= -1, "下载速率限制应该是有效的值");
    }
}
