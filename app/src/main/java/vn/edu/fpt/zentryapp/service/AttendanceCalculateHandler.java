package vn.edu.fpt.zentryapp.service;

import android.content.Context;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CalculateAttendanceData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CalculateAttendanceResponse;

public class AttendanceCalculateHandler {
    private static final String TAG = "AttendanceCalculateHandler";

    private final AttendanceApiService apiService;

    public AttendanceCalculateHandler(Context context) {
        this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);
    }

    /**
     * 🔧 CALCULATE attendance cho một round
     */
    public void calculateRoundAttendance(String sessionId, String roundId,
                                         AttendanceCallbacks.CalculateAttendanceCallback callback) {

        Log.d(TAG, "Calculating attendance for session: " + sessionId + ", round: " + roundId);

        apiService.calculateAttendance(sessionId, roundId)
                .enqueue(new Callback<CalculateAttendanceResponse>() {
                    @Override
                    public void onResponse(Call<CalculateAttendanceResponse> call, Response<CalculateAttendanceResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CalculateAttendanceResponse apiResponse = response.body();

                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                CalculateAttendanceData data = apiResponse.getData();

                                if (callback != null) {
                                    callback.onCalculateSuccess(roundId, data.getAttendedCount(), data.getMessage());
                                }

                                Log.d(TAG, "✅ Calculate success - Round: " + roundId +
                                        ", Attended: " + data.getAttendedCount());

                            } else {
                                String error = apiResponse.getError() != null ?
                                        apiResponse.getError() : "Calculate API returned failure";

                                if (callback != null) {
                                    callback.onCalculateFailure(roundId, error);
                                }

                                Log.e(TAG, "❌ Calculate API error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();

                            if (callback != null) {
                                callback.onCalculateFailure(roundId, error);
                            }

                            Log.e(TAG, "❌ Calculate HTTP error: " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<CalculateAttendanceResponse> call, Throwable t) {
                        String error = "Network Error: " + t.getMessage();

                        if (callback != null) {
                            callback.onCalculateFailure(roundId, error);
                        }

                        Log.e(TAG, "❌ Calculate network error: " + error, t);
                    }
                });
    }
}

