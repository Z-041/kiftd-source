package kohgylw.kiftd.integration;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.PasswordUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class TestEnvironmentSetup {

    private static boolean initialized = false;
    private static String testBasePath;

    public static synchronized void initialize() throws IOException {
        if (initialized) {
            return;
        }

        if (Printer.instance == null) {
            Printer.init(false);
        }

        testBasePath = Files.createTempDirectory("kiftd-test-").toAbsolutePath().toString();
        String testConfDir = testBasePath + File.separator + "conf" + File.separator;
        String testFsPath = testBasePath + File.separator + "filesystem" + File.separator;

        new File(testConfDir).mkdirs();
        new File(testFsPath).mkdirs();
        new File(testFsPath + "fileblocks").mkdirs();
        new File(testFsPath + "filenodes").mkdirs();
        new File(testFsPath + "temporaryfiles").mkdirs();

        createServerProperties(testConfDir, testFsPath);
        createAccountProperties(testConfDir);

        System.setProperty("user.dir", testBasePath);

        initialized = true;
    }

    private static void createServerProperties(String confDir, String fsPath) throws IOException {
        Properties serverProps = new Properties();
        serverProps.setProperty("mustLogin", "O");
        serverProps.setProperty("port", "8080");
        serverProps.setProperty("log", "N");
        serverProps.setProperty("VC.level", "CLOSE");
        serverProps.setProperty("FS.path", fsPath);
        serverProps.setProperty("buff.size", "1048576");
        serverProps.setProperty("password.change", "Y");
        serverProps.setProperty("openFileChain", "CLOSE");
        serverProps.setProperty("mysql.enable", "false");

        try (FileOutputStream fos = new FileOutputStream(confDir + "server.properties")) {
            serverProps.store(fos, "Test server properties");
        }
    }

    private static void createAccountProperties(String confDir) throws IOException {
        Properties accountProps = new Properties();
        accountProps.setProperty("admin.pwd", PasswordUtil.hashPassword("000000"));
        accountProps.setProperty("admin.auth", "cudrm");
        accountProps.setProperty("admin.privilege", "S");
        accountProps.setProperty("authOverall", "l");

        try (FileOutputStream fos = new FileOutputStream(confDir + "account.properties")) {
            accountProps.store(fos, "Test account properties");
        }
    }

    public static String getTestBasePath() {
        return testBasePath;
    }
}
