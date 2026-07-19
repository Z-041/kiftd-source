package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SignUpInfoPojoTest {

    @Test
    void testDefaultValuesAreNull() {
        SignUpInfoPojo signUpInfo = new SignUpInfoPojo();
        assertNull(signUpInfo.getAccount());
        assertNull(signUpInfo.getPwd());
        assertNull(signUpInfo.getTime());
    }

    @Test
    void testSetAndGetAllFields() {
        SignUpInfoPojo signUpInfo = new SignUpInfoPojo();
        String account = "newUser";
        String pwd = "newPassword";
        String time = "2026-01-15 10:30:00";

        signUpInfo.setAccount(account);
        signUpInfo.setPwd(pwd);
        signUpInfo.setTime(time);

        assertEquals(account, signUpInfo.getAccount());
        assertEquals(pwd, signUpInfo.getPwd());
        assertEquals(time, signUpInfo.getTime());
    }

    @Test
    void testSetNullValues() {
        SignUpInfoPojo signUpInfo = new SignUpInfoPojo();
        signUpInfo.setAccount("test");
        signUpInfo.setPwd("test");
        signUpInfo.setTime("test");

        signUpInfo.setAccount(null);
        signUpInfo.setPwd(null);
        signUpInfo.setTime(null);

        assertNull(signUpInfo.getAccount());
        assertNull(signUpInfo.getPwd());
        assertNull(signUpInfo.getTime());
    }

    @Test
    void testEmptyStringValues() {
        SignUpInfoPojo signUpInfo = new SignUpInfoPojo();
        signUpInfo.setAccount("");
        signUpInfo.setPwd("");
        signUpInfo.setTime("");

        assertEquals("", signUpInfo.getAccount());
        assertEquals("", signUpInfo.getPwd());
        assertEquals("", signUpInfo.getTime());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : SignUpInfoPojo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(3, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : SignUpInfoPojo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }

    @Test
    void testLongStringValues() {
        SignUpInfoPojo signUpInfo = new SignUpInfoPojo();
        String longString = "a".repeat(10000);

        signUpInfo.setAccount(longString);
        signUpInfo.setPwd(longString);
        signUpInfo.setTime(longString);

        assertEquals(longString, signUpInfo.getAccount());
        assertEquals(longString, signUpInfo.getPwd());
        assertEquals(longString, signUpInfo.getTime());
    }

    @Test
    void testSpecialCharacterValues() {
        SignUpInfoPojo signUpInfo = new SignUpInfoPojo();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        signUpInfo.setAccount(specialChars);
        signUpInfo.setPwd(specialChars);
        signUpInfo.setTime(specialChars);

        assertEquals(specialChars, signUpInfo.getAccount());
        assertEquals(specialChars, signUpInfo.getPwd());
        assertEquals(specialChars, signUpInfo.getTime());
    }

    @Test
    void testUnicodeValues() {
        SignUpInfoPojo signUpInfo = new SignUpInfoPojo();
        String unicode = "中文测试_日本語テスト_한국어테스트";

        signUpInfo.setAccount(unicode);
        signUpInfo.setPwd(unicode);
        signUpInfo.setTime(unicode);

        assertEquals(unicode, signUpInfo.getAccount());
        assertEquals(unicode, signUpInfo.getPwd());
        assertEquals(unicode, signUpInfo.getTime());
    }

    @Test
    void testMultipleSetAndGet() {
        SignUpInfoPojo signUpInfo = new SignUpInfoPojo();

        for (int i = 0; i < 10; i++) {
            signUpInfo.setAccount("user-" + i);
            signUpInfo.setPwd("pwd-" + i);
            signUpInfo.setTime("time-" + i);
            assertEquals("user-" + i, signUpInfo.getAccount());
            assertEquals("pwd-" + i, signUpInfo.getPwd());
            assertEquals("time-" + i, signUpInfo.getTime());
        }
    }

    @Test
    void testFieldsAreIndependent() {
        SignUpInfoPojo signUpInfo = new SignUpInfoPojo();
        signUpInfo.setAccount("account1");
        signUpInfo.setPwd("password1");
        signUpInfo.setTime("time1");

        signUpInfo.setAccount("account2");
        assertEquals("account2", signUpInfo.getAccount());
        assertEquals("password1", signUpInfo.getPwd());
        assertEquals("time1", signUpInfo.getTime());
    }
}
