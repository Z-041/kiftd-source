package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import kohgylw.kiftd.printer.Printer;

class CircuitBreakerTest {

    private CircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        Printer.instance = Mockito.mock(Printer.class);
        breaker = new CircuitBreaker("test", 2, 50, 2);
    }

    @AfterEach
    void tearDown() {
        Printer.instance = null;
    }

    @Test
    void testClosedAllowsRequests() {
        assertTrue(breaker.isRequestAllowed());
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertEquals(0, breaker.getFailureCount());
        assertEquals("test", breaker.getName());
    }

    @Test
    void testOpensAfterThresholdFailures() {
        breaker.recordFailure(new RuntimeException("f1"));
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        breaker.recordFailure(new RuntimeException("f2"));
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertEquals(2, breaker.getFailureCount());
        assertFalse(breaker.isRequestAllowed());
    }

    @Test
    void testHalfOpenAfterRecoveryTimeout() throws Exception {
        breaker.recordFailure(new RuntimeException("f1"));
        breaker.recordFailure(new RuntimeException("f2"));
        Thread.sleep(60);
        assertTrue(breaker.isRequestAllowed());
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.getState());
    }

    @Test
    void testHalfOpenSuccessThresholdCloses() throws Exception {
        breaker.recordFailure(new RuntimeException("f1"));
        breaker.recordFailure(new RuntimeException("f2"));
        Thread.sleep(60);
        breaker.isRequestAllowed();
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.getState());
        breaker.recordSuccess();
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.getState());
        breaker.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    void testHalfOpenFailureReopens() throws Exception {
        breaker.recordFailure(new RuntimeException("f1"));
        breaker.recordFailure(new RuntimeException("f2"));
        Thread.sleep(60);
        assertTrue(breaker.isRequestAllowed());
        breaker.recordFailure(new RuntimeException("again"));
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertFalse(breaker.isRequestAllowed());
    }

    @Test
    void testExecuteSuccessAndFailure() throws Exception {
        Object result = breaker.execute(() -> "done", "op");
        assertEquals("done", result);

        assertThrows(Exception.class, () -> breaker.execute(() -> {
            throw new RuntimeException("boom");
        }, "op"));
        assertThrows(Exception.class, () -> breaker.execute(() -> {
            throw new RuntimeException("boom2");
        }, "op"));
        // 达到失败阈值后，熔断器打开，后续请求直接快速失败，不再执行操作
        assertThrows(RuntimeException.class, () -> breaker.execute(() -> "never", "op"));
        assertFalse(breaker.isRequestAllowed());
    }

    @Test
    void testReset() {
        breaker.recordFailure(new RuntimeException("f1"));
        breaker.reset();
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertEquals(0, breaker.getFailureCount());
        assertTrue(breaker.isRequestAllowed());
    }

    @Test
    void testInvalidConstructorArgsFallback() {
        CircuitBreaker b = new CircuitBreaker("x", -1, -1, -1);
        assertTrue(b.isRequestAllowed());
        assertEquals("x", b.getName());
    }

    @Test
    void testConstructorWithoutHalfOpenThreshold() {
        CircuitBreaker b = new CircuitBreaker("y", 3, 1000);
        assertTrue(b.isRequestAllowed());
    }

}
