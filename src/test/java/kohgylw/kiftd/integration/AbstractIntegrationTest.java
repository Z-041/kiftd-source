package kohgylw.kiftd.integration;

import kohgylw.kiftd.printer.Printer;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public abstract class AbstractIntegrationTest {

    protected static String testBasePath;
    protected static String testConfDir;
    protected static String testFsPath;

    @BeforeAll
    static void setUpTestEnvironment() throws IOException {
        if (Printer.instance == null) {
            Printer.init(false);
        }

        testBasePath = Files.createTempDirectory("kiftd-test-").toAbsolutePath().toString();
        testConfDir = testBasePath + File.separator + "conf" + File.separator;
        testFsPath = testBasePath + File.separator + "filesystem" + File.separator;

        new File(testConfDir).mkdirs();
        new File(testFsPath).mkdirs();

        createTestServerProperties();
        createTestAccountProperties();

        System.setProperty("user.dir", testBasePath);
    }

    private static void createTestServerProperties() throws IOException {
        Properties serverProps = new Properties();
        serverProps.setProperty("mustLogin", "O");
        serverProps.setProperty("port", "8080");
        serverProps.setProperty("log", "N");
        serverProps.setProperty("VC.level", "CLOSE");
        serverProps.setProperty("FS.path", testFsPath);
        serverProps.setProperty("buff.size", "1048576");
        serverProps.setProperty("password.change", "Y");
        serverProps.setProperty("openFileChain", "CLOSE");
        serverProps.setProperty("mysql.enable", "false");

        try (FileOutputStream fos = new FileOutputStream(testConfDir + "server.properties")) {
            serverProps.store(fos, "Test server properties");
        }
    }

    private static void createTestAccountProperties() throws IOException {
        Properties accountProps = new Properties();
        accountProps.setProperty("admin.pwd", kohgylw.kiftd.server.util.PasswordUtil.hashPassword("000000"));
        accountProps.setProperty("admin.auth", "cudrm");
        accountProps.setProperty("admin.privilege", "S");
        accountProps.setProperty("authOverall", "l");

        try (FileOutputStream fos = new FileOutputStream(testConfDir + "account.properties")) {
            accountProps.store(fos, "Test account properties");
        }
    }
}
