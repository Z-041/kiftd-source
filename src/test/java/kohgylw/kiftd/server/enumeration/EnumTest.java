package kohgylw.kiftd.server.enumeration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AccountAuthTest {

    @Test
    void testEnumValuesCount() {
        assertEquals(6, AccountAuth.values().length);
    }

    @Test
    void testEnumValuesExist() {
        assertNotNull(AccountAuth.valueOf("CREATE_NEW_FOLDER"));
        assertNotNull(AccountAuth.valueOf("UPLOAD_FILES"));
        assertNotNull(AccountAuth.valueOf("DELETE_FILE_OR_FOLDER"));
        assertNotNull(AccountAuth.valueOf("RENAME_FILE_OR_FOLDER"));
        assertNotNull(AccountAuth.valueOf("DOWNLOAD_FILES"));
        assertNotNull(AccountAuth.valueOf("MOVE_FILES"));
    }

    @Test
    void testOrdinalOrder() {
        assertEquals(0, AccountAuth.CREATE_NEW_FOLDER.ordinal());
        assertEquals(1, AccountAuth.UPLOAD_FILES.ordinal());
        assertEquals(2, AccountAuth.DELETE_FILE_OR_FOLDER.ordinal());
        assertEquals(3, AccountAuth.RENAME_FILE_OR_FOLDER.ordinal());
        assertEquals(4, AccountAuth.DOWNLOAD_FILES.ordinal());
        assertEquals(5, AccountAuth.MOVE_FILES.ordinal());
    }

}

class LogLevelTest {

    @Test
    void testEnumValuesCount() {
        assertEquals(3, LogLevel.values().length);
    }

    @Test
    void testEnumValuesExist() {
        assertNotNull(LogLevel.valueOf("None"));
        assertNotNull(LogLevel.valueOf("Runtime_Exception"));
        assertNotNull(LogLevel.valueOf("Event"));
    }

    @Test
    void testOrdinalOrder() {
        assertEquals(0, LogLevel.None.ordinal());
        assertEquals(1, LogLevel.Runtime_Exception.ordinal());
        assertEquals(2, LogLevel.Event.ordinal());
    }

}

class VCLevelTest {

    @Test
    void testEnumValuesCount() {
        assertEquals(3, VCLevel.values().length);
    }

    @Test
    void testEnumValuesExist() {
        assertNotNull(VCLevel.valueOf("Standard"));
        assertNotNull(VCLevel.valueOf("Simplified"));
        assertNotNull(VCLevel.valueOf("Close"));
    }

    @Test
    void testOrdinalOrder() {
        assertEquals(0, VCLevel.Standard.ordinal());
        assertEquals(1, VCLevel.Simplified.ordinal());
        assertEquals(2, VCLevel.Close.ordinal());
    }

}