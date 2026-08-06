package kohgylw.kiftd.util.file_system_manager;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.util.ConfigurationManager;

class FileSystemManagerTest {

    private static MockedStatic<ConfigurationManager> mockedCr;
    private static FileSystemManager fsm;

    @BeforeAll
    static void setup() throws Exception {
        Printer.init(false);

        mockedCr = Mockito.mockStatic(ConfigurationManager.class);
        ConfigurationManager mockReader = Mockito.mock(ConfigurationManager.class);
        mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);

        Mockito.when(mockReader.getFileNodePathDriver()).thenReturn("org.h2.Driver");
        Mockito.when(mockReader.getFileNodePathURL()).thenReturn("jdbc:h2:mem:test");
        Mockito.when(mockReader.getFileNodePathUserName()).thenReturn("sa");
        Mockito.when(mockReader.getFileNodePathPassWord()).thenReturn("");

        java.lang.reflect.Field fsmField = FileSystemManager.class.getDeclaredField("fsm");
        fsmField.setAccessible(true);
        fsmField.set(null, null);

        fsm = FileSystemManager.getInstance();
    }

    @AfterAll
    static void teardown() {
        if (mockedCr != null) {
            mockedCr.close();
        }
    }

    @Test
    void testGetFileFormBlocksWithNullFilePath() throws Exception {
        Node node = new Node();
        node.setFileId("test-id");
        node.setFileName("test.txt");
        node.setFilePath(null);

        Method method = FileSystemManager.class.getDeclaredMethod("getFileFromBlocks", Node.class);
        method.setAccessible(true);

        Object result = method.invoke(fsm, node);
        assertNull(result, "When filePath is null, getFileFromBlocks should return null without throwing NPE");
    }

    @Test
	void testGetFileFormBlocksWithValidFilePath() throws Exception {
		Node node = new Node();
		node.setFileId("test-id");
		node.setFileName("test.txt");
		node.setFilePath("file_test-uuid.block");

		Method method = FileSystemManager.class.getDeclaredMethod("getFileFromBlocks", Node.class);
		method.setAccessible(true);

		Object result = method.invoke(fsm, node);
		assertNull(result, "When block file does not exist, should return null (no crash)");
	}

	@Test
	void testGetFileFormBlocksWithPathTraversal() throws Exception {
		Node node = new Node();
		node.setFileId("test-id");
		node.setFileName("test.txt");
		node.setFilePath("file_../../etc/passwd.block");

		Method method = FileSystemManager.class.getDeclaredMethod("getFileFromBlocks", Node.class);
		method.setAccessible(true);

		Object result = method.invoke(fsm, node);
		assertNull(result, "路径穿越格式的文件块索引应被拦截");
	}

}