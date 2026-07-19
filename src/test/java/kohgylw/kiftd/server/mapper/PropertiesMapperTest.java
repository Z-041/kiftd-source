package kohgylw.kiftd.server.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kohgylw.kiftd.server.model.Propertie;

@ExtendWith(MockitoExtension.class)
class PropertiesMapperTest {

    @Mock
    private PropertiesMapper propertiesMapper;

    @Test
    void testInsertPropertie() {
        Propertie prop = new Propertie();
        prop.setPropertieKey("max_size");
        prop.setPropertieValue("1024000");
        when(propertiesMapper.insert(any(Propertie.class))).thenReturn(1);

        int result = propertiesMapper.insert(prop);

        assertEquals(1, result);
        verify(propertiesMapper, times(1)).insert(prop);
    }

    @Test
    void testSelectById() {
        Propertie prop = new Propertie();
        prop.setPropertieKey("max_size");
        prop.setPropertieValue("1024000");
        when(propertiesMapper.selectById("max_size")).thenReturn(prop);

        Propertie result = propertiesMapper.selectById("max_size");

        assertNotNull(result);
        assertEquals("max_size", result.getPropertieKey());
        assertEquals("1024000", result.getPropertieValue());
    }

    @Test
    void testSelectByIdNotFound() {
        when(propertiesMapper.selectById("nonexistent")).thenReturn(null);

        Propertie result = propertiesMapper.selectById("nonexistent");

        assertNull(result);
    }

    @Test
    void testSelectByKey() {
        Propertie prop = new Propertie();
        prop.setPropertieKey("test_key");
        prop.setPropertieValue("test_value");
        when(propertiesMapper.selectByKey("test_key")).thenReturn(prop);

        Propertie result = propertiesMapper.selectByKey("test_key");

        assertNotNull(result);
        assertEquals("test_key", result.getPropertieKey());
        assertEquals("test_value", result.getPropertieValue());
    }

    @Test
    void testSelectByKeyNotFound() {
        when(propertiesMapper.selectByKey("nonexistent")).thenReturn(null);

        Propertie result = propertiesMapper.selectByKey("nonexistent");

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
        Propertie prop = new Propertie();
        prop.setPropertieKey("max_size");
        prop.setPropertieValue("2048000");
        when(propertiesMapper.updateById(prop)).thenReturn(1);

        int result = propertiesMapper.updateById(prop);

        assertEquals(1, result);
        verify(propertiesMapper, times(1)).updateById(prop);
    }

    @Test
    void testUpdate() {
        Propertie prop = new Propertie();
        prop.setPropertieKey("test_key");
        prop.setPropertieValue("updated_value");
        when(propertiesMapper.update(prop)).thenReturn(1);

        int result = propertiesMapper.update(prop);

        assertEquals(1, result);
        verify(propertiesMapper, times(1)).update(prop);
    }

    @Test
    void testUpdateReturnsZero() {
        Propertie prop = new Propertie();
        prop.setPropertieKey("nonexistent");
        when(propertiesMapper.update(prop)).thenReturn(0);

        int result = propertiesMapper.update(prop);

        assertEquals(0, result);
    }

    @Test
    void testSelectList() {
        List<Propertie> props = new ArrayList<>();
        Propertie p1 = new Propertie();
        p1.setPropertieKey("key1");
        p1.setPropertieValue("value1");
        Propertie p2 = new Propertie();
        p2.setPropertieKey("key2");
        p2.setPropertieValue("value2");
        props.add(p1);
        props.add(p2);
        when(propertiesMapper.selectList(any())).thenReturn(props);

        List<Propertie> result = propertiesMapper.selectList(null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("key1", result.get(0).getPropertieKey());
        assertEquals("key2", result.get(1).getPropertieKey());
    }

    @Test
    void testSelectListEmpty() {
        when(propertiesMapper.selectList(any())).thenReturn(new ArrayList<>());

        List<Propertie> result = propertiesMapper.selectList(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testInsertReturnsZero() {
        Propertie prop = new Propertie();
        when(propertiesMapper.insert(any(Propertie.class))).thenReturn(0);

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
