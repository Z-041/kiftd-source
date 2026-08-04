package kohgylw.kiftd.integration;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigurationManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimpleInitTest {

    static {
        if (Printer.instance == null) {
            Printer.init(false);
        }
    }

    @Test
    void testPrinterInitialized() {
        assertNotNull(Printer.instance, "Printer应该被初始化");
    }

    @Test
    void testConfigurationManagerInit() {
        ConfigurationManager cm = ConfigurationManager.instance();
        assertNotNull(cm, "ConfigurationManager应该被初始化");
        System.out.println("ConfigurationManager status: " + cm.getStatus());
        System.out.println("Driver: " + cm.getFileNodePathDriver());
        System.out.println("URL: " + cm.getFileNodePathURL());
    }
}
