package com.taskflow.service;

import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.model.Project;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.repository.ProjectRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Pure Mockito unit tests for TaskService.
 *
 * These tests run in under 1 second with no Spring context and no DB.
 * They document both correct behaviour and known bugs.
 */
@ExtendWith(MockitoExtension.class)
public class TaskServiceMockTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

    private Task makeTask(Long id, String title, int status, int priority) {
        Task t = new Task();
        t.id = id;
        t.title = title;
        t.status = status;
        t.priority = priority;
        t.type = "task";
        return t;
    }

    // ----------------------------------------------------------------
    // createTask
    // ----------------------------------------------------------------

    @Test
    public void createTask_nullTitle_throwsRuntimeException() {
        Task task = new Task();
        task.description = "no title";

        assertThrows(RuntimeException.class, () -> taskService.createTask(task));
        verifyNoInteractions(taskRepository);
    }

    @Test
    public void createTask_emptyTitle_throwsRuntimeException() {
        Task task = new Task();
        task.title = "   ";

        assertThrows(RuntimeException.class, () -> taskService.createTask(task));
        verifyNoInteractions(taskRepository);
    }

    @Test
    public void createTask_validTask_savedAndReturned() {
        Task task = makeTask(null, "Fix login bug", 0, 3);
        Task saved = makeTask(42L, "Fix login bug", 0, 3);
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        Task result = taskService.createTask(task);

        assertNotNull(result);
        assertEquals(42L, result.getId());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    public void createTask_setsCreatedAndUpdatedDates() {
        Task task = makeTask(null, "Date task", 0, 2);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.createTask(task);

        assertNotNull(result.createdDate);
        assertNotNull(result.updatedDate);
    }

    @Test
    public void createTask_priorityBelowOne_defaultsToTwo() {
        Task task = makeTask(null, "Priority task", 0, 0);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.createTask(task);

        assertEquals(2, result.priority);
    }

    // ----------------------------------------------------------------
    // updateTask
    // ----------------------------------------------------------------

    @Test
    public void updateTask_taskNotFound_throwsRuntimeException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> taskService.updateTask(99L, new Task()));
    }

    @Test
    public void updateTask_updatesTitle() {
        Task existing = makeTask(1L, "Old title", 0, 2);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = new Task();
        update.title = "New title";

        Task result = taskService.updateTask(1L, update);

        assertEquals("New title", result.getTitle());
        verify(taskRepository).save(existing);
    }

    @Test
    public void updateTask_updatesPriority() {
        Task existing = makeTask(1L, "Some task", 0, 1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = new Task();
        update.priority = 4; // critical

        Task result = taskService.updateTask(1L, update);

        assertEquals(4, result.priority);
    }

    @Test
    public void updateTask_updatesStatus() {
        Task existing = makeTask(1L, "Some task", 0, 2);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = new Task();
        update.status = 1; // in-progress

        Task result = taskService.updateTask(1L, update);

        assertEquals(1, result.status);
    }

    // ----------------------------------------------------------------
    // getTask
    // ----------------------------------------------------------------

    @Test
    public void getTask_existingId_returnsTask() {
        Task task = makeTask(5L, "My task", 0, 2);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        Task result = taskService.getTask(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
    }

    @Test
    public void getTask_missingId_returnsNull() {
        when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertNull(taskService.getTask(999L));
    }

    // ----------------------------------------------------------------
    // deleteTask
    // ----------------------------------------------------------------

    @Test
    public void deleteTask_taskNotFound_throwsRuntimeException() {
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> taskService.deleteTask(1L));
    }

    @Test
    public void deleteTask_existingTask_deletedById() {
        Task task = makeTask(7L, "Delete me", 0, 2);
        when(taskRepository.findById(7L)).thenReturn(Optional.of(task));

        taskService.deleteTask(7L);

        verify(taskRepository).deleteById(7L);
    }

    // ----------------------------------------------------------------
    // assignTask
    // ----------------------------------------------------------------

    @Test
    public void assignTask_taskNotFound_throwsRuntimeException() {
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> taskService.assignTask(1L, 10L));
    }

    @Test
    public void assignTask_userNotFound_throwsRuntimeException() {
        Task task = makeTask(1L, "Assign me", 0, 2);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> taskService.assignTask(1L, 10L));
    }

    @Test
    public void assignTask_validTaskAndUser_setsAssigneeId() {
        Task task = makeTask(1L, "Assign me", 0, 2);
        User user = new User();
        user.setId(10L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.assignTask(1L, 10L);

        assertEquals(10L, result.assignee_id);
    }

    // ----------------------------------------------------------------
    // transitionStatus
    // ----------------------------------------------------------------

    @Test
    public void transitionStatus_taskNotFound_throwsRuntimeException() {
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> taskService.transitionStatus(1L, 1));
    }

    @Test
    public void transitionStatus_todoToInProgress_valid() {
        Task task = makeTask(1L, "Transition test", 0 /* TODO */, 2);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.transitionStatus(1L, 1 /* IN_PROGRESS */);

        assertEquals(1, result.status);
    }

    @Test
    public void transitionStatus_todoCancelled_valid() {
        Task task = makeTask(1L, "Cancel me", 0, 2);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.transitionStatus(1L, 3 /* CANCELLED */);

        assertEquals(3, result.status);
    }

    @Test
    public void transitionStatus_todoDone_invalid_throwsRuntimeException() {
        Task task = makeTask(1L, "Skip to done", 0 /* TODO */, 2);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(RuntimeException.class,
                () -> taskService.transitionStatus(1L, 2 /* DONE */));
    }

    @Test
    public void transitionStatus_inProgressToReview_valid() {
        Task task = makeTask(1L, "In progress", 1, 2);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.transitionStatus(1L, 5 /* REVIEW */);

        assertEquals(5, result.status);
    }

    @Test
    public void transitionStatus_reviewToDone_valid() {
        Task task = makeTask(1L, "Review done", 5 /* REVIEW */, 2);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.transitionStatus(1L, 2 /* DONE */);

        assertEquals(2, result.status);
    }

    @Test
    public void transitionStatus_doneToAnything_invalid_throwsRuntimeException() {
        Task task = makeTask(1L, "Done task", 2 /* DONE */, 2);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(RuntimeException.class,
                () -> taskService.transitionStatus(1L, 1 /* IN_PROGRESS */));
    }

    @Test
    public void transitionStatus_doneSetsActualHoursFromEstimated_whenActualIsZero() {
        Task task = makeTask(1L, "Reviewed task", 5 /* REVIEW */, 2);
        task.estimated_hours = 8;
        task.actual_hours = 0;
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.transitionStatus(1L, 2 /* DONE */);

        assertEquals(8, result.actual_hours,
                "actual_hours should be set to estimated_hours when not yet recorded");
    }

    // ----------------------------------------------------------------
    // getTaskStatistics — BUG PIN
    // ----------------------------------------------------------------

    /**
     * BUG PIN — Bug #3: getTaskStatistics() throws ArithmeticException (division by zero)
     * when no tasks have status=DONE (completedCount == 0).
     *
     * This test documents the current (broken) behaviour. Do NOT fix the test;
     * fix the production code instead and verify the test then passes correctly.
     * See: https://github.com/Study4/agentic-workflow-demo/issues/3
     */
    @Test
    public void getTaskStatistics_noCompletedTasks_divisionByZeroBug() {
        Task todo = makeTask(1L, "Todo task", 0, 2);
        when(taskRepository.findAll()).thenReturn(List.of(todo));

        // BUG: ArithmeticException thrown because completedCount == 0
        assertThrows(ArithmeticException.class,
                () -> taskService.getTaskStatistics(),
                "BUG #3: division by zero when no completed tasks exist");
    }

    @Test
    public void getTaskStatistics_withCompletedTasks_returnsCorrectCounts() {
        Task todo = makeTask(1L, "Todo", 0, 2);
        Task done = makeTask(2L, "Done", 2, 2);
        done.estimated_hours = 4;
        done.actual_hours = 5;
        when(taskRepository.findAll()).thenReturn(List.of(todo, done));

        Map<String, Object> stats = taskService.getTaskStatistics();

        assertEquals(2, ((Number) stats.get("total")).intValue());
        assertEquals(1L, stats.get("todo"));
        assertEquals(1L, stats.get("done"));
        assertEquals(0L, stats.get("inProgress"));
        assertEquals(4, stats.get("avgEstimated"));
        assertEquals(5, stats.get("avgActual"));
    }

    // ----------------------------------------------------------------
    // getAllTasks / getTasksByStatus / getTasksByAssignee
    // ----------------------------------------------------------------

    @Test
    public void getAllTasks_returnsRepositoryResults() {
        List<Task> tasks = List.of(makeTask(1L, "A", 0, 1), makeTask(2L, "B", 1, 2));
        when(taskRepository.findAll()).thenReturn(tasks);

        assertEquals(2, taskService.getAllTasks().size());
    }

    @Test
    public void getTasksByStatus_delegatesToRepository() {
        when(taskRepository.findByStatus(1)).thenReturn(List.of(makeTask(1L, "IP", 1, 2)));

        List<Task> result = taskService.getTasksByStatus(1);

        assertEquals(1, result.size());
        verify(taskRepository).findByStatus(1);
    }

    @Test
    public void getTasksByAssignee_delegatesToRepository() {
        when(taskRepository.findByAssigneeId(5L)).thenReturn(List.of(makeTask(1L, "Mine", 0, 2)));

        List<Task> result = taskService.getTasksByAssignee(5L);

        assertEquals(1, result.size());
        verify(taskRepository).findByAssigneeId(5L);
    }
}
