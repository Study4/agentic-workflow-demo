package com.taskflow.repository;

import com.taskflow.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TaskRepository custom queries.
 *
 * Uses @DataJpaTest (H2 in-memory, no Spring MVC context) for fast, isolated tests.
 *
 * Status legend: 0=todo, 1=in-progress, 2=done, 3=cancelled, 4=blocked, 5=review
 * Priority legend: 1=low, 2=medium, 3=high, 4=critical, 5=blocker
 */
@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    // ── findByStatus ────────────────────────────────────────────────────────────

    @Test
    void findByStatus_returnsOnlyMatchingStatus() {
        Task todo = task("Todo task", 0, 2, "task");
        Task inProgress = task("In-progress task", 1, 2, "task");
        Task done = task("Done task", 2, 2, "task");
        taskRepository.saveAll(List.of(todo, inProgress, done));

        List<Task> results = taskRepository.findByStatus(1);

        assertEquals(1, results.size());
        assertEquals("In-progress task", results.get(0).title);
    }

    @Test
    void findByStatus_returnsEmptyWhenNoneMatch() {
        taskRepository.save(task("Todo task", 0, 2, "task"));

        List<Task> results = taskRepository.findByStatus(5);

        assertTrue(results.isEmpty());
    }

    // ── findByAssigneeId ─────────────────────────────────────────────────────────

    @Test
    void findByAssigneeId_returnsTasksForGivenAssignee() {
        Task t1 = task("Alice task 1", 0, 2, "bug");
        t1.assignee_id = 100L;
        Task t2 = task("Alice task 2", 1, 3, "feature");
        t2.assignee_id = 100L;
        Task t3 = task("Bob task", 0, 2, "task");
        t3.assignee_id = 200L;
        taskRepository.saveAll(List.of(t1, t2, t3));

        List<Task> results = taskRepository.findByAssigneeId(100L);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(t -> Long.valueOf(100L).equals(t.assignee_id)));
    }

    @Test
    void findByAssigneeId_returnsEmptyForUnknownAssignee() {
        Task t = task("Some task", 0, 2, "task");
        t.assignee_id = 100L;
        taskRepository.save(t);

        List<Task> results = taskRepository.findByAssigneeId(999L);

        assertTrue(results.isEmpty());
    }

    // ── findByProjectCode ────────────────────────────────────────────────────────

    @Test
    void findByProjectCode_returnsTasksForProject() {
        Task t1 = task("Project A task 1", 0, 2, "task");
        t1.projectCode = "PROJ-001";
        Task t2 = task("Project A task 2", 1, 3, "bug");
        t2.projectCode = "PROJ-001";
        Task t3 = task("Project B task", 0, 2, "task");
        t3.projectCode = "PROJ-002";
        taskRepository.saveAll(List.of(t1, t2, t3));

        List<Task> results = taskRepository.findByProjectCode("PROJ-001");

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(t -> "PROJ-001".equals(t.projectCode)));
    }

    // ── findHighPriorityTasks ────────────────────────────────────────────────────

    @Test
    void findHighPriorityTasks_returnsPriority3AndAbove() {
        Task low = task("Low", 0, 1, "task");
        Task medium = task("Medium", 0, 2, "task");
        Task high = task("High", 0, 3, "task");
        Task critical = task("Critical", 0, 4, "task");
        Task blocker = task("Blocker", 0, 5, "task");
        taskRepository.saveAll(List.of(low, medium, high, critical, blocker));

        List<Task> results = taskRepository.findHighPriorityTasks();

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(t -> t.priority >= 3));
        assertTrue(results.stream().noneMatch(t -> t.priority < 3));
    }

    @Test
    void findHighPriorityTasks_excludesLowAndMediumPriority() {
        taskRepository.save(task("Low priority", 0, 1, "task"));
        taskRepository.save(task("Medium priority", 0, 2, "task"));

        List<Task> results = taskRepository.findHighPriorityTasks();

        assertTrue(results.isEmpty());
    }

    // ── findActiveTasks ──────────────────────────────────────────────────────────

    @Test
    void findActiveTasks_includesTodoAndInProgress() {
        Task todo = task("Todo", 0, 2, "task");
        Task inProgress = task("In progress", 1, 2, "task");
        taskRepository.saveAll(List.of(todo, inProgress));

        List<Task> results = taskRepository.findActiveTasks();

        assertEquals(2, results.size());
    }

    @Test
    void findActiveTasks_excludesDoneTasks() {
        Task done = task("Done task", 2, 2, "task");
        Task active = task("Active task", 0, 2, "task");
        taskRepository.saveAll(List.of(done, active));

        List<Task> results = taskRepository.findActiveTasks();

        assertEquals(1, results.size());
        assertEquals("Active task", results.get(0).title);
    }

    /**
     * BUG PIN — findActiveTasks() returns cancelled tasks (status=3).
     *
     * The JPQL query only excludes status=2 (done), but the FIXME in TaskRepository
     * documents that it should also exclude status=3 (cancelled). This test pins the
     * current (buggy) behaviour so any fix is immediately visible.
     *
     * Expected correct behaviour: cancelled tasks should NOT be returned.
     * Actual current behaviour: cancelled tasks ARE returned (documented here).
     */
    @Test
    void findActiveTasks_bugPin_cancelledTasksAreIncorrectlyReturned() {
        Task cancelled = task("Cancelled task", 3, 2, "task");
        taskRepository.save(cancelled);

        List<Task> results = taskRepository.findActiveTasks();

        // BUG: cancelled task (status=3) is returned because query only excludes status=2
        assertEquals(1, results.size(), "BUG: findActiveTasks() returns cancelled tasks");
        assertEquals("Cancelled task", results.get(0).title);
    }

    // ── findByType ───────────────────────────────────────────────────────────────

    @Test
    void findByType_returnsOnlyMatchingType() {
        taskRepository.save(task("Bug 1", 0, 3, "bug"));
        taskRepository.save(task("Bug 2", 1, 2, "bug"));
        taskRepository.save(task("Feature", 0, 2, "feature"));

        List<Task> bugs = taskRepository.findByType("bug");

        assertEquals(2, bugs.size());
        assertTrue(bugs.stream().allMatch(t -> "bug".equals(t.type)));
    }

    // ── findActiveTasksByAssignee ────────────────────────────────────────────────

    @Test
    void findActiveTasksByAssignee_returnsAssigneesNonDoneTasks() {
        Task t1 = task("Active task", 0, 3, "task");
        t1.assignee_id = 10L;
        Task t2 = task("Done task", 2, 2, "task");
        t2.assignee_id = 10L;
        Task t3 = task("Other user task", 0, 2, "task");
        t3.assignee_id = 20L;
        taskRepository.saveAll(List.of(t1, t2, t3));

        List<Task> results = taskRepository.findActiveTasksByAssignee(10L);

        assertEquals(1, results.size());
        assertEquals("Active task", results.get(0).title);
    }

    @Test
    void findActiveTasksByAssignee_orderedByPriorityDescending() {
        Task low = task("Low priority", 0, 1, "task");
        low.assignee_id = 10L;
        Task high = task("High priority", 0, 4, "task");
        high.assignee_id = 10L;
        Task medium = task("Medium priority", 0, 2, "task");
        medium.assignee_id = 10L;
        taskRepository.saveAll(List.of(low, high, medium));

        List<Task> results = taskRepository.findActiveTasksByAssignee(10L);

        assertEquals(3, results.size());
        assertEquals(4, results.get(0).priority, "highest priority first");
        assertEquals(2, results.get(1).priority);
        assertEquals(1, results.get(2).priority, "lowest priority last");
    }

    /**
     * BUG PIN — findActiveTasksByAssignee() returns cancelled tasks (status=3).
     *
     * Same bug as findActiveTasks(): the query filters status != 2 but not status != 3.
     * Cancelled tasks assigned to a user should not appear in their active task list.
     */
    @Test
    void findActiveTasksByAssignee_bugPin_cancelledTasksAreIncorrectlyReturned() {
        Task cancelled = task("Cancelled", 3, 2, "task");
        cancelled.assignee_id = 10L;
        taskRepository.save(cancelled);

        List<Task> results = taskRepository.findActiveTasksByAssignee(10L);

        // BUG: cancelled task is returned because query only excludes status=2
        assertEquals(1, results.size(), "BUG: cancelled tasks should not be in active list");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private Task task(String title, int status, int priority, String type) {
        Task t = new Task();
        t.title = title;
        t.status = status;
        t.priority = priority;
        t.type = type;
        return t;
    }
}
