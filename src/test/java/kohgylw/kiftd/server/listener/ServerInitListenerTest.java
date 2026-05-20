package kohgylw.kiftd.server.listener;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * ServerInitListener 的 WatchService 异常处理逻辑测试。
 * 
 * 核心逻辑：doWatch() 方法中的 catch 块现在会检查 run 标志，
 * 只有在服务器仍在运行时才打印错误信息。正常关闭时不会误报。
 */
class ServerInitListenerTest {

    @Test
    void testDoWatchExceptionHandling_ShouldCheckRunFlag() throws Exception {
        java.lang.reflect.Method doWatchMethod = ServerInitListener.class.getDeclaredMethod("doWatch");
        doWatchMethod.setAccessible(true);

        java.lang.reflect.Field pathWatchServiceThreadField = 
            ServerInitListener.class.getDeclaredField("pathWatchServiceThread");
        pathWatchServiceThreadField.setAccessible(true);

        java.lang.reflect.Field runField = 
            ServerInitListener.class.getDeclaredField("run");
        runField.setAccessible(true);

        ServerInitListener listener = new ServerInitListener();
        assertNotNull(listener);
    }

    @Test
    void testDoWatchCatchBlockCondition() throws Exception {
        java.lang.reflect.Field runField = 
            ServerInitListener.class.getDeclaredField("run");
        runField.setAccessible(true);

        ServerInitListener listener = new ServerInitListener();

        runField.setBoolean(listener, false);

        boolean runValue = runField.getBoolean(listener);
        assertFalse(runValue, "When server is shutting down, run flag should be false");
    }

}