package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

class PictureViewListTest {

    @Test
    void testDefaultValues() {
        PictureViewList pictureViewList = new PictureViewList();
        assertNull(pictureViewList.getPictureViewList());
        assertEquals(0, pictureViewList.getIndex());
    }

    @Test
    void testSetAndGetAllFields() {
        PictureViewList pictureViewList = new PictureViewList();
        List<PictureInfo> list = new ArrayList<>();
        PictureInfo p1 = new PictureInfo();
        p1.setFileName("pic1.jpg");
        p1.setUrl("http://example.com/pic1.jpg");
        PictureInfo p2 = new PictureInfo();
        p2.setFileName("pic2.jpg");
        p2.setUrl("http://example.com/pic2.jpg");
        list.add(p1);
        list.add(p2);
        int index = 1;

        pictureViewList.setPictureViewList(list);
        pictureViewList.setIndex(index);

        assertNotNull(pictureViewList.getPictureViewList());
        assertEquals(2, pictureViewList.getPictureViewList().size());
        assertEquals("pic1.jpg", pictureViewList.getPictureViewList().get(0).getFileName());
        assertEquals("pic2.jpg", pictureViewList.getPictureViewList().get(1).getFileName());
        assertEquals(index, pictureViewList.getIndex());
    }

    @Test
    void testSetNullPictureViewList() {
        PictureViewList pictureViewList = new PictureViewList();
        pictureViewList.setPictureViewList(new ArrayList<>());
        pictureViewList.setPictureViewList(null);

        assertNull(pictureViewList.getPictureViewList());
    }

    @Test
    void testEmptyPictureViewList() {
        PictureViewList pictureViewList = new PictureViewList();
        pictureViewList.setPictureViewList(new ArrayList<>());

        assertNotNull(pictureViewList.getPictureViewList());
        assertTrue(pictureViewList.getPictureViewList().isEmpty());
    }

    @Test
    void testIndexBoundaryValues() {
        PictureViewList pictureViewList = new PictureViewList();

        pictureViewList.setIndex(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, pictureViewList.getIndex());

        pictureViewList.setIndex(-1);
        assertEquals(-1, pictureViewList.getIndex());

        pictureViewList.setIndex(0);
        assertEquals(0, pictureViewList.getIndex());

        pictureViewList.setIndex(1);
        assertEquals(1, pictureViewList.getIndex());

        pictureViewList.setIndex(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, pictureViewList.getIndex());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : PictureViewList.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(2, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : PictureViewList.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }

    @Test
    void testMultipleSetAndGet() {
        PictureViewList pictureViewList = new PictureViewList();

        for (int i = 0; i < 5; i++) {
            List<PictureInfo> list = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                PictureInfo p = new PictureInfo();
                p.setFileName("pic-" + j + ".jpg");
                list.add(p);
            }
            pictureViewList.setPictureViewList(list);
            pictureViewList.setIndex(i);
            assertEquals(i, pictureViewList.getPictureViewList().size());
            assertEquals(i, pictureViewList.getIndex());
        }
    }

    @Test
    void testListModificationAfterSet() {
        PictureViewList pictureViewList = new PictureViewList();
        List<PictureInfo> list = new ArrayList<>();
        PictureInfo p1 = new PictureInfo();
        p1.setFileName("pic1.jpg");
        list.add(p1);

        pictureViewList.setPictureViewList(list);
        list.clear();

        assertNotNull(pictureViewList.getPictureViewList());
        assertTrue(pictureViewList.getPictureViewList().isEmpty());
    }

    @Test
    void testFieldsAreIndependent() {
        PictureViewList pictureViewList = new PictureViewList();
        List<PictureInfo> list = new ArrayList<>();
        pictureViewList.setPictureViewList(list);
        pictureViewList.setIndex(5);

        pictureViewList.setIndex(10);
        assertNotNull(pictureViewList.getPictureViewList());
        assertEquals(10, pictureViewList.getIndex());
    }
}
