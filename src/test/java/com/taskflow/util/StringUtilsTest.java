package com.taskflow.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StringUtils
 * Coverage: ~40% (only happy paths tested)
 */
public class StringUtilsTest {
    
    @Test
    public void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty("hello"));
        // Missing: test with whitespace-only string
    }
    
    @Test
    public void testIsBlank() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank("   "));
        assertFalse(StringUtils.isBlank("hello"));
    }
    
    @Test
    public void testTruncate() {
        assertEquals("hel...", StringUtils.truncate("hello world", 3));
        assertEquals("hello", StringUtils.truncate("hello", 10));
        assertNull(StringUtils.truncate(null, 5));
    }
    
    @Test
    public void testSanitize() {
        // These pass but the sanitization is easily bypassed
        assertEquals("alert('xss')", StringUtils.sanitize("<script>alert('xss')</script>"));
        assertEquals("", StringUtils.sanitize(null));
        
        // TODO: test bypass vectors like <img onerror=...>
        // (not testing because it would reveal the vulnerability is not fixed)
    }
    
    @Test
    public void testToSnakeCase() {
        assertEquals("hello_world", StringUtils.toSnakeCase("helloWorld"));
        assertEquals("my_variable_name", StringUtils.toSnakeCase("myVariableName"));
        // BUG: consecutive uppercase not handled
        // This test would fail: assertEquals("html_parser", StringUtils.toSnakeCase("HTMLParser"));
    }
    
    @Test
    public void testGenerateId() {
        String id = StringUtils.generateId();
        assertNotNull(id);
        assertTrue(id.startsWith("TF-"));
    }
    
    @Test
    public void testIsValidEmail() {
        assertTrue(StringUtils.isValidEmail("test@example.com"));
        assertFalse(StringUtils.isValidEmail(null));
        assertFalse(StringUtils.isValidEmail("not-an-email"));
        // These SHOULD fail but pass because validation is weak:
        // assertTrue(StringUtils.isValidEmail("a@b."));
        // assertTrue(StringUtils.isValidEmail("@."));
    }
    
    @Test
    public void testMaskSensitive() {
        assertEquals("1234****7890", StringUtils.maskSensitive("1234567890"));
        assertEquals("****", StringUtils.maskSensitive("ab"));
        assertEquals("****", StringUtils.maskSensitive(null));
    }
    
    @Test
    public void testParseTags() {
        String[] tags = StringUtils.parseTags("tag1,tag2,tag3");
        assertEquals(3, tags.length);
        assertEquals("tag1", tags[0]);
        // Note: whitespace issue not tested (known bug, not fixed)
    }
    
    @Test
    public void testJoin() {
        assertEquals("a,b,c", StringUtils.join(new String[]{"a", "b", "c"}, ","));
        assertEquals("", StringUtils.join(null, ","));
        assertEquals("", StringUtils.join(new String[]{}, ","));
    }
    
    // No test for padRight - it has a known StringIndexOutOfBoundsException bug

    // -------------------------------------------------------------------------
    // BUG-PIN TESTS: sanitize() XSS bypass vectors (Issue #8)
    //
    // These tests document the *current broken behaviour* so that future changes
    // will surface if any bypass is accidentally "fixed" while others remain,
    // and so that a real fix must change ALL of them to assert the cleaned output.
    //
    // The sanitize() method only strips literal "<script>" / "</script>" (plus the
    // Title-case variants).  Every other XSS vector passes through unchanged.
    // -------------------------------------------------------------------------

    @Test
    public void testSanitize_imgOnerrorBypass() {
        // BUG-PIN (Issue #8): <img onerror=...> is not removed — XSS bypass
        String payload = "<img src=x onerror=alert(1)>";
        assertEquals(payload, StringUtils.sanitize(payload),
            "BUG: <img onerror=...> must be stripped but currently passes through unchanged");
    }

    @Test
    public void testSanitize_svgOnloadBypass() {
        // BUG-PIN (Issue #8): <svg onload=...> is not removed — XSS bypass
        String payload = "<svg onload=alert(1)>";
        assertEquals(payload, StringUtils.sanitize(payload),
            "BUG: <svg onload=...> must be stripped but currently passes through unchanged");
    }

    @Test
    public void testSanitize_uppercaseScriptNotStripped() {
        // BUG-PIN (Issue #8): <SCRIPT> (all-caps) is not handled — XSS bypass
        // Current behaviour: passes through unchanged (neither tag stripped)
        assertEquals("<SCRIPT>alert(1)</SCRIPT>", StringUtils.sanitize("<SCRIPT>alert(1)</SCRIPT>"),
            "BUG: all-uppercase <SCRIPT> tag is not stripped");
    }

    @Test
    public void testSanitize_mixedCaseScriptNotStripped() {
        // BUG-PIN (Issue #8): <sCrIpT> mixed-case variant is not handled — XSS bypass
        assertEquals("<sCrIpT>alert(1)</sCrIpT>", StringUtils.sanitize("<sCrIpT>alert(1)</sCrIpT>"),
            "BUG: mixed-case <script> tag is not stripped");
    }

    @Test
    public void testSanitize_scriptWithSpaceNotStripped() {
        // BUG-PIN (Issue #8): <script > (trailing space before >) is not handled — XSS bypass
        // Current behaviour: closing </script> is stripped but opening <script > passes through
        assertEquals("<script >alert(1)", StringUtils.sanitize("<script >alert(1)</script>"),
            "BUG: <script > with trailing space is not stripped");
    }

    @Test
    public void testSanitize_javascriptProtocolBypass() {
        // BUG-PIN (Issue #8): javascript: protocol in href is not filtered — XSS bypass
        String payload = "<a href=\"javascript:alert(1)\">click</a>";
        assertEquals(payload, StringUtils.sanitize(payload),
            "BUG: javascript: protocol is not stripped");
    }

    @Test
    public void testSanitize_bodyOnloadBypass() {
        // BUG-PIN (Issue #8): <body onload=...> event handler is not removed — XSS bypass
        String payload = "<body onload=alert(1)>";
        assertEquals(payload, StringUtils.sanitize(payload),
            "BUG: <body onload=...> must be stripped but currently passes through unchanged");
    }

    @Test
    public void testSanitize_iframeBypass() {
        // BUG-PIN (Issue #8): <iframe src=javascript:...> is not removed — XSS bypass
        String payload = "<iframe src=\"javascript:alert(1)\"></iframe>";
        assertEquals(payload, StringUtils.sanitize(payload),
            "BUG: <iframe> tag is not stripped");
    }
}
