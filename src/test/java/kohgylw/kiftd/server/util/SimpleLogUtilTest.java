package kohgylw.kiftd.server.util;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimpleLogUtilTest {

    @Mock
    private LogUtil logUtil;

    @Test
    void testMock() {
        logUtil.writeException(new Exception());
        verify(logUtil, times(1)).writeException(any(Exception.class));
    }
}
