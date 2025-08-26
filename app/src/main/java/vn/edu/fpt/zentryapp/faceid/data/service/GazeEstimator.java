package vn.edu.fpt.zentryapp.faceid.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Gaze direction estimator using TensorFlow Lite iTracker model
 * Based on the iTracker architecture with eye regions and face grid
 */
public class GazeEstimator {
    private static final String TAG = "GazeEstimator";

    // TensorFlow Lite model parameters
    private static final String MODEL_FILE = "itracker_adv_fp32.tflite";
    private static final int INPUT_SIZE = 64; // Model input size (square) for eyes and face
    private static final int FACE_GRID_SIZE = 25; // Face grid size (25x25)
    private static final int FLOAT_BYTES = 4; // Size of float in bytes

    // Input tensor indices for iTracker model
    private int eyeLeftInputIndex = -1;
    private int eyeRightInputIndex = -1;
    private int faceInputIndex = -1;
    private int faceMaskInputIndex = -1;
    
    // Map to store all input tensor indices by name
    private Map<String, Integer> inputTensorIndices = new HashMap<>();

    // Interpreter and associated objects
    private Interpreter interpreter;
    private float[][] outputBuffer; // [1][2] - x,y gaze coordinates (range depends on model)

    // Landmark indices for eye cropping (MediaPipe Face Mesh)
    private static final int[] LEFT_EYE_LANDMARKS = {33, 133, 159, 145};  // outer, inner, top, bottom
    private static final int[] RIGHT_EYE_LANDMARKS = {362, 263, 386, 374}; // outer, inner, top, bottom

    // Last estimated gaze coordinates: -1 (left) to 1 (right), -1 (up) to 1 (down)
    private float gazeX = 0;
    private float gazeY = 0;
    
    // Previous gaze for delta calculation
    private float prevGazeX = 0;
    private float prevGazeY = 0;

    // Tracking state
    private boolean isLookingAway = false;
    private boolean isLookingAtScreen = false;
    private int lookingAwayFrames = 0;
    private static final int LOOKING_AWAY_THRESHOLD = 5; // Frames threshold for looking away

    // For tracking gaze stability
    private float[] gazeHistory = new float[10]; // Last 10 gaze positions (magnitude)
    private int historyIndex = 0;

    // Camera/coordinate config
    private boolean frontCameraMirrored = true;

    // Post-process smoothing
    private float emaGazeX = 0f;
    private float emaGazeY = 0f;
    private float emaAlpha = 0.8f; // smoothing factor - higher alpha = more responsive

    // Head pose blending weight
    private float headPoseWeight = 0.0f; // DISABLED: was causing bias when head slightly turned

    // Bias correction for model output
    private float gazeXBias = 0.25f; // Subtract this from raw gazeX to center around 0
    private float gazeYBias = 0.0f;  // Y bias if needed
    
    // Calibration system
    private boolean isCalibrated = false;
    private float calibrationGazeX = 0.0f; // Baseline when looking straight
    private int calibrationSamples = 0;
    private static final int REQUIRED_CALIBRATION_SAMPLES = 10;

    /**
     * Callback interface for gaze events
     */
    public interface GazeCallback {
        void onGazeUpdate(float x, float y, boolean isLookingAtScreen);

        void onLookingAway(boolean isLookingAway);
    }

    private GazeCallback callback;

    // Async initialization support
    private volatile boolean isInitialized = false;
    private final CountDownLatch initLatch = new CountDownLatch(1);
    private final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Creates a new gaze estimator
     *
     * @param context  Application context
     * @param callback Callback for gaze events
     */
    public GazeEstimator(Context context, GazeCallback callback) {
        this.callback = callback;

        Log.d(TAG, "Starting GazeEstimator initialization...");

        // Initialize asynchronously
        executor.execute(() -> {
            try {
                // Initialize output buffer
                outputBuffer = new float[1][2]; // x,y gaze direction

                Log.d(TAG, "Buffers initialized, loading TensorFlow Lite model...");

                // Initialize TensorFlow Lite with the iTracker model
                initializeTFLite(context);

                // Only set as initialized if interpreter is not null
                isInitialized = (interpreter != null);

                if (isInitialized) {
                    Log.d(TAG, "Gaze estimator initialized successfully with model: " + MODEL_FILE);
                } else {
                    Log.w(TAG, "Gaze estimator initialization incomplete - interpreter is null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing gaze estimator: " + e.getMessage(), e);
                // Fallback to simulate mode if model loading fails
                outputBuffer = new float[1][2];
                Log.w(TAG, "Falling back to simulated mode due to initialization error");
            } finally {
                initLatch.countDown();
            }
        });
    }

    /**
     * Check if the gaze estimator is initialized
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Wait for initialization to complete
     *
     * @param timeoutMs timeout in milliseconds
     * @throws InterruptedException if interrupted
     */
    public void awaitInitialization(long timeoutMs) throws InterruptedException {
        initLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Set the callback for gaze events
     *
     * @param callback The callback to set
     */
    public void setCallback(GazeCallback callback) {
        this.callback = callback;
    }

    public void setFrontCameraMirrored(boolean mirrored) {
        this.frontCameraMirrored = mirrored;
    }

    public void setHeadPoseWeight(float weight) {
        this.headPoseWeight = Math.max(0f, Math.min(1f, weight));
    }

    /**
     * Inspect the model structure to understand input and output tensors
     */
    private void inspectModel() {
        if (interpreter == null) {
            Log.e(TAG, "Cannot inspect model: interpreter is null");
            return;
        }

        try {
            int inputTensorCount = interpreter.getInputTensorCount();
            Log.d(TAG, "Input tensor count: " + inputTensorCount);

            for (int i = 0; i < inputTensorCount; i++) {
                int[] shape = interpreter.getInputTensor(i).shape();
                String shapeStr = Arrays.toString(shape);
                String tensorName = interpreter.getInputTensor(i).name();

                // Store all tensor indices in the map
                inputTensorIndices.put(tensorName, i);
                
                // Map tensor names to indices for iTracker model
                // Primary names from instruction.md specification
                if (tensorName.equals("eye_left")) {
                    eyeLeftInputIndex = i;
                    Log.d(TAG, "Found eye_left tensor at index " + i);
                } else if (tensorName.equals("eye_right")) {
                    eyeRightInputIndex = i;
                    Log.d(TAG, "Found eye_right tensor at index " + i);
                } else if (tensorName.equals("face")) {
                    faceInputIndex = i;
                    Log.d(TAG, "Found face tensor at index " + i);
                } else if (tensorName.equals("face_mask")) {
                    faceMaskInputIndex = i;
                    Log.d(TAG, "Found face_mask tensor at index " + i);
                } 
                // Fallback names (in case model uses different naming)
                else if (tensorName.toLowerCase().contains("eye") && tensorName.toLowerCase().contains("left")) {
                    eyeLeftInputIndex = i;
                    Log.d(TAG, "Found eye_left tensor (fallback) at index " + i + " with name: " + tensorName);
                } else if (tensorName.toLowerCase().contains("eye") && tensorName.toLowerCase().contains("right")) {
                    eyeRightInputIndex = i;
                    Log.d(TAG, "Found eye_right tensor (fallback) at index " + i + " with name: " + tensorName);
                } else if (tensorName.toLowerCase().contains("face") && !tensorName.toLowerCase().contains("mask")) {
                    faceInputIndex = i;
                    Log.d(TAG, "Found face tensor (fallback) at index " + i + " with name: " + tensorName);
                } else if (tensorName.toLowerCase().contains("mask") || tensorName.toLowerCase().contains("grid")) {
                    faceMaskInputIndex = i;
                    Log.d(TAG, "Found face_mask tensor (fallback) at index " + i + " with name: " + tensorName);
                } else {
                    Log.d(TAG, "Unknown tensor name: " + tensorName + " at index " + i);
                }

                Log.d(TAG, "Input tensor " + i + " name: " + tensorName + ", shape: " + shapeStr);
                Log.d(TAG, "Input tensor " + i + " dataType: " + interpreter.getInputTensor(i).dataType());
            }

            int outputTensorCount = interpreter.getOutputTensorCount();
            Log.d(TAG, "Output tensor count: " + outputTensorCount);

            for (int i = 0; i < outputTensorCount; i++) {
                int[] shape = interpreter.getOutputTensor(i).shape();
                String shapeStr = Arrays.toString(shape);
                String tensorName = interpreter.getOutputTensor(i).name();
                Log.d(TAG, "Output tensor " + i + " name: " + tensorName + ", shape: " + shapeStr);
                Log.d(TAG, "Output tensor " + i + " dataType: " + interpreter.getOutputTensor(i).dataType());
            }
            
            // Validate that all required tensors were found
            boolean allTensorsFound = (eyeLeftInputIndex >= 0 && eyeRightInputIndex >= 0 && 
                                     faceInputIndex >= 0 && faceMaskInputIndex >= 0);
            
            if (!allTensorsFound) {
                Log.e(TAG, String.format("Missing required input tensors! eyeLeft=%d, eyeRight=%d, face=%d, faceMask=%d",
                    eyeLeftInputIndex, eyeRightInputIndex, faceInputIndex, faceMaskInputIndex));
                Log.e(TAG, "Available tensor names: " + inputTensorIndices.keySet());
                // Don't set initialized if tensors are missing
                return;
            }
            
            Log.d(TAG, "All required iTracker input tensors found successfully!");
        } catch (Exception e) {
            Log.e(TAG, "Error inspecting model: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize TensorFlow Lite interpreter with the gaze model
     */
    private void initializeTFLite(Context context) throws IOException {
        Log.d(TAG, "Loading model file from assets...");

        // Load model from assets
        ByteBuffer modelBuffer = loadModelFile(context);

        Log.d(TAG, "Setting up interpreter options with TFLiteGpuDelegateManager...");

        // Set up interpreter options
        Interpreter.Options options = new Interpreter.Options();

        try {
            // Try to use GPU delegate manager
            options = TFLiteGpuDelegateManager.getInstance().getInterpreterOptions();
            Log.d(TAG, "Using GPU delegate manager for acceleration");
        } catch (Exception e) {
            Log.w(TAG, "Could not use GPU delegate manager: " + e.getMessage());
            // If GPU delegate manager fails, try direct GPU delegate
            try {
                CompatibilityList compatList = new CompatibilityList();
                if (compatList.isDelegateSupportedOnThisDevice()) {
                    GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
                    GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
                    options.addDelegate(gpuDelegate);
                    Log.d(TAG, "Using direct GPU delegate for acceleration");
                } else {
                    options.setNumThreads(4); // Use 4 threads on CPU
                    Log.d(TAG, "Using CPU for model inference with 4 threads");
                }
            } catch (Exception ex) {
                Log.w(TAG, "Could not use GPU directly: " + ex.getMessage());
                options.setNumThreads(4); // Use 4 threads on CPU
                Log.d(TAG, "Falling back to CPU with 4 threads");
            }
        }

        Log.d(TAG, "Creating TensorFlow Lite interpreter...");

        // Create the interpreter
        interpreter = new Interpreter(modelBuffer, options);

        Log.d(TAG, "TensorFlow Lite interpreter initialized successfully");

        // Inspect model structure
        inspectModel();
    }

    /**
     * Load the TensorFlow Lite model file from assets
     */
    private ByteBuffer loadModelFile(Context context) throws IOException {
        Log.d(TAG, "Attempting to load model: " + MODEL_FILE + " from assets");

        // Get file descriptor for the model file in assets
        try (java.io.InputStream is = context.getAssets().open(MODEL_FILE)) {
            // Get size of the model file
            int modelSize = is.available();
            Log.d(TAG, "Model file size: " + modelSize + " bytes");

            ByteBuffer modelBuffer = ByteBuffer.allocateDirect(modelSize);
            modelBuffer.order(ByteOrder.nativeOrder());

            // Read model into ByteBuffer
            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalRead = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                modelBuffer.put(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            modelBuffer.rewind();

            Log.d(TAG, "Model file loaded successfully: " + MODEL_FILE + " (" + totalRead + "/" + modelSize + " bytes read)");
            return modelBuffer;
        } catch (IOException e) {
            Log.e(TAG, "Error loading model file from assets: " + MODEL_FILE, e);
            throw e;
        }
    }

    /**
     * Estimate gaze direction from face image with landmarks
     *
     * @param faceImage  Full face image bitmap
     * @param landmarks  List of face landmarks (MediaPipe format)
     * @param headPose   Head pose angles [pitch, roll, yaw]
     * @return True if gaze was successfully estimated
     */
    public boolean estimateGaze(Bitmap faceImage, List<PointF> landmarks, float[] headPose) {
        try {
            if (!isInitialized) {
                Log.w(TAG, "TensorFlow Lite interpreter not initialized, falling back to simulation");
                return simulateGazeEstimation(headPose);
            }

            // Check if input image and landmarks are valid
            if (faceImage == null || landmarks == null || landmarks.size() < 468) {
                Log.w(TAG, "Face image or landmarks are invalid, falling back to simulation");
                return simulateGazeEstimation(headPose);
            }

            // Check if interpreter is null
            if (interpreter == null) {
                Log.w(TAG, "TensorFlow Lite interpreter is null, falling back to simulation");
                return simulateGazeEstimation(headPose);
            }

            int imgW = faceImage.getWidth();
            int imgH = faceImage.getHeight();

            // Extract ROI regions from landmarks
            RectF leftEyeRect = rectFromLandmarks(landmarks, imgW, imgH, LEFT_EYE_LANDMARKS, 2.2f);
            RectF rightEyeRect = rectFromLandmarks(landmarks, imgW, imgH, RIGHT_EYE_LANDMARKS, 2.2f);
            RectF faceRect = faceRect(landmarks, imgW, imgH, 1.3f);

            // Crop and resize regions
            Bitmap leftEyeCrop = cropResize(faceImage, leftEyeRect, INPUT_SIZE, INPUT_SIZE);
            Bitmap rightEyeCrop = cropResize(faceImage, rightEyeRect, INPUT_SIZE, INPUT_SIZE);
            Bitmap faceCrop = cropResize(faceImage, faceRect, INPUT_SIZE, INPUT_SIZE);

            // Create face grid (25x25)
            float[] faceGrid = makeFaceGrid(faceRect, imgW, imgH, FACE_GRID_SIZE);

            // Run gaze estimation
            float[] gazeResult = runGaze(leftEyeCrop, rightEyeCrop, faceCrop, faceGrid);

            if (gazeResult != null && gazeResult.length >= 2) {
                // Store previous gaze for delta calculation
                prevGazeX = gazeX;
                prevGazeY = gazeY;
                
                // Extract gaze coordinates from the output
                gazeX = gazeResult[0];
                gazeY = gazeResult[1];

                // Calculate delta for debugging
                float deltaX = gazeX - prevGazeX;
                float deltaY = gazeY - prevGazeY;
                float deltaMagnitude = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                
                // Log inference results and delta
                Log.d(TAG, String.format("iTracker inference SUCCESS: gaze(%.3f, %.3f), Δ(%.3f, %.3f), |Δ|:%.3f", 
                      gazeX, gazeY, deltaX, deltaY, deltaMagnitude));

                Log.d(TAG, String.format("PRE-PROCESSING: raw_model_output=(%.4f, %.4f)", gazeX, gazeY));

                // Apply bias correction BEFORE any other processing
                float rawGazeX = gazeX;
                float rawGazeY = gazeY;
                gazeX = gazeX - gazeXBias;  // Center around 0
                gazeY = gazeY - gazeYBias;
                
                Log.d(TAG, String.format("BIAS CORRECTION: raw=(%.4f,%.4f) → corrected=(%.4f,%.4f), bias=(%.4f,%.4f)", 
                      rawGazeX, rawGazeY, gazeX, gazeY, gazeXBias, gazeYBias));

                // Apply front camera mirroring if needed
                if (frontCameraMirrored) {
                    gazeX = -gazeX;
                    Log.d(TAG, "Applied front camera mirroring: gazeX = " + gazeX);
                }

                // Adjust gaze based on head pose
                if (headPose != null && headPose.length >= 3) {
                    float preAdjustX = gazeX, preAdjustY = gazeY;
                    adjustGazeWithHeadPose(headPose);
                    Log.d(TAG, String.format("Head pose adjustment: (%.3f,%.3f) → (%.3f,%.3f)", 
                          preAdjustX, preAdjustY, gazeX, gazeY));
                }

                // Smooth gaze with EMA to stabilize thresholds
                float preEmaX = emaGazeX, preEmaY = emaGazeY;
                emaGazeX = emaAlpha * gazeX + (1 - emaAlpha) * emaGazeX;
                emaGazeY = emaAlpha * gazeY + (1 - emaAlpha) * emaGazeY;
                
                // *** GAZE DIRECTION CLASSIFICATION DEBUG ***
                String gazeDirection = getGazeDirection(0.08f);
                String emaGazeDirection = getGazeDirectionFromValues(emaGazeX, emaGazeY, 0.08f);
                
                Log.i(TAG, "=== GAZE DIRECTION DEBUG ===");
                Log.i(TAG, String.format("Raw gaze: (%.4f, %.4f) → Direction: %s", gazeX, gazeY, gazeDirection));
                Log.i(TAG, String.format("EMA smoothed: (%.4f, %.4f) → Direction: %s", emaGazeX, emaGazeY, emaGazeDirection));
                Log.i(TAG, String.format("EMA change: (%.4f,%.4f) → (%.4f,%.4f)", preEmaX, preEmaY, emaGazeX, emaGazeY));
                Log.i(TAG, String.format("Threshold τ=0.08: LEFT≤%.3f, CENTER<%.3f, RIGHT≥%.3f", -0.08f, 0.08f, 0.08f));
                Log.i(TAG, String.format("Classification logic: gazeX=%.4f → %s", gazeX, 
                      (gazeX >= 0.08f) ? "RIGHT" : (gazeX <= -0.08f) ? "LEFT" : "CENTER"));
                Log.i(TAG, "============================");

                // Update gaze history
                updateGazeHistory();

                // Check if looking away from the screen
                checkLookingAway();

                // Log the result with direction classification
                String currentDirection = getGazeDirectionFromValues(emaGazeX, emaGazeY, 0.08f);
                Log.i(TAG, String.format("FINAL RESULT: Direction=%s, EMA=(%.3f,%.3f), LookingAtScreen=%b", 
                      currentDirection, emaGazeX, emaGazeY, isLookingAtScreen));

                // Notify callback if available (use smoothed values)
                if (callback != null) {
                    Log.d(TAG, String.format("Sending to callback: gaze=(%.3f,%.3f), direction=%s, lookingAtScreen=%b", 
                          emaGazeX, emaGazeY, currentDirection, isLookingAtScreen));
                    callback.onGazeUpdate(emaGazeX, emaGazeY, isLookingAtScreen);
                    callback.onLookingAway(isLookingAway);
                }

                // Clean up cropped bitmaps
                if (leftEyeCrop != null) leftEyeCrop.recycle();
                if (rightEyeCrop != null) rightEyeCrop.recycle();
                if (faceCrop != null) faceCrop.recycle();

                return true;
            } else {
                Log.w(TAG, "iTracker model inference FAILED - runGaze() returned null or invalid result");
                Log.w(TAG, "Falling back to head pose simulation...");
                return simulateGazeEstimation(headPose);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during model inference", e);
            // Fall back to simulation if model inference fails
            return simulateGazeEstimation(headPose);
        }
    }

    /**
     * Legacy method - Now simplified to detect "looking at camera" only
     * HEAD DIRECTION (LEFT/RIGHT) should be handled by HeadPoseEstimation class
     */
    public boolean estimateGaze(Bitmap leftEyeImage, Bitmap rightEyeImage, float[] headPose) {
        Log.d(TAG, "GazeEstimator: Detecting if user is looking AT CAMERA (not direction)");
        
        if (headPose != null && headPose.length >= 3) {
            float pitch = headPose[0];
            float roll = headPose[1]; 
            float yaw = headPose[2];
            
            // Simple "looking at camera" detection based on eye openness and head pose
            boolean lookingAtCamera = isLookingAtCamera(pitch, yaw, leftEyeImage, rightEyeImage);
            
            // Set gaze to center when looking at camera, otherwise indicate looking away
            if (lookingAtCamera) {
                gazeX = 0.0f;  // Always center for "looking at camera"
                gazeY = 0.0f;
                isLookingAtScreen = true;
                isLookingAway = false;
            } else {
                gazeX = 0.0f;  // Direction is not relevant here
                gazeY = 0.0f; 
                isLookingAtScreen = false;
                isLookingAway = true;
            }
            
            // Apply smoothing
            emaGazeX = emaAlpha * gazeX + (1 - emaAlpha) * emaGazeX;
            emaGazeY = emaAlpha * gazeY + (1 - emaAlpha) * emaGazeY;
            
            Log.i(TAG, "=== GAZE CAMERA DETECTION ===");
            Log.i(TAG, String.format("Head pose: pitch=%.1f°, yaw=%.1f°, roll=%.1f°", pitch, yaw, roll));
            Log.i(TAG, String.format("Looking at camera: %b", lookingAtCamera));
            Log.i(TAG, String.format("Looking at screen: %b", isLookingAtScreen));
            Log.i(TAG, "==============================");
            
            // Notify callback
            if (callback != null) {
                Log.d(TAG, String.format("Sending camera detection result: lookingAtCamera=%b", lookingAtCamera));
                callback.onGazeUpdate(emaGazeX, emaGazeY, isLookingAtScreen);
                callback.onLookingAway(isLookingAway);
            }
            
            return true;
        }
        
        Log.w(TAG, "No head pose data for camera detection");
        return false;
    }
    
    /**
     * Determine if user is looking at camera based on head pose and eye analysis
     * @param pitch head pitch angle
     * @param yaw head yaw angle  
     * @param leftEye left eye image
     * @param rightEye right eye image
     * @return true if looking at camera
     */
    private boolean isLookingAtCamera(float pitch, float yaw, Bitmap leftEye, Bitmap rightEye) {
        // Head should be approximately facing forward
        boolean headFacingForward = Math.abs(yaw) < 20.0f && Math.abs(pitch) < 15.0f;
        
        // Eyes should be open and visible
        boolean eyesOpen = (leftEye != null && rightEye != null);
        
        // Additional checks could include:
        // - Eye aspect ratio analysis
        // - Pupil detection
        // - Iris center analysis
        
        boolean lookingAtCamera = headFacingForward && eyesOpen;
        
        Log.d(TAG, String.format("Camera detection: headForward=%b (yaw=%.1f, pitch=%.1f), eyesOpen=%b → result=%b", 
              headFacingForward, yaw, pitch, eyesOpen, lookingAtCamera));
              
        return lookingAtCamera;
    }

    /**
     * Create ROI rectangle from landmarks
     */
    private RectF rectFromLandmarks(List<PointF> landmarks, int w, int h, int[] indices, float expand) {
        float minX = 1f, minY = 1f, maxX = 0f, maxY = 0f;
        
        for (int id : indices) {
            if (id >= 0 && id < landmarks.size()) {
                PointF lm = landmarks.get(id);
                minX = Math.min(minX, lm.x);
                minY = Math.min(minY, lm.y);
                maxX = Math.max(maxX, lm.x);
                maxY = Math.max(maxY, lm.y);
            }
        }
        
        float cx = (minX + maxX) / 2f;
        float cy = (minY + maxY) / 2f;
        float half = Math.max((maxX - minX), (maxY - minY)) * 0.5f * expand;
        
        RectF r = new RectF((cx - half) * w, (cy - half) * h, (cx + half) * w, (cy + half) * h);
        r.left = Math.max(0, r.left);
        r.top = Math.max(0, r.top);
        r.right = Math.min(w - 1, r.right);
        r.bottom = Math.min(h - 1, r.bottom);
        
        return r;
    }

    /**
     * Create face ROI rectangle from all landmarks
     */
    private RectF faceRect(List<PointF> landmarks, int w, int h, float expand) {
        float minX = 1f, minY = 1f, maxX = 0f, maxY = 0f;
        
        for (PointF lm : landmarks) {
            minX = Math.min(minX, lm.x);
            minY = Math.min(minY, lm.y);
            maxX = Math.max(maxX, lm.x);
            maxY = Math.max(maxY, lm.y);
        }
        
        float cx = (minX + maxX) / 2f;
        float cy = (minY + maxY) / 2f;
        float half = Math.max((maxX - minX), (maxY - minY)) * 0.5f * expand;
        
        RectF r = new RectF((cx - half) * w, (cy - half) * h, (cx + half) * w, (cy + half) * h);
        r.left = Math.max(0, r.left);
        r.top = Math.max(0, r.top);
        r.right = Math.min(w - 1, r.right);
        r.bottom = Math.min(h - 1, r.bottom);
        
        return r;
    }

    /**
     * Crop and resize image region
     */
    private Bitmap cropResize(Bitmap src, RectF rect, int outW, int outH) {
        Rect srcRect = new Rect(
            Math.round(rect.left),
            Math.round(rect.top),
            Math.round(rect.right),
            Math.round(rect.bottom)
        );
        
        Bitmap dst = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, srcRect, new Rect(0, 0, outW, outH), null);
        
        return dst;
    }

    /**
     * Create face grid (25x25 -> 625 elements)
     */
    private float[] makeFaceGrid(RectF faceRectPx, int imgW, int imgH, int gridN) {
        float[] grid = new float[gridN * gridN];
        float cellW = imgW / (float) gridN;
        float cellH = imgH / (float) gridN;
        
        for (int gy = 0; gy < gridN; gy++) {
            for (int gx = 0; gx < gridN; gx++) {
                float x0 = gx * cellW;
                float y0 = gy * cellH;
                RectF cell = new RectF(x0, y0, x0 + cellW, y0 + cellH);
                RectF inter = new RectF();
                
                if (inter.setIntersect(faceRectPx, cell)) {
                    grid[gy * gridN + gx] = 1f;
                }
            }
        }
        
        return grid;
    }

    /**
     * Convert bitmap to NHWC float32 format [0..1]
     */
    private ByteBuffer toNHWCFloat32(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * w * h * 3).order(ByteOrder.nativeOrder());
        
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = pixels[y * w + x];
                // Normalize to [0..1] (not [-1..1] as in old model)
                buffer.putFloat(((c >> 16) & 0xFF) / 255f); // R
                buffer.putFloat(((c >> 8) & 0xFF) / 255f);  // G
                buffer.putFloat((c & 0xFF) / 255f);         // B
            }
        }
        
        buffer.rewind();
        return buffer;
    }

    /**
     * Run gaze estimation with iTracker model
     * Input specification from instruction.md:
     * - eye_left: [1,64,64,3] NHWC float32 [0..1]  
     * - eye_right: [1,64,64,3] NHWC float32 [0..1]
     * - face: [1,64,64,3] NHWC float32 [0..1] 
     * - face_mask: [1,625] face grid 25x25 flattened
     * Output: Add_5: [1,2] → (gaze_x, gaze_y)
     */
    private float[] runGaze(Bitmap eyeLeft64, Bitmap eyeRight64, Bitmap face64, float[] faceGrid625) {
        try {
            // Log input validation
            Log.d(TAG, String.format("runGaze inputs: eyeLeft=%dx%d, eyeRight=%dx%d, face=%dx%d, faceGrid.length=%d",
                eyeLeft64.getWidth(), eyeLeft64.getHeight(),
                eyeRight64.getWidth(), eyeRight64.getHeight(), 
                face64.getWidth(), face64.getHeight(),
                faceGrid625.length));
                
            // Convert bitmaps to input tensors (RGB [0..1], NHWC format)
            ByteBuffer inLeft = toNHWCFloat32(eyeLeft64);
            ByteBuffer inRight = toNHWCFloat32(eyeRight64);
            ByteBuffer inFace = toNHWCFloat32(face64);
            
            // Convert face grid to ByteBuffer
            ByteBuffer inGrid = ByteBuffer.allocateDirect(4 * 625).order(ByteOrder.nativeOrder());
            for (float v : faceGrid625) {
                inGrid.putFloat(v);
            }
            inGrid.rewind();

            // Validate tensor indices are found
            if (eyeLeftInputIndex < 0 || eyeRightInputIndex < 0 || 
                faceInputIndex < 0 || faceMaskInputIndex < 0) {
                Log.e(TAG, String.format("Missing tensor indices: eyeLeft=%d, eyeRight=%d, face=%d, faceMask=%d",
                    eyeLeftInputIndex, eyeRightInputIndex, faceInputIndex, faceMaskInputIndex));
                return null;
            }

            // Prepare inputs array - map by tensor indices found during model inspection
            Object[] inputs = new Object[interpreter.getInputTensorCount()];
            
            // Map inputs to correct tensor indices (based on iTracker specification)
            inputs[eyeLeftInputIndex] = inLeft;    // eye_left: [1,64,64,3]
            inputs[eyeRightInputIndex] = inRight;  // eye_right: [1,64,64,3]  
            inputs[faceInputIndex] = inFace;       // face: [1,64,64,3]
            inputs[faceMaskInputIndex] = inGrid;   // face_mask: [1,625]
            
            Log.d(TAG, String.format("Mapped inputs: eyeLeft→%d, eyeRight→%d, face→%d, faceMask→%d",
                eyeLeftInputIndex, eyeRightInputIndex, faceInputIndex, faceMaskInputIndex));

            // Prepare outputs
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, outputBuffer);

            // Run inference
            interpreter.runForMultipleInputsOutputs(inputs, outputs);
            
            // Log output for debugging
            float[] result = outputBuffer[0];
            Log.d(TAG, String.format("Model output: gaze_x=%.4f, gaze_y=%.4f", result[0], result[1]));
            
            return result; // [gaze_x, gaze_y]
            
        } catch (Exception e) {
            Log.e(TAG, "Error running gaze inference: " + e.getMessage(), e);
            return null;
        }
    }
    /**
     * Simulate gaze estimation when model inference is not possible
     */
    private boolean simulateGazeEstimation(float[] headPose) {
        try {
            // Simulate changing gaze direction based on head pose
            if (headPose != null && headPose.length >= 3) {
                // Use head pose to influence gaze direction
                float yaw = headPose[2]; // Left/right head rotation
                float pitch = headPose[0]; // Up/down head rotation

                // Map head rotation to gaze coordinates
                // Normalize to range [-1, 1]
                gazeX = Math.max(-1.0f, Math.min(1.0f, yaw / 45.0f));
                gazeY = Math.max(-1.0f, Math.min(1.0f, pitch / 30.0f));
            } else {
                // Simulate changing gaze by moving slightly in random directions
                float deltaX = (float) (Math.random() * 0.1 - 0.05);
                float deltaY = (float) (Math.random() * 0.1 - 0.05);

                gazeX = Math.max(-1.0f, Math.min(1.0f, gazeX + deltaX));
                gazeY = Math.max(-1.0f, Math.min(1.0f, gazeY + deltaY));
            }

            // Update gaze history
            updateGazeHistory();

            // Check if looking away from the screen
            checkLookingAway();

            // Log the simulation result with direction
            String simDirection = getGazeDirectionFromValues(gazeX, gazeY, 0.08f);
            Log.w(TAG, "=== SIMULATION MODE DEBUG ===");
            Log.w(TAG, String.format("Simulated gaze: (%.3f, %.3f) → Direction: %s", gazeX, gazeY, simDirection));
            Log.w(TAG, String.format("Head pose: %s", headPose != null ? 
                  String.format("[%.1f, %.1f, %.1f]", headPose[0], headPose[1], headPose[2]) : "null"));
            Log.w(TAG, String.format("Looking at screen: %b", isLookingAtScreen));
            Log.w(TAG, "==============================");

            // Notify callback if available
            if (callback != null) {
                Log.d(TAG, String.format("Sending simulated data to callback: direction=%s", simDirection));
                callback.onGazeUpdate(gazeX, gazeY, isLookingAtScreen);
                callback.onLookingAway(isLookingAway);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error during gaze simulation", e);
            return false;
        }
    }

    /**
     * Adjust the estimated gaze direction based on head pose
     */
    private void adjustGazeWithHeadPose(float[] headPose) {
        if (headPose == null || headPose.length < 3) {
            return;
        }

        // Extract head pose angles in degrees
        float pitch = headPose[0]; // Up/down rotation
        float roll = headPose[1];  // Tilt left/right
        float yaw = headPose[2];   // Left/right rotation

        // Convert degrees to normalized range (-1 to 1)
        float normYaw = yaw / 45.0f;    // Normalize by typical max angle
        float normPitch = pitch / 30.0f; // Normalize by typical max angle

        // Limit to range [-1, 1]
        normYaw = Math.max(-1.0f, Math.min(1.0f, normYaw));
        normPitch = Math.max(-1.0f, Math.min(1.0f, normPitch));

        // Adjust gaze by adding scaled head pose influence
        // Head yaw affects horizontal gaze, head pitch affects vertical gaze
        float headWeight = headPoseWeight; // configurable weight

        // If eyes are likely closed/occluded (based on large |normYaw|), reduce head influence further
        if (Math.abs(normYaw) > 0.8f) {
            headWeight *= 0.5f;
        }

        gazeX = gazeX * (1 - headWeight) + normYaw * headWeight;
        gazeY = gazeY * (1 - headWeight) + normPitch * headWeight;

        // Ensure values are in range [-1, 1]
        gazeX = Math.max(-1.0f, Math.min(1.0f, gazeX));
        gazeY = Math.max(-1.0f, Math.min(1.0f, gazeY));
    }

    /**
     * Update gaze history for tracking stability
     */
    private void updateGazeHistory() {
        // Calculate gaze magnitude (distance from center)
        float gazeMagnitude = (float) Math.sqrt(gazeX * gazeX + gazeY * gazeY);

        // Add to history
        gazeHistory[historyIndex] = gazeMagnitude;
        historyIndex = (historyIndex + 1) % gazeHistory.length;
    }

    /**
     * Check if the user is looking away from the screen
     */
    private void checkLookingAway() {
        // Calculate the magnitude of the gaze vector
        float gazeMagnitude = (float) Math.sqrt(gazeX * gazeX + gazeY * gazeY);

        // Consider looking away if gaze magnitude is large (looking far from center)
        boolean currentlyLookingAway = gazeMagnitude > 0.7f; // Threshold for looking away

        if (currentlyLookingAway) {
            lookingAwayFrames++;

            // Update looking away state after sufficient consistent frames
            if (lookingAwayFrames >= LOOKING_AWAY_THRESHOLD && !isLookingAway) {
                isLookingAway = true;
                isLookingAtScreen = false;
                if (callback != null) {
                    callback.onLookingAway(true);
                }
                Log.d(TAG, "User is now looking away from screen");
            }
        } else {
            lookingAwayFrames = 0;

            // Update state if was previously looking away
            if (isLookingAway) {
                isLookingAway = false;
                isLookingAtScreen = true;
                if (callback != null) {
                    callback.onLookingAway(false);
                }
                Log.d(TAG, "User is now looking at screen");
            }
        }

        // Update looking at screen state
        isLookingAtScreen = !isLookingAway;
    }

    /**
     * Get the current horizontal gaze position (-1 left to 1 right)
     */
    public float getGazeX() {
        return gazeX;
    }

    /**
     * Get the current vertical gaze position (-1 up to 1 down)
     */
    public float getGazeY() {
        return gazeY;
    }
    
    /**
     * Classify current gaze direction based on thresholds
     * @param threshold The threshold value (τ) for classification
     * @return "LEFT", "RIGHT", or "CENTER"
     */
    public String getGazeDirection(float threshold) {
        // Use EMA smoothed values for more stable classification
        if (emaGazeX >= threshold) {
            return "RIGHT";
        } else if (emaGazeX <= -threshold) {
            return "LEFT";
        } else {
            return "CENTER";
        }
    }
    
    /**
     * Get current gaze direction using adaptive threshold based on input method
     * @return "LEFT", "RIGHT", or "CENTER"
     */
    public String getGazeDirection() {
        // Use larger threshold for head pose based estimation (less precise)
        return getGazeDirection(0.12f); // Increased from 0.08f for better stability
    }
    
    /**
     * Get gaze direction from specific values (for debugging)
     * @param x gaze X coordinate
     * @param y gaze Y coordinate  
     * @param threshold classification threshold
     * @return "LEFT", "RIGHT", or "CENTER"
     */
    public String getGazeDirectionFromValues(float x, float y, float threshold) {
        if (x >= threshold) {
            return "RIGHT";
        } else if (x <= -threshold) {
            return "LEFT";
        } else {
            return "CENTER";
        }
    }
    
    /**
     * Check if model is properly loaded and all tensor indices are found
     * @return true if model is ready for inference, false otherwise
     */
    public boolean isModelReady() {
        return isInitialized && interpreter != null && 
               eyeLeftInputIndex >= 0 && eyeRightInputIndex >= 0 && 
               faceInputIndex >= 0 && faceMaskInputIndex >= 0;
    }
    
    /**
     * Get information about the loaded model
     * @return String containing model info
     */
    public String getModelInfo() {
        if (!isInitialized || interpreter == null) {
            return "Model not initialized";
        }
        
        return String.format("Model: %s, Input tensors: %d, Output tensors: %d, " +
                           "Tensor indices - eyeLeft:%d, eyeRight:%d, face:%d, faceMask:%d",
                           MODEL_FILE, 
                           interpreter.getInputTensorCount(),
                           interpreter.getOutputTensorCount(),
                           eyeLeftInputIndex, eyeRightInputIndex, 
                           faceInputIndex, faceMaskInputIndex);
    }
    
    /**
     * Test model inference with dummy data (for debugging)
     * @return true if model can run inference successfully
     */
    public boolean testModelInference() {
        if (!isModelReady()) {
            Log.w(TAG, "Model not ready for testing");
            return false;
        }
        
        try {
            // Create dummy 64x64 ARGB bitmaps
            Bitmap dummyEyeLeft = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            Bitmap dummyEyeRight = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            Bitmap dummyFace = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            
            // Create dummy face grid
            float[] dummyFaceGrid = new float[625];
            for (int i = 0; i < 625; i++) {
                dummyFaceGrid[i] = (i < 300) ? 1.0f : 0.0f; // Half filled
            }
            
            Log.d(TAG, "Testing iTracker model with dummy data...");
            float[] result = runGaze(dummyEyeLeft, dummyEyeRight, dummyFace, dummyFaceGrid);
            
            // Cleanup
            dummyEyeLeft.recycle();
            dummyEyeRight.recycle();
            dummyFace.recycle();
            
            if (result != null && result.length == 2) {
                Log.d(TAG, String.format("Model test PASSED: output=(%.3f, %.3f)", result[0], result[1]));
                return true;
            } else {
                Log.e(TAG, "Model test FAILED: invalid output");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Model test FAILED with exception: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Log current gaze state for debugging (call this manually when needed)
     */
    public void logCurrentState() {
        Log.i(TAG, "=== CURRENT GAZE STATE ===");
        Log.i(TAG, "Model ready: " + isModelReady());
        Log.i(TAG, "Initialized: " + isInitialized);
        Log.i(TAG, "Current raw gaze: (" + gazeX + ", " + gazeY + ")");
        Log.i(TAG, "Current EMA gaze: (" + emaGazeX + ", " + emaGazeY + ")");
        Log.i(TAG, "Current direction (raw): " + getGazeDirection());
        Log.i(TAG, "Current direction (EMA): " + getGazeDirectionFromValues(emaGazeX, emaGazeY, 0.08f));
        Log.i(TAG, "Looking at screen: " + isLookingAtScreen);
        Log.i(TAG, "Looking away: " + isLookingAway);
        Log.i(TAG, "Front camera mirrored: " + frontCameraMirrored);
        Log.i(TAG, "EMA alpha: " + emaAlpha);
        Log.i(TAG, "Head pose weight: " + headPoseWeight);
        if (isModelReady()) {
            Log.i(TAG, getModelInfo());
        }
        Log.i(TAG, "==========================");
    }

    /**
     * Check if the user is looking away from the screen
     */
    public boolean isLookingAway() {
        return isLookingAway;
    }

    /**
     * Check if the user is looking at the screen
     */
    public boolean isLookingAtScreen() {
        return isLookingAtScreen;
    }

    /**
     * Check if the gaze position is stable (not moving much)
     */
    public boolean isGazeStable() {
        // Calculate standard deviation of gaze history
        float mean = 0;
        for (float value : gazeHistory) {
            mean += value;
        }
        mean /= gazeHistory.length;

        float variance = 0;
        for (float value : gazeHistory) {
            variance += (value - mean) * (value - mean);
        }
        variance /= gazeHistory.length;

        float stdDev = (float) Math.sqrt(variance);

        // Gaze is stable if standard deviation is low
        return stdDev < 0.1f;
    }

    /**
     * Create a synthetic face image by combining eye regions
     * This is a workaround for legacy method when full face image is not available
     */
    private Bitmap createSyntheticFaceFromEyes(Bitmap leftEye, Bitmap rightEye) {
        if (leftEye == null || rightEye == null) {
            return null;
        }
        
        try {
            // Create a 64x64 face image (model input size)
            Bitmap syntheticFace = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(syntheticFace);
            
            // Fill with a neutral gray background
            canvas.drawColor(0xFF808080); // Gray background
            
            // Scale eye regions to fit in face image
            int eyeSize = INPUT_SIZE / 4; // Each eye takes 1/4 of face width
            
            // Position eyes in typical face locations (CORRECTED: swap left/right for camera coordinate)
            int leftEyeX = 3 * INPUT_SIZE / 4 - eyeSize / 2;  // Left eye at 3/4 position (was 1/4)
            int rightEyeX = INPUT_SIZE / 4 - eyeSize / 2; // Right eye at 1/4 position (was 3/4)
            int eyeY = INPUT_SIZE / 3 - eyeSize / 2; // Eyes at 1/3 height
            
            // Draw scaled eye regions (SWAPPED to correct coordinate system)
            Bitmap scaledLeftEye = Bitmap.createScaledBitmap(rightEye, eyeSize, eyeSize, true);  // Use rightEye for leftEye position
            Bitmap scaledRightEye = Bitmap.createScaledBitmap(leftEye, eyeSize, eyeSize, true);  // Use leftEye for rightEye position
            
            canvas.drawBitmap(scaledLeftEye, leftEyeX, eyeY, null);
            canvas.drawBitmap(scaledRightEye, rightEyeX, eyeY, null);
            
            // Clean up scaled bitmaps
            scaledLeftEye.recycle();
            scaledRightEye.recycle();
            
            Log.d(TAG, String.format("Synthetic face created: leftEyeX=%d, rightEyeX=%d, eyeY=%d (SWAPPED coordinates)", 
                  leftEyeX, rightEyeX, eyeY));
            Log.d(TAG, "Created synthetic face image from eye regions: " + INPUT_SIZE + "x" + INPUT_SIZE);
            return syntheticFace;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating synthetic face image: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create dummy landmarks for synthetic face image
     * Provides basic MediaPipe-compatible landmark positions
     */
    private List<PointF> createDummyLandmarksForEyes(int faceWidth, int faceHeight) {
        List<PointF> landmarks = new ArrayList<>();
        
        // Create minimal set of 468 landmarks required by MediaPipe
        // Most will be dummy values, but eye landmarks should be reasonably positioned
        
        float centerX = faceWidth / 2.0f;
        float centerY = faceHeight / 2.0f;
        float eyeY = faceHeight / 3.0f;
        float leftEyeX = faceWidth / 4.0f;
        float rightEyeX = 3 * faceWidth / 4.0f;
        
        for (int i = 0; i < 468; i++) {
            PointF point;
            
            // Eye landmarks - use reasonable positions
            if (isLeftEyeLandmark(i)) {
                point = new PointF(leftEyeX + (float)(Math.random() * 20 - 10), eyeY + (float)(Math.random() * 10 - 5));
            } else if (isRightEyeLandmark(i)) {
                point = new PointF(rightEyeX + (float)(Math.random() * 20 - 10), eyeY + (float)(Math.random() * 10 - 5));
            } else {
                // Other landmarks - place around face perimeter
                double angle = 2 * Math.PI * i / 468.0;
                float radius = Math.min(faceWidth, faceHeight) * 0.4f;
                point = new PointF(
                    centerX + radius * (float)Math.cos(angle),
                    centerY + radius * (float)Math.sin(angle)
                );
            }
            
            landmarks.add(point);
        }
        
        Log.d(TAG, "Created " + landmarks.size() + " dummy landmarks for synthetic face");
        return landmarks;
    }
    
    /**
     * Check if landmark index corresponds to left eye region
     */
    private boolean isLeftEyeLandmark(int index) {
        // MediaPipe left eye landmarks (approximate range)
        return (index >= 33 && index <= 41) || 
               (index >= 130 && index <= 145) ||
               (index >= 157 && index <= 163);
    }
    
    /**
     * Check if landmark index corresponds to right eye region  
     */
    private boolean isRightEyeLandmark(int index) {
        // MediaPipe right eye landmarks (approximate range)
        return (index >= 362 && index <= 374) ||
               (index >= 385 && index <= 398) ||
               (index >= 263 && index <= 269);
    }

    /**
     * Calibrate the gaze estimator by having user look straight ahead
     * Call this when user is looking straight at camera
     */
    public void calibrateCenter() {
        if (calibrationSamples < REQUIRED_CALIBRATION_SAMPLES) {
            calibrationGazeX += emaGazeX;
            calibrationSamples++;
            
            Log.d(TAG, String.format("Calibration sample %d/%d: gazeX=%.4f", 
                  calibrationSamples, REQUIRED_CALIBRATION_SAMPLES, emaGazeX));
            
            if (calibrationSamples >= REQUIRED_CALIBRATION_SAMPLES) {
                // Calculate average as new bias
                float avgGazeX = calibrationGazeX / REQUIRED_CALIBRATION_SAMPLES;
                gazeXBias = avgGazeX;
                isCalibrated = true;
                
                Log.i(TAG, String.format("CALIBRATION COMPLETE: New gazeX bias = %.4f", gazeXBias));
                
                // Reset for next calibration
                calibrationGazeX = 0.0f;
                calibrationSamples = 0;
            }
        }
    }
    
    /**
     * Reset calibration
     */
    public void resetCalibration() {
        isCalibrated = false;
        calibrationGazeX = 0.0f;
        calibrationSamples = 0;
        gazeXBias = 0.25f; // Reset to default
        Log.i(TAG, "Calibration reset to defaults");
    }
    
    /**
     * Check if calibrated
     */
    public boolean isCalibrated() {
        return isCalibrated;
    }

    /**
     * Close and release resources
     */
    public void close() {
        try {
            if (interpreter != null) {
                interpreter.close();
                interpreter = null;
                Log.d(TAG, "TensorFlow Lite interpreter closed");
            } else {
                Log.d(TAG, "No TensorFlow Lite interpreter to close (was null)");
            }

            // Note: We don't close the GPU delegate here because it's managed by TFLiteGpuDelegateManager
            // The delegate will be closed when the application exits or when the manager is explicitly closed

            Log.d(TAG, "GazeEstimator resources released");
        } catch (Exception e) {
            Log.e(TAG, "Error closing GazeEstimator resources: " + e.getMessage(), e);
        }
    }


}

    


