package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LoginInfoPojoTest {

    @Test
    void testDefaultValuesAreNull() {
        LoginInfoPojo loginInfo = new LoginInfoPojo();
        assertNull(loginInfo.getAccountId());
        assertNull(loginInfo.getAccountPwd());
        assertNull(loginInfo.getTime());
    }

    @Test
    void testSetAndGetAllFields() {
        LoginInfoPojo loginInfo = new LoginInfoPojo();
        String accountId = "user123";
        String accountPwd = "password123";
        String time = "2026-01-15 10:30:00";

        loginInfo.setAccountId(accountId);
        loginInfo.setAccountPwd(accountPwd);
        loginInfo.setTime(time);

        assertEquals(accountId, loginInfo.getAccountId());
        assertEquals(accountPwd, loginInfo.getAccountPwd());
        assertEquals(time, loginInfo.getTime());
    }

    @Test
    void testSetNullValues() {
        LoginInfoPojo loginInfo = new LoginInfoPojo();
        loginInfo.setAccountId("test");
        loginInfo.setAccountPwd("test");
        loginInfo.setTime("test");

        loginInfo.setAccountId(null);
        loginInfo.setAccountPwd(null);
        loginInfo.setTime(null);

        assertNull(loginInfo.getAccountId());
        assertNull(loginInfo.getAccountPwd());
        assertNull(loginInfo.getTime());
    }

    @Test
    void testEmptyStringValues() {
        LoginInfoPojo loginInfo = new LoginInfoPojo();
        loginInfo.setAccountId("");
        loginInfo.setAccountPwd("");
        loginInfo.setTime("");

        assertEquals("", loginInfo.getAccountId());
        assertEquals("", loginInfo.getAccountPwd());
        assertEquals("", loginInfo.getTime());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : LoginInfoPojo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(3, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : LoginInfoPojo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }

    @Test
    void testLongStringValues() {
        LoginInfoPojo loginInfo = new LoginInfoPojo();
        String longString = "a".repeat(10000);

        loginInfo.setAccountId(longString);
        loginInfo.setAccountPwd(longString);
        loginInfo.setTime(longString);

        assertEquals(longString, loginInfo.getAccountId());
        assertEquals(longString, loginInfo.getAccountPwd());
        assertEquals(longString, loginInfo.getTime());
    }

    @Test
    void testSpecialCharacterValues() {
        LoginInfoPojo loginInfo = new LoginInfoPojo();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        loginInfo.setAccountId(specialChars);
        loginInfo.setAccountPwd(specialChars);
        loginInfo.setTime(specialChars);

        assertEquals(specialChars, loginInfo.getAccountId());
        assertEquals(specialChars, loginInfo.getAccountPwd());
        assertEquals(specialChars, loginInfo.getTime());
    }

    @Test
    void testUnicodeValues() {
        LoginInfoPojo loginInfo = new LoginInfoPojo();
        String unicode = "中文测试_日本語テスト_한국어테스트";

        loginInfo.setAccountId(unicode);
        loginInfo.setAccountPwd(unicode);
        loginInfo.setTime(unicode);

        assertEquals(unicode, loginInfo.getAccountId());
        assertEquals(unicode, loginInfo.getAccountPwd());
        assertEquals(unicode, loginInfo.getTime());
    }

    @Test
    void testMultipleSetAndGet() {
        LoginInfoPojo loginInfo = new LoginInfoPojo();

        for (int i = 0; i < 10; i++) {
            loginInfo.setAccountId("user-" + i);
            loginInfo.setAccountPwd("pwd-" + i);
            loginInfo.setTime("time-" + i);
            assertEquals("user-" + i, loginInfo.getAccountId());
            assertEquals("pwd-" + i, loginInfo.getAccountPwd());
            assertEquals("time-" + i, loginInfo.getTime());
        }
    }

    @Test
    void testFieldsAreIndependent() {
        LoginInfoPojo loginInfo = new LoginInfoPojo();
        loginInfo.setAccountId("account1");
        loginInfo.setAccountPwd("password1");
        loginInfo.setTime("time1");

        loginInfo.setAccountId("account2");
        assertEquals("account2", loginInfo.getAccountId());
        assertEquals("password1", loginInfo.getAccountPwd());
        assertEquals("time1", loginInfo.getTime());
    }
}
