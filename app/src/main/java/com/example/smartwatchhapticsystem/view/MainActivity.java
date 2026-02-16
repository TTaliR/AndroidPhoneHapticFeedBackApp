package com.example.smartwatchhapticsystem.view;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.smartwatchhapticsystem.R;
import com.example.smartwatchhapticsystem.controller.LogManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 101;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 102;

    // UI Elements - Time
    private TextView tvCurrentTime;
    private TextView tvCurrentDate;

    // UI Elements - Status
    private View bluetoothStatusIndicator;
    private TextView tvBluetoothStatus;
    private View n8nStatusIndicator;
    private TextView tvn8nStatus;
    private View locationStatusIndicator;
    private TextView tvLocationStatus;
    private View monitoringStatusIndicator;
    private TextView tvMonitoringType;

    // UI Elements - Logs
    private ScrollView logsScrollView;
    private TextView tvLogs;
    private Button btnClearLogs;
    private Button btnRetryConnection;

    // Handlers and formatters
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());

    // StringBuilder for logs
    private final StringBuilder logsBuilder = new StringBuilder();
    private static final int MAX_LOG_LINES = 100;

    // Broadcast receiver for logs and status updates
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case LogManager.ACTION_LOG:
                    String logMessage = intent.getStringExtra(LogManager.EXTRA_LOG_MESSAGE);
                    if (logMessage != null) {
                        appendLog(logMessage);
                    }
                    break;

                case LogManager.ACTION_STATUS_UPDATE:
                    String statusType = intent.getStringExtra(LogManager.EXTRA_STATUS_TYPE);
                    String statusValue = intent.getStringExtra(LogManager.EXTRA_STATUS_VALUE);
                    int state = intent.getIntExtra(LogManager.EXTRA_STATUS_STATE, LogManager.STATE_DISCONNECTED);
                    updateStatusUI(statusType, statusValue, state);
                    break;
            }
        }
    };

    // Time update runnable
    private final Runnable timeUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateTime();
            timeHandler.postDelayed(this, 1000); // Update every second
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize LogManager
        LogManager.getInstance().init(this);

        // Initialize UI
        initializeViews();
        setupClickListeners();

        // Start time updates
        timeHandler.post(timeUpdateRunnable);

        // Register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(LogManager.ACTION_LOG);
        filter.addAction(LogManager.ACTION_STATUS_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(statusReceiver, filter);

        // Add initial log entry
        LogManager.getInstance().log("App", "Application started");

        // Request permissions
        requestNotificationPermission();
    }

    private void initializeViews() {
        // Time views
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);

        // Bluetooth status
        bluetoothStatusIndicator = findViewById(R.id.bluetoothStatusIndicator);
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);

        // n8n status
        n8nStatusIndicator = findViewById(R.id.n8nStatusIndicator);
        tvn8nStatus = findViewById(R.id.tvn8nStatus);

        // Location status
        locationStatusIndicator = findViewById(R.id.locationStatusIndicator);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);

        // Monitoring type
        monitoringStatusIndicator = findViewById(R.id.monitoringStatusIndicator);
        tvMonitoringType = findViewById(R.id.tvMonitoringType);

        // Logs
        logsScrollView = findViewById(R.id.logsScrollView);
        tvLogs = findViewById(R.id.tvLogs);
        btnClearLogs = findViewById(R.id.btnClearLogs);
        btnRetryConnection = findViewById(R.id.btnRetryConnection);
    }

    private void setupClickListeners() {
        btnClearLogs.setOnClickListener(v -> clearLogs());
        btnRetryConnection.setOnClickListener(v -> retryConnection());
    }

    private void updateTime() {
        Date now = new Date();
        tvCurrentTime.setText(timeFormat.format(now));
        tvCurrentDate.setText(dateFormat.format(now));
    }

    private void appendLog(String message) {
        logsBuilder.append(message).append("\n");

        // Limit log lines
        String[] lines = logsBuilder.toString().split("\n");
        if (lines.length > MAX_LOG_LINES) {
            logsBuilder.setLength(0);
            for (int i = lines.length - MAX_LOG_LINES; i < lines.length; i++) {
                logsBuilder.append(lines[i]).append("\n");
            }
        }

        tvLogs.setText(logsBuilder.toString());

        // Auto-scroll to bottom
        logsScrollView.post(() -> logsScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void clearLogs() {
        logsBuilder.setLength(0);
        tvLogs.setText(R.string.logs_placeholder);
        LogManager.getInstance().log("App", "Logs cleared");
    }

    private void retryConnection() {
        LogManager.getInstance().log("App", "Retrying connection...");

        // Stop the existing service
        Intent stopIntent = new Intent(this, MonitoringService.class);
        stopService(stopIntent);

        // Restart the service after a brief delay to ensure clean shutdown
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent startIntent = new Intent(this, MonitoringService.class);
            startForegroundService(startIntent);
            LogManager.getInstance().log("App", "Service restarted");
        }, 500);
    }

    private void updateStatusUI(String statusType, String statusValue, int state) {
        if (statusType == null) return;

        int colorRes;
        switch (state) {
            case LogManager.STATE_CONNECTED:
                colorRes = R.color.status_connected;
                break;
            case LogManager.STATE_PENDING:
                colorRes = R.color.status_pending;
                break;
            default:
                colorRes = R.color.status_disconnected;
                break;
        }

        int color = ContextCompat.getColor(this, colorRes);

        switch (statusType) {
            case LogManager.STATUS_BLUETOOTH:
                bluetoothStatusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                tvBluetoothStatus.setText(statusValue);
                tvBluetoothStatus.setTextColor(color);
                break;

            case LogManager.STATUS_n8n:
                n8nStatusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                tvn8nStatus.setText(statusValue);
                tvn8nStatus.setTextColor(color);
                break;

            case LogManager.STATUS_LOCATION:
                locationStatusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                tvLocationStatus.setText(statusValue);
                tvLocationStatus.setTextColor(color);
                break;

            case LogManager.STATUS_MONITORING:
                monitoringStatusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                tvMonitoringType.setText(statusValue);
                tvMonitoringType.setTextColor(color);
                break;
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                // Already granted → next
                requestLocationPermissions();
            }
        } else {
            // Not required below Android 13 → skip to next
            requestLocationPermissions();
        }
    }


    /**
     * **Check and Request Bluetooth Permissions**
     */
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        BLUETOOTH_PERMISSION_REQUEST_CODE);
            } else {
                onAllPermissionsGranted(); // All done
            }
        } else {
            onAllPermissionsGranted(); // Not required before Android 12
        }
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            requestBluetoothPermissions(); // Already granted → next
        }
    }

    private void onAllPermissionsGranted() {
        Log.d("Permissions", "✅ All permissions granted or handled. Starting MonitoringService...");
        LogManager.getInstance().log("Permissions", "All permissions granted");

        Intent serviceIntent = new Intent(this, MonitoringService.class);
        startForegroundService(serviceIntent);  // Required for Android 8+
    }

    /**
     * **Handle Permission Requests**
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            // Continue regardless of result
            requestLocationPermissions();

        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            requestBluetoothPermissions();

        } else if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            onAllPermissionsGranted(); // Done
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume time updates
        timeHandler.post(timeUpdateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause time updates when activity is not visible
        timeHandler.removeCallbacks(timeUpdateRunnable);
    }

    /**
     * **Clean up resources on activity destruction**
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeHandler.removeCallbacks(timeUpdateRunnable);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
    }
}
