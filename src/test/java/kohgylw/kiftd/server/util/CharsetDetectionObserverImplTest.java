package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CharsetDetectionObserverImplTest {

    @Test
    void testInitiallyNull() {
        CharsetDetectionObserverImpl observer = new CharsetDetectionObserverImpl();
        assertNull(observer.getCharset());
    }

    @Test
    void testNotifySetsCharset() {
        CharsetDetectionObserverImpl observer = new CharsetDetectionObserverImpl();
        observer.Notify("UTF-8");
        assertEquals("UTF-8", observer.getCharset());
    }

    @Test
    void testNotifyOverwritesPrevious() {
        CharsetDetectionObserverImpl observer = new CharsetDetectionObserverImpl();
        observer.Notify("GBK");
        assertEquals("GBK", observer.getCharset());
        observer.Notify("UTF-8");
        assertEquals("UTF-8", observer.getCharset());
    }

    @Test
    void testNotifyWithNull() {
        CharsetDetectionObserverImpl observer = new CharsetDetectionObserverImpl();
        observer.Notify(null);
        assertNull(observer.getCharset());
    }

}