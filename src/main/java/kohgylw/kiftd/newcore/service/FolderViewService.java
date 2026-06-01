package kohgylw.kiftd.newcore.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public interface FolderViewService {

	String getFolderViewJson(String folderId, HttpSession session, HttpServletRequest request);

	String getRemainingFolderViewJson(HttpServletRequest request);

	String getSearchViewJson(HttpServletRequest request);
}
