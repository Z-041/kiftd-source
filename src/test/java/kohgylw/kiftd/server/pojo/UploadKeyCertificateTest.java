package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UploadKeyCertificateTest {

    @Test
    void testCertificateWithPositiveTermIsInitiallyEffective() {
        UploadKeyCertificate cert = new UploadKeyCertificate(5, "alice");
        assertTrue(cert.isEffective(), "Certificate with term=5 should be effective initially");
        assertEquals("alice", cert.getAccount());
    }

    @Test
    void testCertificateExpiresAfterFullCheckCycles() {
        UploadKeyCertificate cert = new UploadKeyCertificate(3, "bob");
        assertTrue(cert.isEffective(), "Before any check, cert should be effective");

        cert.checked();
        assertTrue(cert.isEffective(), "After check 1/3, cert should still be effective");

        cert.checked();
        assertTrue(cert.isEffective(), "After check 2/3, cert should still be effective");

        cert.checked();
        assertFalse(cert.isEffective(), "After check 3/3, cert should expire");
    }

    @Test
    void testCertificateExpiryBlocksFurtherUploads() {
        UploadKeyCertificate cert = new UploadKeyCertificate(1, "carol");
        assertTrue(cert.isEffective());

        cert.checked();
        assertFalse(cert.isEffective(), "Single-use cert must expire after one check");

        cert.checked();
        assertFalse(cert.isEffective(), "Cert must remain expired after additional checks");
    }

    @Test
    void testZeroTermCertificateIsNeverEffective() {
        UploadKeyCertificate cert = new UploadKeyCertificate(0, "dave");
        assertFalse(cert.isEffective(), "Certificate with term=0 is not effective from the start");
    }

    @Test
    void testLargeTermCertificateDoesNotExpirePrematurely() {
        UploadKeyCertificate cert = new UploadKeyCertificate(100, "eve");
        for (int i = 0; i < 100; i++) {
            cert.checked();
        }
        assertFalse(cert.isEffective(), "Certificate should expire after 100 checks");

        assertTrue(cert.getAccount().equals("eve"));
    }

    @Test
    void testNegativeTermIsNotEffective() {
        UploadKeyCertificate cert = new UploadKeyCertificate(-5, "frank");
        assertFalse(cert.isEffective(), "Certificate with negative term should not be effective");
    }

}