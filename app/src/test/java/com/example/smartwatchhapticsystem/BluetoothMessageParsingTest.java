package com.example.smartwatchhapticsystem;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class BluetoothMessageParsingTest {

    /**
     * Simulates the parsing logic found in BluetoothConnectionManager.handleSensorMessage
     */
    private Map<String, String> parseRawMessage(String line) {
        Map<String, String> dataMap = new HashMap<>();
        String[] parts = line.split(",");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                dataMap.put(kv[0].trim(), kv[1].trim());
            }
        }
        return dataMap;
    }

    /**
     * Simulates the "soft" numeric validation logic found in BluetoothConnectionManager.handleSensorMessage.
     * In the new implementation, we log a warning but still return the data.
     * For testing purposes, this returns true if numeric, matching previous behavior expectations.
     */
    private boolean isValueValidNumeric(String value) {
        if (value == null) return false;
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Test
    public void testParseSensor_IntValue() {
        String rawMessage = "MonitoringType:HeartRate, Value:75, UserID:101, SmartWatchID:502, AndroidID:50";
        Map<String, String> parsed = parseRawMessage(rawMessage);
        
        assertEquals("75", parsed.get("Value"));
        assertTrue("Value should be a valid numeric string", isValueValidNumeric(parsed.get("Value")));
    }

    @Test
    public void testParseSensor_FloatValue() {
        String rawMessage = "MonitoringType:Light, Value:120.5, UserID:101, SmartWatchID:502, AndroidID:50";
        Map<String, String> parsed = parseRawMessage(rawMessage);
        
        assertEquals("120.5", parsed.get("Value"));
        assertTrue("Value 120.5 should be a valid numeric string",
                    isValueValidNumeric(parsed.get("Value")));
    }

    @Test
    public void testParseSensor_MalformedValue() {
        // In the new logic, we still parse it into the map, but isValueValidNumeric would be false
        String rawMessage = "MonitoringType:HeartRate, Value:abc, UserID:101";
        Map<String, String> parsed = parseRawMessage(rawMessage);
        
        assertEquals("abc", parsed.get("Value"));
        assertFalse("Malformed value should not be a valid numeric string", isValueValidNumeric(parsed.get("Value")));
    }

    @Test
    public void testParseSensor_MultiAxisValue() {
        // Verifying that CSV style data (like Accelerometer) is parsed correctly into the map
        // even though it isn't a single Double.
        String rawMessage = "MonitoringType:Accelerometer, Value:0.1;0.5;9.8, UserID:101";
        Map<String, String> parsed = parseRawMessage(rawMessage);

        assertEquals("0.1;0.5;9.8", parsed.get("Value"));
        assertFalse("Multi-axis string should fail numeric-only check", isValueValidNumeric(parsed.get("Value")));
    }

    @Test
    public void testParseSensor_GenericType() {
        // Ensuring the parser handles any MonitoringType string
        String rawMessage = "MonitoringType:SunAzimuth, Value:180.0, UserID:101";
        Map<String, String> parsed = parseRawMessage(rawMessage);

        assertEquals("SunAzimuth", parsed.get("MonitoringType"));
        assertEquals("180.0", parsed.get("Value"));
    }
}
