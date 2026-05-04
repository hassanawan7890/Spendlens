package com.spendlens.app;

import com.spendlens.app.utils.DateUtils;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Calendar;

/**
 * Unit tests for DateUtils
 *
 * Place in: app/src/test/java/com/spendlens/app/DateUtilsTest.java
 * Run with: Right-click → Run 'DateUtilsTest'
 */
public class DateUtilsTest {

    // ── formatDate ────────────────────────────────────────────────────────────

    @Test
    public void formatDate_returnsNonEmptyString() {
        String result = DateUtils.formatDate(System.currentTimeMillis());
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void formatDate_knownTimestamp_returnsCorrectFormat() {
        // Jan 1 2024 00:00:00 UTC
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 1, 0, 0, 0);
        String result = DateUtils.formatDate(cal.getTimeInMillis());
        assertTrue(result.contains("2024"));
        assertTrue(result.contains("Jan"));
    }

    @Test
    public void formatMonthYear_returnsMonthAndYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15);
        String result = DateUtils.formatMonthYear(cal.getTimeInMillis());
        assertTrue(result.contains("2024"));
        assertTrue(result.contains("March"));
    }

    // ── isToday ───────────────────────────────────────────────────────────────

    @Test
    public void isToday_currentTime_returnsTrue() {
        assertTrue(DateUtils.isToday(System.currentTimeMillis()));
    }

    @Test
    public void isToday_yesterday_returnsFalse() {
        long yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000L);
        assertFalse(DateUtils.isToday(yesterday));
    }

    @Test
    public void isToday_tomorrow_returnsFalse() {
        long tomorrow = System.currentTimeMillis() + (24 * 60 * 60 * 1000L);
        assertFalse(DateUtils.isToday(tomorrow));
    }

    @Test
    public void isToday_startOfToday_returnsTrue() {
        long[] range = DateUtils.getTodayRange();
        assertTrue(DateUtils.isToday(range[0]));
    }

    @Test
    public void isToday_endOfToday_returnsTrue() {
        long[] range = DateUtils.getTodayRange();
        assertTrue(DateUtils.isToday(range[1]));
    }

    // ── getTodayRange ─────────────────────────────────────────────────────────

    @Test
    public void getTodayRange_startIsBeforeEnd() {
        long[] range = DateUtils.getTodayRange();
        assertTrue(range[0] < range[1]);
    }

    @Test
    public void getTodayRange_rangeIs24Hours() {
        long[] range = DateUtils.getTodayRange();
        long diff = range[1] - range[0];
        // Should be just under 24 hours in ms
        assertTrue(diff > 0);
        assertTrue(diff <= 24 * 60 * 60 * 1000L);
    }

    // ── getCurrentMonthRange ──────────────────────────────────────────────────

    @Test
    public void getCurrentMonthRange_startIsBeforeEnd() {
        long[] range = DateUtils.getCurrentMonthRange();
        assertTrue(range[0] < range[1]);
    }

    @Test
    public void getCurrentMonthRange_nowIsWithinRange() {
        long[] range = DateUtils.getCurrentMonthRange();
        long now = System.currentTimeMillis();
        assertTrue(now >= range[0] && now <= range[1]);
    }

    @Test
    public void getCurrentMonthRange_startIsFirstOfMonth() {
        long[] range = DateUtils.getCurrentMonthRange();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(range[0]);
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    }

    // ── getDaysElapsedThisMonth ───────────────────────────────────────────────

    @Test
    public void getDaysElapsedThisMonth_isAtLeast1() {
        assertTrue(DateUtils.getDaysElapsedThisMonth() >= 1);
    }

    @Test
    public void getDaysElapsedThisMonth_isNotMoreThan31() {
        assertTrue(DateUtils.getDaysElapsedThisMonth() <= 31);
    }

    // ── getTotalDaysThisMonth ─────────────────────────────────────────────────

    @Test
    public void getTotalDaysThisMonth_isBetween28And31() {
        int total = DateUtils.getTotalDaysThisMonth();
        assertTrue(total >= 28 && total <= 31);
    }

    // ── getDaysRemainingThisMonth ─────────────────────────────────────────────

    @Test
    public void getDaysRemainingThisMonth_isNonNegative() {
        assertTrue(DateUtils.getDaysRemainingThisMonth() >= 0);
    }

    @Test
    public void getDaysRemainingThisMonth_plusElapsedEqualsTotal() {
        int elapsed   = DateUtils.getDaysElapsedThisMonth();
        int remaining = DateUtils.getDaysRemainingThisMonth();
        int total     = DateUtils.getTotalDaysThisMonth();
        assertEquals(total, elapsed + remaining);
    }

    // ── now ───────────────────────────────────────────────────────────────────

    @Test
    public void now_returnsPositiveValue() {
        assertTrue(DateUtils.now() > 0);
    }

    @Test
    public void now_isCloseToSystemTime() {
        long before = System.currentTimeMillis();
        long result = DateUtils.now();
        long after  = System.currentTimeMillis();
        assertTrue(result >= before && result <= after);
    }
}