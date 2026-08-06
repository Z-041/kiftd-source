package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SearchViewTest {

    @Test
    void testDefaultValues() {
        SearchView searchView = new SearchView();
        assertNull(searchView.getKeyWorld());
        assertNull(searchView.getFolder());
        assertNull(searchView.getAccount());
    }

    @Test
    void testSetAndGetKeyWorld() {
        SearchView searchView = new SearchView();
        String keyWorld = "test keyword";

        searchView.setKeyWorld(keyWorld);

        assertEquals(keyWorld, searchView.getKeyWorld());
    }

    @Test
    void testSetNullKeyWorld() {
        SearchView searchView = new SearchView();
        searchView.setKeyWorld("test");
        searchView.setKeyWorld(null);

        assertNull(searchView.getKeyWorld());
    }

    @Test
    void testEmptyKeyWorld() {
        SearchView searchView = new SearchView();
        searchView.setKeyWorld("");

        assertEquals("", searchView.getKeyWorld());
    }

    @Test
    void testIsInstanceOfFolderView() {
        SearchView searchView = new SearchView();
        assertTrue(searchView instanceof FolderView);
        assertTrue(searchView instanceof SearchView);
    }

    @Test
    void testLongKeyWorld() {
        SearchView searchView = new SearchView();
        String longKeyWorld = "a".repeat(10000);

        searchView.setKeyWorld(longKeyWorld);

        assertEquals(longKeyWorld, searchView.getKeyWorld());
    }

    @Test
    void testSpecialCharacterKeyWorld() {
        SearchView searchView = new SearchView();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        searchView.setKeyWorld(specialChars);

        assertEquals(specialChars, searchView.getKeyWorld());
    }

    @Test
    void testUnicodeKeyWorld() {
        SearchView searchView = new SearchView();
        String unicode = "中文测试_日本語テスト_한국어테스트_😀🎉";

        searchView.setKeyWorld(unicode);

        assertEquals(unicode, searchView.getKeyWorld());
    }

    @Test
    void testMultipleSetAndGet() {
        SearchView searchView = new SearchView();

        for (int i = 0; i < 10; i++) {
            searchView.setKeyWorld("keyword-" + i);
            assertEquals("keyword-" + i, searchView.getKeyWorld());
        }
    }

    @Test
    void testInheritedFieldsWork() {
        SearchView searchView = new SearchView();
        searchView.setAccount("testUser");
        searchView.setKeyWorld("test");

        assertEquals("testUser", searchView.getAccount());
        assertEquals("test", searchView.getKeyWorld());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : SearchView.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(1, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : SearchView.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }
}
