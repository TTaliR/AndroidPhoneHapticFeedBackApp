package com.example.smartwatchhapticsystem;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for the 1Hz sensor throttle logic used in MonitoringService.
 * Tests verify that data is sent at most once per second (1000ms).
 */
public class SensorThrottleTest {

    private static final long SENSOR_THROTTLE_MS = 1000; // 1 second interval (1Hz)
    private long lastSensorSendTime;
    private List<Map<String, String>> sentData;

    @Before
    public void setUp() {
        // Initialize to -SENSOR_THROTTLE_MS so first data point is always sent
        // This matches MonitoringService where lastSensorSendTime starts at 0
        // and first real timestamp will be much larger (actual system time)
        lastSensorSendTime = -SENSOR_THROTTLE_MS;
        sentData = new ArrayList<>();
    }

    /**
     * Simulates the throttle logic from MonitoringService.onReceived()
     * Returns true if data was sent, false if throttled.
     */
    private boolean simulateDataReceived(Map<String, String> data, long currentTime) {
        if (currentTime - lastSensorSendTime >= SENSOR_THROTTLE_MS) {
            lastSensorSendTime = currentTime;
            sentData.add(data);
            return true; // Data was sent
        }
        return false; // Data was throttled
    }

    /**
     * Creates mock heart rate data for testing.
     */
    private Map<String, String> createMockData(int heartRate) {
        Map<String, String> data = new HashMap<>();
        data.put("heartRate", String.valueOf(heartRate));
        data.put("userId", "TestUser");
        data.put("smartWatchId", "TestWatch");
        return data;
    }

    @Test
    public void testFirstDataAlwaysSent() {
        // First data point should always be sent (since lastSensorSendTime = 0)
        Map<String, String> data = createMockData(75);
        boolean wasSent = simulateDataReceived(data, 1000);

        assertTrue("First data point should always be sent", wasSent);
        assertEquals("Sent data list should have 1 item", 1, sentData.size());
    }

    @Test
    public void testThrottleBlocksDataWithin1Second() {
        // Send first data point at t=0
        simulateDataReceived(createMockData(70), 0);

        // Try to send more data before 1 second has passed
        boolean sent200ms = simulateDataReceived(createMockData(71), 200);
        boolean sent500ms = simulateDataReceived(createMockData(72), 500);
        boolean sent999ms = simulateDataReceived(createMockData(73), 999);

        assertFalse("Data at 200ms should be throttled", sent200ms);
        assertFalse("Data at 500ms should be throttled", sent500ms);
        assertFalse("Data at 999ms should be throttled", sent999ms);
        assertEquals("Only 1 data point should have been sent", 1, sentData.size());
    }

    @Test
    public void testDataSentAfterExactly1Second() {
        // Send first data point at t=0
        simulateDataReceived(createMockData(70), 0);

        // Try to send exactly at 1 second
        boolean sentAt1000ms = simulateDataReceived(createMockData(75), 1000);

        assertTrue("Data at exactly 1000ms should be sent", sentAt1000ms);
        assertEquals("2 data points should have been sent", 2, sentData.size());
    }

    @Test
    public void testDataSentAfterMoreThan1Second() {
        // Send first data point at t=0
        simulateDataReceived(createMockData(70), 0);

        // Try to send after more than 1 second
        boolean sentAt1500ms = simulateDataReceived(createMockData(80), 1500);

        assertTrue("Data at 1500ms should be sent", sentAt1500ms);
        assertEquals("2 data points should have been sent", 2, sentData.size());
    }

    @Test
    public void testRapidDataStream_OnlySendsAt1Hz() {
        // Simulate data arriving every 200ms over 2.5 seconds
        // Timestamps: 0, 200, 400, 600, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400
        long[] timestamps = {0, 200, 400, 600, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400};

        for (int i = 0; i < timestamps.length; i++) {
            simulateDataReceived(createMockData(70 + i), timestamps[i]);
        }

        // Should only send at: 0ms, 1000ms, 2000ms = 3 times
        assertEquals("Should only send 3 times over 2.4 seconds at 1Hz", 3, sentData.size());
    }

    @Test
    public void testVeryFastDataStream_100msIntervals() {
        // Simulate data arriving every 100ms over 3 seconds (30 data points)
        int sentCount = 0;
        for (long t = 0; t <= 3000; t += 100) {
            if (simulateDataReceived(createMockData((int)(60 + Math.random() * 40)), t)) {
                sentCount++;
            }
        }

        // At 1Hz over 3 seconds, should send 4 times: at 0, 1000, 2000, 3000
        assertEquals("Should send exactly 4 times at 1Hz over 3 seconds", 4, sentCount);
    }

    @Test
    public void testIrregularTimestamps() {
        // Simulate irregular data arrival times
        long[] timestamps = {0, 50, 300, 850, 1050, 1100, 2100, 2150, 3000};
        // Expected sends:     Y   N    N     N     Y      N      Y      N      N (3000 - 2100 = 900 < 1000)

        int sentCount = 0;
        for (long t : timestamps) {
            if (simulateDataReceived(createMockData(75), t)) {
                sentCount++;
            }
        }

        assertEquals("Should send 3 times with irregular timestamps", 3, sentCount);
    }

    @Test
    public void testLongGapBetweenData() {
        // Send first data at t=0
        simulateDataReceived(createMockData(70), 0);

        // Long gap with no data, then data arrives at t=10000 (10 seconds later)
        boolean sentAfterGap = simulateDataReceived(createMockData(80), 10000);

        assertTrue("Data should be sent after long gap", sentAfterGap);
        assertEquals("2 data points should have been sent", 2, sentData.size());
    }

    @Test
    public void testThrottleResetsAfterSend() {
        // Send at t=0
        simulateDataReceived(createMockData(70), 0);

        // Send at t=1000 (should work)
        simulateDataReceived(createMockData(75), 1000);

        // Try at t=1500 (only 500ms after last send, should be throttled)
        boolean throttledAt1500 = simulateDataReceived(createMockData(80), 1500);

        // Try at t=2000 (exactly 1000ms after last send, should work)
        boolean sentAt2000 = simulateDataReceived(createMockData(85), 2000);

        assertFalse("Data at 1500ms should be throttled (500ms after last send)", throttledAt1500);
        assertTrue("Data at 2000ms should be sent (1000ms after last send)", sentAt2000);
        assertEquals("3 data points should have been sent", 3, sentData.size());
    }

    @Test
    public void testCorrectDataIsSent() {
        // Send first data at t=0 with HR=70
        Map<String, String> data1 = createMockData(70);
        simulateDataReceived(data1, 0);

        // Throttled data at t=500 with HR=75 (should NOT be sent)
        Map<String, String> data2 = createMockData(75);
        simulateDataReceived(data2, 500);

        // Sent data at t=1000 with HR=80
        Map<String, String> data3 = createMockData(80);
        simulateDataReceived(data3, 1000);

        // Verify correct data was sent
        assertEquals("First sent data should have HR=70", "70", sentData.get(0).get("heartRate"));
        assertEquals("Second sent data should have HR=80", "80", sentData.get(1).get("heartRate"));
        assertEquals("Throttled data (HR=75) should not be in sent list", 2, sentData.size());
    }

    @Test
    public void testRealWorldScenario_SystemTimeStamps() {
        // Simulate real MonitoringService behavior where lastSensorSendTime starts at 0
        // and actual timestamps are large (like System.currentTimeMillis())
        lastSensorSendTime = 0; // Reset to actual initial value

        // First data arrives at a realistic timestamp (e.g., ~1.7 trillion ms since epoch)
        long baseTime = 1739900000000L; // Approximate Feb 2025 timestamp

        // First data should be sent (baseTime - 0 = huge number >= 1000)
        boolean firstSent = simulateDataReceived(createMockData(70), baseTime);
        assertTrue("First real data should be sent", firstSent);

        // Data 500ms later should be throttled
        boolean throttled = simulateDataReceived(createMockData(75), baseTime + 500);
        assertFalse("Data 500ms later should be throttled", throttled);

        // Data 1000ms later should be sent
        boolean secondSent = simulateDataReceived(createMockData(80), baseTime + 1000);
        assertTrue("Data 1000ms later should be sent", secondSent);

        assertEquals("Should have sent 2 data points", 2, sentData.size());
    }
}

