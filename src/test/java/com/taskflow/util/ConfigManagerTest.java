package com.taskflow.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigManager edge-case behaviour.
 *
 * ConfigManager is a non-resettable singleton whose internal {@link Properties}
 * object is only accessible via reflection. Tests manipulate that object
 * directly, cleaning up in @AfterEach to avoid cross-test pollution.
 */
class ConfigManagerTest {

    private static final String TEST_KEY_INT  = "test.configmanager.int";
    private static final String TEST_KEY_BOOL = "test.configmanager.bool";
    private static final String TEST_KEY_STR  = "test.configmanager.str";

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Properties getConfigProps() throws Exception {
        ConfigManager cm = ConfigManager.getInstance();
        Field f = ConfigManager.class.getDeclaredField("config");
        f.setAccessible(true);
        return (Properties) f.get(cm);
    }

    private void setProperty(String key, String value) throws Exception {
        getConfigProps().setProperty(key, value);
    }

    @AfterEach
    void cleanup() throws Exception {
        Properties props = getConfigProps();
        props.remove(TEST_KEY_INT);
        props.remove(TEST_KEY_BOOL);
        props.remove(TEST_KEY_STR);
    }

    // -----------------------------------------------------------------------
    // get(String key) and get(String key, String defaultValue)
    // -----------------------------------------------------------------------

    @Test
    void get_missingKey_returnsNull() {
        assertNull(ConfigManager.getInstance().get(TEST_KEY_STR));
    }

    @Test
    void get_presentKey_returnsValue() throws Exception {
        setProperty(TEST_KEY_STR, "hello");
        assertEquals("hello", ConfigManager.getInstance().get(TEST_KEY_STR));
    }

    @Test
    void get_missingKeyWithDefault_returnsDefault() {
        assertEquals("fallback", ConfigManager.getInstance().get(TEST_KEY_STR, "fallback"));
    }

    @Test
    void get_presentKeyWithDefault_returnsValue() throws Exception {
        setProperty(TEST_KEY_STR, "actual");
        assertEquals("actual", ConfigManager.getInstance().get(TEST_KEY_STR, "fallback"));
    }

    // -----------------------------------------------------------------------
    // getInt
    // -----------------------------------------------------------------------

    @Test
    void getInt_missingKey_returnsDefault() {
        assertEquals(42, ConfigManager.getInstance().getInt(TEST_KEY_INT, 42));
    }

    @Test
    void getInt_validIntValue_returnsParsedInt() throws Exception {
        setProperty(TEST_KEY_INT, "7");
        assertEquals(7, ConfigManager.getInstance().getInt(TEST_KEY_INT, 0));
    }

    @Test
    void getInt_negativeIntValue_returnsParsedInt() throws Exception {
        setProperty(TEST_KEY_INT, "-5");
        assertEquals(-5, ConfigManager.getInstance().getInt(TEST_KEY_INT, 0));
    }

    @Test
    void getInt_nonNumericValue_returnsDefault() throws Exception {
        setProperty(TEST_KEY_INT, "not-a-number");
        assertEquals(99, ConfigManager.getInstance().getInt(TEST_KEY_INT, 99));
    }

    @Test
    void getInt_decimalValue_returnsDefault() throws Exception {
        // Integer.parseInt("3.14") throws NumberFormatException → default
        setProperty(TEST_KEY_INT, "3.14");
        assertEquals(0, ConfigManager.getInstance().getInt(TEST_KEY_INT, 0));
    }

    @Test
    void getInt_emptyString_returnsDefault() throws Exception {
        setProperty(TEST_KEY_INT, "");
        assertEquals(5, ConfigManager.getInstance().getInt(TEST_KEY_INT, 5));
    }

    // -----------------------------------------------------------------------
    // getBoolean
    // -----------------------------------------------------------------------

    @Test
    void getBoolean_missingKey_returnsDefault() {
        assertTrue(ConfigManager.getInstance().getBoolean(TEST_KEY_BOOL, true));
        assertFalse(ConfigManager.getInstance().getBoolean(TEST_KEY_BOOL, false));
    }

    @Test
    void getBoolean_trueString_returnsTrue() throws Exception {
        setProperty(TEST_KEY_BOOL, "true");
        assertTrue(ConfigManager.getInstance().getBoolean(TEST_KEY_BOOL, false));
    }

    @Test
    void getBoolean_trueStringCaseInsensitive_returnsTrue() throws Exception {
        setProperty(TEST_KEY_BOOL, "TRUE");
        assertTrue(ConfigManager.getInstance().getBoolean(TEST_KEY_BOOL, false));
    }

    @Test
    void getBoolean_falseString_returnsFalse() throws Exception {
        setProperty(TEST_KEY_BOOL, "false");
        assertFalse(ConfigManager.getInstance().getBoolean(TEST_KEY_BOOL, true));
    }

    /** Boolean.parseBoolean treats anything other than "true" (case-insensitive) as false. */
    @Test
    void getBoolean_yesString_returnsFalse() throws Exception {
        setProperty(TEST_KEY_BOOL, "yes");
        assertFalse(ConfigManager.getInstance().getBoolean(TEST_KEY_BOOL, true));
    }

    @Test
    void getBoolean_numericOne_returnsFalse() throws Exception {
        setProperty(TEST_KEY_BOOL, "1");
        assertFalse(ConfigManager.getInstance().getBoolean(TEST_KEY_BOOL, true));
    }
}
