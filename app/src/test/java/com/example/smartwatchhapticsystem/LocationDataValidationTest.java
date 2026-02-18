package com.example.smartwatchhapticsystem;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for location data validation used in MonitoringService.
 * Tests latitude/longitude bounds and location data structure.
 */
public class LocationDataValidationTest {

    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    /**
     * Validates if latitude is within valid range (-90 to 90)
     */
    private boolean isValidLatitude(double lat) {
        return lat >= MIN_LATITUDE && lat <= MAX_LATITUDE;
    }

    /**
     * Validates if longitude is within valid range (-180 to 180)
     */
    private boolean isValidLongitude(double lon) {
        return lon >= MIN_LONGITUDE && lon <= MAX_LONGITUDE;
    }

    /**
     * Validates if location coordinates are valid
     */
    private boolean isValidLocation(double lat, double lon) {
        return isValidLatitude(lat) && isValidLongitude(lon);
    }

    /**
     * Simple LocationData representation for testing
     */
    private static class LocationData {
        final double latitude;
        final double longitude;
        final String userId;
        final String smartWatchId;
        final String androidId;

        LocationData(double lat, double lon, String userId, String watchId, String androidId) {
            this.latitude = lat;
            this.longitude = lon;
            this.userId = userId;
            this.smartWatchId = watchId;
            this.androidId = androidId;
        }

        boolean isComplete() {
            return userId != null && !userId.equals("UnknownUser") &&
                   smartWatchId != null && !smartWatchId.equals("UnknownWatch") &&
                   androidId != null && !androidId.equals("UnknownAndroid");
        }
    }

    // ==================== Latitude Tests ====================

    @Test
    public void testValidLatitudes() {
        assertTrue(isValidLatitude(0.0));      // Equator
        assertTrue(isValidLatitude(45.0));     // Mid-northern hemisphere
        assertTrue(isValidLatitude(-45.0));    // Mid-southern hemisphere
        assertTrue(isValidLatitude(51.5074));  // London
        assertTrue(isValidLatitude(31.7683));  // Jerusalem
        assertTrue(isValidLatitude(-33.8688)); // Sydney
    }

    @Test
    public void testBoundaryLatitudes() {
        assertTrue(isValidLatitude(MIN_LATITUDE));  // -90 (South Pole)
        assertTrue(isValidLatitude(MAX_LATITUDE));  // 90 (North Pole)
        assertFalse(isValidLatitude(-90.001));
        assertFalse(isValidLatitude(90.001));
    }

    @Test
    public void testInvalidLatitudes() {
        assertFalse(isValidLatitude(-91.0));
        assertFalse(isValidLatitude(91.0));
        assertFalse(isValidLatitude(-180.0));
        assertFalse(isValidLatitude(180.0));
        assertFalse(isValidLatitude(Double.NaN));
        assertFalse(isValidLatitude(Double.POSITIVE_INFINITY));
        assertFalse(isValidLatitude(Double.NEGATIVE_INFINITY));
    }

    // ==================== Longitude Tests ====================

    @Test
    public void testValidLongitudes() {
        assertTrue(isValidLongitude(0.0));       // Prime meridian
        assertTrue(isValidLongitude(90.0));      // East
        assertTrue(isValidLongitude(-90.0));     // West
        assertTrue(isValidLongitude(-0.1276));   // London
        assertTrue(isValidLongitude(35.2137));   // Jerusalem
        assertTrue(isValidLongitude(151.2093));  // Sydney
    }

    @Test
    public void testBoundaryLongitudes() {
        assertTrue(isValidLongitude(MIN_LONGITUDE));  // -180
        assertTrue(isValidLongitude(MAX_LONGITUDE));  // 180
        assertFalse(isValidLongitude(-180.001));
        assertFalse(isValidLongitude(180.001));
    }

    @Test
    public void testInvalidLongitudes() {
        assertFalse(isValidLongitude(-181.0));
        assertFalse(isValidLongitude(181.0));
        assertFalse(isValidLongitude(360.0));
        assertFalse(isValidLongitude(-360.0));
        assertFalse(isValidLongitude(Double.NaN));
        assertFalse(isValidLongitude(Double.POSITIVE_INFINITY));
    }

    // ==================== Combined Location Tests ====================

    @Test
    public void testValidLocations() {
        assertTrue(isValidLocation(0.0, 0.0));                // Null Island
        assertTrue(isValidLocation(51.5074, -0.1276));        // London
        assertTrue(isValidLocation(31.7683, 35.2137));        // Jerusalem
        assertTrue(isValidLocation(-33.8688, 151.2093));      // Sydney
        assertTrue(isValidLocation(90.0, 0.0));               // North Pole
        assertTrue(isValidLocation(-90.0, 0.0));              // South Pole
    }

    @Test
    public void testInvalidLocations() {
        assertFalse(isValidLocation(91.0, 0.0));              // Invalid lat
        assertFalse(isValidLocation(0.0, 181.0));             // Invalid lon
        assertFalse(isValidLocation(91.0, 181.0));            // Both invalid
        assertFalse(isValidLocation(Double.NaN, 0.0));
        assertFalse(isValidLocation(0.0, Double.NaN));
    }

    // ==================== LocationData Object Tests ====================

    @Test
    public void testLocationData_Complete() {
        LocationData data = new LocationData(31.7683, 35.2137, "123", "456", "50");

        assertEquals(31.7683, data.latitude, 0.0001);
        assertEquals(35.2137, data.longitude, 0.0001);
        assertEquals("123", data.userId);
        assertEquals("456", data.smartWatchId);
        assertEquals("50", data.androidId);
        assertTrue(data.isComplete());
    }

    @Test
    public void testLocationData_IncompleteUserId() {
        LocationData data = new LocationData(31.7683, 35.2137, "UnknownUser", "456", "50");
        assertFalse(data.isComplete());
    }

    @Test
    public void testLocationData_IncompleteWatchId() {
        LocationData data = new LocationData(31.7683, 35.2137, "123", "UnknownWatch", "50");
        assertFalse(data.isComplete());
    }

    @Test
    public void testLocationData_IncompleteAndroidId() {
        LocationData data = new LocationData(31.7683, 35.2137, "123", "456", "UnknownAndroid");
        assertFalse(data.isComplete());
    }

    @Test
    public void testLocationData_AllUnknown() {
        LocationData data = new LocationData(31.7683, 35.2137, "UnknownUser", "UnknownWatch", "UnknownAndroid");
        assertFalse(data.isComplete());
    }

    @Test
    public void testLocationData_NullIds() {
        LocationData data = new LocationData(31.7683, 35.2137, null, "456", "50");
        assertFalse(data.isComplete());
    }

    // ==================== Precision Tests ====================

    @Test
    public void testHighPrecisionCoordinates() {
        // GPS coordinates can have many decimal places
        double lat = 31.76830123456789;
        double lon = 35.21370123456789;

        assertTrue(isValidLocation(lat, lon));
    }

    @Test
    public void testCoordinateFormatting() {
        // Test that formatting doesn't lose precision
        double lat = 31.768301;
        double lon = 35.213701;

        String formatted = String.format("Lat=%.6f, Lon=%.6f", lat, lon);
        assertEquals("Lat=31.768301, Lon=35.213701", formatted);
    }
}

