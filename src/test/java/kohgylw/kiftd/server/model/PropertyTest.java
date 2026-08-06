package kohgylw.kiftd.server.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

class PropertyTest {

    @Test
    void testPropertyHasTableNameAnnotation() {
        TableName annotation = Property.class.getAnnotation(TableName.class);
        assertNotNull(annotation);
        assertEquals("PROPERTIES", annotation.value());
    }

    @Test
    void testPropertyKeyIsTableId() throws Exception {
        java.lang.reflect.Field keyField = Property.class.getDeclaredField("propertyKey");
        TableId tableId = keyField.getAnnotation(TableId.class);
        assertNotNull(tableId);
        assertEquals("propertie_key", tableId.value());
    }

    @Test
    void testPropertyValueHasTableField() throws Exception {
        java.lang.reflect.Field valueField = Property.class.getDeclaredField("propertyValue");
        TableField tableField = valueField.getAnnotation(TableField.class);
        assertNotNull(tableField);
        assertEquals("propertie_value", tableField.value());
    }

    @Test
    void testSetAndGet() {
        Property prop = new Property();
        prop.setPropertyKey("max_size");
        prop.setPropertyValue("1024000");

        assertEquals("max_size", prop.getPropertyKey());
        assertEquals("1024000", prop.getPropertyValue());
    }

    @Test
    void testDefaultValuesAreNull() {
        Property prop = new Property();
        assertNull(prop.getPropertyKey());
        assertNull(prop.getPropertyValue());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : Property.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(2, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() throws Exception {
        java.lang.reflect.Field keyField = Property.class.getDeclaredField("propertyKey");
        assertTrue(java.lang.reflect.Modifier.isPrivate(keyField.getModifiers()));

        java.lang.reflect.Field valueField = Property.class.getDeclaredField("propertyValue");
        assertTrue(java.lang.reflect.Modifier.isPrivate(valueField.getModifiers()));
    }

    @Test
    void testSetNullValues() {
        Property prop = new Property();
        prop.setPropertyKey(null);
        prop.setPropertyValue(null);

        assertNull(prop.getPropertyKey());
        assertNull(prop.getPropertyValue());
    }

    @Test
    void testEmptyStringValues() {
        Property prop = new Property();
        prop.setPropertyKey("");
        prop.setPropertyValue("");

        assertEquals("", prop.getPropertyKey());
        assertEquals("", prop.getPropertyValue());
    }

    @Test
    void testLongStringValues() {
        Property prop = new Property();
        String longKey = "k".repeat(5000);
        String longValue = "v".repeat(10000);

        prop.setPropertyKey(longKey);
        prop.setPropertyValue(longValue);

        assertEquals(longKey, prop.getPropertyKey());
        assertEquals(longValue, prop.getPropertyValue());
    }

    @Test
    void testSpecialCharacterValues() {
        Property prop = new Property();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?\\n\\t";

        prop.setPropertyKey(specialChars);
        prop.setPropertyValue(specialChars);

        assertEquals(specialChars, prop.getPropertyKey());
        assertEquals(specialChars, prop.getPropertyValue());
    }

    @Test
    void testUnicodeValues() {
        Property prop = new Property();
        String unicode = "中文测试_日本語テスト_한국어테스트_😀🎉";

        prop.setPropertyKey(unicode);
        prop.setPropertyValue(unicode);

        assertEquals(unicode, prop.getPropertyKey());
        assertEquals(unicode, prop.getPropertyValue());
    }

    @Test
    void testMultipleSetAndGet() {
        Property prop = new Property();

        for (int i = 0; i < 10; i++) {
            prop.setPropertyKey("key-" + i);
            prop.setPropertyValue("value-" + i);
            assertEquals("key-" + i, prop.getPropertyKey());
            assertEquals("value-" + i, prop.getPropertyValue());
        }
    }

    @Test
    void testKeyValueIndependence() {
        Property prop = new Property();
        prop.setPropertyKey("testKey");
        prop.setPropertyValue("testValue");

        prop.setPropertyKey("newKey");
        assertEquals("newKey", prop.getPropertyKey());
        assertEquals("testValue", prop.getPropertyValue());
    }

    @Test
    void testNoTransientFields() {
        for (java.lang.reflect.Field field : Property.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertFalse(java.lang.reflect.Modifier.isTransient(field.getModifiers()),
                        "Field " + field.getName() + " must not be transient");
            }
        }
    }

}
