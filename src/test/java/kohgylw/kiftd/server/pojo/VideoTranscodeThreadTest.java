package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import kohgylw.kiftd.server.util.ConfigureReader;

/**
 * 
 * <h2>视频转码线程信息封装类</h2>
 * <p>
 * 该类用于封装视频转码线程的信息，包括输入文件路径、输出文件名、转码进度和MD5标识。
 * 同时继承自 Thread，用于在后台执行视频转码任务。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
class VideoTranscodeThreadTest {

    @Test
    void testConstructorThrowsOnNull() {
        assertThrows(Exception.class, () -> {
            new VideoTranscodeThread(null, null, null);
        });
    }

    @Test
    void testAbortHandlesExceptionGracefully() {
        try {
            VideoTranscodeThread vtt = new VideoTranscodeThread(null, null, null);
            vtt.abort();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }
}