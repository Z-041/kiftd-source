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

}