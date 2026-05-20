package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import kohgylw.kiftd.server.enumeration.LogLevel;
import kohgylw.kiftd.server.enumeration.VCLevel;

class ServerSettingTest {

    @Test
    void testDefaultBooleanValues() {
        ServerSetting setting = new ServerSetting();
        assertFalse(setting.isMustLogin());
        assertFalse(setting.isOpenFileChain());
        assertFalse(setting.isAllowChangePassword());
    }

    @Test
    void testSetMustLogin() {
        ServerSetting setting = new ServerSetting();
        setting.setMustLogin(true);
        assertTrue(setting.isMustLogin());
        setting.setMustLogin(false);
        assertFalse(setting.isMustLogin());
    }

    @Test
    void testSetFileChain() {
        ServerSetting setting = new ServerSetting();
        setting.setFileChain(true);
        assertTrue(setting.isOpenFileChain());
        setting.setFileChain(false);
        assertFalse(setting.isOpenFileChain());
    }

    @Test
    void testSetChangePassword() {
        ServerSetting setting = new ServerSetting();
        setting.setChangePassword(true);
        assertTrue(setting.isAllowChangePassword());
    }

    @Test
    void testSetBuffSize() {
        ServerSetting setting = new ServerSetting();
        assertEquals(0, setting.getBuffSize());

        setting.setBuffSize(65536);
        assertEquals(65536, setting.getBuffSize());

        setting.setBuffSize(-1);
        assertEquals(-1, setting.getBuffSize());
    }

    @Test
    void testSetPort() {
        ServerSetting setting = new ServerSetting();
        assertEquals(0, setting.getPort());

        setting.setPort(8080);
        assertEquals(8080, setting.getPort());
    }

    @Test
    void testSetFsPath() {
        ServerSetting setting = new ServerSetting();
        assertNull(setting.getFsPath());

        setting.setFsPath("/data/kiftd");
        assertEquals("/data/kiftd", setting.getFsPath());
    }

    @Test
    void testSetVcLevel() {
        ServerSetting setting = new ServerSetting();
        assertNull(setting.getVc());

        setting.setVc(VCLevel.Standard);
        assertEquals(VCLevel.Standard, setting.getVc());

        setting.setVc(VCLevel.Close);
        assertEquals(VCLevel.Close, setting.getVc());
    }

    @Test
    void testSetLogLevel() {
        ServerSetting setting = new ServerSetting();
        assertNull(setting.getLog());

        setting.setLog(LogLevel.Event);
        assertEquals(LogLevel.Event, setting.getLog());

        setting.setLog(LogLevel.None);
        assertEquals(LogLevel.None, setting.getLog());
    }

    @Test
    void testSetExtendStores() {
        ServerSetting setting = new ServerSetting();
        assertNull(setting.getExtendStores());

        List<ExtendStores> stores = new ArrayList<>();
        ExtendStores es = new ExtendStores();
        es.setIndex((short) 1);
        stores.add(es);
        setting.setExtendStores(stores);

        assertEquals(1, setting.getExtendStores().size());
        assertEquals(1, setting.getExtendStores().get(0).getIndex());
    }

}