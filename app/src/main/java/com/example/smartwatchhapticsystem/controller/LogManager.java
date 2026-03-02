package com.example.smartwatchhapticsystem.controller;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * LogManager: Singleton utility for managing application logs and broadcasting status updates to UI.
 */
public class LogManager {

    // Broadcast actions
    public static final String ACTION_LOG = "com.example.smartwatchhapticsystem.ACTION_LOG";
    public static final String ACTION_STATUS_UPDATE = "com.example.smartwatchhapticsystem.ACTION_STATUS_UPDATE";

    // Intent extras
    public static final String EXTRA_LOG_MESSAGE = "log_message";
    public static final String EXTRA_STATUS_TYPE = "status_type";
    public static final String EXTRA_STATUS_VALUE = "status_value";
    public static final String EXTRA_STATUS_STATE = "status_state";

    // Status types
    public static final String STATUS_BLUETOOTH = "bluetooth";
    public static final String STATUS_n8n = "n8n";
    public static final String STATUS_LOCATION = "location";
    public static final String STATUS_MONITORING = "monitoring";

    // Status states
    public static final int STATE_CONNECTED = 1;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_PENDING = 2;

    private static LogManager instance;
    private Context applicationContext;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private LogManager() {}

    public static synchronized LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    public void init(Context context) {
        this.applicationContext = context.getApplicationContext();
    }

    /**
     * Logs a message and broadcasts it to the UI
     */
    public void log(String tag, String message) {
        if (applicationContext == null) return;

        String timestamp = timeFormat.format(new Date());
        String formattedLog = String.format("[%s] %s: %s", timestamp, tag, message);

        // Also log to Android logcat
        android.util.Log.d(tag, message);

        // Broadcast to UI
        Intent intent = new Intent(ACTION_LOG);
        intent.putExtra(EXTRA_LOG_MESSAGE, formattedLog);
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent);
    }

    /**
     * Updates a status indicator and broadcasts the change to UI
     */
    public void updateStatus(String statusType, String statusValue, int state) {
        if (applicationContext == null) return;

        Intent intent = new Intent(ACTION_STATUS_UPDATE);
        intent.putExtra(EXTRA_STATUS_TYPE, statusType);
        intent.putExtra(EXTRA_STATUS_VALUE, statusValue);
        intent.putExtra(EXTRA_STATUS_STATE, state);
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent);

        // Also log the status change
        String stateStr = state == STATE_CONNECTED ? "✅" : (state == STATE_PENDING ? "⏳" : "❌");
        log("Status", String.format("%s %s: %s", stateStr, statusType, statusValue));
    }

    /**
     * Convenience methods for common status updates
     */
    public void setBluetoothConnected(String deviceName) {
        updateStatus(STATUS_BLUETOOTH, "Connected to " + deviceName, STATE_CONNECTED);
    }

    public void setBluetoothDisconnected() {
        updateStatus(STATUS_BLUETOOTH, "Disconnected", STATE_DISCONNECTED);
    }

    public void setBluetoothConnecting() {
        updateStatus(STATUS_BLUETOOTH, "Connecting…", STATE_PENDING);
    }

    public void setn8nConnected() {
        updateStatus(STATUS_n8n, "Connected", STATE_CONNECTED);
    }

    public void setn8nDisconnected() {
        updateStatus(STATUS_n8n, "Disconnected", STATE_DISCONNECTED);
    }

    public void setn8nConnecting() {
        updateStatus(STATUS_n8n, "Connecting…", STATE_PENDING);
    }

    public void setLocationActive() {
        updateStatus(STATUS_LOCATION, "Active", STATE_CONNECTED);
    }

    public void setLocationInactive() {
        updateStatus(STATUS_LOCATION, "Inactive", STATE_DISCONNECTED);
    }

    public void setMonitoringType(String type) {
        updateStatus(STATUS_MONITORING, type, STATE_CONNECTED);
    }

    public void setMonitoringLoading() {
        updateStatus(STATUS_MONITORING, "Loading…", STATE_PENDING);
    }

    public void setMonitoringError(String error) {
        updateStatus(STATUS_MONITORING, error, STATE_DISCONNECTED);
    }
}

