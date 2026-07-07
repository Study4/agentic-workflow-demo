package com.taskflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.model.User;
import com.taskflow.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest tests for UserController.
 *
 * Tests the HTTP layer in isolation — no database, no full Spring context.
 * UserService is mocked with @MockBean.
 *
 * Two bug-pin tests document known security/API contract issues:
 *  1. POST /api/users/register returns 200 (should be 201 Created)
 *  2. POST /api/users/password-reset returns the reset token in the response body
 *     (security issue — reset tokens must never be sent over the API)
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // -------------------------------------------------------------------------
    // GET /api/users
    // -------------------------------------------------------------------------

    @Test
    public void getAllUsers_returnsJsonList() throws Exception {
        User u1 = makeUser(1L, "alice", "alice@example.com");
        User u2 = makeUser(2L, "bob", "bob@example.com");
        when(userService.getAllUsers()).thenReturn(Arrays.asList(u1, u2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[1].username").value("bob"));
    }

    @Test
    public void getAllUsers_emptyList_returnsEmptyArray() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // GET /api/users/{id}
    // -------------------------------------------------------------------------

    @Test
    public void getUser_existingId_returns200WithBody() throws Exception {
        User user = makeUser(5L, "charlie", "charlie@example.com");
        when(userService.getUser(5L)).thenReturn(user);

        mockMvc.perform(get("/api/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("charlie"))
                .andExpect(jsonPath("$.email").value("charlie@example.com"));
    }

    @Test
    public void getUser_missingId_returns404() throws Exception {
        when(userService.getUser(9999L)).thenReturn(null);

        mockMvc.perform(get("/api/users/9999"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/users/register
    //
    // BUG: returns 200 instead of 201 Created.
    // UserController.registerUser() calls ResponseEntity.ok(user).
    // Fix: change to ResponseEntity.status(HttpStatus.CREATED).body(user).
    // When fixed, update isOk() → isCreated() below.
    // -------------------------------------------------------------------------

    @Test
    public void registerUser_validBody_BUG_returns200InsteadOf201() throws Exception {
        User saved = makeUser(10L, "newuser", "new@example.com");
        when(userService.createUser("newuser", "new@example.com", "pass123")).thenReturn(saved);

        String body = "{\"username\":\"newuser\",\"email\":\"new@example.com\",\"password\":\"pass123\"}";

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // BUG: should be isCreated() (201). Change when the bug is fixed.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    public void registerUser_duplicateUsername_returns400() throws Exception {
        when(userService.createUser(eq("alice"), anyString(), anyString()))
                .thenThrow(new RuntimeException("Username already exists"));

        String body = "{\"username\":\"alice\",\"email\":\"alice2@example.com\",\"password\":\"pass\"}";

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Username already exists")));
    }

    // -------------------------------------------------------------------------
    // POST /api/users/login
    // -------------------------------------------------------------------------

    @Test
    public void login_validCredentials_returns200WithUserAndToken() throws Exception {
        User user = makeUser(1L, "alice", "alice@example.com");
        when(userService.authenticate("alice", "secret")).thenReturn(user);

        String body = "{\"username\":\"alice\",\"password\":\"secret\"}";

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("alice"))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    public void login_invalidCredentials_returns401() throws Exception {
        when(userService.authenticate("alice", "wrong"))
                .thenThrow(new RuntimeException("Invalid password"));

        String body = "{\"username\":\"alice\",\"password\":\"wrong\"}";

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void login_unknownUser_returns401() throws Exception {
        when(userService.authenticate("ghost", "pass"))
                .thenThrow(new RuntimeException("User not found: ghost"));

        String body = "{\"username\":\"ghost\",\"password\":\"pass\"}";

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // PUT /api/users/{id}
    // -------------------------------------------------------------------------

    @Test
    public void updateUser_existingId_returns200() throws Exception {
        User updated = makeUser(3L, "updatedname", "updated@example.com");
        when(userService.updateUser(eq(3L), any(User.class))).thenReturn(updated);

        mockMvc.perform(put("/api/users/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"updatedname\",\"email\":\"updated@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updatedname"));
    }

    @Test
    public void updateUser_missingId_returns400() throws Exception {
        when(userService.updateUser(eq(9999L), any(User.class)))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(put("/api/users/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/users/{id}
    // -------------------------------------------------------------------------

    @Test
    public void deleteUser_existingId_returns204() throws Exception {
        doNothing().when(userService).deleteUser(7L);

        mockMvc.perform(delete("/api/users/7"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void deleteUser_missingId_returns400() throws Exception {
        doThrow(new RuntimeException("User not found")).when(userService).deleteUser(9999L);

        mockMvc.perform(delete("/api/users/9999"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/users/password-reset
    //
    // BUG: The reset token is returned in the API response body.
    // Reset tokens must NEVER be sent over the API — they should only be sent
    // to the user's email address out-of-band. Returning the token in the
    // response lets an attacker reset any account whose email they know.
    // Fix: remove the "token" field from the response map; return only "message".
    // When fixed, replace content().string(containsString("reset-")) with
    //   jsonPath("$.token").doesNotExist()
    // -------------------------------------------------------------------------

    @Test
    public void requestPasswordReset_knownEmail_BUG_returnsTokenInBody() throws Exception {
        when(userService.requestPasswordReset("victim@example.com"))
                .thenReturn("reset-1234567890");

        String body = "{\"email\":\"victim@example.com\"}";

        mockMvc.perform(post("/api/users/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset email sent"))
                // BUG: token must NOT be in the response. When fixed, assert it does NOT exist.
                .andExpect(jsonPath("$.token").value(containsString("reset-")));
    }

    @Test
    public void requestPasswordReset_unknownEmail_returns400() throws Exception {
        when(userService.requestPasswordReset("nobody@example.com"))
                .thenThrow(new RuntimeException("No account found with email: nobody@example.com"));

        mockMvc.perform(post("/api/users/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/users/password-reset/confirm
    // -------------------------------------------------------------------------

    @Test
    public void resetPassword_validToken_returns200() throws Exception {
        doNothing().when(userService).resetPassword("reset-abc", "newpass");

        mockMvc.perform(post("/api/users/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"reset-abc\",\"newPassword\":\"newpass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }

    @Test
    public void resetPassword_invalidToken_returns400() throws Exception {
        doThrow(new RuntimeException("Invalid or expired token"))
                .when(userService).resetPassword(eq("bad-token"), anyString());

        mockMvc.perform(post("/api/users/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"bad-token\",\"newPassword\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/users/search
    // -------------------------------------------------------------------------

    @Test
    public void searchUsers_matchingQuery_returnsResults() throws Exception {
        List<User> results = Arrays.asList(makeUser(1L, "alice", "alice@example.com"));
        when(userService.searchUsers("ali")).thenReturn(results);

        mockMvc.perform(get("/api/users/search").param("q", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    public void searchUsers_noMatches_returnsEmptyArray() throws Exception {
        when(userService.searchUsers("zzznomatch")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users/search").param("q", "zzznomatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // GET /api/users/by-role/{role}
    // -------------------------------------------------------------------------

    @Test
    public void getUsersByRole_returnsFilteredList() throws Exception {
        List<User> admins = Arrays.asList(makeUser(1L, "admin_user", "admin@example.com"));
        when(userService.getActiveUsersByRole("admin")).thenReturn(admins);

        mockMvc.perform(get("/api/users/by-role/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("admin_user"));
    }

    @Test
    public void getUsersByRole_noUsersForRole_returnsEmptyArray() throws Exception {
        when(userService.getActiveUsersByRole("superadmin")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users/by-role/superadmin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private User makeUser(Long id, String username, String email) {
        User u = new User(username, email, "hashed-or-plaintext-password");
        u.setId(id);
        u.setActive(true);
        return u;
    }
}
