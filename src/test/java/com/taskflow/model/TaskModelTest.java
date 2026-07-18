package com.taskflow.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Task} model.
 *
 * <p>Bug pins for {@link Task#toString()}:
 * <ul>
 *   <li>If {@code assignee_id} is null, {@code assignee_id.toString()} throws NPE.</li>
 *   <li>If {@code due_date} is null, {@code due_date.trim()} throws NPE.</li>
 * </ul>
 */
class TaskModelTest {

    // --- constructor tests ---

    @Test
    void shortConstructor_setsFieldsCorrectly() {
        Task task = new Task("Fix login", 3, "bug", "Login fails on empty password", 42L);
        assertEquals("Fix login", task.getTitle());
        assertEquals(3, task.getPriority());
        assertEquals("bug", task.type);
        assertEquals("Fix login", task.getTitle());
        assertEquals(42L, task.assignee_id);
        assertEquals(0, task.getStatus()); // default status is 0 (todo)
        assertNotNull(task.createdDate);
    }

    @Test
    void defaultConstructor_allFieldsDefaultToNull() {
        Task task = new Task();
        assertNull(task.getId());
        assertNull(task.getTitle());
        assertEquals(0, task.getStatus());
        assertEquals(0, task.getPriority());
    }

    // --- toString() NPE bug-pin tests ---

    /**
     * Bug pin: Task.toString() calls assignee_id.toString() without null check.
     * Constructing a Task via no-arg constructor leaves assignee_id=null → NPE.
     */
    @Test
    void toString_nullAssigneeId_throwsNPE() {
        Task task = new Task();
        task.setTitle("Test task");
        task.due_date = "2026-01-01";
        // BUG: assignee_id.toString() throws NullPointerException
        assertThrows(NullPointerException.class, task::toString);
    }

    /**
     * Bug pin: Task.toString() calls due_date.trim() without null check.
     */
    @Test
    void toString_nullDueDate_throwsNPE() {
        Task task = new Task();
        task.setTitle("Test task");
        task.assignee_id = 1L;
        // due_date is null → BUG: due_date.trim() throws NullPointerException
        assertThrows(NullPointerException.class, task::toString);
    }

    /**
     * Bug pin: Task constructed via no-arg constructor has null assignee_id and due_date;
     * calling toString() throws NPE even for a task that is otherwise valid.
     */
    @Test
    void toString_defaultConstructorFields_throwsNPE() {
        Task task = new Task();
        task.setTitle("Minimal task");
        assertThrows(NullPointerException.class, task::toString);
    }

    /**
     * Positive case: toString() succeeds when both assignee_id and due_date are set.
     */
    @Test
    void toString_allRequiredFieldsSet_noException() {
        Task task = new Task();
        task.setTitle("Deploy");
        task.setStatus(1);
        task.assignee_id = 7L;
        task.due_date = "2026-12-31";
        String result = assertDoesNotThrow(task::toString);
        assertTrue(result.contains("Deploy"));
        assertTrue(result.contains("7"));
    }

    // --- attachmentUrls initial state ---

    @Test
    void attachmentUrls_initializedAsEmptyList() {
        Task task = new Task();
        assertNotNull(task.attachmentUrls);
        assertTrue(task.attachmentUrls.isEmpty());
    }
}
