package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    @Test
    void testHashAndVerifyPassword() {
        String password = "mySecret123";
        String hashed = PasswordUtil.hashPassword(password);
        assertTrue(PasswordUtil.isPasswordHashed(hashed));
        assertTrue(PasswordUtil.verifyPassword(password, hashed));
        assertFalse(PasswordUtil.verifyPassword("wrongPassword", hashed));
    }

    @Test
    void testVerifyPlaintextPassword() {
        assertTrue(PasswordUtil.verifyPassword("plain", "plain"));
        assertFalse(PasswordUtil.verifyPassword("plain", "different"));
    }

    @Test
    void testVerifyNullStoredDoesNotThrow() {
        // 应执行 dummy 派生后返回 false，而不是立即返回
        assertFalse(PasswordUtil.verifyPassword("any", null));
    }

    @Test
    void testVerifyNullPassword() {
        String hashed = PasswordUtil.hashPassword("");
        assertTrue(PasswordUtil.verifyPassword(null, hashed));
        assertFalse(PasswordUtil.verifyPassword(null, "plaintext"));
    }

    @Test
    void testMalformedHashReturnsFalse() {
        assertFalse(PasswordUtil.verifyPassword("pwd", "PBKDF2$malformed"));
    }

    @Test
    void testHashesAreRandomized() {
        String password = "samePassword";
        String hash1 = PasswordUtil.hashPassword(password);
        String hash2 = PasswordUtil.hashPassword(password);
        assertNotEquals(hash1, hash2);
        assertTrue(PasswordUtil.verifyPassword(password, hash1));
        assertTrue(PasswordUtil.verifyPassword(password, hash2));
    }

    @Test
    void testInvalidBase64CharactersDoNotThrow() {
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$invalid\\chars$hash"));
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$salt$invalid\\hash"));
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$\\x00\\x01$hash"));
    }

    @Test
    void testMalformedBase64PaddingDoesNotThrow() {
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$abc$def"));
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$===salt$hash"));
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$salt$===hash"));
    }

    @Test
    void testIncorrectSaltLengthDoesNotThrow() {
        String shortSalt = "dGVzdA==";
        String normalHash = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=";
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$" + shortSalt + "$" + normalHash));
    }

    @Test
    void testIncorrectHashLengthDoesNotThrow() {
        String normalSalt = "YWJjZGVmZ2hpamtsbW5vcA==";
        String shortHash = "dGVzdA==";
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$" + normalSalt + "$" + shortHash));
    }

    @Test
    void testEmptyHashPartsDoNotThrow() {
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$$"));
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$salt$"));
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$$hash"));
    }

    @Test
    void testSpecialCharactersInStoredPassword() {
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$salt/hash+value$more"));
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$   $   "));
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$\n\t\r$bad"));
    }

    @Test
    void testMultipleDollarSigns() {
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$a$b$c$d"));
        assertFalse(PasswordUtil.verifyPassword("testpwd", "PBKDF2$salt$hash$extra"));
    }

    @Test
    void testTimingSafetyWithMalformedHashes() {
        String password = "testPassword123";
        String goodHash = PasswordUtil.hashPassword(password);
        
        long startTime = System.nanoTime();
        PasswordUtil.verifyPassword(password, goodHash);
        long goodTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        PasswordUtil.verifyPassword(password, "PBKDF2$bad\\chars$hash");
        long badTime = System.nanoTime() - startTime;
        
        assertTrue(badTime > goodTime * 0.1);
    }
}
