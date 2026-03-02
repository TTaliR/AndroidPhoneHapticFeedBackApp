package com.example.smartwatchhapticsystem;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for device identifier parsing logic used in MonitoringService.
 * Tests parsing of Android ID, User ID, and SmartWatch ID from device names and aliases.
 */
public class DeviceIdentifierParsingTest {

    /**
     * Parses Android ID from system device name (e.g., "Android-1234" -> "1234")
     */
    private String parseAndroidId(String systemName) {
        if (systemName != null && systemName.matches("^Android-\\d+$")) {
            return systemName.split("-")[1];
        }
        return "UnknownAndroid";
    }

    /**
     * Parses User ID and SmartWatch ID from Bluetooth alias
     * Expected format: "UserID-123-SmartWatchID-456"
     * Returns array: [userId, smartWatchId]
     */
    private String[] parseWatchAlias(String alias) {
        String userId = "UnknownUser";
        String smartWatchId = "UnknownWatch";

        if (alias != null && alias.matches("^UserID-\\d+-SmartWatchID-\\d+$")) {
            String[] tokens = alias.split("-");
            userId = tokens[1];        // "123" from "UserID-123"
            smartWatchId = tokens[3];  // "456" from "SmartWatchID-456"
        }

        return new String[]{userId, smartWatchId};
    }

    // ==================== Android ID Tests ====================

    @Test
    public void testParseAndroidId_ValidFormat() {
        assertEquals("50", parseAndroidId("Android-50"));
        assertEquals("1234", parseAndroidId("Android-1234"));
        assertEquals("999999", parseAndroidId("Android-999999"));
        assertEquals("1", parseAndroidId("Android-1"));
    }

    @Test
    public void testParseAndroidId_InvalidFormat_ReturnsUnknown() {
        assertEquals("UnknownAndroid", parseAndroidId("android-50")); // lowercase
        assertEquals("UnknownAndroid", parseAndroidId("Android50"));   // missing dash
        assertEquals("UnknownAndroid", parseAndroidId("Android-"));    // missing number
        assertEquals("UnknownAndroid", parseAndroidId("Android-abc")); // non-numeric
        assertEquals("UnknownAndroid", parseAndroidId("Phone-50"));    // wrong prefix
        assertEquals("UnknownAndroid", parseAndroidId(""));            // empty
        assertEquals("UnknownAndroid", parseAndroidId(null));          // null
    }

    @Test
    public void testParseAndroidId_EdgeCases() {
        assertEquals("UnknownAndroid", parseAndroidId("Android-50-extra")); // extra parts
        assertEquals("UnknownAndroid", parseAndroidId("  Android-50"));     // leading space
        assertEquals("UnknownAndroid", parseAndroidId("Android-50  "));     // trailing space
        assertEquals("UnknownAndroid", parseAndroidId("Android--50"));      // double dash
    }

    // ==================== Watch Alias Tests ====================

    @Test
    public void testParseWatchAlias_ValidFormat() {
        String[] result = parseWatchAlias("UserID-123-SmartWatchID-456");
        assertEquals("123", result[0]);
        assertEquals("456", result[1]);
    }

    @Test
    public void testParseWatchAlias_LargeNumbers() {
        String[] result = parseWatchAlias("UserID-999999-SmartWatchID-888888");
        assertEquals("999999", result[0]);
        assertEquals("888888", result[1]);
    }

    @Test
    public void testParseWatchAlias_SingleDigitIds() {
        String[] result = parseWatchAlias("UserID-1-SmartWatchID-2");
        assertEquals("1", result[0]);
        assertEquals("2", result[1]);
    }

    @Test
    public void testParseWatchAlias_InvalidFormat_ReturnsUnknown() {
        // Missing parts
        String[] result1 = parseWatchAlias("UserID-123");
        assertEquals("UnknownUser", result1[0]);
        assertEquals("UnknownWatch", result1[1]);

        // Wrong format
        String[] result2 = parseWatchAlias("User-123-Watch-456");
        assertEquals("UnknownUser", result2[0]);
        assertEquals("UnknownWatch", result2[1]);

        // Empty string
        String[] result3 = parseWatchAlias("");
        assertEquals("UnknownUser", result3[0]);
        assertEquals("UnknownWatch", result3[1]);

        // Null
        String[] result4 = parseWatchAlias(null);
        assertEquals("UnknownUser", result4[0]);
        assertEquals("UnknownWatch", result4[1]);
    }

    @Test
    public void testParseWatchAlias_CaseSensitive() {
        // The regex expects exact case
        String[] result = parseWatchAlias("userid-123-smartwatchid-456");
        assertEquals("UnknownUser", result[0]);
        assertEquals("UnknownWatch", result[1]);
    }

    @Test
    public void testParseWatchAlias_NonNumericIds() {
        String[] result = parseWatchAlias("UserID-abc-SmartWatchID-def");
        assertEquals("UnknownUser", result[0]);
        assertEquals("UnknownWatch", result[1]);
    }

    @Test
    public void testParseWatchAlias_ExtraSpaces() {
        String[] result = parseWatchAlias(" UserID-123-SmartWatchID-456 ");
        assertEquals("UnknownUser", result[0]);
        assertEquals("UnknownWatch", result[1]);
    }

    @Test
    public void testParseWatchAlias_MixedValidInvalid() {
        // Valid userId format but invalid watchId format
        String[] result = parseWatchAlias("UserID-123-SmartWatchID-");
        assertEquals("UnknownUser", result[0]);
        assertEquals("UnknownWatch", result[1]);
    }
}

