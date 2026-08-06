package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CreateNewFolderByNameResponseTest {

    @Test
    void testDefaultValuesAreNull() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();
        assertNull(respons.getResult());
        assertNull(respons.getNewName());
    }

    @Test
    void testSetAndGetAllFields() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();
        String result = "success";
        String newName = "newFolderName";

        respons.setResult(result);
        respons.setNewName(newName);

        assertEquals(result, respons.getResult());
        assertEquals(newName, respons.getNewName());
    }

    @Test
    void testSetNullValues() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();
        respons.setResult("test");
        respons.setNewName("test");

        respons.setResult(null);
        respons.setNewName(null);

        assertNull(respons.getResult());
        assertNull(respons.getNewName());
    }

    @Test
    void testEmptyStringValues() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();
        respons.setResult("");
        respons.setNewName("");

        assertEquals("", respons.getResult());
        assertEquals("", respons.getNewName());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : CreateNewFolderByNameResponse.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(2, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : CreateNewFolderByNameResponse.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }

    @Test
    void testLongStringValues() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();
        String longResult = "r".repeat(5000);
        String longName = "n".repeat(5000);

        respons.setResult(longResult);
        respons.setNewName(longName);

        assertEquals(longResult, respons.getResult());
        assertEquals(longName, respons.getNewName());
    }

    @Test
    void testSpecialCharacterValues() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        respons.setResult(specialChars);
        respons.setNewName(specialChars);

        assertEquals(specialChars, respons.getResult());
        assertEquals(specialChars, respons.getNewName());
    }

    @Test
    void testUnicodeValues() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();
        String unicode = "中文测试_日本語テスト_한국어테스트_😀🎉";

        respons.setResult(unicode);
        respons.setNewName(unicode);

        assertEquals(unicode, respons.getResult());
        assertEquals(unicode, respons.getNewName());
    }

    @Test
    void testMultipleSetAndGet() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();

        for (int i = 0; i < 10; i++) {
            respons.setResult("result-" + i);
            respons.setNewName("name-" + i);
            assertEquals("result-" + i, respons.getResult());
            assertEquals("name-" + i, respons.getNewName());
        }
    }

    @Test
    void testFieldsAreIndependent() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();
        respons.setResult("result1");
        respons.setNewName("name1");

        respons.setResult("result2");
        assertEquals("result2", respons.getResult());
        assertEquals("name1", respons.getNewName());
    }

    @Test
    void testSuccessResult() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();
        respons.setResult("success");
        respons.setNewName("myFolder");

        assertEquals("success", respons.getResult());
        assertEquals("myFolder", respons.getNewName());
    }

    @Test
    void testErrorResult() {
        CreateNewFolderByNameResponse respons = new CreateNewFolderByNameResponse();
        respons.setResult("error");
        respons.setNewName(null);

        assertEquals("error", respons.getResult());
        assertNull(respons.getNewName());
    }
}
