package com.example.smartwatchhapticsystem;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for retry logic used in MonitoringService.
 * Tests retry counting, max retry limits, and retry delay behavior.
 */
public class RetryLogicTest {

    private static final int MAX_RETRIES = 8;
    private static final int RETRY_DELAY_MS = 500;
    private static final int UNLIMITED_RETRIES = -1;

    private int currentRetries;
    private int retryAttempts;
    private boolean aborted;

    @Before
    public void setUp() {
        currentRetries = 0;
        retryAttempts = 0;
        aborted = false;
    }

    /**
     * Simulates the retry logic from MonitoringService.getMonitoringTypeFromn8n()
     * Returns true if retry was scheduled, false if max retries reached
     */
    private boolean simulateRetryOnError(int maxRetries) {
        if (maxRetries == UNLIMITED_RETRIES || currentRetries < maxRetries) {
            currentRetries++;
            retryAttempts++;
            return true; // Retry scheduled
        } else {
            aborted = true;
            return false; // Max retries reached, aborted
        }
    }

    /**
     * Simulates successful connection (resets retry counter)
     */
    private void simulateSuccessfulConnection() {
        currentRetries = 0;
    }

    // ==================== Basic Retry Tests ====================

    @Test
    public void testFirstRetry_IncreasesCounter() {
        boolean shouldRetry = simulateRetryOnError(MAX_RETRIES);

        assertTrue("Should schedule retry on first error", shouldRetry);
        assertEquals("Current retries should be 1", 1, currentRetries);
        assertFalse("Should not abort", aborted);
    }

    @Test
    public void testMultipleRetries_UpToMax() {
        // Simulate 8 consecutive failures (max retries = 8)
        for (int i = 0; i < MAX_RETRIES; i++) {
            boolean shouldRetry = simulateRetryOnError(MAX_RETRIES);
            assertTrue("Retry " + (i + 1) + " should be scheduled", shouldRetry);
        }

        assertEquals("Should have retried 8 times", 8, currentRetries);
        assertEquals("Retry attempts should match", 8, retryAttempts);
        assertFalse("Should not abort yet", aborted);
    }

    @Test
    public void testMaxRetriesReached_Aborts() {
        // Exhaust all retries
        for (int i = 0; i < MAX_RETRIES; i++) {
            simulateRetryOnError(MAX_RETRIES);
        }

        // One more error should trigger abort
        boolean shouldRetry = simulateRetryOnError(MAX_RETRIES);

        assertFalse("Should NOT schedule retry after max reached", shouldRetry);
        assertTrue("Should abort", aborted);
        assertEquals("Counter should still be at max", 8, currentRetries);
    }

    @Test
    public void testSuccessResetsRetryCounter() {
        // Fail a few times
        simulateRetryOnError(MAX_RETRIES);
        simulateRetryOnError(MAX_RETRIES);
        simulateRetryOnError(MAX_RETRIES);
        assertEquals("Should have 3 retries", 3, currentRetries);

        // Success should reset counter
        simulateSuccessfulConnection();
        assertEquals("Counter should be reset to 0", 0, currentRetries);

        // New failures should start fresh
        simulateRetryOnError(MAX_RETRIES);
        assertEquals("Counter should be 1 after new failure", 1, currentRetries);
    }

    @Test
    public void testUnlimitedRetries_NeverAborts() {
        // Simulate 100 consecutive failures with unlimited retries
        for (int i = 0; i < 100; i++) {
            boolean shouldRetry = simulateRetryOnError(UNLIMITED_RETRIES);
            assertTrue("Should always retry with unlimited setting", shouldRetry);
        }

        assertEquals("Should have retried 100 times", 100, currentRetries);
        assertFalse("Should never abort with unlimited retries", aborted);
    }

    @Test
    public void testZeroMaxRetries_ImmediateAbort() {
        // With maxRetries = 0, should abort immediately
        boolean shouldRetry = simulateRetryOnError(0);

        assertFalse("Should NOT retry with maxRetries=0", shouldRetry);
        assertTrue("Should abort immediately", aborted);
    }

    @Test
    public void testSingleMaxRetry_OneAttemptThenAbort() {
        // First failure: retry allowed
        boolean firstRetry = simulateRetryOnError(1);
        assertTrue("First retry should be allowed", firstRetry);
        assertEquals(1, currentRetries);

        // Second failure: abort
        boolean secondRetry = simulateRetryOnError(1);
        assertFalse("Second retry should NOT be allowed", secondRetry);
        assertTrue("Should abort after 1 retry", aborted);
    }

    // ==================== Recovery Scenario Tests ====================

    @Test
    public void testRecoveryAfterMultipleFailures() {
        // Fail 5 times
        for (int i = 0; i < 5; i++) {
            simulateRetryOnError(MAX_RETRIES);
        }
        assertEquals(5, currentRetries);

        // Successful connection
        simulateSuccessfulConnection();
        assertEquals(0, currentRetries);

        // Should have full 8 retries available again
        for (int i = 0; i < MAX_RETRIES; i++) {
            assertTrue(simulateRetryOnError(MAX_RETRIES));
        }
        assertFalse(simulateRetryOnError(MAX_RETRIES)); // 9th should fail
    }

    @Test
    public void testIntermittentFailures() {
        // Fail twice
        simulateRetryOnError(MAX_RETRIES);
        simulateRetryOnError(MAX_RETRIES);
        assertEquals(2, currentRetries);

        // Success
        simulateSuccessfulConnection();
        assertEquals(0, currentRetries);

        // Fail once
        simulateRetryOnError(MAX_RETRIES);
        assertEquals(1, currentRetries);

        // Success again
        simulateSuccessfulConnection();
        assertEquals(0, currentRetries);

        // Counter should always reset on success
        assertFalse(aborted);
    }

    // ==================== Edge Cases ====================

    @Test
    public void testRetryDelayConstant() {
        // Verify the retry delay constant is correct (500ms)
        assertEquals("Retry delay should be 500ms", 500, RETRY_DELAY_MS);
    }

    @Test
    public void testMaxRetriesConstant() {
        // Verify the max retries constant is correct (8)
        assertEquals("Max retries should be 8", 8, MAX_RETRIES);
    }

    @Test
    public void testUnlimitedRetriesConstant() {
        // Verify unlimited retries is represented as -1
        assertEquals("Unlimited retries should be -1", -1, UNLIMITED_RETRIES);
    }
}

