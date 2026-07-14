package com.taskflow.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug-pin tests for Project model methods.
 *
 * Tests marked with a BUG comment document currently broken behaviour.
 * They should be updated (and the underlying issue fixed) when the bug
 * is resolved.
 */
class ProjectTest {

    // -----------------------------------------------------------------------
    // getProgress() – division by zero when taskCount == 0
    // -----------------------------------------------------------------------

    /**
     * BUG: getProgress() divides by taskCount without guarding against zero,
     * producing NaN.  Related to Bug #3 / Issue #3.
     */
    @Test
    void getProgress_withZeroTasks_returnsNaN() {
        Project project = new Project("Demo", "DEMO");
        // taskCount and completedTaskCount both default to 0
        double progress = project.getProgress();
        // BUG: should return 0.0 but returns NaN (0.0 / 0 = NaN)
        assertTrue(Double.isNaN(progress),
                "BUG: getProgress() should guard against taskCount == 0 but currently returns NaN");
    }

    @Test
    void getProgress_withNonZeroTasks_returnsCorrectPercentage() {
        Project project = new Project("Demo", "DEMO");
        project.setTaskCount(10);
        project.setCompletedTaskCount(5);
        assertEquals(50.0, project.getProgress(), 0.001);
    }

    @Test
    void getProgress_withAllTasksCompleted_returns100() {
        Project project = new Project("Demo", "DEMO");
        project.setTaskCount(4);
        project.setCompletedTaskCount(4);
        assertEquals(100.0, project.getProgress(), 0.001);
    }

    @Test
    void getProgress_withNoCompletedTasks_returns0() {
        Project project = new Project("Demo", "DEMO");
        project.setTaskCount(8);
        project.setCompletedTaskCount(0);
        assertEquals(0.0, project.getProgress(), 0.001);
    }

    // -----------------------------------------------------------------------
    // getMemberIds() – NPE on null members, NumberFormatException on bad data
    // -----------------------------------------------------------------------

    @Test
    void getMemberIds_withValidMembers_returnsParsedIds() {
        Project project = new Project("Demo", "DEMO");
        project.setMembers("1,5,12,23");
        List<Long> ids = project.getMemberIds();
        assertEquals(4, ids.size());
        assertEquals(1L, ids.get(0));
        assertEquals(5L, ids.get(1));
        assertEquals(12L, ids.get(2));
        assertEquals(23L, ids.get(3));
    }

    @Test
    void getMemberIds_withSingleMember_returnsSingleId() {
        Project project = new Project("Demo", "DEMO");
        project.setMembers("42");
        List<Long> ids = project.getMemberIds();
        assertEquals(1, ids.size());
        assertEquals(42L, ids.get(0));
    }

    /**
     * BUG: getMemberIds() calls members.split(",") without a null-check,
     * throwing NullPointerException when members has never been set.
     */
    @Test
    void getMemberIds_withNullMembers_throwsNullPointerException() {
        Project project = new Project("Demo", "DEMO");
        // members field is null (constructor does not initialise it)
        assertThrows(NullPointerException.class, project::getMemberIds,
                "BUG: getMemberIds() should handle null members gracefully but throws NPE");
    }

    /**
     * Java's String.split(",") with default limit drops trailing empty tokens,
     * so a trailing comma is silently tolerated — the caller may not notice
     * the missing empty entry.  This test documents the actual (safe) behaviour.
     */
    @Test
    void getMemberIds_withTrailingComma_parsesSuccessfully() {
        Project project = new Project("Demo", "DEMO");
        project.setMembers("1,2,3,");
        // String.split(",") with limit=0 strips the trailing empty token —
        // the result is [1, 2, 3], same as without the trailing comma.
        List<Long> ids = project.getMemberIds();
        assertEquals(3, ids.size());
        assertEquals(1L, ids.get(0));
        assertEquals(2L, ids.get(1));
        assertEquals(3L, ids.get(2));
    }

    /**
     * BUG: getMemberIds() does not trim whitespace before calling
     * Long.parseLong(), causing NumberFormatException on "1, 2, 3".
     */
    @Test
    void getMemberIds_withWhitespacePaddedIds_throwsNumberFormatException() {
        Project project = new Project("Demo", "DEMO");
        project.setMembers("1, 2, 3");
        assertThrows(NumberFormatException.class, project::getMemberIds,
                "BUG: getMemberIds() should trim whitespace before parsing but throws NumberFormatException");
    }
}
