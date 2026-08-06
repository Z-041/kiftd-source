package kohgylw.kiftd.server.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.server.model.Property;

@ExtendWith(MockitoExtension.class)
class PropertiesMapperTest {

    @Mock
    private PropertiesMapper propertiesMapper;

    @Test
    void testInsertProperty() {
        Property prop = new Property();
        prop.setPropertyKey("max_size");
        prop.setPropertyValue("1024000");
        when(propertiesMapper.insert(any(Property.class))).thenReturn(1);

        int result = propertiesMapper.insert(prop);

        assertEquals(1, result);
        verify(propertiesMapper, times(1)).insert(prop);
    }

    @Test
    void testSelectById() {
        Property prop = new Property();
        prop.setPropertyKey("max_size");
        prop.setPropertyValue("1024000");
        when(propertiesMapper.selectById("max_size")).thenReturn(prop);

        Property result = propertiesMapper.selectById("max_size");

        assertNotNull(result);
        assertEquals("max_size", result.getPropertyKey());
        assertEquals("1024000", result.getPropertyValue());
    }

    @Test
    void testSelectByIdNotFound() {
        when(propertiesMapper.selectById("nonexistent")).thenReturn(null);

        Property result = propertiesMapper.selectById("nonexistent");

        assertNull(result);
    }

    @Test
    void testSelectByKey() {
        Property prop = new Property();
        prop.setPropertyKey("test_key");
        prop.setPropertyValue("test_value");
        when(propertiesMapper.selectByKey("test_key")).thenReturn(prop);

        Property result = propertiesMapper.selectByKey("test_key");

        assertNotNull(result);
        assertEquals("test_key", result.getPropertyKey());
        assertEquals("test_value", result.getPropertyValue());
    }

    @Test
    void testSelectByKeyNotFound() {
        when(propertiesMapper.selectByKey("nonexistent")).thenReturn(null);

        Property result = propertiesMapper.selectByKey("nonexistent");

        assertNull(result);
    }

    @Test
    void testDeleteById() {
        when(propertiesMapper.deleteById("max_size")).thenReturn(1);

        int result = propertiesMapper.deleteById("max_size");

        assertEquals(1, result);
        verify(propertiesMapper, times(1)).deleteById("max_size");
    }

    @Test
    void testDeleteByIdNotFound() {
        when(propertiesMapper.deleteById("nonexistent")).thenReturn(0);

        int result = propertiesMapper.deleteById("nonexistent");

        assertEquals(0, result);
    }

    @Test
    void testDeleteByKey() {
        when(propertiesMapper.deleteByKey("test_key")).thenReturn(1);

        int result = propertiesMapper.deleteByKey("test_key");

        assertEquals(1, result);
        verify(propertiesMapper, times(1)).deleteByKey("test_key");
    }

    @Test
    void testDeleteByKeyNotFound() {
        when(propertiesMapper.deleteByKey("nonexistent")).thenReturn(0);

        int result = propertiesMapper.deleteByKey("nonexistent");

        assertEquals(0, result);
    }

    @Test
    void testUpdateById() {
        Property prop = new Property();
        prop.setPropertyKey("max_size");
        prop.setPropertyValue("2048000");
        when(propertiesMapper.updateById(prop)).thenReturn(1);

        int result = propertiesMapper.updateById(prop);

        assertEquals(1, result);
        verify(propertiesMapper, times(1)).updateById(prop);
    }

    @Test
    void testUpdate() {
        Property prop = new Property();
        prop.setPropertyKey("test_key");
        prop.setPropertyValue("updated_value");
        when(propertiesMapper.update(prop)).thenReturn(1);

        int result = propertiesMapper.update(prop);

        assertEquals(1, result);
        verify(propertiesMapper, times(1)).update(prop);
    }

    @Test
    void testUpdateReturnsZero() {
        Property prop = new Property();
        prop.setPropertyKey("nonexistent");
        when(propertiesMapper.update(prop)).thenReturn(0);

        int result = propertiesMapper.update(prop);

        assertEquals(0, result);
    }

    @Test
    void testSelectList() {
        List<Property> props = new ArrayList<>();
        Property p1 = new Property();
        p1.setPropertyKey("key1");
        p1.setPropertyValue("value1");
        Property p2 = new Property();
        p2.setPropertyKey("key2");
        p2.setPropertyValue("value2");
        props.add(p1);
        props.add(p2);
        when(propertiesMapper.selectList(any())).thenReturn(props);

        List<Property> result = propertiesMapper.selectList(null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("key1", result.get(0).getPropertyKey());
        assertEquals("key2", result.get(1).getPropertyKey());
    }

    @Test
    void testSelectListEmpty() {
        when(propertiesMapper.selectList(any())).thenReturn(new ArrayList<>());

        List<Property> result = propertiesMapper.selectList(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testInsertReturnsZero() {
        Property prop = new Property();
        when(propertiesMapper.insert(any(Property.class))).thenReturn(0);

        int result = propertiesMapper.insert(prop);

        assertEquals(0, result);
    }

    @Test
    void testSelectCount() {
        when(propertiesMapper.selectCount(any())).thenReturn(3L);

        Long result = propertiesMapper.selectCount(null);

        assertEquals(3L, result);
    }
}
