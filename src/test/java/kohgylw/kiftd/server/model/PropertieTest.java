package kohgylw.kiftd.server.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

class PropertieTest {

    @Test
    void testPropertieHasTableNameAnnotation() {
        TableName annotation = Propertie.class.getAnnotation(TableName.class);
        assertNotNull(annotation);
        assertEquals("PROPERTIES", annotation.value());
    }

    @Test
    void testPropertieKeyIsTableId() throws Exception {
        java.lang.reflect.Field keyField = Propertie.class.getDeclaredField("propertieKey");
        TableId tableId = keyField.getAnnotation(TableId.class);
        assertNotNull(tableId);
        assertEquals("propertie_key", tableId.value());
    }

    @Test
    void testPropertieValueHasTableField() throws Exception {
        java.lang.reflect.Field valueField = Propertie.class.getDeclaredField("propertieValue");
        TableField tableField = valueField.getAnnotation(TableField.class);
        assertNotNull(tableField);
        assertEquals("propertie_value", tableField.value());
    }

    @Test
    void testSetAndGet() {
        Propertie prop = new Propertie();
        prop.setPropertieKey("max_size");
        prop.setPropertieValue("1024000");

        assertEquals("max_size", prop.getPropertieKey());
        assertEquals("1024000", prop.getPropertieValue());
    }

    @Test
    void testDefaultValuesAreNull() {
        Propertie prop = new Propertie();
        assertNull(prop.getPropertieKey());
        assertNull(prop.getPropertieValue());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : Propertie.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(2, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() throws Exception {
        java.lang.reflect.Field keyField = Propertie.class.getDeclaredField("propertieKey");
        assertTrue(java.lang.reflect.Modifier.isPrivate(keyField.getModifiers()));

        java.lang.reflect.Field valueField = Propertie.class.getDeclaredField("propertieValue");
        assertTrue(java.lang.reflect.Modifier.isPrivate(valueField.getModifiers()));
    }

    @Test
    void testSetNullValues() {
        Propertie prop = new Propertie();
        prop.setPropertieKey(null);
        prop.setPropertieValue(null);

        assertNull(prop.getPropertieKey());
        assertNull(prop.getPropertieValue());
    }

    @Test
    void testEmptyStringValues() {
        Propertie prop = new Propertie();
        prop.setPropertieKey("");
        prop.setPropertieValue("");

        assertEquals("", prop.getPropertieKey());
        assertEquals("", prop.getPropertieValue());
    }

    @Test
    void testLongStringValues() {
        Propertie prop = new Propertie();
        String longKey = "k".repeat(5000);
        String longValue = "v".repeat(10000);

        prop.setPropertieKey(longKey);
        prop.setPropertieValue(longValue);

        assertEquals(longKey, prop.getPropertieKey());
        assertEquals(longValue, prop.getPropertieValue());
    }

    @Test
    void testSpecialCharacterValues() {
        Propertie prop = new Propertie();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?\\n\\t";

        prop.setPropertieKey(specialChars);
        prop.setPropertieValue(specialChars);

        assertEquals(specialChars, prop.getPropertieKey());
        assertEquals(specialChars, prop.getPropertieValue());
    }

    @Test
    void testUnicodeValues() {
        Propertie prop = new Propertie();
        String unicode = "中文测试_日本語テスト_한국어테스트_😀🎉";

        prop.setPropertieKey(unicode);
        prop.setPropertieValue(unicode);

        assertEquals(unicode, prop.getPropertieKey());
        assertEquals(unicode, prop.getPropertieValue());
    }

    @Test
    void testMultipleSetAndGet() {
        Propertie prop = new Propertie();

        for (int i = 0; i < 10; i++) {
            prop.setPropertieKey("key-" + i);
            prop.setPropertieValue("value-" + i);
            assertEquals("key-" + i, prop.getPropertieKey());
            assertEquals("value-" + i, prop.getPropertieValue());
        }
    }

    @Test
    void testKeyValueIndependence() {
        Propertie prop = new Propertie();
        prop.setPropertieKey("testKey");
        prop.setPropertieValue("testValue");

        prop.setPropertieKey("newKey");
        assertEquals("newKey", prop.getPropertieKey());
        assertEquals("testValue", prop.getPropertieValue());
    }

    @Test
    void testNoTransientFields() {
        for (java.lang.reflect.Field field : Propertie.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertFalse(java.lang.reflect.Modifier.isTransient(field.getModifiers()),
                        "Field " + field.getName() + " must not be transient");
            }
        }
    }

}