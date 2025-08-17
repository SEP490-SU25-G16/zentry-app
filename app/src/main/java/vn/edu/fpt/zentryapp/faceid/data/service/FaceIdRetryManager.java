package vn.edu.fpt.zentryapp.faceid.data.service;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manager để xử lý retry logic với exponential backoff
 * - Exponential backoff strategy
 * - Configurable retry attempts
 * - Jitter để tránh thundering herd
 */
public class FaceIdRetryManager {
    private static final String TAG = "FaceIdRetryManager";
    
    // Configuration
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_INITIAL_DELAY_MS = 1000; // 1 giây
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final long MAX_DELAY_MS = 30000; // 30 giây
    private static final double JITTER_FACTOR = 0.1; // 10% jitter
    
    private final ScheduledExecutorService scheduler;
    private final int maxRetries;
    private final long initialDelayMs;
    
    public FaceIdRetryManager() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY_MS);
    }
    
    public FaceIdRetryManager(int maxRetries, long initialDelayMs) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * Execute task với retry logic
     */
    public <T> void executeWithRetry(Supplier<T> task, RetryCallback<T> callback) {
        executeWithRetryInternal(task, callback, 0);
    }
    
    /**
     * Execute task với retry logic (async)
     */
    public <T> void executeWithRetryAsync(Supplier<T> task, RetryCallback<T> callback) {
        scheduler.execute(() -> executeWithRetryInternal(task, callback, 0));
    }
    
    /**
     * Execute task với retry logic (internal)
     */
    private <T> void executeWithRetryInternal(Supplier<T> task, RetryCallback<T> callback, int attempt) {
        try {
            Log.d(TAG, "🔄 Executing task, attempt " + (attempt + 1) + "/" + (maxRetries + 1));
            
            T result = task.get();
            Log.d(TAG, "✅ Task completed successfully on attempt " + (attempt + 1));
            callback.onSuccess(result);
            
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Task failed on attempt " + (attempt + 1) + ": " + e.getMessage());
            
            if (attempt < maxRetries) {
                long delay = calculateDelay(attempt);
                Log.d(TAG, "⏰ Scheduling retry in " + delay + "ms");
                
                scheduler.schedule(() -> {
                    executeWithRetryInternal(task, callback, attempt + 1);
                }, delay, TimeUnit.MILLISECONDS);
                
            } else {
                Log.e(TAG, "❌ Task failed after " + (maxRetries + 1) + " attempts");
                callback.onFailure(e);
            }
        }
    }
    
    /**
     * Tính toán delay cho retry với exponential backoff và jitter
     */
    private long calculateDelay(int attempt) {
        // Exponential backoff: delay = initialDelay * (backoffMultiplier ^ attempt)
        long delay = (long) (initialDelayMs * Math.pow(BACKOFF_MULTIPLIER, attempt));
        
        // Cap delay ở max delay
        delay = Math.min(delay, MAX_DELAY_MS);
        
        // Thêm jitter để tránh thundering herd
        double jitter = delay * JITTER_FACTOR * (Math.random() - 0.5);
        delay = Math.max(0, delay + (long) jitter);
        
        Log.d(TAG, "📊 Calculated delay: " + delay + "ms (attempt " + attempt + ")");
        return delay;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        scheduler.shutdown();
        Log.d(TAG, "🧹 Cleaned up FaceIdRetryManager");
    }
    
    /**
     * Callback interface cho retry operations
     */
    public interface RetryCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception error);
    }
    
    // Getters
    public int getMaxRetries() { return maxRetries; }
    public long getInitialDelayMs() { return initialDelayMs; }
}
