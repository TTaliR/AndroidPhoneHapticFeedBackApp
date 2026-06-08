package com.example.smartwatchhapticsystem.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * BroadcastReceiver that listens for device boot events.
 * If monitoring was active before the device was shut down,
 * it automatically restarts the MonitoringService.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String SHARED_PREFS_NAME = "monitoring_state";
    private static final String IS_MONITORING_KEY = "is_monitoring";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "📱 Device booted. Checking monitoring state...");

            // Check if monitoring was active before shutdown
            if (isMonitoringActive(context)) {
                Log.d("BootReceiver", "✅ Monitoring was active. Restarting MonitoringService...");
                startMonitoringService(context);
            } else {
                Log.d("BootReceiver", "⚠️ Monitoring was not active. Not restarting.");
            }
        }
    }

    /**
     * Checks if monitoring was active before the device was shut down.
     */
    private boolean isMonitoringActive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(IS_MONITORING_KEY, false);
    }

    /**
     * Starts the MonitoringService after device boot.
     */
    private void startMonitoringService(Context context) {
        Intent serviceIntent = new Intent(context, MonitoringService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    /**
     * Static helper method to set the monitoring state.
     * Call this from MonitoringService when it starts/stops.
     */
    public static void setMonitoringState(Context context, boolean isActive) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_MONITORING_KEY, isActive);
        editor.apply();
        Log.d("BootReceiver", "📝 Monitoring state saved: " + isActive);
    }

    /**
     * Static helper method to get the monitoring state.
     */
    public static boolean getMonitoringState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(IS_MONITORING_KEY, false);
    }
}

