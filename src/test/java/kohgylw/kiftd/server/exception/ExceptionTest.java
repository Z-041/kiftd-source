package kohgylw.kiftd.server.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FilesTotalOutOfLimitExceptionTest {

    @Test
    void testExceptionIsThrowable() {
        assertThrows(FilesTotalOutOfLimitException.class, () -> {
            throw new FilesTotalOutOfLimitException();
        });
    }

    @Test
    void testExceptionExtendsException() {
        FilesTotalOutOfLimitException e = new FilesTotalOutOfLimitException();
        assertInstanceOf(Exception.class, e);
    }

    @Test
    void testExceptionMessageIsNull() {
        FilesTotalOutOfLimitException e = new FilesTotalOutOfLimitException();
        assertNull(e.getMessage());
    }
}

class FoldersTotalOutOfLimitExceptionTest {

    @Test
    void testExceptionIsThrowable() {
        assertThrows(FoldersTotalOutOfLimitException.class, () -> {
            throw new FoldersTotalOutOfLimitException();
        });
    }

    @Test
    void testExceptionExtendsException() {
        FoldersTotalOutOfLimitException e = new FoldersTotalOutOfLimitException();
        assertInstanceOf(Exception.class, e);
    }

    @Test
    void testExceptionMessageIsNull() {
        FoldersTotalOutOfLimitException e = new FoldersTotalOutOfLimitException();
        assertNull(e.getMessage());
    }
}