package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChangePasswordInfoPojoTest {

    @Test
    void testDefaultValuesAreNull() {
        ChangePasswordInfoPojo changePwdInfo = new ChangePasswordInfoPojo();
        assertNull(changePwdInfo.getOldPwd());
        assertNull(changePwdInfo.getNewPwd());
        assertNull(changePwdInfo.getTime());
    }

    @Test
    void testSetAndGetAllFields() {
        ChangePasswordInfoPojo changePwdInfo = new ChangePasswordInfoPojo();
        String oldPwd = "oldPassword";
        String newPwd = "newPassword";
        String time = "2026-01-15 10:30:00";

        changePwdInfo.setOldPwd(oldPwd);
        changePwdInfo.setNewPwd(newPwd);
        changePwdInfo.setTime(time);

        assertEquals(oldPwd, changePwdInfo.getOldPwd());
        assertEquals(newPwd, changePwdInfo.getNewPwd());
        assertEquals(time, changePwdInfo.getTime());
    }

    @Test
    void testSetNullValues() {
        ChangePasswordInfoPojo changePwdInfo = new ChangePasswordInfoPojo();
        changePwdInfo.setOldPwd("test");
        changePwdInfo.setNewPwd("test");
        changePwdInfo.setTime("test");

        changePwdInfo.setOldPwd(null);
        changePwdInfo.setNewPwd(null);
        changePwdInfo.setTime(null);

        assertNull(changePwdInfo.getOldPwd());
        assertNull(changePwdInfo.getNewPwd());
        assertNull(changePwdInfo.getTime());
    }

    @Test
    void testEmptyStringValues() {
        ChangePasswordInfoPojo changePwdInfo = new ChangePasswordInfoPojo();
        changePwdInfo.setOldPwd("");
        changePwdInfo.setNewPwd("");
        changePwdInfo.setTime("");

        assertEquals("", changePwdInfo.getOldPwd());
        assertEquals("", changePwdInfo.getNewPwd());
        assertEquals("", changePwdInfo.getTime());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : ChangePasswordInfoPojo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(3, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : ChangePasswordInfoPojo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }

    @Test
    void testLongStringValues() {
        ChangePasswordInfoPojo changePwdInfo = new ChangePasswordInfoPojo();
        String longString = "a".repeat(10000);

        changePwdInfo.setOldPwd(longString);
        changePwdInfo.setNewPwd(longString);
        changePwdInfo.setTime(longString);

        assertEquals(longString, changePwdInfo.getOldPwd());
        assertEquals(longString, changePwdInfo.getNewPwd());
        assertEquals(longString, changePwdInfo.getTime());
    }

    @Test
    void testSpecialCharacterValues() {
        ChangePasswordInfoPojo changePwdInfo = new ChangePasswordInfoPojo();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        changePwdInfo.setOldPwd(specialChars);
        changePwdInfo.setNewPwd(specialChars);
        changePwdInfo.setTime(specialChars);

        assertEquals(specialChars, changePwdInfo.getOldPwd());
        assertEquals(specialChars, changePwdInfo.getNewPwd());
        assertEquals(specialChars, changePwdInfo.getTime());
    }

    @Test
    void testUnicodeValues() {
        ChangePasswordInfoPojo changePwdInfo = new ChangePasswordInfoPojo();
        String unicode = "中文测试_日本語テスト_한국어테스트";

        changePwdInfo.setOldPwd(unicode);
        changePwdInfo.setNewPwd(unicode);
        changePwdInfo.setTime(unicode);

        assertEquals(unicode, changePwdInfo.getOldPwd());
        assertEquals(unicode, changePwdInfo.getNewPwd());
        assertEquals(unicode, changePwdInfo.getTime());
    }

    @Test
    void testMultipleSetAndGet() {
        ChangePasswordInfoPojo changePwdInfo = new ChangePasswordInfoPojo();

        for (int i = 0; i < 10; i++) {
            changePwdInfo.setOldPwd("old-" + i);
            changePwdInfo.setNewPwd("new-" + i);
            changePwdInfo.setTime("time-" + i);
            assertEquals("old-" + i, changePwdInfo.getOldPwd());
            assertEquals("new-" + i, changePwdInfo.getNewPwd());
            assertEquals("time-" + i, changePwdInfo.getTime());
        }
    }

    @Test
    void testFieldsAreIndependent() {
        ChangePasswordInfoPojo changePwdInfo = new ChangePasswordInfoPojo();
        changePwdInfo.setOldPwd("old1");
        changePwdInfo.setNewPwd("new1");
        changePwdInfo.setTime("time1");

        changePwdInfo.setOldPwd("old2");
        assertEquals("old2", changePwdInfo.getOldPwd());
        assertEquals("new1", changePwdInfo.getNewPwd());
        assertEquals("time1", changePwdInfo.getTime());
    }
}
