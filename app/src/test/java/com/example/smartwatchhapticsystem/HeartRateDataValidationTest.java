package com.example.smartwatchhapticsystem;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for heart rate data validation and parsing.
 * Tests validation of heart rate values and data map structure.
 */
public class HeartRateDataValidationTest {

    private static final int MIN_VALID_HR = 30;   // Minimum physiologically possible HR
    private static final int MAX_VALID_HR = 250;  // Maximum physiologically possible HR
    private static final int NORMAL_HR_LOW = 60;
    private static final int NORMAL_HR_HIGH = 100;

    /**
     * Validates if a heart rate value is within physiological range
     */
    private boolean isValidHeartRate(int heartRate) {
        return heartRate >= MIN_VALID_HR && heartRate <= MAX_VALID_HR;
    }

    /**
     * Validates if heart rate is in normal resting range
     */
    private boolean isNormalRestingHR(int heartRate) {
        return heartRate >= NORMAL_HR_LOW && heartRate <= NORMAL_HR_HIGH;
    }

    /**
     * Parses heart rate from data map (returns -1 if invalid)
     */
    private int parseHeartRate(Map<String, String> data) {
        if (data == null || !data.containsKey("heartRate")) {
            return -1;
        }
        try {
            return Integer.parseInt(data.get("heartRate"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Validates that required fields exist in data map
     */
    private boolean hasRequiredFields(Map<String, String> data) {
        if (data == null) return false;
        return data.containsKey("heartRate") &&
               data.containsKey("userId") &&
               data.containsKey("smartWatchId");
    }

    // ==================== Heart Rate Value Tests ====================

    @Test
    public void testValidHeartRates() {
        assertTrue(isValidHeartRate(60));
        assertTrue(isValidHeartRate(72));
        assertTrue(isValidHeartRate(100));
        assertTrue(isValidHeartRate(150)); // Exercise HR
        assertTrue(isValidHeartRate(180)); // High intensity
    }

    @Test
    public void testBoundaryHeartRates() {
        assertTrue(isValidHeartRate(MIN_VALID_HR));   // 30 bpm - extreme bradycardia
        assertTrue(isValidHeartRate(MAX_VALID_HR));   // 250 bpm - extreme tachycardia
        assertFalse(isValidHeartRate(MIN_VALID_HR - 1)); // 29 - too low
        assertFalse(isValidHeartRate(MAX_VALID_HR + 1)); // 251 - too high
    }

    @Test
    public void testInvalidHeartRates() {
        assertFalse(isValidHeartRate(0));
        assertFalse(isValidHeartRate(-1));
        assertFalse(isValidHeartRate(-100));
        assertFalse(isValidHeartRate(300));
        assertFalse(isValidHeartRate(1000));
    }

    @Test
    public void testNormalRestingHeartRate() {
        assertTrue(isNormalRestingHR(60));
        assertTrue(isNormalRestingHR(72));
        assertTrue(isNormalRestingHR(100));
        assertFalse(isNormalRestingHR(59));  // Below normal
        assertFalse(isNormalRestingHR(101)); // Above normal (but valid)
    }

    // ==================== Data Parsing Tests ====================

    @Test
    public void testParseHeartRate_ValidData() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "72");
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertEquals(72, parseHeartRate(data));
    }

    @Test
    public void testParseHeartRate_MissingKey() {
        Map<String, String> data = new HashMap<>();
        data.put("userId", "123");

        assertEquals(-1, parseHeartRate(data));
    }

    @Test
    public void testParseHeartRate_NullMap() {
        assertEquals(-1, parseHeartRate(null));
    }

    @Test
    public void testParseHeartRate_EmptyMap() {
        assertEquals(-1, parseHeartRate(new HashMap<>()));
    }

    @Test
    public void testParseHeartRate_NonNumericValue() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "abc");

        assertEquals(-1, parseHeartRate(data));
    }

    @Test
    public void testParseHeartRate_EmptyValue() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "");

        assertEquals(-1, parseHeartRate(data));
    }

    @Test
    public void testParseHeartRate_NullValue() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", null);

        assertEquals(-1, parseHeartRate(data));
    }

    @Test
    public void testParseHeartRate_FloatValue() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "72.5");

        // Integer.parseInt doesn't handle floats
        assertEquals(-1, parseHeartRate(data));
    }

    @Test
    public void testParseHeartRate_WithWhitespace() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", " 72 ");

        // Integer.parseInt doesn't handle whitespace
        assertEquals(-1, parseHeartRate(data));
    }

    // ==================== Required Fields Tests ====================

    @Test
    public void testHasRequiredFields_AllPresent() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "72");
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertTrue(hasRequiredFields(data));
    }

    @Test
    public void testHasRequiredFields_MissingHeartRate() {
        Map<String, String> data = new HashMap<>();
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertFalse(hasRequiredFields(data));
    }

    @Test
    public void testHasRequiredFields_MissingUserId() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "72");
        data.put("smartWatchId", "456");

        assertFalse(hasRequiredFields(data));
    }

    @Test
    public void testHasRequiredFields_MissingSmartWatchId() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "72");
        data.put("userId", "123");

        assertFalse(hasRequiredFields(data));
    }

    @Test
    public void testHasRequiredFields_NullMap() {
        assertFalse(hasRequiredFields(null));
    }

    @Test
    public void testHasRequiredFields_EmptyMap() {
        assertFalse(hasRequiredFields(new HashMap<>()));
    }

    @Test
    public void testHasRequiredFields_ExtraFieldsAreOk() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "72");
        data.put("userId", "123");
        data.put("smartWatchId", "456");
        data.put("timestamp", "1234567890");
        data.put("extraField", "someValue");

        assertTrue(hasRequiredFields(data));
    }

    // ==================== Combined Validation Tests ====================

    @Test
    public void testCompleteValidation_ValidData() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "72");
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertTrue(hasRequiredFields(data));
        int hr = parseHeartRate(data);
        assertTrue(isValidHeartRate(hr));
        assertTrue(isNormalRestingHR(hr));
    }

    @Test
    public void testCompleteValidation_HighHeartRate() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "180");
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertTrue(hasRequiredFields(data));
        int hr = parseHeartRate(data);
        assertTrue(isValidHeartRate(hr));     // Valid physiologically
        assertFalse(isNormalRestingHR(hr));   // Not normal resting
    }

    @Test
    public void testCompleteValidation_InvalidHeartRate() {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", "0");
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertTrue(hasRequiredFields(data));
        int hr = parseHeartRate(data);
        assertFalse(isValidHeartRate(hr));    // Not valid
    }
}

