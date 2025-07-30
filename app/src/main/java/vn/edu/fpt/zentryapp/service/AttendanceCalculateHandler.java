package vn.edu.fpt.zentryapp.service;

import android.content.Context;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CalculateAttendanceData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CalculateAttendanceResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AttendanceCalculateHandler {
    private static final String TAG = "AttendanceCalculateHandler";

    private final AttendanceApiService apiService;
    private final Context context;
    private final SimpleDateFormat timeFormat;

    public AttendanceCalculateHandler(Context context) {
        Log.d(TAG, "=== INITIALIZING ATTENDANCE CALCULATE HANDLER ===");

        this.context = context;
        this.timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

        if (context == null) {
            Log.e(TAG, "❌ FATAL: Context is NULL");
            throw new IllegalArgumentException("Context cannot be null");
        }

        Log.d(TAG, "Context: " + context.getClass().getSimpleName());

        try {
            this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);
            Log.d(TAG, "✅ ApiService created successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create ApiService", e);
            throw new RuntimeException("Failed to initialize AttendanceCalculateHandler", e);
        }

        Log.d(TAG, "AttendanceCalculateHandler initialized successfully");
        Log.d(TAG, "===============================================");
    }

    /**
     * 🔧 CALCULATE attendance cho một round
     */
    public void calculateRoundAttendance(String sessionId, String roundId,
                                         AttendanceCallbacks.CalculateAttendanceCallback callback) {

        Log.i(TAG, "=== ATTENDANCE CALCULATION START ===");
        Log.i(TAG, "Start timestamp: " + timeFormat.format(new Date()));

        // 🔍 VALIDATE INPUT DATA
        if (!validateInputs(sessionId, roundId, callback)) {
            return; // Error already logged in validate method
        }

        // 🔍 LOG CALCULATION DETAILS
        logCalculationDetails(sessionId, roundId);

        // 🔍 LOG API CALL INFO
        logApiCallInfo();

        // 🔧 CALL API
        try {
            Log.d(TAG, "🚀 Initiating calculate API call...");
            Call<CalculateAttendanceResponse> call = apiService.calculateAttendance(sessionId, roundId);

            if (call == null) {
                Log.e(TAG, "❌ FATAL: API call is NULL");
                callback.onCalculateFailure(roundId, "Failed to create API call");
                logCalculationEnd(false);
                return;
            }

            Log.d(TAG, "📞 API call created successfully");
            Log.d(TAG, "📞 Call URL: " + call.request().url());
            Log.d(TAG, "📞 Call method: " + call.request().method());

            call.enqueue(new Callback<CalculateAttendanceResponse>() {
                @Override
                public void onResponse(Call<CalculateAttendanceResponse> call, Response<CalculateAttendanceResponse> response) {
                    handleApiResponse(call, response, sessionId, roundId, callback);
                }

                @Override
                public void onFailure(Call<CalculateAttendanceResponse> call, Throwable t) {
                    handleApiFailure(call, t, sessionId, roundId, callback);
                }
            });

            Log.d(TAG, "📞 API call enqueued successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION during API call setup", e);
            callback.onCalculateFailure(roundId, "Exception: " + e.getMessage());
            logCalculationEnd(false);
        }
    }

    private boolean validateInputs(String sessionId, String roundId,
                                   AttendanceCallbacks.CalculateAttendanceCallback callback) {
        Log.d(TAG, "🔍 VALIDATING INPUT DATA:");

        boolean isValid = true;

        // Check Session ID
        if (sessionId == null || sessionId.trim().isEmpty()) {
            Log.e(TAG, "❌ Session ID is null or empty");
            isValid = false;
        } else {
            Log.d(TAG, "✅ Session ID is valid: " + sessionId.length() + " characters");
        }

        // Check Round ID
        if (roundId == null || roundId.trim().isEmpty()) {
            Log.e(TAG, "❌ Round ID is null or empty");
            isValid = false;
        } else {
            Log.d(TAG, "✅ Round ID is valid: " + roundId.length() + " characters");
        }

        // Check callback
        if (callback == null) {
            Log.e(TAG, "❌ Callback is null");
            isValid = false;
        } else {
            Log.d(TAG, "✅ Callback is valid");
        }

        // Check API service
        if (apiService == null) {
            Log.e(TAG, "❌ API service is null");
            isValid = false;
        } else {
            Log.d(TAG, "✅ API service is ready");
        }

        if (!isValid) {
            Log.e(TAG, "❌ Input validation failed");
            if (callback != null) {
                callback.onCalculateFailure(roundId, "Invalid input parameters");
            }
            logCalculationEnd(false);
        }

        return isValid;
    }

    private void logCalculationDetails(String sessionId, String roundId) {
        Log.i(TAG, "📋 CALCULATION DETAILS:");
        Log.i(TAG, "  Session ID: '" + sessionId + "'");
        Log.i(TAG, "  Round ID: '" + roundId + "'");

        // Validate ID formats
        if (sessionId != null && sessionId.contains("-")) {
            Log.d(TAG, "  Session ID format: UUID-like");
        }

        if (roundId != null && roundId.contains("-")) {
            Log.d(TAG, "  Round ID format: UUID-like");
        }
    }

    private void logApiCallInfo() {
        Log.d(TAG, "🌐 API CALL INFORMATION:");
        Log.d(TAG, "  Endpoint: POST /api/attendance/calculate");
        Log.d(TAG, "  Method: calculateAttendance(sessionId, roundId)");
        Log.d(TAG, "  ApiService: " + (apiService != null ? "Ready" : "NULL"));
        Log.d(TAG, "  Context: " + context.getClass().getSimpleName());
    }

    private void handleApiResponse(Call<CalculateAttendanceResponse> call,
                                   Response<CalculateAttendanceResponse> response,
                                   String sessionId, String roundId,
                                   AttendanceCallbacks.CalculateAttendanceCallback callback) {

        Log.d(TAG, "📥 CALCULATE API RESPONSE RECEIVED:");
        Log.d(TAG, "  Response code: " + response.code());
        Log.d(TAG, "  Response message: '" + response.message() + "'");
        Log.d(TAG, "  Is successful: " + response.isSuccessful());
        Log.d(TAG, "  Response body: " + (response.body() != null ? "Present" : "NULL"));
        Log.d(TAG, "  Session ID: " + sessionId);
        Log.d(TAG, "  Round ID: " + roundId);

        // Log response headers
        if (response.headers() != null) {
            Log.d(TAG, "  Response headers count: " + response.headers().size());
            for (String name : response.headers().names()) {
                Log.d(TAG, "    " + name + ": " + response.headers().get(name));
            }
        }

        if (response.isSuccessful()) {
            handleSuccessfulCalculateResponse(response, sessionId, roundId, callback);
        } else {
            handleErrorCalculateResponse(response, roundId, callback);
        }
    }

    private void handleSuccessfulCalculateResponse(Response<CalculateAttendanceResponse> response,
                                                   String sessionId, String roundId,
                                                   AttendanceCallbacks.CalculateAttendanceCallback callback) {

        Log.d(TAG, "✅ HTTP RESPONSE SUCCESSFUL");

        if (response.body() == null) {
            Log.e(TAG, "❌ Response body is NULL despite successful HTTP status");
            callback.onCalculateFailure(roundId, "Empty response body");
            logCalculationEnd(false);
            return;
        }

        CalculateAttendanceResponse apiResponse = response.body();

        Log.d(TAG, "📋 CALCULATE API RESPONSE DETAILS:");
        Log.d(TAG, "  Success flag: " + apiResponse.isSuccess());
        Log.d(TAG, "  Message: '" + apiResponse.getMessage() + "'");
        Log.d(TAG, "  Error: '" + apiResponse.getError() + "'");
        Log.d(TAG, "  Data: " + (apiResponse.getData() != null ? "Present" : "NULL"));

        // Log full response for debugging
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            String responseJson = gson.toJson(apiResponse);
            Log.d(TAG, "  Full response JSON:");
            String[] lines = responseJson.split("\n");
            for (String line : lines) {
                Log.d(TAG, "    " + line);
            }
        } catch (Exception e) {
            Log.w(TAG, "  Could not serialize response to JSON", e);
        }

        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            handleCalculateSuccess(apiResponse, sessionId, roundId, callback);
        } else {
            handleCalculateApiError(apiResponse, roundId, callback);
        }
    }

    private void handleCalculateSuccess(CalculateAttendanceResponse apiResponse,
                                        String sessionId, String roundId,
                                        AttendanceCallbacks.CalculateAttendanceCallback callback) {

        Log.d(TAG, "✅ CALCULATE OPERATION SUCCESSFUL");

        CalculateAttendanceData data = apiResponse.getData();

        Log.d(TAG, "📊 ATTENDANCE CALCULATION RESULTS:");
        Log.d(TAG, "  Session ID: " + sessionId);
        Log.d(TAG, "  Round ID: " + roundId);
        Log.d(TAG, "  Attended count: " + data.getAttendedCount());
        Log.d(TAG, "  Result message: '" + data.getMessage() + "'");

        // Validate attendance data
        validateAttendanceResults(data);

        Log.d(TAG, "  API message: " + apiResponse.getMessage());

        try {
            callback.onCalculateSuccess(roundId, data.getAttendedCount(), data.getMessage());
            Log.d(TAG, "✅ Success callback executed");
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in success callback", e);
        }

        logCalculationEnd(true);
    }

    private void validateAttendanceResults(CalculateAttendanceData data) {
        Log.d(TAG, "🔍 VALIDATING ATTENDANCE RESULTS:");

        if (data.getAttendedCount() < 0) {
            Log.w(TAG, "⚠️ Attended count is negative: " + data.getAttendedCount());
        } else if (data.getAttendedCount() == 0) {
            Log.w(TAG, "⚠️ No students attended this round");
        } else {
            Log.d(TAG, "✅ Attended count is valid");
        }

        if (data.getMessage() == null || data.getMessage().trim().isEmpty()) {
            Log.w(TAG, "⚠️ Result message is empty");
        } else {
            Log.d(TAG, "✅ Result message is present: " + data.getMessage().length() + " characters");
        }
    }

    private void handleCalculateApiError(CalculateAttendanceResponse apiResponse,
                                         String roundId,
                                         AttendanceCallbacks.CalculateAttendanceCallback callback) {

        Log.e(TAG, "❌ CALCULATE API OPERATION FAILED");

        String error = apiResponse.getError() != null ?
                apiResponse.getError() : "Calculate API returned failure";

        Log.e(TAG, "  API Error: " + error);
        Log.e(TAG, "  API Success flag: " + apiResponse.isSuccess());
        Log.e(TAG, "  API Message: " + apiResponse.getMessage());
        Log.e(TAG, "  Round ID: " + roundId);

        try {
            callback.onCalculateFailure(roundId, "API Error: " + error);
            Log.d(TAG, "Failure callback executed");
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in failure callback", e);
        }

        logCalculationEnd(false);
    }

    private void handleErrorCalculateResponse(Response<CalculateAttendanceResponse> response,
                                              String roundId,
                                              AttendanceCallbacks.CalculateAttendanceCallback callback) {

        Log.e(TAG, "❌ HTTP ERROR RESPONSE");
        Log.e(TAG, "  Status code: " + response.code());
        Log.e(TAG, "  Status message: '" + response.message() + "'");
        Log.e(TAG, "  Round ID: " + roundId);
        Log.e(TAG, "  Request URL: " + response.raw().request().url());

        // Try to log error body
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                Log.e(TAG, "  Error body: " + errorBody);

                // Try to parse error body as JSON
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    Object errorJson = gson.fromJson(errorBody, Object.class);
                    String prettyError = gson.toJson(errorJson);
                    Log.e(TAG, "  Parsed error body: " + prettyError);
                } catch (Exception e) {
                    Log.e(TAG, "  Error body is not valid JSON");
                }
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

        try {
            callback.onCalculateFailure(roundId, errorMessage);
            Log.d(TAG, "Failure callback executed");
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in failure callback", e);
        }

        logCalculationEnd(false);
    }

    private void handleApiFailure(Call<CalculateAttendanceResponse> call,
                                  Throwable t,
                                  String sessionId, String roundId,
                                  AttendanceCallbacks.CalculateAttendanceCallback callback) {

        Log.e(TAG, "❌ CALCULATE API CALL FAILURE");
        Log.e(TAG, "  Exception type: " + t.getClass().getSimpleName());
        Log.e(TAG, "  Exception message: " + t.getMessage());
        Log.e(TAG, "  Call URL: " + (call != null && call.request() != null ? call.request().url() : "NULL"));
        Log.e(TAG, "  Session ID: " + sessionId);
        Log.e(TAG, "  Round ID: " + roundId);

        // Log full stack trace for debugging
        Log.e(TAG, "  Full exception:", t);

        // Categorize the error
        String errorCategory = categorizeNetworkError(t);
        Log.e(TAG, "  Error category: " + errorCategory);

        String errorMessage = "Network Error (" + errorCategory + "): " + t.getMessage();

        try {
            callback.onCalculateFailure(roundId, errorMessage);
            Log.d(TAG, "Failure callback executed");
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in failure callback", e);
        }

        logCalculationEnd(false);
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

    private void logCalculationEnd(boolean success) {
        Log.i(TAG, "=== ATTENDANCE CALCULATION " + (success ? "SUCCESS" : "FAILED") + " ===");
        Log.i(TAG, "End timestamp: " + timeFormat.format(new Date()));
        Log.i(TAG, "============================================");
    }
}
