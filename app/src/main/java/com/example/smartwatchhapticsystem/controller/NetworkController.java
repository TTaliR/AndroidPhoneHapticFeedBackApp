package com.example.smartwatchhapticsystem.controller;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartwatchhapticsystem.model.LocationData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.ResponseBody;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * NetworkController: Handles communication with n8n
 */
public class NetworkController {
    private final n8nApis api;
    private final RequestQueue requestQueue;
    private final String myIp = "https://entomb-imprudent-diffusion.ngrok-free.dev/webhook";
    private final String n8n_CONFIG_URL =  myIp + "/monitoring-config";
    private final String n8n_POST_URL = myIp + "/heartRate";
    private final BluetoothConnectionManager bluetoothConnectionManager;
    private Context context;

    /**
     * Constructor: Initialize Retrofit and Volley
     */

    public NetworkController(Context context, BluetoothConnectionManager bluetoothManager) {
        this.context = context;
        this.bluetoothConnectionManager = bluetoothManager;

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(myIp + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(n8nApis.class);

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(context);

        // Initialize Bluetooth Manager for Watch Communication
    }


    /**
     * Makes an HTTP GET request to the n8n backend to fetch the monitoring type for a specific user.
     * The request includes the userId as a query parameter to allow for user-specific configurations.
     *
     * @param userId   The parsed ID of the user (e.g., from the smartwatch identity).
     * @param listener A callback interface to receive either the valid monitoring type or an error.
     */
    public void getMonitoringType(int userId, OnMonitoringTypeReceived listener) {

        // Step 1: Append the userId as a query parameter to the URL
        // Your n8n workflow extracts this using: req.query?.userId
        String urlWithParams = n8n_CONFIG_URL + "?userId=" + userId;

        // Step 2: Create the GET request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                urlWithParams,
                null, // No body needed for GET
                response -> {
                    try {
                        // Log the full response for debugging
                        Log.d("NetworkController", "✅ Response for User " + userId + ": " + response.toString());

                        // Step 3: Check the "success" field from your n8n "Format monitoring config response" node
                        boolean success = response.optBoolean("success", false);
                        String monitoringType = response.optString("monitoringType", "Unknown");

                        if (success && !monitoringType.equals("Unknown")) {
                            // Success path: notify the listener with the assigned use case
                            listener.onReceived(monitoringType);
                        } else {
                            // Error path: extract the error message from the n8n response if available
                            String errorMsg = response.optString("error", "No configuration found for this user");
                            listener.onError("❌ Server Error: " + errorMsg);
                        }

                    } catch (Exception e) {
                        listener.onError("❌ JSON Parsing Error: " + e.getMessage());
                    }
                },
                error -> {
                    listener.onError("❌ Network Error: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e("NetworkController", "❌ HTTP Status Code: " + error.networkResponse.statusCode);
                    }
                }
        );

        // Step 4: Configure retry policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        // Step 5: Execute the request
        requestQueue.add(jsonObjectRequest);
    }


    /**
     * Sends location data (latitude, longitude, and device/user IDs) to the n8n backend
     * for SunAzimuth monitoring. If valid, triggers vibration feedback based on server response.
     *
     * @param locationData The LocationData object containing coordinates and identifiers.
     * @param context      The Android context used for displaying toasts and logging.
     */
    public void sendLocation(LocationData locationData, Context context, String monitoringType) {
        // Step 1: Validate that all required IDs are present
        if ("UnknownUser".equals(locationData.getUserId()) ||
                "UnknownWatch".equals(locationData.getSmartWatchId()) ||
                "UnknownAndroid".equals(locationData.getAndroidId())) {

            Log.w("NetworkController", "⚠️ Location not sent. One or more IDs are unknown: " +
                    "UserID=" + locationData.getUserId() +
                    ", SmartWatchID=" + locationData.getSmartWatchId() +
                    ", AndroidID=" + locationData.getAndroidId());

            Toast.makeText(context, "⚠️ Cannot send location. IDs are incomplete.", Toast.LENGTH_LONG).show();
            return;
        }

        // Step 2: Send location data based on monitoring type
        Call<ResponseBody> call = null;
        if ("SunAzimuth".equals(monitoringType)) {
            call = api.sendSunLocation(locationData);
        } else if ("MoonAzimuth".equals(monitoringType)) {
            call = api.sendMoonLocation(locationData);
        } else if ("Pollution".equals(monitoringType)) {
            call = api.sendPollutionLocation(locationData);
        } else if ("Temperature".equals(monitoringType)) {
            call = api.sendTemperatureLocation(locationData);
        }

        if (call == null) {
            Log.e("NetworkController", "❌ Invalid monitoring type: " + monitoringType);
            Toast.makeText(context, "Invalid monitoring type.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Step 3: Enqueue the Retrofit call
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseString = response.body() != null ? response.body().string() : "";
                        Log.d("NetworkController", "✅ Location Sent. Raw Response: " + responseString);

                        // Handle empty response
                        if (responseString == null || responseString.trim().isEmpty()) {
                            Log.d("NetworkController", "ℹ️ Server returned empty response.");
                            Toast.makeText(context, "Location Sent (no feedback data).", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Parse JSON response
                        JsonParser parser = new JsonParser();
                        JsonObject jsonResponse = parser.parse(responseString).getAsJsonObject();

                        String message = jsonResponse.has("message") && !jsonResponse.get("message").isJsonNull()
                                ? jsonResponse.get("message").getAsString()
                                : "No message in response.";

                        int pulses = jsonResponse.has("pulses") ? jsonResponse.get("pulses").getAsInt() : 0;
                        int intensity = jsonResponse.has("intensity") ? jsonResponse.get("intensity").getAsInt() : 0;
                        int duration = jsonResponse.has("duration") ? jsonResponse.get("duration").getAsInt() : 0;
                        int interval = jsonResponse.has("interval") ? jsonResponse.get("interval").getAsInt() : 0;

                        Log.d("NetworkController", "📲 Vibration Parameters: " +
                                "Pulses=" + pulses + ", Intensity=" + intensity +
                                ", Duration=" + duration + ", Interval=" + interval);

                        Toast.makeText(context, "Location Sent: " + message, Toast.LENGTH_SHORT).show();

                        if (pulses > 0) {
                            bluetoothConnectionManager.sendVibrationCommand(intensity, pulses, duration, interval);
                        } else {
                            Log.d("NetworkController", "ℹ️ No vibration needed (pulses=0).");
                        }

                    } catch (Exception e) {
                        Log.e("NetworkController", "❌ Error parsing response: " + e.getMessage());
                        Toast.makeText(context, "Location sent, but failed to parse response.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("NetworkController", "❌ Failed to send location. Response Code: " + response.code());
                    Toast.makeText(context, "Failed to send location.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e("NetworkController", "❌ Network Error: " + t.getMessage());
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    /**
     * Sends a map of heart rate data to a n8n server for processing.
     * Receives feedback parameters (vibration settings) from the server and triggers a vibration
     * command via Bluetooth if the response is valid.
     *
     * @param data A map containing heart rate data (e.g., value, user ID, watch ID, android ID).
     */
    public void sendHeartRateTon8n(Map<String, String> data) {
        // Step 1: Ensure the request queue has been initialized
        if (requestQueue == null) {
            Log.e("NetworkController", "❌ RequestQueue is not initialized!");
            return;
        }

        // Step 2: Convert the heart rate data (Map) into a JSON object for POST body
        JSONObject jsonBody = new JSONObject(data);
        Log.d("NetworkController", "📤 Sending to n8n: " + jsonBody.toString());

        // Step 3: Prepare a JsonObjectRequest to send the data to n8n via HTTP POST
        final JsonObjectRequest[] jsonObjectRequest = new JsonObjectRequest[1]; // Use array to allow inner class reuse

        jsonObjectRequest[0] = new JsonObjectRequest(
                Request.Method.POST,           // HTTP method: POST
                n8n_POST_URL,             // URL to send heart rate data to
                jsonBody,                      // JSON body to send
                response -> {  // Success callback
                    Log.d("NetworkController", "✅ Response from n8n: " + response.toString());

                    // Step 4: Extract vibration feedback parameters from the JSON response
                    int intensity = response.optInt("intensity", 0);
                    int pulses = response.optInt("pulses", 0);
                    int duration = response.optInt("duration", 0);
                    int interval = response.optInt("interval", 0);

                    // Step 5: Trigger the smartwatch to vibrate if connection manager is available
                    if (bluetoothConnectionManager != null) {
                        bluetoothConnectionManager.sendVibrationCommand(intensity, pulses, duration, interval);
                    } else {
                        Log.e("NetworkController", "❌ BluetoothConnectionManager is null!");
                    }
                },
                error -> {  // Error callback
                    Log.e("NetworkController", "❌ Error sending to n8n: " + error.toString());

                    // Log additional HTTP status if available
                    if (error.networkResponse != null) {
                        Log.e("NetworkController", "❌ HTTP Status Code: " + error.networkResponse.statusCode);
                    }

                    // Retry the request (custom retry method)
                    retryRequest(jsonObjectRequest[0]);
                }
        ) {
            // Step 6: Add custom HTTP headers (e.g., content type)
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Step 7: Set retry policy for network reliability
        jsonObjectRequest[0].setRetryPolicy(new DefaultRetryPolicy(
                5000,                                       // Timeout in ms
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,    // Max retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT    // Backoff multiplier
        ));

        // Step 8: Add the request to the Volley queue to send it
        requestQueue.add(jsonObjectRequest[0]);
    }




    /**
     * Helper method to retry request after a delay
     */
    private void retryRequest(final JsonObjectRequest request) {
        Log.d("NetworkController", "🔄 Retrying request in 3 seconds...");
        new Handler().postDelayed(() -> requestQueue.add(request), 3000);
    }


    /**
     * Listener Interface for Monitoring Type
     */
    public interface OnMonitoringTypeReceived {
        void onReceived(String monitoringType);
        void onError(String errorMessage);
    }

    /**
     * Listener Interface for Use Cases
     */
    public interface OnUseCasesReceived {
        void onReceived(List<String> useCases);
        void onError(String errorMessage);
    }

    /**
     * Fetches the list of available use cases from n8n backend.
     * Parses the JSON array response and extracts the "name" field from each use case object.
     *
     * @param listener A callback interface to receive either the list of use case names or an error message.
     */
    public void getUseCases(OnUseCasesReceived listener) {
        Call<JsonArray> call = api.getUseCases();

        call.enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(@NonNull Call<JsonArray> call, @NonNull retrofit2.Response<JsonArray> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonArray jsonArray = response.body();
                    List<String> useCaseNames = new ArrayList<>();

                    // Extract the "name" field from each use case object
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject useCase = jsonArray.get(i).getAsJsonObject();
                        if (useCase.has("name") && !useCase.get("name").isJsonNull()) {
                            useCaseNames.add(useCase.get("name").getAsString());
                        }
                    }

                    Log.d("NetworkController", "✅ Use cases received: " + useCaseNames.toString());
                    listener.onReceived(useCaseNames);
                } else {
                    Log.e("NetworkController", "❌ Failed to fetch use cases. Response Code: " + response.code());
                    listener.onError("Failed to fetch use cases. Response Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonArray> call, @NonNull Throwable t) {
                Log.e("NetworkController", "❌ Network Error fetching use cases: " + t.getMessage());
                listener.onError("Network Error: " + t.getMessage());
            }
        });
    }
}
