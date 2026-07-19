package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PublicKeyInfoTest {

    @Test
    void testDefaultValues() {
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
        assertNull(publicKeyInfo.getPublicKey());
        assertEquals(0L, publicKeyInfo.getTime());
    }

    @Test
    void testSetAndGetAllFields() {
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...";
        long time = 1705312800000L;

        publicKeyInfo.setPublicKey(publicKey);
        publicKeyInfo.setTime(time);

        assertEquals(publicKey, publicKeyInfo.getPublicKey());
        assertEquals(time, publicKeyInfo.getTime());
    }

    @Test
    void testSetNullPublicKey() {
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
        publicKeyInfo.setPublicKey("testKey");
        publicKeyInfo.setPublicKey(null);

        assertNull(publicKeyInfo.getPublicKey());
    }

    @Test
    void testEmptyPublicKey() {
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
        publicKeyInfo.setPublicKey("");

        assertEquals("", publicKeyInfo.getPublicKey());
    }

    @Test
    void testTimeBoundaryValues() {
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();

        publicKeyInfo.setTime(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, publicKeyInfo.getTime());

        publicKeyInfo.setTime(-1L);
        assertEquals(-1L, publicKeyInfo.getTime());

        publicKeyInfo.setTime(0L);
        assertEquals(0L, publicKeyInfo.getTime());

        publicKeyInfo.setTime(1L);
        assertEquals(1L, publicKeyInfo.getTime());

        publicKeyInfo.setTime(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, publicKeyInfo.getTime());
    }

    @Test
    void testFieldCount() {
        int fieldCount = 0;
        for (java.lang.reflect.Field field : PublicKeyInfo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                fieldCount++;
            }
        }
        assertEquals(2, fieldCount);
    }

    @Test
    void testAllFieldsArePrivate() {
        for (java.lang.reflect.Field field : PublicKeyInfo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                        "Field " + field.getName() + " must be private");
            }
        }
    }

    @Test
    void testLongPublicKey() {
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
        String longKey = "A".repeat(10000);

        publicKeyInfo.setPublicKey(longKey);

        assertEquals(longKey, publicKeyInfo.getPublicKey());
    }

    @Test
    void testMultipleSetAndGet() {
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();

        for (int i = 0; i < 10; i++) {
            publicKeyInfo.setPublicKey("key-" + i);
            publicKeyInfo.setTime(i * 1000L);
            assertEquals("key-" + i, publicKeyInfo.getPublicKey());
            assertEquals(i * 1000L, publicKeyInfo.getTime());
        }
    }

    @Test
    void testFieldsAreIndependent() {
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
        publicKeyInfo.setPublicKey("key1");
        publicKeyInfo.setTime(1000L);

        publicKeyInfo.setPublicKey("key2");
        assertEquals("key2", publicKeyInfo.getPublicKey());
        assertEquals(1000L, publicKeyInfo.getTime());
    }

    @Test
    void testSpecialCharacterPublicKey() {
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        publicKeyInfo.setPublicKey(specialChars);

        assertEquals(specialChars, publicKeyInfo.getPublicKey());
    }
}
