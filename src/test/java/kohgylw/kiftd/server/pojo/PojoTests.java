package kohgylw.kiftd.server.pojo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;

class PictureInfoTest {

    @Test
    void testSetAndGetFileName() {
        PictureInfo pi = new PictureInfo();
        pi.setFileName("photo.jpg");
        assertEquals("photo.jpg", pi.getFileName());
    }

    @Test
    void testSetAndGetUrl() {
        PictureInfo pi = new PictureInfo();
        pi.setUrl("/file/photo.jpg");
        assertEquals("/file/photo.jpg", pi.getUrl());
    }

    @Test
    void testDefaultNullValues() {
        PictureInfo pi = new PictureInfo();
        assertNull(pi.getFileName());
        assertNull(pi.getUrl());
    }
}

class LoginInfoPojoTest {

    @Test
    void testSetAndGetAccountId() {
        LoginInfoPojo pojo = new LoginInfoPojo();
        pojo.setAccountId("admin");
        assertEquals("admin", pojo.getAccountId());
    }

    @Test
    void testSetAndGetAccountPwd() {
        LoginInfoPojo pojo = new LoginInfoPojo();
        pojo.setAccountPwd("encrypted-password");
        assertEquals("encrypted-password", pojo.getAccountPwd());
    }

    @Test
    void testSetAndGetTime() {
        LoginInfoPojo pojo = new LoginInfoPojo();
        pojo.setTime("2026-05-20 12:00:00");
        assertEquals("2026-05-20 12:00:00", pojo.getTime());
    }

    @Test
    void testDefaultNullValues() {
        LoginInfoPojo pojo = new LoginInfoPojo();
        assertNull(pojo.getAccountId());
        assertNull(pojo.getAccountPwd());
        assertNull(pojo.getTime());
    }
}

class UploadKeyCertificateTest {

    @Test
    void testConstructorAndGetters() {
        UploadKeyCertificate cert = new UploadKeyCertificate(5, "testUser");
        assertEquals("testUser", cert.getAccount());
        assertTrue(cert.isEffective());
    }

    @Test
    void testCheckedDecrementsTerm() {
        UploadKeyCertificate cert = new UploadKeyCertificate(1, "testUser");
        assertTrue(cert.isEffective());
        cert.checked();
        assertFalse(cert.isEffective());
    }

    @Test
    void testCheckedMultipleTimes() {
        UploadKeyCertificate cert = new UploadKeyCertificate(3, "testUser");
        cert.checked();
        assertTrue(cert.isEffective());
        cert.checked();
        assertTrue(cert.isEffective());
        cert.checked();
        assertFalse(cert.isEffective());
    }

    @Test
    void testInitiallyEffective() {
        UploadKeyCertificate cert = new UploadKeyCertificate(1, "user");
        assertTrue(cert.isEffective());
    }

    @Test
    void testZeroTermNotEffective() {
        UploadKeyCertificate cert = new UploadKeyCertificate(0, "user");
        assertFalse(cert.isEffective());
    }
}

class SignUpInfoPojoTest {

    @Test
    void testSetAndGetAccount() {
        SignUpInfoPojo pojo = new SignUpInfoPojo();
        pojo.setAccount("newUser");
        assertEquals("newUser", pojo.getAccount());
    }

    @Test
    void testSetAndGetPwd() {
        SignUpInfoPojo pojo = new SignUpInfoPojo();
        pojo.setPwd("encrypted-pwd");
        assertEquals("encrypted-pwd", pojo.getPwd());
    }

    @Test
    void testSetAndGetTime() {
        SignUpInfoPojo pojo = new SignUpInfoPojo();
        pojo.setTime("2026-05-20");
        assertEquals("2026-05-20", pojo.getTime());
    }

    @Test
    void testDefaultNullValues() {
        SignUpInfoPojo pojo = new SignUpInfoPojo();
        assertNull(pojo.getAccount());
        assertNull(pojo.getPwd());
        assertNull(pojo.getTime());
    }
}

class PictureViewListTest {

    @Test
    void testSetAndGetPictureViewList() {
        PictureViewList pvl = new PictureViewList();
        assertNull(pvl.getPictureViewList());
        List<PictureInfo> list = new ArrayList<>();
        list.add(new PictureInfo());
        pvl.setPictureViewList(list);
        assertEquals(1, pvl.getPictureViewList().size());
    }

    @Test
    void testSetAndGetIndex() {
        PictureViewList pvl = new PictureViewList();
        assertEquals(0, pvl.getIndex());
        pvl.setIndex(3);
        assertEquals(3, pvl.getIndex());
    }
}

class ChangePasswordInfoPojoTest {

    @Test
    void testSetAndGetOldPwd() {
        ChangePasswordInfoPojo pojo = new ChangePasswordInfoPojo();
        pojo.setOldPwd("old-encrypted");
        assertEquals("old-encrypted", pojo.getOldPwd());
    }

    @Test
    void testSetAndGetNewPwd() {
        ChangePasswordInfoPojo pojo = new ChangePasswordInfoPojo();
        pojo.setNewPwd("new-encrypted");
        assertEquals("new-encrypted", pojo.getNewPwd());
    }

    @Test
    void testSetAndGetTime() {
        ChangePasswordInfoPojo pojo = new ChangePasswordInfoPojo();
        pojo.setTime("2026-05-20");
        assertEquals("2026-05-20", pojo.getTime());
    }

    @Test
    void testDefaultNullValues() {
        ChangePasswordInfoPojo pojo = new ChangePasswordInfoPojo();
        assertNull(pojo.getOldPwd());
        assertNull(pojo.getNewPwd());
        assertNull(pojo.getTime());
    }
}

class CheckUploadFilesResponsTest {

    @Test
    void testSetAndGetCheckResult() {
        CheckUploadFilesRespons resp = new CheckUploadFilesRespons();
        resp.setCheckResult("permitUpload");
        assertEquals("permitUpload", resp.getCheckResult());
    }

    @Test
    void testSetAndGetPereFileNameList() {
        CheckUploadFilesRespons resp = new CheckUploadFilesRespons();
        List<String> list = new ArrayList<>();
        list.add("file1.txt");
        resp.setPereFileNameList(list);
        assertEquals(1, resp.getPereFileNameList().size());
        assertEquals("file1.txt", resp.getPereFileNameList().get(0));
    }

    @Test
    void testSetAndGetOverSizeFile() {
        CheckUploadFilesRespons resp = new CheckUploadFilesRespons();
        resp.setOverSizeFile("large.mp4");
        assertEquals("large.mp4", resp.getOverSizeFile());
    }

    @Test
    void testSetAndGetMaxUploadFileSize() {
        CheckUploadFilesRespons resp = new CheckUploadFilesRespons();
        resp.setMaxUploadFileSize("100MB");
        assertEquals("100MB", resp.getMaxUploadFileSize());
    }

    @Test
    void testDefaultNullValues() {
        CheckUploadFilesRespons resp = new CheckUploadFilesRespons();
        assertNull(resp.getCheckResult());
        assertNull(resp.getPereFileNameList());
        assertNull(resp.getOverSizeFile());
        assertNull(resp.getMaxUploadFileSize());
    }
}

class CheckImportFolderResponsTest {

    @Test
    void testSetAndGetResult() {
        CheckImportFolderRespons resp = new CheckImportFolderRespons();
        resp.setResult("success");
        assertEquals("success", resp.getResult());
    }

    @Test
    void testSetAndGetMaxSize() {
        CheckImportFolderRespons resp = new CheckImportFolderRespons();
        resp.setMaxSize("500MB");
        assertEquals("500MB", resp.getMaxSize());
    }

    @Test
    void testDefaultNullValues() {
        CheckImportFolderRespons resp = new CheckImportFolderRespons();
        assertNull(resp.getResult());
        assertNull(resp.getMaxSize());
    }
}

class SreachViewTest {

    @Test
    void testSetAndGetKeyWorld() {
        SreachView sv = new SreachView();
        sv.setKeyWorld("search-term");
        assertEquals("search-term", sv.getKeyWorld());
    }

    @Test
    void testDefaultKeyWorldNull() {
        SreachView sv = new SreachView();
        assertNull(sv.getKeyWorld());
    }

    @Test
    void testInheritsFolderViewFields() {
        SreachView sv = new SreachView();
        sv.setAccount("testAccount");
        assertEquals("testAccount", sv.getAccount());
    }
}

class PublicKeyInfoTest {

    @Test
    void testSetAndGetPublicKey() {
        PublicKeyInfo pki = new PublicKeyInfo();
        pki.setPublicKey("base64-encoded-public-key");
        assertEquals("base64-encoded-public-key", pki.getPublicKey());
    }

    @Test
    void testSetAndGetTime() {
        PublicKeyInfo pki = new PublicKeyInfo();
        assertEquals(0, pki.getTime());
        pki.setTime(123456789L);
        assertEquals(123456789L, pki.getTime());
    }

    @Test
    void testDefaultValues() {
        PublicKeyInfo pki = new PublicKeyInfo();
        assertNull(pki.getPublicKey());
        assertEquals(0, pki.getTime());
    }
}

class RemainingFolderViewTest {

    @Test
    void testSetAndGetFolderList() {
        RemainingFolderView rfv = new RemainingFolderView();
        assertNull(rfv.getFolderList());
        List<Folder> folders = new ArrayList<>();
        folders.add(new Folder());
        rfv.setFolderList(folders);
        assertEquals(1, rfv.getFolderList().size());
    }

    @Test
    void testSetAndGetFileList() {
        RemainingFolderView rfv = new RemainingFolderView();
        assertNull(rfv.getFileList());
        List<Node> files = new ArrayList<>();
        files.add(new Node());
        rfv.setFileList(files);
        assertEquals(1, rfv.getFileList().size());
    }
}

class FolderViewTest {

    @Test
    void testSetAndGetFolder() {
        FolderView fv = new FolderView();
        Folder folder = new Folder();
        folder.setFolderId("test-id");
        fv.setFolder(folder);
        assertEquals("test-id", fv.getFolder().getFolderId());
    }

    @Test
    void testSetAndGetParentList() {
        FolderView fv = new FolderView();
        List<Folder> parents = new ArrayList<>();
        parents.add(new Folder());
        fv.setParentList(parents);
        assertEquals(1, fv.getParentList().size());
    }

    @Test
    void testSetAndGetAccount() {
        FolderView fv = new FolderView();
        fv.setAccount("admin");
        assertEquals("admin", fv.getAccount());
    }

    @Test
    void testSetAndGetAuthList() {
        FolderView fv = new FolderView();
        List<String> auths = new ArrayList<>();
        auths.add("CREATE_NEW_FOLDER");
        fv.setAuthList(auths);
        assertEquals(1, fv.getAuthList().size());
    }

    @Test
    void testSetAndGetPublishTime() {
        FolderView fv = new FolderView();
        fv.setPublishTime("2026-05-20");
        assertEquals("2026-05-20", fv.getPublishTime());
    }

    @Test
    void testSetAndGetAllowChangePassword() {
        FolderView fv = new FolderView();
        fv.setAllowChangePassword("true");
        assertEquals("true", fv.getAllowChangePassword());
    }

    @Test
    void testSetAndGetShowFileChain() {
        FolderView fv = new FolderView();
        fv.setShowFileChain("false");
        assertEquals("false", fv.getShowFileChain());
    }

    @Test
    void testSetAndGetAllowSignUp() {
        FolderView fv = new FolderView();
        fv.setAllowSignUp("true");
        assertEquals("true", fv.getAllowSignUp());
    }

    @Test
    void testEnableDownloadZip() {
        FolderView fv = new FolderView();
        assertFalse(fv.isEnableDownloadZip());
        fv.setEnableDownloadZip(true);
        assertTrue(fv.isEnableDownloadZip());
    }

    @Test
    void testEnableFFMPEG() {
        FolderView fv = new FolderView();
        assertFalse(fv.isEnableFFMPEG());
        fv.setEnableFFMPEG(true);
        assertTrue(fv.isEnableFFMPEG());
    }

    @Test
    void testSetAndGetSelectStep() {
        FolderView fv = new FolderView();
        assertEquals(0, fv.getSelectStep());
        fv.setSelectStep(50);
        assertEquals(50, fv.getSelectStep());
    }

    @Test
    void testSetAndGetOffsets() {
        FolderView fv = new FolderView();
        assertEquals(0, fv.getFoldersOffset());
        assertEquals(0, fv.getFilesOffset());
        fv.setFoldersOffset(10);
        fv.setFilesOffset(20);
        assertEquals(10, fv.getFoldersOffset());
        assertEquals(20, fv.getFilesOffset());
    }
}

class FolderCountResultTest {

    @Test
    void testSetAndGetTotalSize() {
        FolderCountResult fcr = new FolderCountResult();
        assertEquals(0, fcr.getTotalSize());
        fcr.setTotalSize(1024000);
        assertEquals(1024000, fcr.getTotalSize());
    }

    @Test
    void testSetAndGetFolderNum() {
        FolderCountResult fcr = new FolderCountResult();
        assertEquals(0, fcr.getFolderNum());
        fcr.setFolderNum(5);
        assertEquals(5, fcr.getFolderNum());
    }

    @Test
    void testSetAndGetFileNum() {
        FolderCountResult fcr = new FolderCountResult();
        assertEquals(0, fcr.getFileNum());
        fcr.setFileNum(100);
        assertEquals(100, fcr.getFileNum());
    }
}

class CreateNewFolderByNameResponsTest {

    @Test
    void testSetAndGetResult() {
        CreateNewFolderByNameRespons resp = new CreateNewFolderByNameRespons();
        resp.setResult("success");
        assertEquals("success", resp.getResult());
    }

    @Test
    void testSetAndGetNewName() {
        CreateNewFolderByNameRespons resp = new CreateNewFolderByNameRespons();
        resp.setNewName("newfolder (1)");
        assertEquals("newfolder (1)", resp.getNewName());
    }

    @Test
    void testDefaultNullValues() {
        CreateNewFolderByNameRespons resp = new CreateNewFolderByNameRespons();
        assertNull(resp.getResult());
        assertNull(resp.getNewName());
    }
}