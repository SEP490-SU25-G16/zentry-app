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

    public AttendanceSubmissionHandler(Context context) {
        this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);
    }

    public void submitAttendance(AttendanceModels.AttendanceSubmission submission,
                                 AttendanceCallbacks.AttendanceSubmissionCallback callback) {

        Log.i(TAG, "=== ATTENDANCE SUBMISSION START ===");
        Log.i(TAG, "Submitter Device MAC: " + submission.getSubmitterDeviceMacAddress());
        Log.i(TAG, "Session ID: " + submission.getSessionId());
        Log.i(TAG, "Timestamp: " + submission.getTimestamp());
        Log.i(TAG, "Number of scanned devices: " + submission.getScannedDevices().size());

        // Log chi tiết từng thiết bị được scan
        for (int i = 0; i < submission.getScannedDevices().size(); i++) {
            AttendanceModels.ScannedDevice device = submission.getScannedDevices().get(i);
            Log.d(TAG, "Device[" + i + "]: MAC=" + device.getMacAddress() +
                    ", RSSI=" + device.getRssi() + "dBm");
        }

        // Log JSON payload for debugging
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String jsonPayload = gson.toJson(submission);
            Log.d(TAG, "JSON Payload: " + jsonPayload);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize submission to JSON", e);
        }

        Log.d(TAG, "Calling API: POST /api/attendance/sessions/scan");

        // 🔧 CALL API HERE
        apiService.submitAttendanceScan(submission)
                .enqueue(new Callback<AttendanceApiResponse>() {
                    @Override
                    public void onResponse(Call<AttendanceApiResponse> call, Response<AttendanceApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            AttendanceApiResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                Log.d(TAG, "✅ API Response Success: " + apiResponse.getMessage());
                                Log.d(TAG, "=== ATTENDANCE SUBMISSION END ===");
                                callback.onSubmissionSuccess(submission);
                            } else {
                                Log.e(TAG, "❌ API Response Failed: " + apiResponse.getError());
                                Log.d(TAG, "=== ATTENDANCE SUBMISSION END ===");
                                callback.onSubmissionFailure(0, "API Error: " + apiResponse.getError());
                            }
                        } else {
                            Log.e(TAG, "❌ HTTP Error: " + response.code() + " - " + response.message());
                            Log.d(TAG, "=== ATTENDANCE SUBMISSION END ===");
                            callback.onSubmissionFailure(0, "HTTP Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<AttendanceApiResponse> call, Throwable t) {
                        Log.e(TAG, "❌ Network Error", t);
                        Log.d(TAG, "=== ATTENDANCE SUBMISSION END ===");
                        callback.onSubmissionFailure(0, "Network Error: " + t.getMessage());
                    }
                });
    }
}
