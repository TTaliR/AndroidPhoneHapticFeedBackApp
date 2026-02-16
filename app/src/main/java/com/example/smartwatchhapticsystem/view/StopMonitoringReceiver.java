package com.example.smartwatchhapticsystem.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver that handles the "End Monitoring" action from the notification.
 * When triggered, it stops the MonitoringService completely.
 */
public class StopMonitoringReceiver extends BroadcastReceiver {

    public static final String ACTION_STOP_MONITORING = "com.example.smartwatchhapticsystem.STOP_MONITORING";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_STOP_MONITORING.equals(intent.getAction())) {
            Log.d("StopMonitoringReceiver", "🛑 End Monitoring button pressed. Stopping service...");

            // Stop the MonitoringService
            Intent stopIntent = new Intent(context, MonitoringService.class);
            context.stopService(stopIntent);
        }
    }
}

