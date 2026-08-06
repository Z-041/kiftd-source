package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class VariableSpeedBufferedOutputStreamTest {

    @Mock
    private HttpSession session;

    @Test
    void testConstructor() {
        OutputStream os = new ByteArrayOutputStream();
        VariableSpeedBufferedOutputStream vsbos = new VariableSpeedBufferedOutputStream(os, 1024, session);
        assertNotNull(vsbos);
    }

    @Test
    void testWriteWithNegativeMaxRateNoLimit() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        VariableSpeedBufferedOutputStream vsbos = new VariableSpeedBufferedOutputStream(baos, -1, session);
        byte[] data = "hello world".getBytes();
        vsbos.write(data, 0, data.length);
        vsbos.flush();
        assertEquals("hello world", baos.toString());
    }

    @Test
    void testWriteSmallData() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        VariableSpeedBufferedOutputStream vsbos = new VariableSpeedBufferedOutputStream(baos, 102400, session);
        byte[] data = "small data".getBytes();
        vsbos.write(data, 0, data.length);
        vsbos.flush();
        assertEquals("small data", baos.toString());
    }

    @Test
    void testWriteThrowsOnZeroRate() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        VariableSpeedBufferedOutputStream vsbos = new VariableSpeedBufferedOutputStream(baos, 0, session);
        byte[] data = "test".getBytes();
        assertThrows(IllegalArgumentException.class, () -> {
            vsbos.write(data, 0, data.length);
        });
    }

    @Test
    void testWriteMoreThanMaxRateWritesAllData() throws IOException {
        // 一次写入超过每秒预算，触发窗口重置等待路径（PERF-003 重构），全部数据必须完整写出
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        VariableSpeedBufferedOutputStream vsbos = new VariableSpeedBufferedOutputStream(baos, 1024, session);
        byte[] data = new byte[3000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 251);
        }
        vsbos.write(data, 0, data.length);
        vsbos.flush();
        assertArrayEquals(data, baos.toByteArray());
    }

    @Test
    void testConcurrentWritesSharedSessionCompleteAllData() throws Exception {
        // 同一会话下并发下载共享限速预算：wait 释放监视器，两任务必须都能完成且无死锁
        int maxRate = 1024;
        byte[] dataA = new byte[1024];
        byte[] dataB = new byte[1024];
        for (int i = 0; i < dataA.length; i++) {
            dataA[i] = (byte) (i % 251);
            dataB[i] = (byte) ((i + 100) % 251);
        }
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Future<byte[]> futureA = pool.submit(() -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            VariableSpeedBufferedOutputStream vsbos = new VariableSpeedBufferedOutputStream(baos, maxRate, session);
            ready.countDown();
            start.await();
            vsbos.write(dataA, 0, dataA.length);
            vsbos.flush();
            return baos.toByteArray();
        });
        java.util.concurrent.Future<byte[]> futureB = pool.submit(() -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            VariableSpeedBufferedOutputStream vsbos = new VariableSpeedBufferedOutputStream(baos, maxRate, session);
            ready.countDown();
            start.await();
            vsbos.write(dataB, 0, dataB.length);
            vsbos.flush();
            return baos.toByteArray();
        });
        ready.await(5, java.util.concurrent.TimeUnit.SECONDS);
        start.countDown();
        assertArrayEquals(dataA, futureA.get(10, java.util.concurrent.TimeUnit.SECONDS));
        assertArrayEquals(dataB, futureB.get(10, java.util.concurrent.TimeUnit.SECONDS));
        pool.shutdownNow();
    }

}