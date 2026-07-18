package com.taskflow.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link User#getDisplayName()}.
 *
 * <p>getDisplayName() has three branches:
 * <ol>
 *   <li>Both firstName AND lastName non-null → "firstName lastName"</li>
 *   <li>Either first/last is null, but full_name non-null → full_name</li>
 *   <li>full_name also null → username</li>
 * </ol>
 *
 * <p>Bug documented: if firstName is set but lastName is null (or vice versa),
 * the set name is silently ignored and the method falls through to full_name/username.
 */
class UserModelTest {

    // --- happy path ---

    @Test
    void getDisplayName_bothFirstAndLastName_returnsCombined() {
        User user = new User("jsmith", "j@example.com", "pass");
        user.setFirstName("John");
        user.setLastName("Smith");
        assertEquals("John Smith", user.getDisplayName());
    }

    @Test
    void getDisplayName_noFirstOrLastName_usesFullName() {
        User user = new User("jsmith", "j@example.com", "pass");
        user.setFull_name("John Smith");
        assertEquals("John Smith", user.getDisplayName());
    }

    @Test
    void getDisplayName_noNamesAtAll_usesUsername() {
        User user = new User("jsmith", "j@example.com", "pass");
        assertEquals("jsmith", user.getDisplayName());
    }

    // --- bug-pin tests: partial name silently ignored ---

    /**
     * Bug pin: firstName set but lastName is null.
     * The caller might expect "John" but the method skips to full_name/username.
     * Current (buggy) behaviour: falls through to full_name if set, else username.
     */
    @Test
    void getDisplayName_firstNameSetButLastNameNull_fallsBackToFullName() {
        User user = new User("jsmith", "j@example.com", "pass");
        user.setFirstName("John");
        // lastName is null; full_name is set
        user.setFull_name("John Smith (full)");
        // BUG: firstName is ignored; full_name is returned instead
        assertEquals("John Smith (full)", user.getDisplayName());
    }

    @Test
    void getDisplayName_firstNameSetLastNameNullNoFullName_fallsBackToUsername() {
        User user = new User("jsmith", "j@example.com", "pass");
        user.setFirstName("John");
        // lastName is null; full_name is also null
        // BUG: the caller-supplied firstName is silently discarded
        assertEquals("jsmith", user.getDisplayName());
    }

    @Test
    void getDisplayName_lastNameSetButFirstNameNull_fallsBackToUsername() {
        User user = new User("jsmith", "j@example.com", "pass");
        user.setLastName("Smith");
        // firstName is null; full_name is also null
        // BUG: the caller-supplied lastName is silently discarded
        assertEquals("jsmith", user.getDisplayName());
    }

    // --- precedence ---

    /**
     * firstName+lastName wins over full_name when both are set.
     */
    @Test
    void getDisplayName_firstAndLastNamePrecedeFullName() {
        User user = new User("jsmith", "j@example.com", "pass");
        user.setFirstName("John");
        user.setLastName("Smith");
        user.setFull_name("J. Smith (legacy)");
        assertEquals("John Smith", user.getDisplayName());
    }

    /**
     * full_name wins over username when first/last are missing.
     */
    @Test
    void getDisplayName_fullNamePrecedesUsername() {
        User user = new User("jsmith", "j@example.com", "pass");
        user.setFull_name("John Smith");
        assertEquals("John Smith", user.getDisplayName());
    }

    // --- edge cases ---

    @Test
    void getDisplayName_emptyFirstAndLastName_returnsSpaceSeparated() {
        // Both are non-null (empty strings) → concatenated as " "
        User user = new User("jsmith", "j@example.com", "pass");
        user.setFirstName("");
        user.setLastName("");
        assertEquals(" ", user.getDisplayName());
    }

    @Test
    void getDisplayName_defaultConstructor_noNPE() {
        User user = new User();
        // All name fields are null; username is null too → returns null (no NPE)
        assertNull(user.getDisplayName());
    }
}
