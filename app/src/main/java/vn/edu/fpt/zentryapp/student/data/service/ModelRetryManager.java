package vn.edu.fpt.zentryapp.student.data.service;

import android.util.Log;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manager for handling retry logic for model operations
 * Provides exponential backoff and graceful error handling
 */
public class ModelRetryManager {
    private static final String TAG = "ModelRetryManager";
    
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;
    private static final long MAX_RETRY_DELAY_MS = 5000;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    private final int maxRetries;
    private final long initialRetryDelayMs;
    private final ExecutorService executor;
    
    public ModelRetryManager() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS);
    }
    
    public ModelRetryManager(int maxRetries, long initialRetryDelayMs) {
        this.maxRetries = maxRetries;
        this.initialRetryDelayMs = initialRetryDelayMs;
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Execute operation with retry logic
     */
    public <T> T executeWithRetry(Supplier<T> operation) throws ModelRetryException {
        return executeWithRetry(operation, maxRetries);
    }
    
    /**
     * Execute operation with custom retry count
     */
    public <T> T executeWithRetry(Supplier<T> operation, int customMaxRetries) throws ModelRetryException {
        Exception lastException = null;
        long currentDelay = initialRetryDelayMs;
        
        for (int attempt = 0; attempt <= customMaxRetries; attempt++) {
            try {
                Log.d(TAG, "Executing operation, attempt " + (attempt + 1) + "/" + (customMaxRetries + 1));
                return operation.get();
                
            } catch (Exception e) {
                lastException = e;
                Log.w(TAG, "Operation failed on attempt " + (attempt + 1) + ": " + e.getMessage());
                
                if (attempt < customMaxRetries) {
                    try {
                        Log.d(TAG, "Retrying in " + currentDelay + "ms");
                        Thread.sleep(currentDelay);
                        currentDelay = Math.min(currentDelay * (long) BACKOFF_MULTIPLIER, MAX_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ModelRetryException("Retry interrupted", ie);
                    }
                }
            }
        }
        
        throw new ModelRetryException("Operation failed after " + (customMaxRetries + 1) + " attempts", lastException);
    }
    
    /**
     * Execute operation asynchronously with retry
     */
    public <T> void executeWithRetryAsync(Supplier<T> operation, 
                                         RetryCallback<T> callback) {
        executor.execute(() -> {
            try {
                T result = executeWithRetry(operation);
                callback.onSuccess(result);
            } catch (ModelRetryException e) {
                callback.onFailure(e);
            }
        });
    }
    
    /**
     * Execute operation with timeout
     */
    public <T> T executeWithTimeout(Supplier<T> operation, long timeoutMs) throws ModelRetryException {
        try {
            return executor.submit(() -> {
                try {
                    return operation.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new ModelRetryException("Operation timed out after " + timeoutMs + "ms", e);
        }
    }
    
    /**
     * Execute operation with health check
     */
    public <T> T executeWithHealthCheck(Supplier<T> operation, 
                                       HealthChecker healthChecker) throws ModelRetryException {
        if (!healthChecker.isHealthy()) {
            throw new ModelRetryException("System is not healthy", null);
        }
        
        return executeWithRetry(operation);
    }
    
    /**
     * Shutdown the retry manager
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Callback interface for async retry operations
     */
    public interface RetryCallback<T> {
        void onSuccess(T result);
        void onFailure(ModelRetryException exception);
    }
    
    /**
     * Health checker interface
     */
    public interface HealthChecker {
        boolean isHealthy();
    }
    
    /**
     * Exception thrown when retry operations fail
     */
    public static class ModelRetryException extends Exception {
        public ModelRetryException(String message) {
            super(message);
        }
        
        public ModelRetryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Builder for creating ModelRetryManager with custom configuration
     */
    public static class Builder {
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private long initialRetryDelayMs = DEFAULT_RETRY_DELAY_MS;
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder initialRetryDelayMs(long initialRetryDelayMs) {
            this.initialRetryDelayMs = initialRetryDelayMs;
            return this;
        }
        
        public ModelRetryManager build() {
            return new ModelRetryManager(maxRetries, initialRetryDelayMs);
        }
    }
} 