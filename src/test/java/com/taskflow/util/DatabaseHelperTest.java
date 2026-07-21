package com.taskflow.util;

import com.taskflow.model.Task;
import com.taskflow.repository.TaskRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DatabaseHelper — raw JDBC utility layer.
 *
 * DatabaseHelper uses a direct H2 connection to the same in-memory database
 * that Spring/JPA manages, so a @SpringBootTest context is required for the
 * schema to exist.
 *
 * Several tests here are BUG-PIN tests: they document *current* (buggy)
 * behaviour so that any future fix is immediately visible. See inline comments.
 */
@SpringBootTest
public class DatabaseHelperTest {

    @Autowired
    private TaskRepository taskRepository;

    private Task savedTask;

    @BeforeEach
    void setUp() {
        Task t = new Task();
        t.title = "DB Helper Test Task";
        t.description = "test description";
        t.status = 0;
        t.priority = 2;
        t.type = "task";
        t.projectCode = "DBTEST";
        savedTask = taskRepository.save(t);
    }

    @AfterEach
    void tearDown() {
        if (savedTask != null && taskRepository.existsById(savedTask.id)) {
            taskRepository.deleteById(savedTask.id);
        }
    }

    // -----------------------------------------------------------------------
    // getConnection
    // -----------------------------------------------------------------------

    @Test
    void getConnection_returnsOpenConnection() throws SQLException {
        var conn = DatabaseHelper.getConnection();
        assertNotNull(conn, "getConnection() must not return null");
        assertFalse(conn.isClosed(), "connection should be open");
    }

    @Test
    void getConnection_reusesSameConnection() throws SQLException {
        var c1 = DatabaseHelper.getConnection();
        var c2 = DatabaseHelper.getConnection();
        assertSame(c1, c2, "getConnection() should return the same cached connection");
    }

    // -----------------------------------------------------------------------
    // searchTasks
    // -----------------------------------------------------------------------

    @Test
    void searchTasks_matchesByTitle() throws SQLException {
        List<Map<String, Object>> results = DatabaseHelper.searchTasks("DB Helper Test Task");
        assertTrue(results.stream().anyMatch(r ->
                "DB Helper Test Task".equals(r.get("TITLE"))),
                "searchTasks should return rows matching the title keyword");
    }

    @Test
    void searchTasks_noMatch_returnsEmptyList() throws SQLException {
        List<Map<String, Object>> results =
                DatabaseHelper.searchTasks("ZZZNOMATCHKEYWORD999");
        assertNotNull(results);
        assertTrue(results.isEmpty(), "searchTasks with no matching keyword should return empty list");
    }

    /**
     * BUG-PIN: SQL injection in searchTasks.
     *
     * The query is built via string concatenation:
     *   "SELECT * FROM tasks WHERE title LIKE '%<keyword>%' ..."
     * A carefully crafted keyword can break out of the LIKE literal and
     * inject arbitrary SQL.  Here, "' OR '1'='1" turns the WHERE clause
     * into an always-true condition, returning ALL rows.
     *
     * Fix: replace Statement+string concat with PreparedStatement.
     */
    @Test
    void searchTasks_sqlInjection_BUG_returnsAllRows() throws SQLException {
        long totalRows = taskRepository.count();
        // Inject: closes the LIKE literal then adds OR 1=1
        List<Map<String, Object>> injected = DatabaseHelper.searchTasks("' OR '1'='1");
        // BUG: injection succeeds — returns every row in the table
        assertEquals(totalRows, injected.size(),
                "BUG: SQL injection in searchTasks returns all rows (should be 0)");
    }

    // -----------------------------------------------------------------------
    // getTasksByUser
    // -----------------------------------------------------------------------

    @Test
    void getTasksByUser_returnsTasksForAssignee() throws SQLException {
        Task assigned = new Task();
        assigned.title = "Assigned Task";
        assigned.assignee_id = 999L;
        assigned.projectCode = "DBTEST";
        Task saved2 = taskRepository.save(assigned);
        try {
            List<Map<String, Object>> results = DatabaseHelper.getTasksByUser("999");
            // getTasksByUser puts keys as lowercase strings ("id", "title", etc.)
            assertTrue(results.stream().anyMatch(r ->
                    "Assigned Task".equals(r.get("title"))),
                    "getTasksByUser should return tasks assigned to the specified user");
        } finally {
            taskRepository.deleteById(saved2.id);
        }
    }

    // -----------------------------------------------------------------------
    // updateTaskStatus
    // -----------------------------------------------------------------------

    @Test
    void updateTaskStatus_existingTask_returnsTrue() throws SQLException {
        boolean updated = DatabaseHelper.updateTaskStatus(savedTask.id, 2);
        assertTrue(updated, "updateTaskStatus on an existing task should return true");
    }

    @Test
    void updateTaskStatus_nonExistentId_returnsFalse() throws SQLException {
        boolean updated = DatabaseHelper.updateTaskStatus(Long.MAX_VALUE, 1);
        assertFalse(updated, "updateTaskStatus for non-existent id should return false");
    }

    // -----------------------------------------------------------------------
    // deleteTasks
    // -----------------------------------------------------------------------

    @Test
    void deleteTasks_singleId_deletesRow() throws SQLException {
        Task toDelete = new Task();
        toDelete.title = "To Delete";
        toDelete.projectCode = "DBTEST";
        Task saved2 = taskRepository.save(toDelete);

        int deleted = DatabaseHelper.deleteTasks(Collections.singletonList(saved2.id));
        assertEquals(1, deleted, "deleteTasks with one id should delete exactly one row");
        assertFalse(taskRepository.existsById(saved2.id), "row should no longer exist after deleteTasks");
    }

    /**
     * BUG-PIN: deleteTasks with an empty list generates potentially non-portable SQL.
     *
     * When taskIds is empty the loop produces:
     *   DELETE FROM tasks WHERE id IN ()
     * H2 accepts this and returns 0 rows affected, but many production databases
     * (MySQL, PostgreSQL) treat an empty IN list as a syntax error.  This makes
     * the method database-dependent and fragile when targeting production.
     *
     * Fix: guard with {@code if (taskIds.isEmpty()) return 0;} before
     * building the statement.
     */
    @Test
    void deleteTasks_emptyList_BUG_returnsZeroInH2() throws SQLException {
        // H2 silently accepts "DELETE FROM tasks WHERE id IN ()" and deletes nothing.
        // On MySQL/PostgreSQL this would throw a syntax error.
        int deleted = DatabaseHelper.deleteTasks(Collections.emptyList());
        assertEquals(0, deleted,
                "BUG: deleteTasks([]) produces non-portable SQL; H2 returns 0 but production DBs throw");
    }

    @Test
    void deleteTasks_multipleIds_deletesAll() throws SQLException {
        Task t2 = new Task(); t2.title = "Del2"; t2.projectCode = "DBTEST";
        Task t3 = new Task(); t3.title = "Del3"; t3.projectCode = "DBTEST";
        Task s2 = taskRepository.save(t2);
        Task s3 = taskRepository.save(t3);

        int deleted = DatabaseHelper.deleteTasks(Arrays.asList(s2.id, s3.id));
        assertEquals(2, deleted, "deleteTasks with two ids should delete two rows");
        assertFalse(taskRepository.existsById(s2.id));
        assertFalse(taskRepository.existsById(s3.id));
    }
}
