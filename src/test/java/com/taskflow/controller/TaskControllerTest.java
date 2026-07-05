package com.taskflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.model.Task;
import com.taskflow.service.TaskService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for TaskController — tests the HTTP layer in isolation using a mocked TaskService.
 *
 * Several tests below are "bug-pin" tests: they assert the current (incorrect) behaviour so
 * that any accidental regression is caught. When the underlying bug is fixed the expected
 * status code shown in the test comment should replace the current assertion.
 */
@WebMvcTest(TaskController.class)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    // ── GET /api/tasks ──────────────────────────────────────────────────────

    @Test
    void getAllTasks_returnsJsonList() throws Exception {
        Task t = new Task();
        t.id = 1L;
        t.title = "Test Task";
        when(taskService.getAllTasks()).thenReturn(List.of(t));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

    @Test
    void getAllTasks_emptyList_returnsEmptyArray() throws Exception {
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/tasks/{id} ──────────────────────────────────────────────────

    @Test
    void getTask_existingId_returns200WithBody() throws Exception {
        Task t = new Task();
        t.id = 42L;
        t.title = "Found Task";
        t.priority = 3;
        when(taskService.getTask(42L)).thenReturn(t);

        mockMvc.perform(get("/api/tasks/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.title").value("Found Task"));
    }

    /**
     * BUG-PIN: getTask() for a missing ID should return 404 Not Found.
     * Current behaviour is 200 OK with a null/empty body because the controller calls
     * {@code taskService.getTask(id)} (which returns null) and returns it directly
     * instead of wrapping it in ResponseEntity.notFound().build().
     *
     * When the bug is fixed, replace {@code isOk()} with {@code status().isNotFound()}.
     */
    @Test
    void getTask_missingId_BUG_returns200InsteadOf404() throws Exception {
        when(taskService.getTask(9999L)).thenReturn(null);

        mockMvc.perform(get("/api/tasks/9999"))
                .andExpect(status().isOk()); // BUG: should be 404 Not Found
    }

    // ── POST /api/tasks ──────────────────────────────────────────────────────

    @Test
    void createTask_validBody_returns201WithCreatedTask() throws Exception {
        Task input = new Task();
        input.title = "New Task";
        input.priority = 2;
        input.type = "task";

        Task saved = new Task();
        saved.id = 10L;
        saved.title = "New Task";
        saved.priority = 2;
        saved.status = 0;
        when(taskService.createTask(any(Task.class))).thenReturn(saved);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("New Task"));
    }

    @Test
    void createTask_serviceThrows_returns400() throws Exception {
        when(taskService.createTask(any(Task.class)))
                .thenThrow(new RuntimeException("Title is required"));

        Task input = new Task();  // no title set
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/tasks/{id} ───────────────────────────────────────────────────

    @Test
    void updateTask_existingId_returns200() throws Exception {
        Task updated = new Task();
        updated.id = 5L;
        updated.title = "Updated Title";
        when(taskService.updateTask(eq(5L), any(Task.class))).thenReturn(updated);

        Task body = new Task();
        body.title = "Updated Title";
        mockMvc.perform(put("/api/tasks/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    // ── DELETE /api/tasks/{id} ────────────────────────────────────────────────

    @Test
    void deleteTask_existingId_returns204() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());
    }

    /**
     * BUG-PIN: deleteTask() for a missing ID should return 404 Not Found.
     * Current behaviour is 400 Bad Request because the controller catches
     * RuntimeException and returns ResponseEntity.badRequest() for all errors,
     * including "not found" errors that logically deserve 404.
     *
     * When the bug is fixed, replace {@code isBadRequest()} with {@code status().isNotFound()}.
     */
    @Test
    void deleteTask_missingId_BUG_returns400InsteadOf404() throws Exception {
        doThrow(new RuntimeException("Task not found")).when(taskService).deleteTask(9999L);

        mockMvc.perform(delete("/api/tasks/9999"))
                .andExpect(status().isBadRequest()); // BUG: should be 404 Not Found
    }

    // ── GET /api/tasks/status/{status} ──────────────────────────────────────

    @Test
    void getTasksByStatus_returns200WithList() throws Exception {
        Task t = new Task();
        t.id = 3L;
        t.title = "In Progress";
        t.status = 1;
        when(taskService.getTasksByStatus(1)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/tasks/status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value(1));
    }

    // ── GET /api/v1/tasks (legacy) ────────────────────────────────────────────

    @Test
    void getLegacyTasks_returnsWrappedResponse() throws Exception {
        Task t = new Task();
        t.id = 7L;
        t.title = "Legacy Task";
        when(taskService.getAllTasks()).thenReturn(List.of(t));

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void getLegacyTask_found_returnsSuccessTrue() throws Exception {
        Task t = new Task();
        t.id = 5L;
        t.title = "Legacy Found";
        when(taskService.getTask(5L)).thenReturn(t);

        mockMvc.perform(get("/api/v1/tasks/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    void getLegacyTask_notFound_returnsSuccessFalseWithError() throws Exception {
        when(taskService.getTask(99L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/tasks/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Task not found"));
    }
}
