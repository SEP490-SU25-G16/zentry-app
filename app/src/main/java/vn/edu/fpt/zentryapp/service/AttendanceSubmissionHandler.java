package vn.edu.fpt.zentryapp.service;

import android.content.Context;
import android.util.Log;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;

public class AttendanceSubmissionHandler {
    private static final String TAG = "AttendanceSubmissionHandler";
    private static final int MAX_RETRY_COUNT = 3;
    private final AttendanceApiService apiService;
    private final Context context;
    private final OfflineSubmissionManager offlineManager;
    private final NetworkStateManager networkManager;

    public AttendanceSubmissionHandler(Context context) {
        Log.d(TAG, "=== INITIALIZING ATTENDANCE SUBMISSION HANDLER ===");
        this.context = context;

        try {
            this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);
            Log.d(TAG, "✅ ApiService created successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create ApiService", e);
            throw new RuntimeException("Failed to initialize AttendanceSubmissionHandler", e);
        }

        // Initialize offline support managers
        this.offlineManager = new OfflineSubmissionManager(context);
        this.networkManager = new NetworkStateManager(context);

        // Start network monitoring for auto-sync
        startNetworkMonitoring();

        Log.d(TAG, "AttendanceSubmissionHandler initialized with offline support");
        Log.d(TAG, "Context: " + context.getClass().getSimpleName());
        Log.d(TAG, "================================================");
    }

    private void startNetworkMonitoring() {
        networkManager.startMonitoring(new NetworkStateManager.NetworkStateListener() {
            @Override
            public void onNetworkAvailable() {
                Log.d(TAG, "🟢 Network available - attempting to sync cached submissions");
                syncCachedSubmissions();
            }

            @Override
            public void onNetworkLost() {
                Log.d(TAG, "🔴 Network lost - future submissions will be cached");
            }
        });
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
        Log.i(TAG, "  Submitter Device Android Id: '" + submission.getSubmitterDeviceAndroidId() + "'");
        Log.i(TAG, "  Session ID: '" + submission.getSessionId() + "'");
        Log.i(TAG, "  Submission Timestamp: '" + submission.getTimestamp() + "'");
        Log.i(TAG, "  Scanned Devices Count: " + submission.getScannedDevices().size());

        // 🔍 VALIDATE REQUIRED FIELDS
        validateSubmissionData(submission);

        // 🔍 LOG CHI TIẾT TỪNG THIẾT BỊ ĐƯỢC SCAN
        logScannedDevices(submission);

        // 🔍 LOG JSON PAYLOAD
        logJsonPayload(submission);

        // ✅ CHECK NETWORK AVAILABILITY FIRST
        if (!networkManager.isNetworkAvailable()) {
            Log.w(TAG, "📵 No network available - caching submission for later");
            handleOfflineSubmission(submission, callback);
            return;
        }

        // ✅ NETWORK AVAILABLE: Perform online submission
        performOnlineSubmission(submission, callback, false);
    }

    // ✅ NEW: Handle offline submission
    private void handleOfflineSubmission(AttendanceModels.AttendanceSubmission submission,
                                         AttendanceCallbacks.AttendanceSubmissionCallback callback) {

        Log.d(TAG, "💾 Caching submission for offline mode");
        Log.d(TAG, "  Session: " + submission.getSessionId());
        Log.d(TAG, "  Devices: " + submission.getScannedDevices().size());

        // Cache submission using wrapper
        offlineManager.cacheSubmission(submission);

        Log.d(TAG, "✅ Submission cached successfully");
        Log.d(TAG, "  Total cached submissions: " + offlineManager.getCachedSubmissionCount());

        // From UX perspective, this is still "successful"
        callback.onSubmissionSuccess(submission);

        // Show offline notification
        showOfflineNotification();

        logSubmissionEnd(true, "CACHED");
    }

    // ✅ NEW: Perform online submission (original logic)
    private void performOnlineSubmission(AttendanceModels.AttendanceSubmission submission,
                                         AttendanceCallbacks.AttendanceSubmissionCallback callback,
                                         boolean isRetry) {

        Log.d(TAG, "🌐 Performing online submission" + (isRetry ? " (RETRY)" : ""));

        // 🔍 LOG API CALL INFO
        Log.d(TAG, "🌐 API CALL INFORMATION:");
        Log.d(TAG, "  Endpoint: POST /api/attendance/sessions/scan");
        Log.d(TAG, "  ApiService: " + (apiService != null ? "Ready" : "NULL"));
        Log.d(TAG, "  Context: " + context.getClass().getSimpleName());
        Log.d(TAG, "  Is Retry: " + isRetry);

        // 🔧 CALL API
        try {
            Log.d(TAG, "🚀 Initiating API call...");
            Call<AttendanceApiResponse> call = apiService.submitAttendanceScan(submission);

            if (call == null) {
                Log.e(TAG, "❌ FATAL: API call is NULL");
                handleSubmissionFailure(submission, callback, isRetry, "Failed to create API call");
                return;
            }

            Log.d(TAG, "📞 API call created successfully");
            Log.d(TAG, "📞 Call URL: " + call.request().url());
            Log.d(TAG, "📞 Call method: " + call.request().method());

            call.enqueue(new Callback<AttendanceApiResponse>() {
                @Override
                public void onResponse(Call<AttendanceApiResponse> call, Response<AttendanceApiResponse> response) {
                    handleApiResponse(call, response, submission, callback, isRetry);
                }

                @Override
                public void onFailure(Call<AttendanceApiResponse> call, Throwable t) {
                    handleApiFailure(call, t, submission, callback, isRetry);
                }
            });

            Log.d(TAG, "📞 API call enqueued successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION during API call setup", e);
            handleSubmissionFailure(submission, callback, isRetry, "Exception: " + e.getMessage());
        }
    }

    // ✅ UPDATED: Handle submission failure with retry logic
    private void handleSubmissionFailure(AttendanceModels.AttendanceSubmission submission,
                                         AttendanceCallbacks.AttendanceSubmissionCallback callback,
                                         boolean wasRetry,
                                         String error) {
        Log.e(TAG, "❌ Submission failed: " + error);

        if (wasRetry) {
            // This was a retry of cached submission -> increment retry count
            offlineManager.incrementRetryCount(submission);

            // Check retry count from cache
            OfflineSubmissionManager.CachedSubmissionWrapper wrapper = findCachedWrapper(submission);
            if (wrapper != null && wrapper.getRetryCount() >= MAX_RETRY_COUNT) {
                Log.e(TAG, "❌ Max retries reached for cached submission");
                Log.e(TAG, "  Retries: " + wrapper.getRetryCount() + "/" + MAX_RETRY_COUNT);
                callback.onSubmissionFailure(0, "Max retries exceeded: " + error);
                logSubmissionEnd(false, "MAX_RETRIES_EXCEEDED");
            } else {
                Log.w(TAG, "⚠️ Will retry cached submission later");
                if (wrapper != null) {
                    Log.w(TAG, "  Current retries: " + wrapper.getRetryCount() + "/" + MAX_RETRY_COUNT);
                }
                callback.onSubmissionFailure(0, "Will retry: " + error);
                logSubmissionEnd(false, "RETRY_PENDING");
            }
        } else {
            // New submission failed -> cache it for later retry
            Log.w(TAG, "💾 New submission failed - caching for later retry");
            offlineManager.cacheSubmission(submission);

            // From UX perspective: still "successful"
            callback.onSubmissionSuccess(submission);
            showOfflineNotification();
            logSubmissionEnd(true, "CACHED_ON_FAILURE");
        }
    }

    // ✅ NEW: Find cached wrapper for retry count management
    private OfflineSubmissionManager.CachedSubmissionWrapper findCachedWrapper(AttendanceModels.AttendanceSubmission submission) {
        for (OfflineSubmissionManager.CachedSubmissionWrapper wrapper : offlineManager.getCachedSubmissionsWithMetadata()) {
            if (wrapper.getSubmission() != null &&
                    wrapper.getSubmission().getSessionId().equals(submission.getSessionId()) &&
                    wrapper.getSubmission().getTimestamp().equals(submission.getTimestamp())) {
                return wrapper;
            }
        }
        return null;
    }

    // ✅ NEW: Sync all cached submissions when network becomes available
    public void syncCachedSubmissions() {
        List<OfflineSubmissionManager.CachedSubmissionWrapper> cachedWrappers = offlineManager.getCachedSubmissionsWithMetadata();
        if (!networkManager.isNetworkAvailable()) {
            Log.d(TAG, "❌ Network not available during sync attempt");
            return;
        }

        if (cachedWrappers.isEmpty()) {
            Log.d(TAG, "No cached submissions to sync");
            return;
        }

        Log.d(TAG, "🔄 Syncing " + cachedWrappers.size() + " cached submissions");

        for (OfflineSubmissionManager.CachedSubmissionWrapper wrapper : cachedWrappers) {
            // Skip if max retries reached
            if (wrapper.getRetryCount() >= MAX_RETRY_COUNT) {
                Log.w(TAG, "⚠️ Skipping submission with max retries");
                Log.w(TAG, "  Session: " + wrapper.getSubmission().getSessionId());
                Log.w(TAG, "  Retries: " + wrapper.getRetryCount() + "/" + MAX_RETRY_COUNT);
                continue;
            }

            Log.d(TAG, "♻️ Retrying cached submission");
            Log.d(TAG, "  Session: " + wrapper.getSubmission().getSessionId());
            Log.d(TAG, "  Attempt: " + (wrapper.getRetryCount() + 1) + "/" + MAX_RETRY_COUNT);

            // Retry submission with dummy callback for background sync
            performOnlineSubmission(wrapper.getSubmission(), new AttendanceCallbacks.AttendanceSubmissionCallback() {
                @Override
                public void onSubmissionSuccess(AttendanceModels.AttendanceSubmission submission) {
                    Log.d(TAG, "✅ Cached submission synced successfully: " + submission.getSessionId());
                }

                @Override
                public void onSubmissionFailure(int roundNumber, String error) {
                    Log.e(TAG, "❌ Cached submission sync failed: " + error);
                }
            }, true);
        }
    }

    // ✅ NEW: Show offline notification
    private void showOfflineNotification() {
        int cachedCount = offlineManager.getCachedSubmissionCount();
        Log.d(TAG, "📱 Offline Mode Active:");
        Log.d(TAG, "  Total cached submissions: " + cachedCount);
        Log.d(TAG, "  Will sync automatically when network available");

        // TODO: Implement actual notification or broadcast to UI
        // Intent broadcast = new Intent("vn.edu.fpt.zentryapp.OFFLINE_SUBMISSION");
        // broadcast.putExtra("cachedCount", cachedCount);
        // context.sendBroadcast(broadcast);
    }

    // ✅ UPDATED: Handle API response with retry logic
    private void handleApiResponse(Call<AttendanceApiResponse> call,
                                   Response<AttendanceApiResponse> response,
                                   AttendanceModels.AttendanceSubmission submission,
                                   AttendanceCallbacks.AttendanceSubmissionCallback callback,
                                   boolean isRetry) {

        if (response.isSuccessful()) {
            handleSuccessfulResponse(response, submission, callback, isRetry);
        } else {
            // Log submission info on error
            logSubmissionInfo(submission);
            handleErrorResponse(response, submission, callback, isRetry);
        }
    }

    // ✅ UPDATED: Handle successful response with cache cleanup
    private void handleSuccessfulResponse(Response<AttendanceApiResponse> response,
                                          AttendanceModels.AttendanceSubmission submission,
                                          AttendanceCallbacks.AttendanceSubmissionCallback callback,
                                          boolean isRetry) {

        Log.d(TAG, "✅ HTTP RESPONSE SUCCESSFUL");

        if (response.body() == null) {
            Log.e(TAG, "❌ Response body is NULL despite successful HTTP status");
            handleSubmissionFailure(submission, callback, isRetry, "Empty response body");
            return;
        }

        AttendanceApiResponse apiResponse = response.body();
        Log.d(TAG, "📋 API RESPONSE DETAILS:");
        Log.d(TAG, "  Success flag: " + apiResponse.isSuccess());
        Log.d(TAG, "  Message: '" + apiResponse.getMessage() + "'");
        Log.d(TAG, "  Error: '" + apiResponse.getError() + "'");
        Log.d(TAG, "  Is Retry: " + isRetry);

        // Log full response JSON
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

            // ✅ SUCCESS: Remove from cache if this was a retry
            if (isRetry) {
                offlineManager.removeSubmission(submission);
                Log.d(TAG, "✅ Cached submission successfully synced and removed from cache");
                Log.d(TAG, "  Remaining cached submissions: " + offlineManager.getCachedSubmissionCount());
            }

            logSubmissionEnd(true, isRetry ? "RETRY_SUCCESS" : "ONLINE_SUCCESS");
            callback.onSubmissionSuccess(submission);
        } else {
            Log.e(TAG, "❌ API OPERATION FAILED");
            Log.e(TAG, "  API Error: " + apiResponse.getError());

            handleSubmissionFailure(submission, callback, isRetry, "API Error: " + apiResponse.getError());
        }
    }

    // ✅ UPDATED: Handle error response with retry logic
    private void handleErrorResponse(Response<AttendanceApiResponse> response,
                                     AttendanceModels.AttendanceSubmission submission,
                                     AttendanceCallbacks.AttendanceSubmissionCallback callback,
                                     boolean isRetry) {

        Log.e(TAG, "❌ HTTP ERROR RESPONSE");
        Log.e(TAG, "  Status code: " + response.code());
        Log.e(TAG, "  Status message: '" + response.message() + "'");
        Log.e(TAG, "  Request URL: " + response.raw().request().url());
        Log.e(TAG, "  Is Retry: " + isRetry);

        // Try to log error body
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

        handleSubmissionFailure(submission, callback, isRetry, errorMessage);
    }

    // ✅ UPDATED: Handle API failure with retry logic
    private void handleApiFailure(Call<AttendanceApiResponse> call,
                                  Throwable t,
                                  AttendanceModels.AttendanceSubmission submission,
                                  AttendanceCallbacks.AttendanceSubmissionCallback callback,
                                  boolean isRetry) {

        Log.e(TAG, "❌ API CALL FAILURE");
        Log.e(TAG, "  Exception type: " + t.getClass().getSimpleName());
        Log.e(TAG, "  Exception message: " + t.getMessage());
        Log.e(TAG, "  Call URL: " + (call != null && call.request() != null ? call.request().url() : "NULL"));
        Log.e(TAG, "  Submitted session: " + submission.getSessionId());
        Log.e(TAG, "  Submitted devices: " + submission.getScannedDevices().size());
        Log.e(TAG, "  Is Retry: " + isRetry);
        Log.e(TAG, "  Full exception:", t);

        String errorCategory = categorizeNetworkError(t);
        Log.e(TAG, "  Error category: " + errorCategory);

        String errorMessage = "Network Error (" + errorCategory + "): " + t.getMessage();

        handleSubmissionFailure(submission, callback, isRetry, errorMessage);
    }

    // ✅ NEW: Log submission info for debugging
    private void logSubmissionInfo(AttendanceModels.AttendanceSubmission submission) {
        Log.d(TAG, "📤 ATTENDANCE SUBMISSION INFO:");
        if (submission != null) {
            Log.d(TAG, "  Submitter Android Id: " + submission.getSubmitterDeviceAndroidId());
            Log.d(TAG, "  Session ID: " + submission.getSessionId());
            Log.d(TAG, "  Timestamp: " + submission.getTimestamp());

            List<AttendanceModels.ScannedDevice> scannedDevices = submission.getScannedDevices();
            if (scannedDevices != null && !scannedDevices.isEmpty()) {
                Log.d(TAG, "  Scanned devices count: " + scannedDevices.size());
                for (int i = 0; i < scannedDevices.size(); i++) {
                    AttendanceModels.ScannedDevice device = scannedDevices.get(i);
                    Log.d(TAG, "    Device " + (i + 1) + " - Android Id: " + device.getAndroidId() + ", RSSI: " + device.getRssi());
                }
            } else {
                Log.d(TAG, "  No scanned devices");
            }
        } else {
            Log.w(TAG, "  Submission is null");
        }
    }

    // ✅ PUBLIC METHODS FOR EXTERNAL ACCESS

    /**
     * Get count of cached submissions (for UI display)
     */
    public int getCachedSubmissionCount() {
        return offlineManager.getCachedSubmissionCount();
    }

    /**
     * Check if there are cached submissions
     */
    public boolean hasCachedSubmissions() {
        return offlineManager.hasCachedSubmissions();
    }

    /**
     * Cleanup resources
     */
    public void destroy() {
        Log.d(TAG, "🔄 Destroying AttendanceSubmissionHandler");
        networkManager.stopMonitoring();
        Log.d(TAG, "✅ Resources cleaned up");
    }

    private void validateSubmissionData(AttendanceModels.AttendanceSubmission submission) {
        Log.d(TAG, "🔍 VALIDATING SUBMISSION DATA:");

        String androidId = submission.getSubmitterDeviceAndroidId();
        if (androidId == null || androidId.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Submitter Android Id is null or empty");
        }

        String sessionId = submission.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Session ID is null or empty");
        } else {
            Log.d(TAG, "✅ Session ID is valid: " + sessionId.length() + " characters");
        }

        String timestamp = submission.getTimestamp();
        if (timestamp == null || timestamp.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Timestamp is null or empty");
        } else {
            Log.d(TAG, "✅ Timestamp is valid: " + timestamp);
        }

        if (submission.getScannedDevices() == null) {
            Log.w(TAG, "⚠️ Scanned devices list is null");
        } else if (submission.getScannedDevices().isEmpty()) {
            Log.w(TAG, "⚠️ No devices were scanned");
        } else {
            Log.d(TAG, "✅ Scanned devices list is valid");
        }
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
            Log.d(TAG, "    Android Id: '" + device.getAndroidId() + "'");
            Log.d(TAG, "    RSSI: " + device.getRssi() + " dBm");
            Log.d(TAG, "    Signal strength: " + getSignalStrengthDescription(device.getRssi()));

            if (device.getAndroidId() == null || device.getAndroidId().trim().isEmpty()) {
                Log.w(TAG, "    ⚠️ Device Android Id is null or empty");
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

    // ✅ UPDATED: Enhanced logging with operation type
    private void logSubmissionEnd(boolean success, String operationType) {
        Log.i(TAG, "=== ATTENDANCE SUBMISSION " + (success ? "SUCCESS" : "FAILED") + " ===");
        Log.i(TAG, "Operation Type: " + operationType);
        Log.i(TAG, "End timestamp: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",
                java.util.Locale.getDefault()).format(new java.util.Date()));
        Log.i(TAG, "Cached submissions remaining: " + offlineManager.getCachedSubmissionCount());
        Log.i(TAG, "==============================================");
    }
}