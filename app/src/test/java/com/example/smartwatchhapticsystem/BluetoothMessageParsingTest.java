package com.example.smartwatchhapticsystem;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class BluetoothMessageParsingTest {

    /**
     * Simulates the parsing logic found in BluetoothConnectionManager.handleHeartRateMessage
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
     * Simulates the numeric validation logic found in BluetoothConnectionManager.handleHeartRateMessage
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
    public void testParseHeartRate_IntValue() {
        String rawMessage = "MonitoringType:HeartRate, Value:75, UserID:101, SmartWatchID:502, AndroidID:50";
        Map<String, String> parsed = parseRawMessage(rawMessage);
        
        assertEquals("75", parsed.get("Value"));
        assertTrue("Value should be a valid numeric string", isValueValidNumeric(parsed.get("Value")));
    }

    @Test
    public void testParseHeartRate_FloatValue() {
        // The watch sends a float instead of an int
        String rawMessage = "MonitoringType:HeartRate, Value:75.5, UserID:101, SmartWatchID:502, AndroidID:50";
        Map<String, String> parsed = parseRawMessage(rawMessage);
        
        assertEquals("75.5", parsed.get("Value"));
        assertTrue("Value 75.5 should be a valid numeric string",
                    isValueValidNumeric(parsed.get("Value")));
    }

    @Test
    public void testParseHeartRate_MalformedValue() {
        String rawMessage = "MonitoringType:HeartRate, Value:abc, UserID:101";
        Map<String, String> parsed = parseRawMessage(rawMessage);
        
        assertEquals("abc", parsed.get("Value"));
        assertFalse("Malformed value should not be a valid numeric string", isValueValidNumeric(parsed.get("Value")));
    }
}
