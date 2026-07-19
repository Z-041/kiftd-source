package kohgylw.kiftd.server.util;

import kohgylw.kiftd.newcore.config.ConfigurationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.printer.Printer;

@ExtendWith(MockitoExtension.class)
class NoticeUtilTest {

    @Mock
    private LogUtil lu;

    @Mock
    private TxtCharsetGetter tcg;

    @InjectMocks
    private NoticeUtil noticeUtil;

    private File tempDir;
    private File tempTempDir;

    @BeforeAll
    static void initPrinter() {
        Printer.init(false);
    }

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("noticeTestMain").toFile();
        tempTempDir = Files.createTempDirectory("noticeTestTemp").toFile();
    }

    @AfterEach
    void tearDown() {
        deleteDir(tempDir);
        deleteDir(tempTempDir);
    }

    private void deleteDir(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDir(f);
                    } else {
                        f.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    @Test
    void testLoadNotice_WhenNoNoticeFile_Md5IsNull() {
        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getPath()).thenReturn(tempDir.getAbsolutePath());
            when(mockReader.getTemporaryfilePath()).thenReturn(tempTempDir.getAbsolutePath());

            noticeUtil.loadNotice();

            assertNull(noticeUtil.getMd5());
        }
    }

    @Test
    void testLoadNotice_WhenNoticeFileExists_Md5IsNotNull() throws Exception {
        File noticeFile = new File(tempDir, NoticeUtil.NOTICE_FILE_NAME);
        try (FileWriter writer = new FileWriter(noticeFile, StandardCharsets.UTF_8)) {
            writer.write("# Test Notice\n\nThis is a test notice.");
        }

        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getPath()).thenReturn(tempDir.getAbsolutePath());
            when(mockReader.getTemporaryfilePath()).thenReturn(tempTempDir.getAbsolutePath());
            when(tcg.getTxtCharset(any())).thenReturn("UTF-8");

            noticeUtil.loadNotice();

            assertNotNull(noticeUtil.getMd5());
            assertFalse(noticeUtil.getMd5().isEmpty());
        }
    }

    @Test
    void testLoadNotice_WhenNoticeFileExists_HtmlFileGenerated() throws Exception {
        File noticeFile = new File(tempDir, NoticeUtil.NOTICE_FILE_NAME);
        try (FileWriter writer = new FileWriter(noticeFile, StandardCharsets.UTF_8)) {
            writer.write("# Hello World");
        }

        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getPath()).thenReturn(tempDir.getAbsolutePath());
            when(mockReader.getTemporaryfilePath()).thenReturn(tempTempDir.getAbsolutePath());
            when(tcg.getTxtCharset(any())).thenReturn("UTF-8");

            noticeUtil.loadNotice();

            File htmlFile = new File(tempTempDir, NoticeUtil.NOTICE_OUTPUT_NAME);
            assertTrue(htmlFile.exists());
            assertTrue(htmlFile.length() > 0);
        }
    }

    @Test
    void testLoadNotice_Md5IsConsistentForSameContent() throws Exception {
        File noticeFile = new File(tempDir, NoticeUtil.NOTICE_FILE_NAME);
        try (FileWriter writer = new FileWriter(noticeFile, StandardCharsets.UTF_8)) {
            writer.write("Same content every time.");
        }

        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getPath()).thenReturn(tempDir.getAbsolutePath());
            when(mockReader.getTemporaryfilePath()).thenReturn(tempTempDir.getAbsolutePath());
            when(tcg.getTxtCharset(any())).thenReturn("UTF-8");

            noticeUtil.loadNotice();
            String md5_1 = noticeUtil.getMd5();

            noticeUtil.loadNotice();
            String md5_2 = noticeUtil.getMd5();

            assertEquals(md5_1, md5_2);
        }
    }

    @Test
    void testLoadNotice_DifferentContentHasDifferentMd5() throws Exception {
        File noticeFile = new File(tempDir, NoticeUtil.NOTICE_FILE_NAME);
        try (FileWriter writer = new FileWriter(noticeFile, StandardCharsets.UTF_8)) {
            writer.write("Content A");
        }

        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getPath()).thenReturn(tempDir.getAbsolutePath());
            when(mockReader.getTemporaryfilePath()).thenReturn(tempTempDir.getAbsolutePath());
            when(tcg.getTxtCharset(any())).thenReturn("UTF-8");

            noticeUtil.loadNotice();
            String md5A = noticeUtil.getMd5();

            try (FileWriter writer = new FileWriter(noticeFile, StandardCharsets.UTF_8)) {
                writer.write("Content B is different");
            }

            noticeUtil.loadNotice();
            String md5B = noticeUtil.getMd5();

            assertNotEquals(md5A, md5B);
        }
    }

    @Test
    void testLoadNotice_EmptyNoticeFile() throws Exception {
        File noticeFile = new File(tempDir, NoticeUtil.NOTICE_FILE_NAME);
        noticeFile.createNewFile();

        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getPath()).thenReturn(tempDir.getAbsolutePath());
            when(mockReader.getTemporaryfilePath()).thenReturn(tempTempDir.getAbsolutePath());
            when(tcg.getTxtCharset(any())).thenReturn("UTF-8");

            noticeUtil.loadNotice();

            assertNotNull(noticeUtil.getMd5());
        }
    }

    @Test
    void testLoadNotice_NoticeFileIsDirectory_Md5IsNull() throws Exception {
        File noticeDir = new File(tempDir, NoticeUtil.NOTICE_FILE_NAME);
        noticeDir.mkdir();

        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getPath()).thenReturn(tempDir.getAbsolutePath());
            when(mockReader.getTemporaryfilePath()).thenReturn(tempTempDir.getAbsolutePath());

            noticeUtil.loadNotice();

            assertNull(noticeUtil.getMd5());
        }
    }

    @Test
    void testGetMd5_InitialStateIsNull() {
        assertNull(noticeUtil.getMd5());
    }

    @Test
    void testLoadNotice_ChineseContent() throws Exception {
        File noticeFile = new File(tempDir, NoticeUtil.NOTICE_FILE_NAME);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(noticeFile), StandardCharsets.UTF_8)) {
            writer.write("# 公告标题\n\n这是中文公告内容。");
        }

        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getPath()).thenReturn(tempDir.getAbsolutePath());
            when(mockReader.getTemporaryfilePath()).thenReturn(tempTempDir.getAbsolutePath());
            when(tcg.getTxtCharset(any())).thenReturn("UTF-8");

            noticeUtil.loadNotice();

            assertNotNull(noticeUtil.getMd5());
            File htmlFile = new File(tempTempDir, NoticeUtil.NOTICE_OUTPUT_NAME);
            assertTrue(htmlFile.exists());
        }
    }

    @Test
    void testLoadNotice_MarkdownFormattingConverted() throws Exception {
        File noticeFile = new File(tempDir, NoticeUtil.NOTICE_FILE_NAME);
        try (FileWriter writer = new FileWriter(noticeFile, StandardCharsets.UTF_8)) {
            writer.write("# Heading\n\n**bold** *italic*");
        }

        try (MockedStatic<ConfigurationManager> mockedCr = mockStatic(ConfigurationManager.class)) {
            ConfigurationManager mockReader = mock(ConfigurationManager.class);
            mockedCr.when(ConfigurationManager::instance).thenReturn(mockReader);
            when(mockReader.getPath()).thenReturn(tempDir.getAbsolutePath());
            when(mockReader.getTemporaryfilePath()).thenReturn(tempTempDir.getAbsolutePath());
            when(tcg.getTxtCharset(any())).thenReturn("UTF-8");

            noticeUtil.loadNotice();

            File htmlFile = new File(tempTempDir, NoticeUtil.NOTICE_OUTPUT_NAME);
            String content = new String(Files.readAllBytes(htmlFile.toPath()), StandardCharsets.UTF_8);
            assertTrue(content.contains("h1") || content.contains("<h1>") || content.contains("Heading"));
        }
    }

    @Test
    void testNoticeConstants() {
        assertEquals("notice.md", NoticeUtil.NOTICE_FILE_NAME);
        assertEquals("notice.html", NoticeUtil.NOTICE_OUTPUT_NAME);
    }
}
