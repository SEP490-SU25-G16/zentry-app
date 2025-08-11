# GazeEstimator Initialization Issue Analysis

## Problem Summary
GazeEstimator model không được khởi tạo khi startup app, trong khi các model khác (FaceDetector, FaceEmbedding, FaceSpoofDetector) đều được khởi tạo thành công.

## Root Cause Analysis

### 1. Initialization Flow for Other Models
- **FaceDetector, FaceEmbedding, FaceSpoofDetector** được khởi tạo trong:
  - `ZentryApplication.onCreate()` → `preloadFaceIdService()`
  - `FaceIdServiceManager.initialize()` 
  - `FaceIdService.initializeModelsAsync()` (async initialization với CountDownLatch)

### 2. GazeEstimator Initialization Flow
- **GazeEstimator** chỉ được khởi tạo khi:
  - User navigate đến `FaceIdEnhancerActivity`
  - `FaceIdEnhancer` constructor được gọi
  - `gazeEstimator = new GazeEstimator(context, this)` (synchronous initialization)

### 3. Key Differences
1. **Timing**: Other models load during app startup, GazeEstimator loads on-demand
2. **Location**: Other models in FaceIdService, GazeEstimator in FaceIdEnhancer
3. **Pattern**: Other models use async initialization, GazeEstimator uses sync initialization

## Solution Required
1. Integrate GazeEstimator into FaceIdService initialization process
2. Make GazeEstimator initialization asynchronous like other models
3. Update FaceIdService to include GazeEstimator in model loading
4. Ensure GazeEstimator is available when FaceIdEnhancer needs it

## Files to Modify
- `FaceIdService.java` - Add GazeEstimator to initialization
- `GazeEstimator.java` - Make initialization async
- `FaceIdEnhancer.java` - Use GazeEstimator from FaceIdService instead of creating new instance