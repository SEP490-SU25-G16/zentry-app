package vn.edu.fpt.zentryapp.service;

import android.content.Context;
import android.util.Log;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;

public class ManualAttendanceSyncService {
    private static final String TAG = "ManualSyncService";
    private final Context context;
    private final OfflineSubmissionManager offlineManager;
    private final AttendanceApiService apiService;

    public ManualAttendanceSyncService(Context context) {
        this.context = context;
        this.offlineManager = new OfflineSubmissionManager(context);
        this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);
    }

    /**
     * ✅ Manual sync all cached submissions (triggered from schedule screen)
     */
    public void syncAllCachedSubmissions(OfflineSubmissionManager.ManualSyncCallback callback) {
        List<OfflineSubmissionManager.CachedSubmissionWrapper> cached =
                offlineManager.getCachedSubmissionsWithMetadata();

        if (cached.isEmpty()) {
            Log.d(TAG, "No cached submissions to sync");
            callback.onSyncCompleted(0, 0);
            return;
        }

        Log.d(TAG, "🔄 Manual sync started: " + cached.size() + " submissions");
        callback.onSyncStarted(cached.size());

        syncSubmissionsSequentially(cached, 0, callback, 0, 0);
    }

    /**
     * ✅ Sync submissions một cách tuần tự để tránh overwhelm server
     */
    private void syncSubmissionsSequentially(
            List<OfflineSubmissionManager.CachedSubmissionWrapper> submissions,
            int currentIndex,
            OfflineSubmissionManager.ManualSyncCallback callback,
            int successCount,
            int failCount) {

        if (currentIndex >= submissions.size()) {
            Log.d(TAG, "✅ Manual sync completed: " + successCount + " success, " + failCount + " failed");
            callback.onSyncCompleted(successCount, failCount);
            return;
        }

        OfflineSubmissionManager.CachedSubmissionWrapper wrapper = submissions.get(currentIndex);

        // Skip if max retries reached
        if (wrapper.getRetryCount() >= 3) {
            Log.w(TAG, "⚠️ Skipping max retry submission: " + wrapper.getSubmission().getSessionId());
            syncSubmissionsSequentially(submissions, currentIndex + 1, callback, successCount, failCount + 1);
            return;
        }

        Log.d(TAG, "♻️ Manual syncing: " + wrapper.getSubmission().getSessionId());

        apiService.submitAttendanceScan(wrapper.getSubmission())
                .enqueue(new Callback<AttendanceApiResponse>() {
                    @Override
                    public void onResponse(Call<AttendanceApiResponse> call, Response<AttendanceApiResponse> response) {
                        int remaining = submissions.size() - currentIndex - 1;

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Log.d(TAG, "✅ Manual sync success: " + wrapper.getSubmission().getSessionId());
                            offlineManager.removeSubmission(wrapper.getSubmission());
                            callback.onSubmissionSynced(wrapper.getSubmission(), remaining);

                            // Continue với next submission
                            syncSubmissionsSequentially(submissions, currentIndex + 1, callback, successCount + 1, failCount);

                        } else {
                            Log.e(TAG, "❌ Manual sync failed: " + wrapper.getSubmission().getSessionId());
                            offlineManager.incrementRetryCount(wrapper.getSubmission());
                            callback.onSubmissionFailed(wrapper.getSubmission(), "API Error", remaining);

                            // Continue với next submission
                            syncSubmissionsSequentially(submissions, currentIndex + 1, callback, successCount, failCount + 1);
                        }
                    }

                    @Override
                    public void onFailure(Call<AttendanceApiResponse> call, Throwable t) {
                        int remaining = submissions.size() - currentIndex - 1;
                        Log.e(TAG, "❌ Manual sync network failure: " + wrapper.getSubmission().getSessionId());
                        offlineManager.incrementRetryCount(wrapper.getSubmission());
                        callback.onSubmissionFailed(wrapper.getSubmission(), t.getMessage(), remaining);

                        // Continue với next submission
                        syncSubmissionsSequentially(submissions, currentIndex + 1, callback, successCount, failCount + 1);
                    }
                });
    }

    /**
     * ✅ Quick check if sync is needed (for UI indicators)
     */
    public boolean needsSync() {
        return offlineManager.hasCachedSubmissions();
    }

    /**
     * ✅ Get summary for UI display
     */
    public OfflineSubmissionManager.CachedSubmissionSummary getSyncSummary() {
        return offlineManager.getCachedSubmissionSummary();
    }
}
