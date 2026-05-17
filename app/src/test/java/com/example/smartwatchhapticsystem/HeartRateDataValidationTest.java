package com.example.smartwatchhapticsystem;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for sensor data validation and parsing.
 * Updated to reflect the generic "Value" field used in the refactored architecture.
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
     * Parses sensor value from data map (returns -1 if invalid)
     * Matches the generic "Value" field.
     */
    private int parseSensorValue(Map<String, String> data) {
        if (data == null || !data.containsKey("Value") || data.get("Value") == null) {
            return -1;
        }
        try {
            return (int) Double.parseDouble(data.get("Value"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Validates that required fields exist in data map
     */
    private boolean hasRequiredFields(Map<String, String> data) {
        if (data == null) return false;
        return data.containsKey("Value") &&
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
    public void testParseSensorValue_ValidData() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", "72");
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertEquals(72, parseSensorValue(data));
    }

    @Test
    public void testParseSensorValue_MissingKey() {
        Map<String, String> data = new HashMap<>();
        data.put("userId", "123");

        assertEquals(-1, parseSensorValue(data));
    }

    @Test
    public void testParseSensorValue_NullMap() {
        assertEquals(-1, parseSensorValue(null));
    }

    @Test
    public void testParseSensorValue_EmptyMap() {
        assertEquals(-1, parseSensorValue(new HashMap<>()));
    }

    @Test
    public void testParseSensorValue_NonNumericValue() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", "abc");

        assertEquals(-1, parseSensorValue(data));
    }

    @Test
    public void testParseSensorValue_EmptyValue() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", "");

        assertEquals(-1, parseSensorValue(data));
    }

    @Test
    public void testParseSensorValue_NullValue() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", null);

        assertEquals(-1, parseSensorValue(data));
    }

    @Test
    public void testParseSensorValue_FloatValue() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", "72.5");

        // Double.parseDouble handles floats, and we cast to int
        assertEquals(72, parseSensorValue(data));
    }

    @Test
    public void testParseSensorValue_WithWhitespace() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", " 72 ");

        // Double.parseDouble handles whitespace
        assertEquals(72, parseSensorValue(data));
    }

    // ==================== Required Fields Tests ====================

    @Test
    public void testHasRequiredFields_AllPresent() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", "72");
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertTrue(hasRequiredFields(data));
    }

    @Test
    public void testHasRequiredFields_MissingValue() {
        Map<String, String> data = new HashMap<>();
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertFalse(hasRequiredFields(data));
    }

    @Test
    public void testHasRequiredFields_MissingUserId() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", "72");
        data.put("smartWatchId", "456");

        assertFalse(hasRequiredFields(data));
    }

    @Test
    public void testHasRequiredFields_MissingSmartWatchId() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", "72");
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
        data.put("Value", "72");
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
        data.put("Value", "72");
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertTrue(hasRequiredFields(data));
        int val = parseSensorValue(data);
        assertTrue(isValidHeartRate(val));
        assertTrue(isNormalRestingHR(val));
    }

    @Test
    public void testCompleteValidation_HighValue() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", "180");
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertTrue(hasRequiredFields(data));
        int val = parseSensorValue(data);
        assertTrue(isValidHeartRate(val));     // Valid physiologically
        assertFalse(isNormalRestingHR(val));   // Not normal resting
    }

    @Test
    public void testCompleteValidation_InvalidValue() {
        Map<String, String> data = new HashMap<>();
        data.put("Value", "0");
        data.put("userId", "123");
        data.put("smartWatchId", "456");

        assertTrue(hasRequiredFields(data));
        int val = parseSensorValue(data);
        assertFalse(isValidHeartRate(val));    // Not valid
    }
}
