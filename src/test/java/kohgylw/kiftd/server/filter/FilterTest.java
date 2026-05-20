package kohgylw.kiftd.server.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;

class ProtectedURLFilterTest {

    @Test
    void testFilterImplementsFilter() {
        ProtectedURLFilter filter = new ProtectedURLFilter();
        assertInstanceOf(Filter.class, filter);
    }

    @Test
    void testInitDoesNotThrow() {
        ProtectedURLFilter filter = new ProtectedURLFilter();
        assertDoesNotThrow(() -> filter.init(null));
        assertDoesNotThrow(() -> filter.destroy());
    }
}

class MastLoginFilterTest {

    @Test
    void testFilterImplementsFilter() {
        MastLoginFilter filter = new MastLoginFilter();
        assertInstanceOf(Filter.class, filter);
    }

    @Test
    void testInitDoesNotThrow() {
        MastLoginFilter filter = new MastLoginFilter();
        assertDoesNotThrow(() -> filter.init(null));
        assertDoesNotThrow(() -> filter.destroy());
    }
}

class IPFilterTest {

    @Test
    void testFilterImplementsFilter() {
        IPFilter filter = new IPFilter();
        assertInstanceOf(Filter.class, filter);
    }

    @Test
    void testDestroyDoesNotThrow() {
        IPFilter filter = new IPFilter();
        assertDoesNotThrow(() -> filter.destroy());
    }
}