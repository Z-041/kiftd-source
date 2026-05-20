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

}