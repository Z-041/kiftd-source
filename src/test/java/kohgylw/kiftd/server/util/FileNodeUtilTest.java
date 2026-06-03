package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;

class FileNodeUtilTest {

    @Test
    void testMaxNumConstant() {
        assertEquals(10000, FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER);
    }

    @Test
    void testGetNewNodeNameNoConflict() {
        Node n = new Node();
        n.setFileName("test.txt");
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node());
        nodes.get(0).setFileName("other.txt");

        String newName = FileNodeUtil.getNewNodeName("test.txt", nodes);
        assertEquals("test.txt", newName);
    }

    @Test
    void testGetNewNodeNameWithConflict() {
        Node n1 = new Node();
        n1.setFileName("test.txt");
        List<Node> nodes = new ArrayList<>();
        nodes.add(n1);

        String newName = FileNodeUtil.getNewNodeName("test.txt", nodes);
        assertEquals("test (1).txt", newName);
    }

    @Test
    void testGetNewNodeNameWithMultipleConflicts() {
        List<Node> nodes = new ArrayList<>();
        Node n1 = new Node();
        n1.setFileName("test.txt");
        nodes.add(n1);
        Node n2 = new Node();
        n2.setFileName("test (1).txt");
        nodes.add(n2);
        Node n3 = new Node();
        n3.setFileName("test (2).txt");
        nodes.add(n3);

        String newName = FileNodeUtil.getNewNodeName("test.txt", nodes);
        assertEquals("test (3).txt", newName);
    }

    @Test
    void testGetNewNodeNameFileNoExtension() {
        Node n1 = new Node();
        n1.setFileName("README");
        List<Node> nodes = new ArrayList<>();
        nodes.add(n1);

        String newName = FileNodeUtil.getNewNodeName("README", nodes);
        assertEquals("README (1)", newName);
    }

    @Test
    void testGetNewNodeNameWithDotInName() {
        Node n1 = new Node();
        n1.setFileName("file.v1.2.txt");
        List<Node> nodes = new ArrayList<>();
        nodes.add(n1);

        String newName = FileNodeUtil.getNewNodeName("file.v1.2.txt", nodes);
        assertEquals("file.v1.2 (1).txt", newName);
    }

    @Test
    void testGetNewNodeNameEmptyList() {
        List<Node> nodes = new ArrayList<>();
        String newName = FileNodeUtil.getNewNodeName("test.txt", nodes);
        assertEquals("test.txt", newName);
    }

    @Test
    void testGetNewFolderNameNoConflict() {
        Folder f1 = new Folder();
        f1.setFolderName("docs");
        List<Folder> folders = new ArrayList<>();
        folders.add(f1);

        String newName = FileNodeUtil.getNewFolderName("photos", folders);
        assertEquals("photos", newName);
    }

    @Test
    void testGetNewFolderNameWithConflict() {
        Folder f1 = new Folder();
        f1.setFolderName("docs");
        List<Folder> folders = new ArrayList<>();
        folders.add(f1);

        String newName = FileNodeUtil.getNewFolderName("docs", folders);
        assertEquals("docs 1", newName);
    }

    @Test
    void testGetNewFolderNameWithMultipleConflicts() {
        List<Folder> folders = new ArrayList<>();
        Folder f1 = new Folder();
        f1.setFolderName("docs");
        folders.add(f1);
        Folder f2 = new Folder();
        f2.setFolderName("docs 1");
        folders.add(f2);
        Folder f3 = new Folder();
        f3.setFolderName("docs 2");
        folders.add(f3);

        String newName = FileNodeUtil.getNewFolderName("docs", folders);
        assertEquals("docs 3", newName);
    }

    @Test
    void testGetNewFolderNameEmptyList() {
        List<Folder> folders = new ArrayList<>();
        String newName = FileNodeUtil.getNewFolderName("docs", folders);
        assertEquals("docs", newName);
    }

    @Test
    void testConnectionInitiallyNull() {
        assertNull(FileNodeUtil.getNodeDBConnection());
    }

}