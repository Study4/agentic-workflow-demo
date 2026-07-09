package com.taskflow.service;

import com.taskflow.model.User;
import com.taskflow.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 *
 * Uses Mockito to mock UserRepository — no Spring context, no database.
 * Covers authentication, user management, password reset, and search paths.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // ─── createUser ──────────────────────────────────────────────────────────

    @Test
    public void createUser_success_savesAndReturnsUser() {
        when(userRepository.findByUsername("alice")).thenReturn(null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(null);
        User saved = new User("alice", "alice@example.com", "secret");
        saved.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.createUser("alice", "alice@example.com", "secret");

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void createUser_duplicateUsername_throwsException() {
        User existing = new User("alice", "other@example.com", "pw");
        when(userRepository.findByUsername("alice")).thenReturn(existing);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.createUser("alice", "alice@example.com", "secret"));
        assertTrue(ex.getMessage().contains("Username already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void createUser_duplicateEmail_throwsException() {
        when(userRepository.findByUsername("alice")).thenReturn(null);
        User existing = new User("other", "alice@example.com", "pw");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(existing);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.createUser("alice", "alice@example.com", "secret"));
        assertTrue(ex.getMessage().contains("Email already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    // ─── authenticate ────────────────────────────────────────────────────────

    @Test
    public void authenticate_validCredentials_returnsUser() {
        User user = new User("alice", "alice@example.com", "secret");
        user.setFailedLoginAttempts(3);
        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.authenticate("alice", "secret");

        assertNotNull(result);
        assertEquals(0, result.getFailedLoginAttempts());
        assertNotNull(result.getLastLogin());
    }

    @Test
    public void authenticate_unknownUser_throwsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.authenticate("ghost", "pw"));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    public void authenticate_inactiveUser_throwsException() {
        User user = new User("alice", "alice@example.com", "secret");
        user.setActive(false);
        when(userRepository.findByUsername("alice")).thenReturn(user);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.authenticate("alice", "secret"));
        assertTrue(ex.getMessage().contains("deactivated"));
    }

    @Test
    public void authenticate_wrongPassword_throwsException() {
        User user = new User("alice", "alice@example.com", "secret");
        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.authenticate("alice", "wrong"));
        assertTrue(ex.getMessage().contains("Invalid password"));
    }

    @Test
    public void authenticate_wrongPassword_incrementsFailedAttempts() {
        User user = new User("alice", "alice@example.com", "secret");
        user.setFailedLoginAttempts(2);
        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        assertThrows(RuntimeException.class,
                () -> userService.authenticate("alice", "wrong"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getFailedLoginAttempts());
    }

    // ─── getUser ─────────────────────────────────────────────────────────────

    @Test
    public void getUser_found_returnsUser() {
        User user = new User("alice", "alice@example.com", "secret");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUser(1L);
        assertNotNull(result);
        assertEquals("alice", result.getUsername());
    }

    @Test
    public void getUser_notFound_returnsNull() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertNull(userService.getUser(999L));
    }

    // ─── updateUser ──────────────────────────────────────────────────────────

    @Test
    public void updateUser_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.updateUser(99L, new User()));
    }

    @Test
    public void updateUser_nullFieldsNotOverwritten() {
        User existing = new User("alice", "alice@example.com", "secret");
        existing.setId(1L);
        existing.setFirstName("Alice");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User update = new User();
        update.setLastName("Smith"); // only lastName provided
        userService.updateUser(1L, update);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("Alice", captor.getValue().getFirstName(), "firstName must not be cleared");
        assertEquals("Smith", captor.getValue().getLastName());
    }

    /**
     * BUG PIN: No authorization check on role updates. Any caller can escalate
     * a regular user account to admin by passing role="admin" in the update body.
     * This test documents the current (buggy) behaviour so a future fix is visible.
     */
    @Test
    public void updateUser_roleEscalationBugPin_anyUserCanBecomeAdmin() {
        User existing = new User("alice", "alice@example.com", "secret");
        existing.setId(1L);
        existing.setRole("user");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User update = new User();
        update.setRole("admin");
        userService.updateUser(1L, update);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        // Documents bug: role is accepted without any authorization check
        assertEquals("admin", captor.getValue().getRole());
    }

    // ─── requestPasswordReset ────────────────────────────────────────────────

    @Test
    public void requestPasswordReset_unknownEmail_throwsException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.requestPasswordReset("ghost@example.com"));
        assertTrue(ex.getMessage().contains("No account found with email"));
    }

    @Test
    public void requestPasswordReset_returnsTokenAndSetsOnUser() {
        User user = new User("alice", "alice@example.com", "secret");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        String token = userService.requestPasswordReset("alice@example.com");

        assertNotNull(token);
        assertTrue(token.startsWith("reset-"),
                "Expected token to start with 'reset-' but was: " + token);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(token, captor.getValue().getResetToken());
    }

    // ─── resetPassword ───────────────────────────────────────────────────────

    @Test
    public void resetPassword_invalidToken_throwsException() {
        when(userRepository.findByResetToken("bad-token")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.resetPassword("bad-token", "newpass"));
        assertTrue(ex.getMessage().contains("Invalid or expired token"));
    }

    @Test
    public void resetPassword_success_clearsTokenAndUpdatesPassword() {
        User user = new User("alice", "alice@example.com", "oldpass");
        user.setResetToken("reset-123456");
        when(userRepository.findByResetToken("reset-123456")).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        userService.resetPassword("reset-123456", "newpass");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("newpass", captor.getValue().getPassword());
        assertNull(captor.getValue().getResetToken(), "Reset token should be cleared after use");
    }

    // ─── searchUsers ─────────────────────────────────────────────────────────

    @Test
    public void searchUsers_blankKeyword_returnsEmptyList() {
        List<User> result = userService.searchUsers("   ");
        assertTrue(result.isEmpty());
        verify(userRepository, never()).searchUsers(anyString());
    }

    @Test
    public void searchUsers_nullKeyword_returnsEmptyList() {
        List<User> result = userService.searchUsers(null);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).searchUsers(anyString());
    }

    @Test
    public void searchUsers_validKeyword_delegatesToRepository() {
        User user = new User("alice", "alice@example.com", "secret");
        when(userRepository.searchUsers("alice")).thenReturn(Collections.singletonList(user));

        List<User> result = userService.searchUsers("alice");

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getUsername());
    }

    // ─── getUserDisplayName ──────────────────────────────────────────────────

    @Test
    public void getUserDisplayName_notFound_returnsUnknown() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertEquals("Unknown User", userService.getUserDisplayName(99L));
    }

    @Test
    public void getUserDisplayName_firstAndLastName_returnsFullName() {
        User user = new User("alice", "alice@example.com", "secret");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertEquals("Alice Smith", userService.getUserDisplayName(1L));
    }

    @Test
    public void getUserDisplayName_fullNameFallback_returnsFullName() {
        User user = new User("alice", "alice@example.com", "secret");
        user.setFull_name("Alice Smith");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertEquals("Alice Smith", userService.getUserDisplayName(1L));
    }

    @Test
    public void getUserDisplayName_usernameFallback_returnsUsername() {
        User user = new User("alice", "alice@example.com", "secret");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertEquals("alice", userService.getUserDisplayName(1L));
    }

    // ─── getActiveUsersByRole ────────────────────────────────────────────────

    @Test
    public void getActiveUsersByRole_filtersInactiveAndDeletedUsers() {
        User activeUser = new User("alice", "a@example.com", "pw");
        // active=true, deleted=false by default from constructor

        User inactiveUser = new User("bob", "b@example.com", "pw");
        inactiveUser.setActive(false);

        User deletedUser = new User("charlie", "c@example.com", "pw");
        deletedUser.setDeleted(true);

        when(userRepository.findByRole("user"))
                .thenReturn(Arrays.asList(activeUser, inactiveUser, deletedUser));

        List<User> result = userService.getActiveUsersByRole("user");

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getUsername());
    }

    // ─── deactivateUsers ─────────────────────────────────────────────────────

    @Test
    public void deactivateUsers_returnsCountOfSuccessfullyDeactivated() {
        User user1 = new User("alice", "a@example.com", "pw");
        User user2 = new User("bob", "b@example.com", "pw");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        int count = userService.deactivateUsers(Arrays.asList(1L, 2L));

        assertEquals(2, count);
        assertFalse(user1.isActive());
        assertFalse(user2.isActive());
    }

    @Test
    public void deactivateUsers_skipsNonExistentUsers() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        int count = userService.deactivateUsers(Collections.singletonList(999L));

        assertEquals(0, count);
        verify(userRepository, never()).save(any(User.class));
    }
}
