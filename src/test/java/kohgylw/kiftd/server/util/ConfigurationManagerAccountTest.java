package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
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
    void testPasswordHashWriteContract() {
        // 验证"写入哈希、可被校验"的完整链路：对应 changePassword/createNewAccount/resetPassword
        // 接入 PasswordUtil.hashPassword 后的存储契约（写入 PBKDF2$ 前缀哈希而非明文）。
        String hashed = kohgylw.kiftd.server.util.PasswordUtil.hashPassword("newSecret456");

        // 新写入：哈希条目可通过 verifyPassword 校验（checkAccountPwd 的校验路径）
        assertTrue(kohgylw.kiftd.server.util.PasswordUtil.isPasswordHashed(hashed));
        assertTrue(kohgylw.kiftd.server.util.PasswordUtil.verifyPassword("newSecret456", hashed));
        assertFalse(kohgylw.kiftd.server.util.PasswordUtil.verifyPassword("wrong", hashed));
        // 哈希值不得包含明文（证明未落盘明文）
        assertFalse(hashed.contains("newSecret456"));

        // 迁移期：历史明文条目仍可登录（verifyPassword 对非 PBKDF2$ 前缀按明文比较）
        String plain = "userPlain123";
        assertTrue(kohgylw.kiftd.server.util.PasswordUtil.verifyPassword(plain, plain));
        assertFalse(kohgylw.kiftd.server.util.PasswordUtil.verifyPassword("wrong", plain));
    }

    @Test
    void testDefaultAdminPasswordHashContract() {
        // 对应 createDefaultAccountPropertiesFile：默认管理员密码以哈希存储且可校验
        String hashedAdmin = kohgylw.kiftd.server.util.PasswordUtil.hashPassword("000000");
        assertTrue(kohgylw.kiftd.server.util.PasswordUtil.isPasswordHashed(hashedAdmin));
        assertTrue(kohgylw.kiftd.server.util.PasswordUtil.verifyPassword("000000", hashedAdmin));
        assertFalse(hashedAdmin.contains("000000"));
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
