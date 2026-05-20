package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

class ExtendStoresTest {

    @Test
    void testSetAndGetIndex() {
        ExtendStores es = new ExtendStores();
        assertEquals(0, es.getIndex());

        es.setIndex((short) 5);
        assertEquals(5, es.getIndex());
    }

    @Test
    void testSetAndGetPath() {
        ExtendStores es = new ExtendStores();
        assertNull(es.getPath());

        File path = new File("D:" + File.separator + "extend_store");
        es.setPath(path);
        assertEquals(path, es.getPath());
    }

    @Test
    void testNegativeIndex() {
        ExtendStores es = new ExtendStores();
        es.setIndex((short) -1);
        assertEquals(-1, es.getIndex());
    }

    @Test
    void testMaxShortIndex() {
        ExtendStores es = new ExtendStores();
        es.setIndex(Short.MAX_VALUE);
        assertEquals(Short.MAX_VALUE, es.getIndex());
    }

}