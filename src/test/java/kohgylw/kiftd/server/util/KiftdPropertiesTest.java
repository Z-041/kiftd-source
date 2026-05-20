package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KiftdPropertiesTest {

    private KiftdProperties props;

    @BeforeEach
    void setUp() {
        props = new KiftdProperties();
    }

    @Test
    void testGetPropertyReturnsNullForMissingKey() {
        assertNull(props.getProperty("nonexistent"));
    }

    @Test
    void testGetPropertyReturnsNullForNullKey() {
        assertNull(props.getProperty(null));
    }

    @Test
    void testGetPropertyWithDefault() {
        assertEquals("defaultVal", props.getProperty("missing", "defaultVal"));
    }

    @Test
    void testSetAndGetProperty() {
        props.setProperty("key1", "value1");
        assertEquals("value1", props.getProperty("key1"));
    }

    @Test
    void testSetPropertyOverwritesExisting() {
        props.setProperty("key1", "oldValue");
        props.setProperty("key1", "newValue");
        assertEquals("newValue", props.getProperty("key1"));
    }

    @Test
    void testSetPropertyWithNullKey() {
        props.setProperty(null, "value");
        assertNull(props.getProperty(null));
    }

    @Test
    void testLoadFromSimpleInputStream() throws IOException {
        String content = "key1=value1\nkey2=value2\n";
        props.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)));

        assertEquals("value1", props.getProperty("key1"));
        assertEquals("value2", props.getProperty("key2"));
    }

    @Test
    void testLoadWithCommentLines() throws IOException {
        String content = "# This is a comment\nkey1=value1\n";
        props.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)));

        assertEquals("value1", props.getProperty("key1"));
        assertNull(props.getProperty("# This is a comment"));
    }

    @Test
    void testLoadWithColonDelimiter() throws IOException {
        String content = "key1:value1\nkey2:value2\n";
        props.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)));

        assertNull(props.getProperty("key1"), "Colons without = are not parsed as delimiters in KiftdProperties");
        assertNull(props.getProperty("key2"));
    }

    @Test
    void testLoadWithMixedDelimiters() throws IOException {
        String content = "key1=value1\nkey2:value2\n";
        props.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)));

        assertEquals("value1", props.getProperty("key1"));
        assertNull(props.getProperty("key2"), "Colons without = are not parsed as delimiters in KiftdProperties");
    }

    @Test
    void testLoadClearsPreviousProperties() throws IOException {
        props.setProperty("oldKey", "oldValue");
        String content = "newKey=newValue\n";
        props.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)));

        assertNull(props.getProperty("oldKey"));
        assertEquals("newValue", props.getProperty("newKey"));
    }

    @Test
    void testStoreWithHeader() throws IOException {
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, "Test Header");

        String output = baos.toString(StandardCharsets.ISO_8859_1);
        assertTrue(output.contains("#Test Header"));
        assertTrue(output.contains("key1=value1"));
        assertTrue(output.contains("key2=value2"));
    }

    @Test
    void testStoreWithoutHeader() throws IOException {
        props.setProperty("key1", "value1");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, null);

        String output = baos.toString(StandardCharsets.ISO_8859_1);
        assertTrue(output.contains("key1=value1"));
    }

    @Test
    void testStorePreservesComments() throws IOException {
        String content = "# This is a comment\nkey1=value1\n";
        props.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, null);

        String output = baos.toString(StandardCharsets.ISO_8859_1);
        assertTrue(output.contains("# This is a comment"));
    }

    @Test
    void testStringPropertieNames() {
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");

        assertEquals(2, props.stringPropertieNames().size());
        assertTrue(props.stringPropertieNames().contains("key1"));
        assertTrue(props.stringPropertieNames().contains("key2"));
    }

    @Test
    void testRemoveProperty() {
        props.setProperty("key1", "value1");
        props.removeProperty("key1");
        assertNull(props.getProperty("key1"));
    }

    @Test
    void testRemovePropertyWithNullKey() {
        props.setProperty("key1", "value1");
        props.removeProperty(null);
        assertEquals("value1", props.getProperty("key1"));
    }

    @Test
    void testRemoveNonExistentProperty() {
        props.removeProperty("nonexistent");
        assertNull(props.getProperty("nonexistent"));
    }

    @Test
    void testClear() {
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");
        props.clear();
        assertNull(props.getProperty("key1"));
        assertNull(props.getProperty("key2"));
        assertEquals(0, props.stringPropertieNames().size());
    }

}