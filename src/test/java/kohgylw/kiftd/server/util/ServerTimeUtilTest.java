package kohgylw.kiftd.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ServerTimeUtilTest {

    @Test
    void testAccurateToSecondFormat() {
        String result = ServerTimeUtil.accurateToSecond();
        assertTrue(result.contains("年"));
        assertTrue(result.contains("月"));
        assertTrue(result.contains("日"));
        assertTrue(result.contains(":"));
    }

    @Test
    void testAccurateToMinuteFormat() {
        String result = ServerTimeUtil.accurateToMinute();
        assertTrue(result.contains("年"));
        assertTrue(result.contains("月"));
        assertTrue(result.contains("日"));
        assertTrue(result.contains(":"));
    }

    @Test
    void testAccurateToMinuteHasTwoColons() {
        String result = ServerTimeUtil.accurateToMinute();
        assertEquals(1, result.chars().filter(c -> c == ':').count(),
                "Minute format should have exactly one colon (HH:mm)");
    }

    @Test
    void testAccurateToSecondHasTwoColons() {
        String result = ServerTimeUtil.accurateToSecond();
        assertEquals(2, result.chars().filter(c -> c == ':').count(),
                "Second format should have exactly two colons (HH:mm:ss)");
    }

    @Test
    void testAccurateToDayFormat() {
        String result = ServerTimeUtil.accurateToDay();
        assertTrue(result.contains("年"));
        assertTrue(result.contains("月"));
        assertTrue(result.contains("日"));
    }

    @Test
    void testAccurateToDayHasNoColons() {
        String result = ServerTimeUtil.accurateToDay();
        assertFalse(result.contains(":"));
    }

    @Test
    void testTimesAreConsistent() {
        String day = ServerTimeUtil.accurateToDay();
        String minute = ServerTimeUtil.accurateToMinute();
        String second = ServerTimeUtil.accurateToSecond();
        assertTrue(minute.startsWith(day),
                "accurateToMinute should start with accurateToDay prefix");
        assertTrue(second.startsWith(day),
                "accurateToSecond should start with accurateToDay prefix");
    }

    @Test
    void testAccurateToMinuteDoesNotContainSeconds() {
        String result = ServerTimeUtil.accurateToMinute();
        assertTrue(result.matches("^\\d{4}年\\d{2}月\\d{2}日 \\d{2}:\\d{2}$"),
                "Minute format should be yyyy年MM月dd日 HH:mm");
    }

    @Test
    void testAccurateToSecondMatchesPattern() {
        String result = ServerTimeUtil.accurateToSecond();
        assertTrue(result.matches("^\\d{4}年\\d{2}月\\d{2}日 \\d{2}:\\d{2}:\\d{2}$"),
                "Second format should be yyyy年MM月dd日 HH:mm:ss");
    }

}