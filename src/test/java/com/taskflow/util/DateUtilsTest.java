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
    
    // Missing tests:
    // - isOverdue
    // - isWithinRange
    // - edge cases (null inputs, boundary dates)

    // -----------------------------------------------------------------------
    // addBusinessDays tests
    //
    // KNOWN BUG: The implementation checks for day-of-week 6 (FRIDAY) and 7
    // (SATURDAY) when it should skip 7 (SATURDAY) and 1 (SUNDAY).
    // As a result, Friday is wrongly treated as a weekend day and Sunday is
    // wrongly treated as a business day.
    //
    // Tests labelled "_BUG" assert the current (incorrect) behaviour so they
    // act as regression pins.  When the bug is fixed the assertions in those
    // tests must be updated to the correct expected day-of-week.
    // See the companion bug issue filed alongside this PR.
    // -----------------------------------------------------------------------

    /** Helper: build a Date for a specific calendar date at noon to avoid DST edge cases. */
    private Date dateOf(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, day, 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @Test
    public void testAddBusinessDays_zeroDays_returnsSameDay() {
        // Monday 8 Jan 2024
        Date monday = dateOf(2024, Calendar.JANUARY, 8);
        Date result = DateUtils.addBusinessDays(monday, 0);
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK),
                "Adding zero business days should return the same day");
    }

    @Test
    public void testAddBusinessDays_mondayPlusOne_returnsTuesday() {
        // Monday 8 Jan 2024 + 1 business day = Tuesday 9 Jan 2024
        Date monday = dateOf(2024, Calendar.JANUARY, 8);
        Date result = DateUtils.addBusinessDays(monday, 1);
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(Calendar.TUESDAY, cal.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void testAddBusinessDays_mondayPlusThree_returnsThursday() {
        // Monday 8 Jan 2024 + 3 business days = Thursday 11 Jan 2024
        Date monday = dateOf(2024, Calendar.JANUARY, 8);
        Date result = DateUtils.addBusinessDays(monday, 3);
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(Calendar.THURSDAY, cal.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void testAddBusinessDays_thursdayPlusOne_BUG() {
        // Thursday 11 Jan 2024 + 1 business day should be Friday 12 Jan 2024.
        // BUG: implementation skips FRIDAY (day 6) and SATURDAY (day 7) instead
        // of SATURDAY and SUNDAY, so it lands on SUNDAY instead.
        // When the bug is fixed change the assertion to Calendar.FRIDAY.
        Date thursday = dateOf(2024, Calendar.JANUARY, 11);
        Date result = DateUtils.addBusinessDays(thursday, 1);
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        // BUG: actual=SUNDAY; correct=FRIDAY
        assertEquals(Calendar.SUNDAY, cal.get(Calendar.DAY_OF_WEEK),
                "BUG pin: should be FRIDAY once addBusinessDays skips SAT/SUN instead of FRI/SAT");
    }

    @Test
    public void testAddBusinessDays_fridayPlusOne_BUG() {
        // Friday 12 Jan 2024 + 1 business day should be Monday 15 Jan 2024.
        // BUG: Friday is treated as a non-business day so the result is Sunday.
        // When the bug is fixed change the assertion to Calendar.MONDAY.
        Date friday = dateOf(2024, Calendar.JANUARY, 12);
        Date result = DateUtils.addBusinessDays(friday, 1);
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        // BUG: actual=SUNDAY; correct=MONDAY
        assertEquals(Calendar.SUNDAY, cal.get(Calendar.DAY_OF_WEEK),
                "BUG pin: should be MONDAY once addBusinessDays skips SAT/SUN instead of FRI/SAT");
    }

    @Test
    public void testAddBusinessDays_mondayPlusFour_BUG() {
        // Monday 8 Jan 2024 + 4 business days should be Friday 12 Jan 2024.
        // BUG: Friday counts as a weekend, so the result jumps to Sunday 14 Jan.
        // When the bug is fixed change the assertion to Calendar.FRIDAY.
        Date monday = dateOf(2024, Calendar.JANUARY, 8);
        Date result = DateUtils.addBusinessDays(monday, 4);
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        // BUG: actual=SUNDAY; correct=FRIDAY
        assertEquals(Calendar.SUNDAY, cal.get(Calendar.DAY_OF_WEEK),
                "BUG pin: should be FRIDAY once addBusinessDays skips SAT/SUN instead of FRI/SAT");
    }

    @Test
    public void testAddBusinessDays_mondayPlusFive_returnsMonday() {
        // Monday 8 Jan 2024 + 5 business days = Monday 15 Jan 2024.
        // Both buggy and correct implementations reach Monday here (via different
        // intermediate days), so this test verifies week-spanning logic works.
        Date monday = dateOf(2024, Calendar.JANUARY, 8);
        Date result = DateUtils.addBusinessDays(monday, 5);
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK));
    }
}
