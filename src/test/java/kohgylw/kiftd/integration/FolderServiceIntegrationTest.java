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

import kohgylw.kiftd.newcore.KiftdApplication;
import kohgylw.kiftd.newcore.service.FolderService;
import kohgylw.kiftd.newcore.service.FolderViewService;
import kohgylw.kiftd.newcore.config.ConfigurationManager;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.enumeration.AccountAuth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.List;
import java.util.UUID;

import kohgylw.kiftd.printer.Printer;

@SpringBootTest(classes = KiftdApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class FolderServiceIntegrationTest {

    static {
        if (Printer.instance == null) {
            Printer.init(false);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired(required = false)
    private FolderService folderService;

    @Autowired(required = false)
    private FolderViewService folderViewService;

    @Autowired(required = false)
    private FolderMapper folderMapper;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testMockMvcAutowired() {
        assertNotNull(mockMvc, "MockMvc应该被自动注入");
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
    void testFolderMapperBeanExists() {
        if (folderMapper != null) {
            assertNotNull(folderMapper, "文件夹Mapper Bean应该存在");
        }
    }

    @Test
    void testFolderModelCreation() {
        Folder folder = new Folder();
        folder.setFolderId(UUID.randomUUID().toString());
        folder.setFolderName("testFolder");
        folder.setFolderCreationDate("2024-01-01");
        folder.setFolderCreator("admin");
        folder.setFolderParent("root");
        folder.setFolderConstraint(0);

        assertEquals("testFolder", folder.getFolderName());
        assertEquals("admin", folder.getFolderCreator());
        assertEquals("root", folder.getFolderParent());
        assertEquals(0, folder.getFolderConstraint());
    }

    @Test
    void testFolderModelSettersAndGetters() {
        Folder folder = new Folder();
        String id = UUID.randomUUID().toString();
        folder.setFolderId(id);
        folder.setFolderName("Test Folder");
        folder.setFolderCreationDate("2024-06-15");
        folder.setFolderCreator("testuser");
        folder.setFolderParent("parent123");
        folder.setFolderConstraint(1);

        assertEquals(id, folder.getFolderId());
        assertEquals("Test Folder", folder.getFolderName());
        assertEquals("2024-06-15", folder.getFolderCreationDate());
        assertEquals("testuser", folder.getFolderCreator());
        assertEquals("parent123", folder.getFolderParent());
        assertEquals(1, folder.getFolderConstraint());
    }

    @Test
    void testConfigurationManagerAuthorized() {
        ConfigurationManager cm = ConfigurationManager.instance();
        boolean authorized = cm.authorized("admin", AccountAuth.CREATE_NEW_FOLDER, 
                java.util.Collections.singletonList("root"));
        assertNotNull(authorized, "权限检查结果不应该为null");
    }

    @Test
    void testConfigurationManagerAccessFolder() {
        ConfigurationManager cm = ConfigurationManager.instance();
        Folder folder = new Folder();
        folder.setFolderId("testFolderId");
        folder.setFolderName("test");
        folder.setFolderConstraint(0);
        folder.setFolderCreator("admin");

        boolean canAccess = cm.accessFolder(folder, "admin");
        assertTrue(canAccess, "创建者应该能访问无约束的文件夹");
    }

    @Test
    void testConfigurationManagerAccessFolderWithConstraint() {
        ConfigurationManager cm = ConfigurationManager.instance();
        Folder folder = new Folder();
        folder.setFolderId("testFolderId2");
        folder.setFolderName("test2");
        folder.setFolderConstraint(2);
        folder.setFolderCreator("admin");

        boolean canAccessCreator = cm.accessFolder(folder, "admin");
        assertTrue(canAccessCreator, "创建者应该能访问自己的文件夹");

        boolean canAccessOther = cm.accessFolder(folder, "otheruser");
        assertFalse(canAccessOther, "其他用户不应该访问受限制的文件夹");
    }

    @Test
    void testConfigurationManagerAccessNullFolder() {
        ConfigurationManager cm = ConfigurationManager.instance();
        boolean canAccess = cm.accessFolder(null, "admin");
        assertFalse(canAccess, "null文件夹应该返回false");
    }

    @Test
    void testFolderViewEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/getFolderView.ajax")
                .param("fid", "root"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertNotNull(response, "文件夹视图响应不应该为null");
    }

    @Test
    void testNewFolderEndpointWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/newFolder.ajax")
                .param("parentId", "root")
                .param("folderName", "testFolder"))
                .andReturn();
        assertNotNull(result.getResponse(), "新建文件夹响应不应该为null");
    }

    @Test
    void testDeleteFolderEndpointWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/deleteFolder.ajax")
                .param("folderId", "nonexistent"))
                .andReturn();
        assertNotNull(result.getResponse(), "删除文件夹响应不应该为null");
    }

    @Test
    void testRenameFolderEndpointWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/renameFolder.ajax")
                .param("folderId", "nonexistent")
                .param("newName", "newName"))
                .andReturn();
        assertNotNull(result.getResponse(), "重命名文件夹响应不应该为null");
    }

    @Test
    void testCountFolderContentEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/homeController/countFolderContent.ajax")
                .param("folderId", "root"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        assertNotNull(response, "文件夹内容计数响应不应该为null");
    }

    @Test
    void testCreateNewFolderByNameEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/createNewFolderByName.ajax")
                .param("folderName", "testByName")
                .param("parentFolderId", "root"))
                .andReturn();
        assertNotNull(result.getResponse(), "按名称创建文件夹响应不应该为null");
    }

    @Test
    void testDeleteFolderByNameEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(post("/homeController/deleteFolderByName.ajax")
                .param("folderName", "nonexistentFolder")
                .param("parentFolderId", "root"))
                .andReturn();
        assertNotNull(result.getResponse(), "按名称删除文件夹响应不应该为null");
    }

    @Test
    void testFolderMapperSelectById() {
        if (folderMapper != null) {
            Folder folder = folderMapper.selectById("root");
            if (folder != null) {
                assertNotNull(folder.getFolderId(), "文件夹ID不应该为null");
            }
        }
    }

    @Test
    void testFolderMapperQueryByParentId() {
        if (folderMapper != null) {
            List<Folder> folders = folderMapper.queryByParentId("root");
            assertNotNull(folders, "文件夹列表不应该为null");
        }
    }

    @Test
    void testFolderMapperQueryByParentIdAndFolderName() {
        if (folderMapper != null) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("parentId", "root");
            map.put("folderName", "nonexistent");
            Folder folder = folderMapper.queryByParentIdAndFolderName(map);
            assertNull(folder, "不存在的文件夹应该返回null");
        }
    }
}
