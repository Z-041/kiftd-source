package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

class VerificationCodeFactoryTest {

    @Test
    void testFactoryConstructorValid() {
        assertDoesNotThrow(() -> {
            new VerificationCodeFactory(30, 5, 3, 'A', 'B', 'C', '1', '2', '3');
        });
    }

    @Test
    void testFactoryConstructorInvalidCharSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            new VerificationCodeFactory(0, 5, 3, 'A', 'B');
        });
    }

    @Test
    void testFactoryConstructorNegativeMaxLine() {
        assertThrows(IllegalArgumentException.class, () -> {
            new VerificationCodeFactory(30, -1, 3, 'A', 'B');
        });
    }

    @Test
    void testFactoryConstructorNoAlternative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new VerificationCodeFactory(30, 5, 3);
        });
    }

    @Test
    void testFactoryNextInvalidLength() {
        VerificationCodeFactory factory = new VerificationCodeFactory(30, 5, 3, 'A', 'B');
        assertThrows(IllegalArgumentException.class, () -> {
            factory.next(0);
        });
    }

    @Test
    void testFactoryNextValidLength() {
        VerificationCodeFactory factory = new VerificationCodeFactory(30, 5, 3, 'A', 'B', 'C', 'D');
        VerificationCode code = factory.next(4);
        assertNotNull(code);
        assertEquals(4, code.getCode().length());
    }

    @Test
    void testFactoryNextProducesImage() {
        VerificationCodeFactory factory = new VerificationCodeFactory(30, 5, 3, '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9');
        VerificationCode code = factory.next(4);
        assertNotNull(code.getImage());
        assertTrue(code.getImage().getWidth() > 0);
        assertTrue(code.getImage().getHeight() > 0);
    }

    @Test
    void testCodeToLowerCase() {
        VerificationCode code = new VerificationCode();
        code.setCode("AbCd");
        assertEquals("abcd", code.getCode());
    }

    @Test
    void testCodeAllUpperCase() {
        VerificationCode code = new VerificationCode();
        code.setCode("ABCD");
        assertEquals("abcd", code.getCode());
    }

    @Test
    void testCodeNumbersOnly() {
        VerificationCode code = new VerificationCode();
        code.setCode("1234");
        assertEquals("1234", code.getCode());
    }

    @Test
    void testVerifyCodeSaveToOutputStream() throws Exception {
        VerificationCode code = new VerificationCode();
        code.setCode("test");
        BufferedImage image = new BufferedImage(100, 40, BufferedImage.TYPE_INT_RGB);
        code.setImage(image);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> code.saveTo(baos));
        assertTrue(baos.size() > 0);
    }

    @Test
    void testFactoryWithSimplifiedChars() {
        VerificationCodeFactory factory = new VerificationCodeFactory(24, 3, 2,
                '2', '3', '4', '5', '6', '7', '8', '9');
        VerificationCode code = factory.next(6);
        assertEquals(6, code.getCode().length());
        assertFalse(code.getCode().contains("0"));
        assertFalse(code.getCode().contains("1"));
    }

}