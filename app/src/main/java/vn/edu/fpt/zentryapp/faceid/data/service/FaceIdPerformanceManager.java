package vn.edu.fpt.zentryapp.faceid.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔧 NEW: Performance optimization for Face ID processing
 * Implements caching, batch processing, and optimized sequential processing
 */
public class FaceIdPerformanceManager {
    private static final String TAG = "FaceIdPerformanceManager";
    
    private final Context context;
    private final FaceIdConfig.PerformanceConfig config;
    
    // Caching system
    private final ConcurrentHashMap<String, CachedResult> resultCache;
    private final AtomicInteger cacheHits;
    private final AtomicInteger cacheMisses;
    
    // Batch processing
    private final List<BatchItem> batchQueue;
    private final ExecutorService batchExecutor;
    private final AtomicInteger batchCount;
    
    // Performance monitoring
    private final AtomicInteger totalProcessedFrames;
    private final AtomicInteger totalProcessingTimeMs;
    private long startTime;
    
    public FaceIdPerformanceManager(Context context, FaceIdConfig.PerformanceConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
        this.resultCache = new ConcurrentHashMap<>();
        this.cacheHits = new AtomicInteger(0);
        this.cacheMisses = new AtomicInteger(0);
        this.batchQueue = new ArrayList<>();
        this.batchExecutor = config.enableParallelProcessing ? 
            Executors.newCachedThreadPool() : Executors.newSingleThreadExecutor();
        this.batchCount = new AtomicInteger(0);
        this.totalProcessedFrames = new AtomicInteger(0);
        this.totalProcessingTimeMs = new AtomicInteger(0);
        this.startTime = System.currentTimeMillis();
        
        // Start cache cleanup thread
        startCacheCleanupThread();
    }
    
    /**
     * Process a single frame with caching
     */
    public CachedResult processFrameWithCache(Bitmap bitmap, String cacheKey) {
        long startTime = System.currentTimeMillis();
        
        // Check cache first
        CachedResult cached = resultCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            cacheHits.incrementAndGet();
            Log.d(TAG, "Cache hit for key: " + cacheKey);
            return cached;
        }
        
        cacheMisses.incrementAndGet();
        Log.d(TAG, "Cache miss for key: " + cacheKey);
        
        // Process frame (this would be the actual face detection logic)
        CachedResult result = new CachedResult(bitmap, System.currentTimeMillis() + config.cacheExpiryMs);
        
        // Cache the result
        resultCache.put(cacheKey, result);
        
        // Update statistics
        long processingTime = System.currentTimeMillis() - startTime;
        totalProcessingTimeMs.addAndGet((int) processingTime);
        totalProcessedFrames.incrementAndGet();
        
        Log.d(TAG, "Processed frame in " + processingTime + "ms, cache size: " + resultCache.size());
        
        return result;
    }
    
    /**
     * Add frame to batch processing queue
     */
    public void addToBatch(Bitmap bitmap, BatchCallback callback) {
        BatchItem item = new BatchItem(bitmap, callback);
        batchQueue.add(item);
        
        // Process batch if it reaches the size limit
        if (batchQueue.size() >= config.batchSize) {
            processBatch();
        }
    }
    
    /**
     * Process the current batch
     */
    public void processBatch() {
        if (batchQueue.isEmpty()) {
            return;
        }
        
        List<BatchItem> currentBatch = new ArrayList<>(batchQueue);
        batchQueue.clear();
        
        if (config.enableBatchProcessing) {
            // Process batch in parallel
            List<Future<BatchResult>> futures = new ArrayList<>();
            
            for (BatchItem item : currentBatch) {
                Future<BatchResult> future = batchExecutor.submit(() -> {
                    long startTime = System.currentTimeMillis();
                    // Simulate processing
                    try {
                        Thread.sleep(50); // Simulate processing time
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    BatchResult result = new BatchResult(item.bitmap, System.currentTimeMillis() - startTime);
                    item.callback.onBatchProcessed(result);
                    return result;
                });
                futures.add(future);
            }
            
            // Wait for all futures to complete
            for (Future<BatchResult> future : futures) {
                try {
                    future.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Log.e(TAG, "Error in batch processing", e);
                }
            }
            
            batchCount.incrementAndGet();
            Log.d(TAG, "Processed batch of " + currentBatch.size() + " items, total batches: " + batchCount.get());
        } else {
            // Process sequentially
            for (BatchItem item : currentBatch) {
                long startTime = System.currentTimeMillis();
                // Simulate processing
                try {
                    Thread.sleep(50); // Simulate processing time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                BatchResult result = new BatchResult(item.bitmap, System.currentTimeMillis() - startTime);
                item.callback.onBatchProcessed(result);
            }
        }
    }
    
    /**
     * Optimized sequential processing with caching
     */
    public void processSequentialOptimized(List<Bitmap> bitmaps, SequentialCallback callback) {
        long batchStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < bitmaps.size(); i++) {
            Bitmap bitmap = bitmaps.get(i);
            String cacheKey = generateCacheKey(bitmap, i);
            
            CachedResult result = processFrameWithCache(bitmap, cacheKey);
            callback.onFrameProcessed(i, result);
        }
        
        long totalTime = System.currentTimeMillis() - batchStartTime;
        Log.d(TAG, "Sequential processing completed: " + bitmaps.size() + " frames in " + totalTime + "ms");
    }
    
    /**
     * Get performance statistics
     */
    public PerformanceStats getPerformanceStats() {
        long uptime = System.currentTimeMillis() - startTime;
        int totalFrames = totalProcessedFrames.get();
        int totalTime = totalProcessingTimeMs.get();
        
        double avgProcessingTime = totalFrames > 0 ? (double) totalTime / totalFrames : 0;
        double framesPerSecond = uptime > 0 ? (totalFrames * 1000.0) / uptime : 0;
        
        return new PerformanceStats(
            totalFrames,
            totalTime,
            avgProcessingTime,
            framesPerSecond,
            cacheHits.get(),
            cacheMisses.get(),
            resultCache.size(),
            batchCount.get(),
            uptime
        );
    }
    
    /**
     * Clear cache
     */
    public void clearCache() {
        int cleared = resultCache.size();
        resultCache.clear();
        Log.d(TAG, "Cleared cache: " + cleared + " items");
    }
    
    /**
     * Shutdown the performance manager
     */
    public void shutdown() {
        batchExecutor.shutdown();
        try {
            if (!batchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        Log.d(TAG, "Performance manager shutdown completed");
    }
    
    /**
     * Start cache cleanup thread
     */
    private void startCacheCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(config.cacheExpiryMs);
                    cleanupExpiredCache();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    /**
     * Cleanup expired cache entries
     */
    private void cleanupExpiredCache() {
        AtomicInteger expiredCount = new AtomicInteger(0);
        long currentTime = System.currentTimeMillis();
        
        resultCache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(currentTime)) {
                expiredCount.incrementAndGet();
                return true;
            }
            return false;
        });
        
        int count = expiredCount.get();
        if (count > 0) {
            Log.d(TAG, "Cleaned up " + count + " expired cache entries");
        }
    }
    
    /**
     * Generate cache key for bitmap
     */
    private String generateCacheKey(Bitmap bitmap, int index) {
        return bitmap.getWidth() + "x" + bitmap.getHeight() + "_" + index + "_" + System.currentTimeMillis();
    }
    
    /**
     * Cached result class
     */
    public static class CachedResult {
        public final Bitmap bitmap;
        public final long expiryTime;
        
        public CachedResult(Bitmap bitmap, long expiryTime) {
            this.bitmap = bitmap;
            this.expiryTime = expiryTime;
        }
        
        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }
        
        public boolean isExpired(long currentTime) {
            return currentTime > expiryTime;
        }
    }
    
    /**
     * Batch item class
     */
    private static class BatchItem {
        public final Bitmap bitmap;
        public final BatchCallback callback;
        
        public BatchItem(Bitmap bitmap, BatchCallback callback) {
            this.bitmap = bitmap;
            this.callback = callback;
        }
    }
    
    /**
     * Batch result class
     */
    public static class BatchResult {
        public final Bitmap bitmap;
        public final long processingTimeMs;
        
        public BatchResult(Bitmap bitmap, long processingTimeMs) {
            this.bitmap = bitmap;
            this.processingTimeMs = processingTimeMs;
        }
    }
    
    /**
     * Batch callback interface
     */
    public interface BatchCallback {
        void onBatchProcessed(BatchResult result);
    }
    
    /**
     * Sequential callback interface
     */
    public interface SequentialCallback {
        void onFrameProcessed(int index, CachedResult result);
    }
    
    /**
     * Performance statistics class
     */
    public static class PerformanceStats {
        public final int totalFrames;
        public final int totalProcessingTimeMs;
        public final double avgProcessingTimeMs;
        public final double framesPerSecond;
        public final int cacheHits;
        public final int cacheMisses;
        public final int cacheSize;
        public final int batchCount;
        public final long uptimeMs;
        
        public PerformanceStats(int totalFrames, int totalProcessingTimeMs, double avgProcessingTimeMs,
                              double framesPerSecond, int cacheHits, int cacheMisses,
                              int cacheSize, int batchCount, long uptimeMs) {
            this.totalFrames = totalFrames;
            this.totalProcessingTimeMs = totalProcessingTimeMs;
            this.avgProcessingTimeMs = avgProcessingTimeMs;
            this.framesPerSecond = framesPerSecond;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.cacheSize = cacheSize;
            this.batchCount = batchCount;
            this.uptimeMs = uptimeMs;
        }
        
        @Override
        public String toString() {
            double cacheHitRate = (cacheHits + cacheMisses) > 0 ? 
                (double) cacheHits / (cacheHits + cacheMisses) * 100 : 0;
            
            return String.format("PerformanceStats{frames=%d, avgTime=%.2fms, fps=%.2f, " +
                               "cacheHitRate=%.1f%%, cacheSize=%d, batches=%d, uptime=%ds}",
                               totalFrames, avgProcessingTimeMs, framesPerSecond,
                               cacheHitRate, cacheSize, batchCount, uptimeMs / 1000);
        }
    }
} 