package com.taskflow.service;

import com.taskflow.model.Task;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.repository.ProjectRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TaskService
 * 
 * Status: MOSTLY BROKEN
 * Last updated: 2021-08-15
 * 
 * Some tests are disabled because "they were failing and blocking the build"
 * Some tests have no assertions (just exist to inflate coverage numbers)
 * Some tests depend on execution order (fragile)
 */
@SpringBootTest
public class TaskServiceTest {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    // NOTE: No @BeforeEach to clean up data - tests interfere with each other
    // DataInitializer also runs, adding extra data
    
    @Test
    public void testCreateTask() {
        Task task = new Task();
        task.title = "Test Task";
        task.description = "Test Description";
        task.priority = 2;
        task.type = "task";
        
        Task created = taskService.createTask(task);
        
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Test Task", created.getTitle());
        // Missing: assert status, priority, createdDate, etc.
    }
    
    @Test
    public void testCreateTaskWithoutTitle() {
        Task task = new Task();
        task.description = "No title";
        
        // Should throw exception
        assertThrows(RuntimeException.class, () -> {
            taskService.createTask(task);
        });
    }
    
    @Test
    public void testGetTask() {
        // Depends on DataInitializer having run - fragile
        Task task = taskService.getTask(1L);
        // Sometimes passes, sometimes fails depending on test order
        assertNotNull(task);
    }
    
    @Test
    public void testGetNonExistentTask() {
        Task task = taskService.getTask(99999L);
        assertNull(task); // Returns null instead of throwing - debatable design
    }
    
    @Test
    @Disabled("Disabled because it fails intermittently - TASK-567")
    public void testGetOverdueTasks() {
        List<Task> overdue = taskService.getOverdueTasks();
        // This test depends on the current date and sample data
        assertNotNull(overdue);
        assertTrue(overdue.size() > 0); // Fragile: depends on DataInitializer data
    }
    
    @Test
    public void testGetAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        assertNotNull(tasks);
        // DataInitializer seeds tasks at startup, so the list should be non-empty
        assertFalse(tasks.isEmpty(), "getAllTasks() should return seeded DataInitializer tasks");
    }
    
    @Test
    public void testGetTaskStatistics() {
        Map<String, Object> stats = taskService.getTaskStatistics();
        assertNotNull(stats);
        // FIXME: This test fails when no tasks are completed (division by zero)
    }
    
    @Test
    public void testSearchTasks() {
        // SECURITY: This test doesn't verify SQL injection protection
        // because there IS no SQL injection protection
        List<Map<String, Object>> results = taskService.searchTasks("test");
        assertNotNull(results);
    }
    
    @Test
    public void testUpdateTask() {
        // Create a task first
        Task task = new Task();
        task.title = "Original Title";
        task.priority = 1;
        task.type = "task";
        Task created = taskService.createTask(task);
        
        // Update it
        Task update = new Task();
        update.title = "Updated Title";
        update.priority = 3;
        
        Task updated = taskService.updateTask(created.getId(), update);
        assertEquals("Updated Title", updated.getTitle());
        // BUG: Doesn't check if priority was actually updated
    }
    
    @Test
    public void testDeleteTask() {
        Task task = new Task();
        task.title = "To Be Deleted";
        task.priority = 1;
        task.type = "task";
        Task created = taskService.createTask(task);
        
        taskService.deleteTask(created.getId());
        
        assertNull(taskService.getTask(created.getId()));
    }
    
    @Test
    @Disabled("Flaky test - depends on user data from DataInitializer")
    public void testAssignTask() {
        Task task = new Task();
        task.title = "Assign Me";
        task.priority = 2;
        task.type = "task";
        Task created = taskService.createTask(task);
        
        // Assumes user with ID 1 exists (from DataInitializer)
        Task assigned = taskService.assignTask(created.getId(), 1L);
        assertEquals(1L, (long) assigned.assignee_id);
    }
    
    @Test
    public void testTransitionStatus() {
        Task task = new Task();
        task.title = "Status Test";
        task.priority = 2;
        task.type = "task";
        task.status = 0; // TODO
        Task created = taskService.createTask(task);
        
        // TODO -> IN_PROGRESS should work
        Task inProgress = taskService.transitionStatus(created.getId(), 1);
        assertEquals(1, inProgress.getStatus());
    }
    
    @Test
    public void testInvalidTransition() {
        Task task = new Task();
        task.title = "Invalid Transition";
        task.priority = 2;
        task.type = "task";
        task.status = 0; // TODO
        Task created = taskService.createTask(task);
        
        // TODO -> DONE should fail (based on current transition rules)
        assertThrows(RuntimeException.class, () -> {
            taskService.transitionStatus(created.getId(), 2); // DONE
        });
    }
    
    @Test
    @Disabled("CSV import test - disabled because it's slow and flaky")
    public void testImportTasks() {
        String csv = "title,description,priority,type\n" +
                      "Import Task 1,Desc 1,2,task\n" +
                      "Import Task 2,Desc 2,3,bug\n";
        
        List<Task> imported = taskService.importTasks(csv);
        assertEquals(2, imported.size());
    }
    
    @Test
    public void testExportTasks() {
        String csv = taskService.exportTasks(null);
        assertNotNull(csv);
        assertTrue(csv.contains("ID,Title")); // Header check only
    }
    
    // These tests have NO assertions - they were added to inflate coverage numbers.
    // Now replaced with meaningful assertions that verify return-value contracts.
    @Test
    public void testGetTasksByStatus() {
        List<Task> todoTasks = taskService.getTasksByStatus(0);
        List<Task> inProgressTasks = taskService.getTasksByStatus(1);
        List<Task> doneTasks = taskService.getTasksByStatus(2);

        assertNotNull(todoTasks);
        assertNotNull(inProgressTasks);
        assertNotNull(doneTasks);
        // Every task returned must actually have the requested status
        todoTasks.forEach(t -> assertEquals(0, t.status,
                "getTasksByStatus(0) returned a task with status != 0"));
        inProgressTasks.forEach(t -> assertEquals(1, t.status,
                "getTasksByStatus(1) returned a task with status != 1"));
        doneTasks.forEach(t -> assertEquals(2, t.status,
                "getTasksByStatus(2) returned a task with status != 2"));
    }

    @Test
    public void testGetTasksByAssignee() {
        // user 999 does not exist — expect an empty list, not a crash
        List<Task> none = taskService.getTasksByAssignee(999L);
        assertNotNull(none);
        assertTrue(none.isEmpty(), "Non-existent assignee should yield an empty list");

        // Create a task assigned to a known-absent user and verify retrieval
        Task task = new Task();
        task.title = "Assigned Task";
        task.priority = 1;
        task.type = "task";
        task.assignee_id = 42L;
        Task created = taskService.createTask(task);

        List<Task> assigned = taskService.getTasksByAssignee(42L);
        assertNotNull(assigned);
        assertFalse(assigned.isEmpty(), "Should find the task just assigned");
        assigned.forEach(t -> assertEquals(42L, t.assignee_id,
                "getTasksByAssignee(42) returned a task with wrong assignee_id"));

        // Cleanup to avoid polluting other tests
        taskService.deleteTask(created.getId());
    }
    
    // Empty @Disabled tests removed — they added no value and cluttered the suite.
    // Implement and re-enable when the relevant features are production-ready.
}
