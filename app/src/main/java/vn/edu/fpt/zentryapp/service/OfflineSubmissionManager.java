package vn.edu.fpt.zentryapp.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class OfflineSubmissionManager {
    private static final String TAG = "OfflineSubmissionManager";
    private static final String PREF_NAME = "offline_submissions";
    private static final String KEY_SUBMISSIONS = "cached_submissions";

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;

    /**
     * ✅ NEW: Manual sync all cached submissions (for schedule screen)
     */
    public interface ManualSyncCallback {
        void onSyncStarted(int totalSubmissions);
        void onSubmissionSynced(AttendanceModels.AttendanceSubmission submission, int remaining);
        void onSubmissionFailed(AttendanceModels.AttendanceSubmission submission, String error, int remaining);
        void onSyncCompleted(int successful, int failed);
    }

    // ✅ Wrapper class để lưu thêm metadata mà không sửa AttendanceSubmission
    public static class CachedSubmissionWrapper {
        private AttendanceModels.AttendanceSubmission submission;
        private long cachedTimestamp;
        private int retryCount;

        public CachedSubmissionWrapper() {} // Default constructor cho Gson

        public CachedSubmissionWrapper(AttendanceModels.AttendanceSubmission submission) {
            this.submission = submission;
            this.cachedTimestamp = System.currentTimeMillis();
            this.retryCount = 0;
        }

        // Getters and setters
        public AttendanceModels.AttendanceSubmission getSubmission() { return submission; }
        public void setSubmission(AttendanceModels.AttendanceSubmission submission) { this.submission = submission; }

        public long getCachedTimestamp() { return cachedTimestamp; }
        public void setCachedTimestamp(long cachedTimestamp) { this.cachedTimestamp = cachedTimestamp; }

        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

        public void incrementRetryCount() { this.retryCount++; }
    }

    public OfflineSubmissionManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Cache submission khi network fail
     */
    public void cacheSubmission(AttendanceModels.AttendanceSubmission submission) {
        Log.d(TAG, "💾 Caching submission for offline: " + submission.getSessionId());

        List<CachedSubmissionWrapper> cachedWrappers = getCachedWrappers();

        // Wrap submission với metadata
        CachedSubmissionWrapper wrapper = new CachedSubmissionWrapper(submission);
        cachedWrappers.add(wrapper);

        // Save to SharedPreferences
        String json = gson.toJson(cachedWrappers);
        prefs.edit().putString(KEY_SUBMISSIONS, json).apply();

        Log.d(TAG, "✅ Submission cached. Total cached: " + cachedWrappers.size());
    }

    /**
     * Lấy tất cả cached wrappers
     */
    private List<CachedSubmissionWrapper> getCachedWrappers() {
        String json = prefs.getString(KEY_SUBMISSIONS, "[]");
        Type listType = new TypeToken<List<CachedSubmissionWrapper>>(){}.getType();

        try {
            List<CachedSubmissionWrapper> wrappers = gson.fromJson(json, listType);
            return wrappers != null ? wrappers : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing cached submissions", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy tất cả submissions đã cache (chỉ trả về submission, không có metadata)
     */
    public List<AttendanceModels.AttendanceSubmission> getCachedSubmissions() {
        List<CachedSubmissionWrapper> wrappers = getCachedWrappers();
        List<AttendanceModels.AttendanceSubmission> submissions = new ArrayList<>();

        for (CachedSubmissionWrapper wrapper : wrappers) {
            if (wrapper.getSubmission() != null) {
                submissions.add(wrapper.getSubmission());
            }
        }

        return submissions;
    }

    /**
     * Lấy wrapper để có thể access metadata
     */
    public List<CachedSubmissionWrapper> getCachedSubmissionsWithMetadata() {
        return getCachedWrappers();
    }

    /**
     * Update retry count cho một submission
     */
    public void incrementRetryCount(AttendanceModels.AttendanceSubmission submission) {
        List<CachedSubmissionWrapper> wrappers = getCachedWrappers();
        boolean updated = false;

        for (CachedSubmissionWrapper wrapper : wrappers) {
            if (wrapper.getSubmission() != null &&
                    wrapper.getSubmission().getSessionId().equals(submission.getSessionId()) &&
                    wrapper.getSubmission().getTimestamp().equals(submission.getTimestamp())) {

                wrapper.incrementRetryCount();
                updated = true;
                break;
            }
        }

        if (updated) {
            String json = gson.toJson(wrappers);
            prefs.edit().putString(KEY_SUBMISSIONS, json).apply();
            Log.d(TAG, "📈 Incremented retry count for submission");
        }
    }

    /**
     * Xóa submission đã submit thành công
     */
    public void removeSubmission(AttendanceModels.AttendanceSubmission submission) {
        List<CachedSubmissionWrapper> wrappers = getCachedWrappers();

        // Remove by sessionId and timestamp
        wrappers.removeIf(wrapper ->
                wrapper.getSubmission() != null &&
                        wrapper.getSubmission().getSessionId().equals(submission.getSessionId()) &&
                        wrapper.getSubmission().getTimestamp().equals(submission.getTimestamp()));

        // Save back
        String json = gson.toJson(wrappers);
        prefs.edit().putString(KEY_SUBMISSIONS, json).apply();

        Log.d(TAG, "🗑️ Removed cached submission. Remaining: " + wrappers.size());
    }

    /**
     * Clear tất cả cache
     */
    public void clearAllCache() {
        prefs.edit().remove(KEY_SUBMISSIONS).apply();
        Log.d(TAG, "🧹 All cached submissions cleared");
    }

    /**
     * Kiểm tra có submissions cần retry không
     */
    public boolean hasCachedSubmissions() {
        return !getCachedWrappers().isEmpty();
    }

    public int getCachedSubmissionCount() {
        return getCachedWrappers().size();
    }

    /**
     * ✅ Get cached submissions cho specific session (nếu cần filter)
     */
    public List<CachedSubmissionWrapper> getCachedSubmissionsForSession(String sessionId) {
        List<CachedSubmissionWrapper> allCached = getCachedWrappers();
        List<CachedSubmissionWrapper> sessionCached = new ArrayList<>();

        for (CachedSubmissionWrapper wrapper : allCached) {
            if (wrapper.getSubmission() != null &&
                    sessionId.equals(wrapper.getSubmission().getSessionId())) {
                sessionCached.add(wrapper);
            }
        }

        Log.d(TAG, "📋 Found " + sessionCached.size() + " cached submissions for session: " + sessionId);
        return sessionCached;
    }

    /**
     * ✅ Get summary của cached submissions để hiển thị UI
     */
    public CachedSubmissionSummary getCachedSubmissionSummary() {
        List<CachedSubmissionWrapper> wrappers = getCachedWrappers();
        CachedSubmissionSummary summary = new CachedSubmissionSummary();

        for (CachedSubmissionWrapper wrapper : wrappers) {
            summary.totalCount++;

            if (wrapper.getRetryCount() >= 3) {
                summary.failedCount++;
            } else {
                summary.pendingCount++;
            }

            // Track oldest cached timestamp
            if (summary.oldestTimestamp == 0 || wrapper.getCachedTimestamp() < summary.oldestTimestamp) {
                summary.oldestTimestamp = wrapper.getCachedTimestamp();
            }
        }

        return summary;
    }

    /**
     * ✅ Summary class for UI display
     */
    public static class CachedSubmissionSummary {
        public int totalCount = 0;
        public int pendingCount = 0;
        public int failedCount = 0;
        public long oldestTimestamp = 0;

        public boolean hasCachedData() {
            return totalCount > 0;
        }

        public String getDisplayText() {
            if (totalCount == 0) return "No pending submissions";

            StringBuilder text = new StringBuilder();
            text.append(totalCount).append(" pending submission");
            if (totalCount > 1) text.append("s");

            if (failedCount > 0) {
                text.append(" (").append(failedCount).append(" failed)");
            }

            return text.toString();
        }
    }
}
