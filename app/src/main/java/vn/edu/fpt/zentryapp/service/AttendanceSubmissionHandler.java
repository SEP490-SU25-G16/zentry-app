package vn.edu.fpt.zentryapp.service;

import android.content.Context;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;

public class AttendanceSubmissionHandler {
    private static final String TAG = "AttendanceSubmissionHandler";
    private final AttendanceApiService apiService;
    private final Context context;

    public AttendanceSubmissionHandler(Context context) {
        Log.d(TAG, "=== INITIALIZING ATTENDANCE SUBMISSION HANDLER ===");
        this.context = context;

        try {
            this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);
            Log.d(TAG, "✅ ApiService created successfully");
            Log.d(TAG, "Context: " + context.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create ApiService", e);
            throw new RuntimeException("Failed to initialize AttendanceSubmissionHandler", e);
        }

        Log.d(TAG, "AttendanceSubmissionHandler initialized successfully");
        Log.d(TAG, "================================================");
    }

    public void submitAttendance(AttendanceModels.AttendanceSubmission submission,
                                 AttendanceCallbacks.AttendanceSubmissionCallback callback) {

        Log.i(TAG, "=== ATTENDANCE SUBMISSION START ===");
        Log.i(TAG, "Timestamp: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",
                java.util.Locale.getDefault()).format(new java.util.Date()));

        // 🔍 VALIDATE INPUT DATA
        if (submission == null) {
            Log.e(TAG, "❌ FATAL: Submission is NULL");
            callback.onSubmissionFailure(0, "Submission data is null");
            return;
        }

        if (callback == null) {
            Log.e(TAG, "❌ FATAL: Callback is NULL");
            return;
        }

        // 🔍 LOG SUBMISSION DETAILS
        Log.i(TAG, "📋 SUBMISSION DETAILS:");
        Log.i(TAG, "  Submitter Device MAC: '" + submission.getSubmitterDeviceMacAddress() + "'");
        Log.i(TAG, "  Session ID: '" + submission.getSessionId() + "'");
        Log.i(TAG, "  Submission Timestamp: '" + submission.getTimestamp() + "'");
        Log.i(TAG, "  Scanned Devices Count: " + submission.getScannedDevices().size());

        // 🔍 VALIDATE REQUIRED FIELDS
        validateSubmissionData(submission);

        // 🔍 LOG CHI TIẾT TỪNG THIẾT BỊ ĐƯỢC SCAN
        logScannedDevices(submission);

        // 🔍 LOG JSON PAYLOAD
        logJsonPayload(submission);

        // 🔍 LOG API CALL INFO
        Log.d(TAG, "🌐 API CALL INFORMATION:");
        Log.d(TAG, "  Endpoint: POST /api/attendance/sessions/scan");
        Log.d(TAG, "  ApiService: " + (apiService != null ? "Ready" : "NULL"));
        Log.d(TAG, "  Context: " + context.getClass().getSimpleName());

        // 🔧 CALL API
        try {
            Log.d(TAG, "🚀 Initiating API call...");
            Call<AttendanceApiResponse> call = apiService.submitAttendanceScan(submission);

            if (call == null) {
                Log.e(TAG, "❌ FATAL: API call is NULL");
                callback.onSubmissionFailure(0, "Failed to create API call");
                return;
            }

            Log.d(TAG, "📞 API call created successfully");
            Log.d(TAG, "📞 Call URL: " + call.request().url());
            Log.d(TAG, "📞 Call method: " + call.request().method());

            call.enqueue(new Callback<AttendanceApiResponse>() {
                @Override
                public void onResponse(Call<AttendanceApiResponse> call, Response<AttendanceApiResponse> response) {
                    handleApiResponse(call, response, submission, callback);
                }

                @Override
                public void onFailure(Call<AttendanceApiResponse> call, Throwable t) {
                    handleApiFailure(call, t, submission, callback);
                }
            });

            Log.d(TAG, "📞 API call enqueued successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION during API call setup", e);
            callback.onSubmissionFailure(0, "Exception: " + e.getMessage());
        }
    }

    private void validateSubmissionData(AttendanceModels.AttendanceSubmission submission) {
        Log.d(TAG, "🔍 VALIDATING SUBMISSION DATA:");

        // Check MAC address
        String mac = submission.getSubmitterDeviceMacAddress();
        if (mac == null || mac.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Submitter MAC is null or empty");
        } else if (!isValidMacAddress(mac)) {
            Log.w(TAG, "⚠️ Submitter MAC format might be invalid: " + mac);
        } else {
            Log.d(TAG, "✅ Submitter MAC is valid");
        }

        // Check Session ID
        String sessionId = submission.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Session ID is null or empty");
        } else {
            Log.d(TAG, "✅ Session ID is valid: " + sessionId.length() + " characters");
        }

        // Check timestamp
        String timestamp = submission.getTimestamp();
        if (timestamp == null || timestamp.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Timestamp is null or empty");
        } else {
            Log.d(TAG, "✅ Timestamp is valid: " + timestamp);
        }

        // Check scanned devices
        if (submission.getScannedDevices() == null) {
            Log.w(TAG, "⚠️ Scanned devices list is null");
        } else if (submission.getScannedDevices().isEmpty()) {
            Log.w(TAG, "⚠️ No devices were scanned");
        } else {
            Log.d(TAG, "✅ Scanned devices list is valid");
        }
    }

    private boolean isValidMacAddress(String mac) {
        // Basic MAC address pattern check
        return mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }

    private void logScannedDevices(AttendanceModels.AttendanceSubmission submission) {
        Log.d(TAG, "📱 SCANNED DEVICES DETAILS:");

        if (submission.getScannedDevices() == null) {
            Log.w(TAG, "  Scanned devices list is NULL");
            return;
        }

        if (submission.getScannedDevices().isEmpty()) {
            Log.w(TAG, "  No devices in scanned list");
            return;
        }

        Log.d(TAG, "  Total devices: " + submission.getScannedDevices().size());

        for (int i = 0; i < submission.getScannedDevices().size(); i++) {
            AttendanceModels.ScannedDevice device = submission.getScannedDevices().get(i);

            if (device == null) {
                Log.w(TAG, "  Device[" + i + "]: NULL");
                continue;
            }

            Log.d(TAG, "  Device[" + i + "]:");
            Log.d(TAG, "    MAC: '" + device.getMacAddress() + "'");
            Log.d(TAG, "    RSSI: " + device.getRssi() + " dBm");
            Log.d(TAG, "    Signal strength: " + getSignalStrengthDescription(device.getRssi()));

            // Validate device data
            if (device.getMacAddress() == null || device.getMacAddress().trim().isEmpty()) {
                Log.w(TAG, "    ⚠️ Device MAC is null or empty");
            } else if (!isValidMacAddress(device.getMacAddress())) {
                Log.w(TAG, "    ⚠️ Device MAC format might be invalid");
            }
        }
    }

    private String getSignalStrengthDescription(int rssi) {
        if (rssi > -50) return "Excellent";
        if (rssi > -60) return "Good";
        if (rssi > -70) return "Fair";
        if (rssi > -80) return "Weak";
        return "Very Weak";
    }

    private void logJsonPayload(AttendanceModels.AttendanceSubmission submission) {
        Log.d(TAG, "📄 JSON PAYLOAD:");

        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            String jsonPayload = gson.toJson(submission);

            Log.d(TAG, "  Payload size: " + jsonPayload.length() + " characters");
            Log.d(TAG, "  JSON content:");

            // Split long JSON into multiple log lines for readability
            String[] lines = jsonPayload.split("\n");
            for (String line : lines) {
                Log.d(TAG, "    " + line);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to serialize submission to JSON", e);
            Log.e(TAG, "  Exception type: " + e.getClass().getSimpleName());
            Log.e(TAG, "  Exception message: " + e.getMessage());
        }
    }

    private void handleApiResponse(Call<AttendanceApiResponse> call,
                                   Response<AttendanceApiResponse> response,
                                   AttendanceModels.AttendanceSubmission submission,
                                   AttendanceCallbacks.AttendanceSubmissionCallback callback) {
        // Log response headers
        if (response.headers() != null) {
            Log.d(TAG, "  Response headers count: " + response.headers().size());
            for (String name : response.headers().names()) {
                Log.d(TAG, "    " + name + ": " + response.headers().get(name));
            }
        }

        if (response.isSuccessful()) {
            handleSuccessfulResponse(response, submission, callback);
        } else {
            handleErrorResponse(response, callback);
        }
    }

    private void handleSuccessfulResponse(Response<AttendanceApiResponse> response,
                                          AttendanceModels.AttendanceSubmission submission,
                                          AttendanceCallbacks.AttendanceSubmissionCallback callback) {

        Log.d(TAG, "✅ HTTP RESPONSE SUCCESSFUL");

        if (response.body() == null) {
            Log.e(TAG, "❌ Response body is NULL despite successful HTTP status");
            callback.onSubmissionFailure(0, "Empty response body");
            logSubmissionEnd(false);
            return;
        }

        AttendanceApiResponse apiResponse = response.body();
        Log.d(TAG, "📋 API RESPONSE DETAILS:");
        Log.d(TAG, "  Success flag: " + apiResponse.isSuccess());
        Log.d(TAG, "  Message: '" + apiResponse.getMessage() + "'");
        Log.d(TAG, "  Error: '" + apiResponse.getError() + "'");

        // Log response data if available
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String responseJson = gson.toJson(apiResponse);
            Log.d(TAG, "  Full response JSON: " + responseJson);
        } catch (Exception e) {
            Log.w(TAG, "  Could not serialize response to JSON", e);
        }

        if (apiResponse.isSuccess()) {
            Log.d(TAG, "✅ API OPERATION SUCCESSFUL");
            Log.d(TAG, "  API Message: " + apiResponse.getMessage());
            Log.d(TAG, "  Submitted session: " + submission.getSessionId());
            Log.d(TAG, "  Submitted devices: " + submission.getScannedDevices().size());

            logSubmissionEnd(true);
            callback.onSubmissionSuccess(submission);
        } else {
            Log.e(TAG, "❌ API OPERATION FAILED");
            Log.e(TAG, "  API Error: " + apiResponse.getError());

            logSubmissionEnd(false);
            callback.onSubmissionFailure(0, "API Error: " + apiResponse.getError());
        }
    }

    private void handleErrorResponse(Response<AttendanceApiResponse> response,
                                     AttendanceCallbacks.AttendanceSubmissionCallback callback) {

        Log.e(TAG, "❌ HTTP ERROR RESPONSE");
        Log.e(TAG, "  Status code: " + response.code());
        Log.e(TAG, "  Status message: '" + response.message() + "'");

        // Try to log error body if available
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                Log.e(TAG, "  Error body: " + errorBody);
            } else {
                Log.e(TAG, "  Error body: NULL");
            }
        } catch (Exception e) {
            Log.e(TAG, "  Could not read error body", e);
        }

        String errorMessage = "HTTP Error: " + response.code();
        if (response.message() != null && !response.message().isEmpty()) {
            errorMessage += " - " + response.message();
        }

        logSubmissionEnd(false);
        callback.onSubmissionFailure(0, errorMessage);
    }

    private void handleApiFailure(Call<AttendanceApiResponse> call,
                                  Throwable t,
                                  AttendanceModels.AttendanceSubmission submission,
                                  AttendanceCallbacks.AttendanceSubmissionCallback callback) {

        Log.e(TAG, "❌ API CALL FAILURE");
        Log.e(TAG, "  Exception type: " + t.getClass().getSimpleName());
        Log.e(TAG, "  Exception message: " + t.getMessage());
        Log.e(TAG, "  Call URL: " + (call != null && call.request() != null ? call.request().url() : "NULL"));
        Log.e(TAG, "  Submitted session: " + submission.getSessionId());
        Log.e(TAG, "  Submitted devices: " + submission.getScannedDevices().size());

        // Log full stack trace for debugging
        Log.e(TAG, "  Full exception:", t);

        // Categorize the error
        String errorCategory = categorizeNetworkError(t);
        Log.e(TAG, "  Error category: " + errorCategory);

        String errorMessage = "Network Error (" + errorCategory + "): " + t.getMessage();

        logSubmissionEnd(false);
        callback.onSubmissionFailure(0, errorMessage);
    }

    private String categorizeNetworkError(Throwable t) {
        String className = t.getClass().getSimpleName();

        if (className.contains("UnknownHost")) {
            return "DNS/Host Resolution";
        } else if (className.contains("ConnectException")) {
            return "Connection Failed";
        } else if (className.contains("SocketTimeout")) {
            return "Request Timeout";
        } else if (className.contains("SSLException")) {
            return "SSL/Security";
        } else if (className.contains("IOException")) {
            return "Network I/O";
        } else {
            return "Unknown";
        }
    }

    private void logSubmissionEnd(boolean success) {
        Log.i(TAG, "=== ATTENDANCE SUBMISSION " + (success ? "SUCCESS" : "FAILED") + " ===");
        Log.i(TAG, "End timestamp: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",
                java.util.Locale.getDefault()).format(new java.util.Date()));
        Log.i(TAG, "==============================================");
    }
}
