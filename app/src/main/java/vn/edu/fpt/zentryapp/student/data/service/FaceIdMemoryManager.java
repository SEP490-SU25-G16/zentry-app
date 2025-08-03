package vn.edu.fpt.zentryapp.student.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🔧 NEW: Memory management for Face ID processing
 * Implements object pooling, memory cleanup, and monitoring
 */
public class FaceIdMemoryManager {
    private static final String TAG = "FaceIdMemoryManager";
    
    private final Context context;
    private final FaceIdConfig.MemoryConfig config;
    
    // Object pools
    private final ConcurrentLinkedQueue<WeakReference<Bitmap>> bitmapPool;
    private final ConcurrentLinkedQueue<WeakReference<Rect>> rectPool;
    
    // Memory monitoring
    private final AtomicLong currentMemoryUsage;
    private final AtomicLong peakMemoryUsage;
    private final AtomicLong totalAllocations;
    private final AtomicLong totalDeallocations;
    
    // Memory cleanup tracking
    private long lastCleanupTime;
    private int cleanupCount;
    
    public FaceIdMemoryManager(Context context, FaceIdConfig.MemoryConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
        this.bitmapPool = new ConcurrentLinkedQueue<>();
        this.rectPool = new ConcurrentLinkedQueue<>();
        this.currentMemoryUsage = new AtomicLong(0);
        this.peakMemoryUsage = new AtomicLong(0);
        this.totalAllocations = new AtomicLong(0);
        this.totalDeallocations = new AtomicLong(0);
        this.lastCleanupTime = System.currentTimeMillis();
        this.cleanupCount = 0;
        
        if (this.config.enableMemoryMonitoring) {
            startMemoryMonitoring();
        }
    }
    
    /**
     * Get a Bitmap from pool or create new one
     */
    public Bitmap acquireBitmap(int width, int height, Bitmap.Config bitmapConfig) {
        WeakReference<Bitmap> ref = bitmapPool.poll();
        Bitmap bitmap = null;
        
        if (ref != null) {
            bitmap = ref.get();
            if (bitmap != null && !bitmap.isRecycled() && 
                bitmap.getWidth() == width && bitmap.getHeight() == height) {
                Log.d(TAG, "Reused bitmap from pool: " + width + "x" + height);
                return bitmap;
            }
        }
        
        // Create new bitmap
        bitmap = Bitmap.createBitmap(width, height, bitmapConfig);
        long memoryUsed = getBitmapMemorySize(bitmap);
        currentMemoryUsage.addAndGet(memoryUsed);
        totalAllocations.incrementAndGet();
        
        updatePeakMemoryUsage();
        
        if (this.config.enableMemoryMonitoring) {
            Log.d(TAG, "Created new bitmap: " + width + "x" + height + 
                  ", memory: " + formatBytes(memoryUsed) + 
                  ", total: " + formatBytes(currentMemoryUsage.get()));
        }
        
        return bitmap;
    }
    
    /**
     * Return a Bitmap to the pool
     */
    public void releaseBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        
        if (bitmapPool.size() < this.config.bitmapPoolSize) {
            bitmapPool.offer(new WeakReference<>(bitmap));
            Log.d(TAG, "Added bitmap to pool, size: " + bitmapPool.size());
        } else {
            // Pool is full, recycle the bitmap
            recycleBitmap(bitmap);
        }
    }
    
    /**
     * Get a Rect from pool or create new one
     */
    public Rect acquireRect() {
        WeakReference<Rect> ref = rectPool.poll();
        Rect rect = null;
        
        if (ref != null) {
            rect = ref.get();
            if (rect != null) {
                Log.d(TAG, "Reused rect from pool");
                return rect;
            }
        }
        
        // Create new rect
        rect = new Rect();
        totalAllocations.incrementAndGet();
        
        if (this.config.enableMemoryMonitoring) {
            Log.d(TAG, "Created new rect, total allocations: " + totalAllocations.get());
        }
        
        return rect;
    }
    
    /**
     * Return a Rect to the pool
     */
    public void releaseRect(Rect rect) {
        if (rect == null) {
            return;
        }
        
        if (rectPool.size() < this.config.rectPoolSize) {
            rectPool.offer(new WeakReference<>(rect));
            Log.d(TAG, "Added rect to pool, size: " + rectPool.size());
        }
        // Rects are lightweight, no need to explicitly recycle
    }
    
    /**
     * Explicitly recycle a bitmap and update memory usage
     */
    public void recycleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        
        long memoryFreed = getBitmapMemorySize(bitmap);
        bitmap.recycle();
        currentMemoryUsage.addAndGet(-memoryFreed);
        totalDeallocations.incrementAndGet();
        
        if (this.config.enableMemoryMonitoring) {
            Log.d(TAG, "Recycled bitmap, freed: " + formatBytes(memoryFreed) + 
                  ", total: " + formatBytes(currentMemoryUsage.get()));
        }
    }
    
    /**
     * Force cleanup of all pooled objects
     */
    public void forceCleanup() {
        Log.d(TAG, "Starting forced cleanup");
        
        // Cleanup bitmap pool
        WeakReference<Bitmap> bitmapRef;
        int recycledBitmaps = 0;
        while ((bitmapRef = bitmapPool.poll()) != null) {
            Bitmap bitmap = bitmapRef.get();
            if (bitmap != null && !bitmap.isRecycled()) {
                recycleBitmap(bitmap);
                recycledBitmaps++;
            }
        }
        
        // Cleanup rect pool
        WeakReference<Rect> rectRef;
        int clearedRects = 0;
        while ((rectRef = rectPool.poll()) != null) {
            clearedRects++;
        }
        
        cleanupCount++;
        lastCleanupTime = System.currentTimeMillis();
        
        Log.d(TAG, "Forced cleanup completed - Recycled bitmaps: " + recycledBitmaps + 
              ", Cleared rects: " + clearedRects + 
              ", Total cleanups: " + cleanupCount);
    }
    
    /**
     * Check if memory usage is within limits
     */
    public boolean isMemoryUsageAcceptable() {
        long currentUsage = currentMemoryUsage.get();
        boolean acceptable = currentUsage <= this.config.maxMemoryUsageBytes;
        
        if (!acceptable && this.config.enableMemoryMonitoring) {
            Log.w(TAG, "Memory usage exceeded limit: " + formatBytes(currentUsage) + 
                  " > " + formatBytes(this.config.maxMemoryUsageBytes));
        }
        
        return acceptable;
    }
    
    /**
     * Get current memory statistics
     */
    public MemoryStats getMemoryStats() {
        return new MemoryStats(
            currentMemoryUsage.get(),
            peakMemoryUsage.get(),
            totalAllocations.get(),
            totalDeallocations.get(),
            bitmapPool.size(),
            rectPool.size(),
            cleanupCount,
            lastCleanupTime
        );
    }
    
    /**
     * Start memory monitoring
     */
    private void startMemoryMonitoring() {
        Log.i(TAG, "Memory monitoring enabled - Max usage: " + formatBytes(this.config.maxMemoryUsageBytes));
    }
    
    /**
     * Update peak memory usage
     */
    private void updatePeakMemoryUsage() {
        long current = currentMemoryUsage.get();
        long peak = peakMemoryUsage.get();
        while (current > peak) {
            if (peakMemoryUsage.compareAndSet(peak, current)) {
                Log.d(TAG, "New peak memory usage: " + formatBytes(current));
                break;
            }
            peak = peakMemoryUsage.get();
        }
    }
    
    /**
     * Calculate memory size of a bitmap
     */
    private long getBitmapMemorySize(Bitmap bitmap) {
        if (bitmap == null) return 0;
        
        int bytesPerPixel;
        switch (bitmap.getConfig()) {
            case ARGB_8888:
                bytesPerPixel = 4;
                break;
            case RGB_565:
                bytesPerPixel = 2;
                break;
            case ARGB_4444:
                bytesPerPixel = 2;
                break;
            case ALPHA_8:
                bytesPerPixel = 1;
                break;
            default:
                bytesPerPixel = 4;
                break;
        }
        
        return (long) bitmap.getWidth() * bitmap.getHeight() * bytesPerPixel;
    }
    
    /**
     * Format bytes to human readable string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Memory statistics class
     */
    public static class MemoryStats {
        public final long currentUsage;
        public final long peakUsage;
        public final long totalAllocations;
        public final long totalDeallocations;
        public final int bitmapPoolSize;
        public final int rectPoolSize;
        public final int cleanupCount;
        public final long lastCleanupTime;
        
        public MemoryStats(long currentUsage, long peakUsage, long totalAllocations,
                         long totalDeallocations, int bitmapPoolSize, int rectPoolSize,
                         int cleanupCount, long lastCleanupTime) {
            this.currentUsage = currentUsage;
            this.peakUsage = peakUsage;
            this.totalAllocations = totalAllocations;
            this.totalDeallocations = totalDeallocations;
            this.bitmapPoolSize = bitmapPoolSize;
            this.rectPoolSize = rectPoolSize;
            this.cleanupCount = cleanupCount;
            this.lastCleanupTime = lastCleanupTime;
        }
        
        @Override
        public String toString() {
            return String.format("MemoryStats{current=%s, peak=%s, allocations=%d, deallocations=%d, " +
                               "bitmapPool=%d, rectPool=%d, cleanups=%d}",
                               formatBytes(currentUsage), formatBytes(peakUsage),
                               totalAllocations, totalDeallocations,
                               bitmapPoolSize, rectPoolSize, cleanupCount);
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + "B";
            if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
            return String.format("%.1fGB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
} 