package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import kohgylw.kiftd.printer.Printer;

class RetryUtilTest {

    @BeforeEach
    void setUp() {
        Printer.instance = Mockito.mock(Printer.class);
    }

    @AfterEach
    void tearDown() {
        Printer.instance = null;
    }

    @Test
    void testSuccessOnFirstAttempt() {
        String result = RetryUtil.executeWithRetry(() -> "ok", "test-op", 3, 100, 2.0);
        assertEquals("ok", result);
    }

    @Test
    void testSuccessAfterRetries() {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = RetryUtil.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("fail");
            }
            return "ok";
        }, "test-op", 3, 1, 2.0);
        assertEquals("ok", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void testExhaustRetriesReturnsNull() {
        String result = RetryUtil.executeWithRetry(() -> {
            throw new RuntimeException("always fail");
        }, "test-op", 2, 1, 2.0);
        assertNull(result);
    }

    @Test
    void testInvalidParamsFallbackToDefaults() {
        String result = RetryUtil.executeWithRetry(() -> "ok", "test-op", 0, 0, 1.0);
        assertEquals("ok", result);
    }

}
