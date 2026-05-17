package com.example.smartwatchhapticsystem.controller;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

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
    private final String myIp = "https://marcella-unguerdoned-ayanna.ngrok-free.dev/webhook";
    private final String n8n_CONFIG_URL =  myIp + "/monitoring-config";
    private final BluetoothConnectionManager bluetoothConnectionManager;

    /**
     * Constructor: Initialize Retrofit and Volley
     */

    public NetworkController(Context context, BluetoothConnectionManager bluetoothManager) {
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
     * Sends location data to the n8n backend for unified routing.
     *
     * @param locationData The LocationData object containing coordinates and identifiers.
     * @param context      The Android context used for displaying toasts and logging.
     * @param monitoringType The type of monitoring currently active.
     */
    public void sendLocation(LocationData locationData, Context context, String monitoringType) {
        // Step 1: Validate IDs
        if ("UnknownUser".equals(locationData.getUserId()) ||
                "UnknownWatch".equals(locationData.getSmartWatchId()) ||
                "UnknownAndroid".equals(locationData.getAndroidId())) {

            Log.w("NetworkController", "⚠️ Data not sent. IDs are incomplete.");
            return;
        }

        // Step 2: Prepare JSON body for n8n routing script
        JsonObject body = new JsonObject();
        body.addProperty("userId", locationData.getUserId());
        body.addProperty("smartWatchId", locationData.getSmartWatchId());
        body.addProperty("androidId", locationData.getAndroidId());
        body.addProperty("type", monitoringType);
        body.addProperty("lat", locationData.getLat());
        body.addProperty("lon", locationData.getLon());
        body.addProperty("value", (String) null); // No sensor value for pure location updates

        postToN8n(body, context);
    }

    /**
     * Sends generic sensor data to n8n for routing.
     *
     * @param data Map containing sensor data and IDs.
     * @param context Android context for feedback.
     */
    public void sendSensorData(Map<String, String> data, Context context) {
        JsonObject body = new JsonObject();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            body.addProperty(entry.getKey(), entry.getValue());
        }

        // Ensure mandatory fields for routing script are mapped correctly if missing
        if (!body.has("userId") && data.containsKey("UserID")) body.addProperty("userId", data.get("UserID"));
        if (!body.has("smartWatchId") && data.containsKey("SmartWatchID")) body.addProperty("smartWatchId", data.get("SmartWatchID"));
        if (!body.has("androidId") && data.containsKey("AndroidID")) body.addProperty("androidId", data.get("AndroidID"));
        if (!body.has("type") && data.containsKey("sensorType")) body.addProperty("type", data.get("sensorType"));

        postToN8n(body, context);
    }

    private void postToN8n(JsonObject body, Context context) {
        Log.d("NetworkController", "📤 Sending to n8n: " + body.toString());

        api.postUseCaseRouting(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    handleN8nResponse(response.body());
                } else {
                    Log.e("NetworkController", "❌ Failed. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e("NetworkController", "❌ Network Error: " + t.getMessage());
            }
        });
    }

    private void handleN8nResponse(ResponseBody responseBody) {
        try {
            String responseString = responseBody != null ? responseBody.string() : "";
            if (responseString.isEmpty()) return;

            JsonParser parser = new JsonParser();
            JsonObject jsonResponse = parser.parse(responseString).getAsJsonObject();

            int pulses = jsonResponse.has("pulses") ? jsonResponse.get("pulses").getAsInt() : 0;
            int intensity = jsonResponse.has("intensity") ? jsonResponse.get("intensity").getAsInt() : 0;
            int duration = jsonResponse.has("duration") ? jsonResponse.get("duration").getAsInt() : 0;
            int interval = jsonResponse.has("interval") ? jsonResponse.get("interval").getAsInt() : 0;

            if (pulses > 0) {
                bluetoothConnectionManager.sendVibrationCommand(intensity, pulses, duration, interval);
            }
        } catch (Exception e) {
            Log.e("NetworkController", "❌ Error parsing: " + e.getMessage());
        }
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
