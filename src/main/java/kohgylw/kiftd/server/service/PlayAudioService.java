package kohgylw.kiftd.server.service;

import jakarta.servlet.http.*;

public interface PlayAudioService
{
    String getAudioInfoListByJson(final HttpServletRequest request);
}
