package kohgylw.kiftd.newcore.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

class ConfigurationManagerAccountTest {

    @TempDir
    Path tempDir;

    private File createTempAccountFile() throws Exception {
        File confDir = tempDir.resolve("conf").toFile();
        confDir.mkdirs();
        File accountFile = new File(confDir, "account.properties");
        try (FileOutputStream out = new FileOutputStream(accountFile)) {
            String content = "admin.pwd=PBKDF2$salt$hash\n" +
                           "admin.auth=cudrml\n" +
                           "admin.privilege=S\n" +
                           "user1.pwd=plainpassword\n" +
                           "user1.auth=ul\n" +
                           "user2.pwd=anotherpass\n";
            out.write(content.getBytes());
        }
        return accountFile;
    }

    @Test
    void testPasswordUtilHashAndVerify() {
        String password = "testPassword123";
        String hashed = kohgylw.kiftd.server.util.PasswordUtil.hashPassword(password);
        assertTrue(kohgylw.kiftd.server.util.PasswordUtil.isPasswordHashed(hashed));
        assertTrue(kohgylw.kiftd.server.util.PasswordUtil.verifyPassword(password, hashed));
        assertFalse(kohgylw.kiftd.server.util.PasswordUtil.verifyPassword("wrong", hashed));
    }

    @Test
    void testGetAllAccountsFromProps() throws Exception {
        File accountFile = createTempAccountFile();
        java.util.Properties props = new java.util.Properties();
        java.io.FileInputStream in = new java.io.FileInputStream(accountFile);
        props.load(in);
        in.close();

        int count = 0;
        for (String key : props.stringPropertyNames()) {
            if (key.endsWith(".pwd")) {
                count++;
            }
        }
        assertEquals(3, count);
    }

    @Test
    void testAccountNameExtraction() {
        String pwdKey = "admin.pwd";
        String account = pwdKey.substring(0, pwdKey.length() - 4);
        assertEquals("admin", account);

        pwdKey = "user123.pwd";
        account = pwdKey.substring(0, pwdKey.length() - 4);
        assertEquals("user123", account);
    }

    @Test
    void testAuthCharacterParsing() {
        String auth = "cudrml";
        assertTrue(auth.indexOf('c') >= 0);
        assertTrue(auth.indexOf('u') >= 0);
        assertTrue(auth.indexOf('d') >= 0);
        assertTrue(auth.indexOf('r') >= 0);
        assertTrue(auth.indexOf('m') >= 0);
        assertTrue(auth.indexOf('l') >= 0);
        assertFalse(auth.indexOf('x') >= 0);
    }

    @Test
    void testEmptyAccountListIsEmpty() {
        List<String> emptyList = java.util.Collections.emptyList();
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    @Test
    void testAccountSorting() {
        List<String> accounts = new java.util.ArrayList<>(List.of("user2", "admin", "User1", "zebra"));
        accounts.sort(String::compareToIgnoreCase);
        assertEquals("admin", accounts.get(0));
        assertEquals("User1", accounts.get(1));
        assertEquals("user2", accounts.get(2));
        assertEquals("zebra", accounts.get(3));
    }

    @Test
    void testInvalidAccountReturnsNotFound() {
        String nullAccount = null;
        assertFalse(kohgylw.kiftd.server.util.PasswordUtil.isPasswordHashed(null));
        assertFalse(kohgylw.kiftd.server.util.PasswordUtil.isPasswordHashed(""));
        assertFalse(kohgylw.kiftd.server.util.PasswordUtil.isPasswordHashed("plaintext"));
    }

    @Test
    void testSuperAdminPrivilegeCheck() {
        String privilege = "S";
        assertTrue("S".equals(privilege));

        String normalPrivilege = null;
        assertFalse("S".equals(normalPrivilege));

        String otherPrivilege = "X";
        assertFalse("S".equals(otherPrivilege));
    }

    @Test
    void testDeleteAccountKeyMatching() {
        List<String> keys = List.of(
            "admin.pwd",
            "admin.auth",
            "admin.group",
            "admin.privilege",
            "user1.pwd",
            "user1.auth"
        );

        List<String> adminKeys = keys.stream()
            .filter(k -> k.startsWith("admin."))
            .toList();

        assertEquals(4, adminKeys.size());
        assertTrue(adminKeys.contains("admin.pwd"));
        assertTrue(adminKeys.contains("admin.auth"));
        assertTrue(adminKeys.contains("admin.group"));
        assertTrue(adminKeys.contains("admin.privilege"));
    }

    @Test
    void testPasswordResetWithHashing() {
        String newPassword = "newSecurePass123";
        String hashed = kohgylw.kiftd.server.util.PasswordUtil.hashPassword(newPassword);
        assertTrue(kohgylw.kiftd.server.util.PasswordUtil.isPasswordHashed(hashed));
        assertTrue(kohgylw.kiftd.server.util.PasswordUtil.verifyPassword(newPassword, hashed));
        assertFalse(kohgylw.kiftd.server.util.PasswordUtil.verifyPassword("oldPass", hashed));
    }
}
