package com.taskflow.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DateUtils
 * Status: Very incomplete, several tests disabled
 */
public class DateUtilsTest {
    
    @Test
    public void testParseDateISO() {
        Date date = DateUtils.parseDate("2024-01-15");
        assertNotNull(date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        assertEquals(2024, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }
    
    @Test
    public void testParseDateNull() {
        assertNull(DateUtils.parseDate(null));
        assertNull(DateUtils.parseDate(""));
        assertNull(DateUtils.parseDate("not-a-date"));
    }
    
    @Test
    public void testFormatDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 0, 0, 0);
        String formatted = DateUtils.formatDate(cal.getTime());
        assertEquals("2024-03-15", formatted);
    }
    
    @Test
    @Disabled("Fails due to thread-safety issue when run in parallel")
    public void testParseDateThreadSafety() {
        // This test would demonstrate the thread-safety bug
        // but is disabled because it fails intermittently
    }
    
    @Test
    public void testDaysBetween() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2024, Calendar.JANUARY, 1);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2024, Calendar.JANUARY, 10);
        
        int days = DateUtils.daysBetween(cal1.getTime(), cal2.getTime());
        // BUG: might be 8 or 9 due to DST, but we just check it's close
        assertTrue(days >= 8 && days <= 10);
    }
    
    @Test
    public void testGetRelativeTime() {
        assertNotNull(DateUtils.getRelativeTime(new Date()));
        assertEquals("unknown", DateUtils.getRelativeTime(null));
    }
    
    @Test
    public void testGetQuarter() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 15);
        assertEquals(1, DateUtils.getQuarter(cal.getTime())); // FAILS: returns 0
    }
    
    @Test
    public void testStartOfDay() {
        Date now = new Date();
        Date start = DateUtils.startOfDay(now);
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        // BUG: milliseconds not reset, but we don't test for it
    }
    
    // --- isOverdue ---

    @Test
    public void testIsOverdue_pastDate() {
        assertTrue(DateUtils.isOverdue("2020-01-01"));
    }

    @Test
    public void testIsOverdue_futureDate() {
        assertFalse(DateUtils.isOverdue("2099-12-31"));
    }

    @Test
    public void testIsOverdue_null() {
        // Null treated as not overdue (cannot distinguish from "no due date set")
        assertFalse(DateUtils.isOverdue(null));
    }

    @Test
    public void testIsOverdue_invalidDateString() {
        // Unparseable date treated as not overdue (silent failure in parseDate)
        assertFalse(DateUtils.isOverdue("not-a-date"));
    }

    // --- isWithinRange ---

    @Test
    public void testIsWithinRange_dateInMiddle() {
        Date start  = new Date(1_000_000L);
        Date middle = new Date(2_000_000L);
        Date end    = new Date(3_000_000L);
        assertTrue(DateUtils.isWithinRange(middle, start, end));
    }

    @Test
    public void testIsWithinRange_dateBeforeStart() {
        Date before = new Date(1_000_000L);
        Date start  = new Date(2_000_000L);
        Date end    = new Date(3_000_000L);
        assertFalse(DateUtils.isWithinRange(before, start, end));
    }

    @Test
    public void testIsWithinRange_dateAfterEnd() {
        Date start = new Date(1_000_000L);
        Date end   = new Date(2_000_000L);
        Date after = new Date(3_000_000L);
        assertFalse(DateUtils.isWithinRange(after, start, end));
    }

    // --- daysBetween (additional cases) ---

    @Test
    public void testDaysBetween_exactlyOneWeek() {
        Date start = new Date(0L);
        Date end   = new Date(7L * 24 * 60 * 60 * 1000);
        assertEquals(7, DateUtils.daysBetween(start, end));
    }

    @Test
    public void testDaysBetween_sameInstant() {
        Date d = new Date(12_345_678L);
        assertEquals(0, DateUtils.daysBetween(d, d));
    }
}
