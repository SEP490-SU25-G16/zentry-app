# Executor Lifecycle Management Fixes

## Issue Analysis

### Root Cause
The app was crashing with `RejectedExecutionException` because:
1. **MediaPipeFaceLandmarkExtractor** was using a `SingleThreadExecutor` that was being shut down
2. The UI was still trying to process frames and call `extractLandmarks()` after the executor was terminated
3. No proper lifecycle management was in place to check if the executor was still active

### Error Details
```
java.util.concurrent.RejectedExecutionException: Task vn.edu.fpt.zentryapp.student.data.service.MediaPipeFaceLandmarkExtractor$$ExternalSyntheticLambda5@2129186 rejected from java.util.concurrent.ThreadPoolExecutor@de2fa47[Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 7]
```

## Fixes Implemented

### 1. MediaPipeFaceLandmarkExtractor.java
**Added proper executor lifecycle management:**

- **Added volatile flag**: `private volatile boolean isExecutorActive = true;`
- **Enhanced extractLandmarks()**: Added checks for executor state before executing tasks
- **Improved close() method**: Proper shutdown with timeout and graceful termination
- **Added isActive() method**: Public method to check if extractor is still usable

**Key Changes:**
```java
// Added executor state tracking
private volatile boolean isExecutorActive = true;

// Enhanced extractLandmarks with state checks
public void extractLandmarks(Bitmap faceBitmap, Rect faceRect, LandmarkExtractionCallback callback) {
    if (!isExecutorActive || executor.isShutdown() || executor.isTerminated()) {
        Log.w(TAG, "Executor is not active, skipping landmark extraction");
        if (callback != null) {
            runOnMainThread(() -> callback.onLandmarksExtracted(false));
        }
        return;
    }
    // ... rest of method
}

// Improved close method with graceful shutdown
public void close() {
    isExecutorActive = false;
    if (executor != null && !executor.isShutdown()) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### 2. FaceIdEnhancer.java
**Added extractor state validation:**

- **Enhanced processFaceFrame()**: Added check for landmarkExtractor.isActive() before use
- **Prevents crashes**: Skips processing if extractor is not active

**Key Changes:**
```java
public void processFaceFrame(Bitmap faceBitmap, Rect faceRect) {
    // Check if landmarkExtractor is still active
    if (landmarkExtractor == null || !landmarkExtractor.isActive()) {
        Log.w(TAG, "LandmarkExtractor is not active, skipping frame processing");
        isProcessing.set(false);
        return;
    }
    // ... rest of method
}
```

### 3. FaceIdService.java
**Added proper resource management:**

- **Added close() method**: Properly closes MediaPipeFaceLandmarkExtractor and other components
- **Resource cleanup**: Ensures all components are properly released

**Key Changes:**
```java
public void close() {
    try {
        Log.d(TAG, "Closing FaceIdService and releasing resources");
        
        // Close MediaPipeFaceLandmarkExtractor
        if (mediaPipeFaceLandmarkExtractor != null) {
            mediaPipeFaceLandmarkExtractor.close();
            mediaPipeFaceLandmarkExtractor = null;
        }
        
        // Close other components if they have close methods
        // ... additional cleanup code
    } catch (Exception e) {
        Log.e(TAG, "Error closing FaceIdService", e);
    }
}
```

### 4. StudentSettingRegisterFaceIdFragment.java
**Enhanced fragment lifecycle management:**

- **Updated onDestroyView()**: Added proper cleanup of FaceIdService
- **Resource management**: Ensures FaceIdService is closed when fragment is destroyed

**Key Changes:**
```java
@Override
public void onDestroyView() {
    super.onDestroyView();
    
    // ... existing cleanup code
    
    // Close FaceIdService to properly release MediaPipeFaceLandmarkExtractor
    if (faceIdService != null) {
        faceIdService.close();
        faceIdService = null;
    }
    
    // ... rest of cleanup
}
```

## Technical Details

### Executor State Management
- **Volatile flag**: Ensures thread-safe access to executor state
- **Double-checking**: Both before and during task execution
- **Graceful shutdown**: 2-second timeout for normal shutdown, 1-second for forced shutdown

### Error Prevention
- **State validation**: All methods check if executor is active before use
- **Null checks**: Comprehensive null and state checking
- **Logging**: Detailed logging for debugging and monitoring

### Resource Cleanup
- **Proper shutdown**: All components are properly closed
- **Memory management**: Resources are released to prevent memory leaks
- **Lifecycle awareness**: Components respect Android lifecycle events

## Impact
These fixes ensure that:
1. **No more crashes**: Executor rejection exceptions are prevented
2. **Proper cleanup**: All resources are properly released
3. **Better performance**: No memory leaks from unclosed resources
4. **Improved stability**: App handles lifecycle changes gracefully
5. **Better debugging**: Comprehensive logging for troubleshooting

## Testing Recommendations
1. Test face registration flow multiple times
2. Test app backgrounding/foregrounding during face processing
3. Test rapid navigation between screens
4. Monitor memory usage during extended use
5. Verify no crashes occur during normal operation