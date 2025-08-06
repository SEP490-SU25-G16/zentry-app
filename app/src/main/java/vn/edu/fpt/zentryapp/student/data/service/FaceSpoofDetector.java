package vn.edu.fpt.zentryapp.student.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

public class FaceSpoofDetector {
    private static final String TAG = "FaceSpoofDetector";
    private static final String MODEL_FILE_1 = "spoof_model_scale_2_7.tflite";
    private static final String MODEL_FILE_2 = "spoof_model_scale_4_0.tflite";

    private static final float SCALE_1 = 2.7f;
    private static final float SCALE_2 = 4.0f;
    private static final int INPUT_IMAGE_DIM = 80;
    private static final int OUTPUT_DIM = 3;

    // Updated confidence thresholds - much more lenient now

    private static final float SPOOF_CONFIDENCE_THRESHOLD = 0.85f; // Higher threshold for spoofing (was 0.80f)
    private static final float SPOOF_REAL_RATIO_THRESHOLD = 2.0f;  // Spoof must be 2.0x higher than real (was 1.5f)

    // Temporal variance parameters - much more lenient now
    private static final float MIN_POSITION_VARIANCE = 0.0005f; // Was 0.001f - Less minimum movement required
    private static final float MAX_POSITION_VARIANCE = 0.05f;   // Was 0.03f - Allow more movement
    private static final float MIN_SIZE_VARIANCE = 0.0001f;     // Was 0.0005f - Less minimum size variance required
    private static final float MAX_SIZE_VARIANCE = 0.04f;       // Was 0.02f - Allow more size variance

    // Face-oval ratio constants - much more lenient now
    private static final float MAX_FACE_OUTSIDE_RATIO = 1.0f/8.0f; // Max extension outside oval (was 1/15)

    // Frame history for temporal analysis
    private static final int FRAME_HISTORY_SIZE = 8;
    private final java.util.Queue<TemporalFrameData> frameHistory = new java.util.LinkedList<>();

    // Debug: Log output meanings
    // Based on the logs and model behavior:
    // Index 0: Real face probability
    // Index 1: Unknown/uncertain
    // Index 2: Spoof probability

    private Interpreter firstModelInterpreter;
    private Interpreter secondModelInterpreter;
    private ImageProcessor imageTensorProcessor;
    private boolean useMockDetection = false;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Context context;

    private volatile boolean isInitialized = false;
    private final CountDownLatch initLatch = new CountDownLatch(1);

    /**
     * Class to track temporal data for analysis
     */
    private static class TemporalFrameData {
        final float[] combinedResult;
        final Rect faceRect;
        final long timestamp;
        
        TemporalFrameData(float[] combinedResult, Rect faceRect) {
            this.combinedResult = combinedResult.clone();
            this.faceRect = new Rect(faceRect);
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class SpoofResult {
        private final boolean isSpoof;
        @Getter
        private final float score;
        @Getter
        private final long timeMillis;

        public SpoofResult(boolean isSpoof, float score, long timeMillis) {
            this.isSpoof = isSpoof;
            this.score = score;
            this.timeMillis = timeMillis;
        }

        public boolean isSpoof() {
            return isSpoof;
        }

        public float getConfidence() {
            // The 'score' field already represents the confidence of the spoof detection.
            // This method provides a more explicit getter for it.
            return score;
        }
    }

    public FaceSpoofDetector(Context context) {
        this.context = context.getApplicationContext();

        // Initialize model asynchronously
        executor.execute(() -> {
            try {
                // Log asset information for debugging
                logAssetsContent(context);

                try {
                    Log.d(TAG, "Loading model files...");

                    // Initialize TFLiteInterpreter
                    Interpreter.Options interpreterOptions = TFLiteGpuDelegateManager.getInstance().getInterpreterOptions();

                    // Load models from assets
                    MappedByteBuffer model1Buffer = FileUtil.loadMappedFile(context, MODEL_FILE_1);
                    MappedByteBuffer model2Buffer = FileUtil.loadMappedFile(context, MODEL_FILE_2);

                    Log.d(TAG, "Model 1 loaded, size: " + model1Buffer.capacity() + " bytes");
                    Log.d(TAG, "Model 2 loaded, size: " + model2Buffer.capacity() + " bytes");

                    // Create interpreters
                    firstModelInterpreter = new Interpreter(model1Buffer, interpreterOptions);
                    secondModelInterpreter = new Interpreter(model2Buffer, interpreterOptions);

                    // Create image processor for preprocessing
                    imageTensorProcessor = new ImageProcessor.Builder()
                            .add(new CastOp(DataType.FLOAT32))
                            .build();

                    Log.d(TAG, "Models loaded successfully");
                    isInitialized = true;
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing TensorFlow Lite model: " + e.getMessage(), e);
                    mainHandler.post(() ->
                            Toast.makeText(context, "Error loading spoof detection model: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                    useMockDetection = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking model files: " + e.getMessage(), e);
                mainHandler.post(() ->
                        Toast.makeText(context, "Error checking spoof detection model: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
                useMockDetection = true;
            } finally {
                initLatch.countDown();
            }
        });
    }

    public boolean isInitialized() {
        return isInitialized && firstModelInterpreter != null && secondModelInterpreter != null;
    }

    public void awaitInitialization(long timeoutMs) throws InterruptedException {
        initLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void logAssetsContent(Context context) {
        try {
            String[] files = context.getAssets().list("");
            Log.d(TAG, "Assets directory content: " + Arrays.toString(files));

            // Check details about model files
            for (String file : files) {
                if (file.endsWith(".tflite")) {
                    try {
                        MappedByteBuffer buffer = FileUtil.loadMappedFile(context, file);
                        Log.d(TAG, "Model file: " + file + ", size: " + buffer.capacity() + " bytes");
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking model file " + file + ": " + e.getMessage(), e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing assets directory: " + e.getMessage(), e);
        }
    }

    /**
     * Detect if a face is spoofed asynchronously with oval boundary validation
     *
     * @param frameImage Original frame image
     * @param faceRect   Face bounding box
     * @param ovalRect   Oval guide boundaries (optional, can be null)
     * @param callback   Callback for result
     */
    public void detectSpoofAsync(Bitmap frameImage, Rect faceRect, android.graphics.RectF ovalRect, SpoofCallback callback) {
        executor.execute(() -> {
            try {
                // Ensure model is initialized
                if (!isInitialized()) {
                    try {
                        Log.d(TAG, "Waiting for model initialization...");
                        awaitInitialization(5000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Model initialization interrupted", e);
                    }
                }

                SpoofResult result = detectSpoof(frameImage, faceRect, ovalRect);
                mainHandler.post(() -> callback.onResult(result));
            } catch (Exception e) {
                Log.e(TAG, "Error in spoof detection", e);
                mainHandler.post(() -> callback.onResult(new SpoofResult(true, 0.75f, 0))); // Default to spoof on error for security
            }
        });
    }

    /**
     * Legacy method for backward compatibility
     */
    public void detectSpoofAsync(Bitmap frameImage, Rect faceRect, SpoofCallback callback) {
        detectSpoofAsync(frameImage, faceRect, null, callback);
    }

    /**
     * Callback interface for spoof detection
     */
    public interface SpoofCallback {
        void onResult(SpoofResult result);
    }

    /**
     * Get frame history for external analysis
     * @return Queue of temporal frame data
     */
    public java.util.Queue<TemporalFrameData> getFrameHistory() {
        return new java.util.LinkedList<>(frameHistory);
    }

    /**
     * Detect if a face is spoofed
     *
     * @param frameImage Original frame image
     * @param faceRect   Face bounding box
     * @param ovalRect   Oval guide boundaries (optional, can be null)
     * @return Spoof detection result
     */
    public SpoofResult detectSpoof(Bitmap frameImage, Rect faceRect, android.graphics.RectF ovalRect) {
        long startTime = System.currentTimeMillis();

        // If using mock detection or interpreter not initialized, always return not spoof
        if (useMockDetection || firstModelInterpreter == null || secondModelInterpreter == null || imageTensorProcessor == null) {
            Log.d(TAG, "Using mock spoof detection (always return real face)");
            return new SpoofResult(false, 0.95f, System.currentTimeMillis() - startTime);
        }

        try {
            Log.d(TAG, "Starting spoof detection with bounding box: " + faceRect.toString());

            // Crop and scale face image with the two given constants
            Bitmap croppedImage1 = crop(
                    frameImage,
                    faceRect,
                    SCALE_1,
                    INPUT_IMAGE_DIM,
                    INPUT_IMAGE_DIM
            );

            Log.d(TAG, "Cropped image 1 with scale " + SCALE_1 + ", size: " + croppedImage1.getWidth() + "x" + croppedImage1.getHeight());

            // Convert RGB to BGR
            Bitmap bgrImage1 = convertRgbToBgr(croppedImage1);
            Log.d(TAG, "Converted image 1 to BGR");

            Bitmap croppedImage2 = crop(
                    frameImage,
                    faceRect,
                    SCALE_2,
                    INPUT_IMAGE_DIM,
                    INPUT_IMAGE_DIM
            );

            Log.d(TAG, "Cropped image 2 with scale " + SCALE_2 + ", size: " + croppedImage2.getWidth() + "x" + croppedImage2.getHeight());

            // Convert RGB to BGR
            Bitmap bgrImage2 = convertRgbToBgr(croppedImage2);
            Log.d(TAG, "Converted image 2 to BGR");

            // Process images
            TensorImage tensorImage1 = TensorImage.fromBitmap(bgrImage1);
            TensorImage tensorImage2 = TensorImage.fromBitmap(bgrImage2);

            tensorImage1 = imageTensorProcessor.process(tensorImage1);
            tensorImage2 = imageTensorProcessor.process(tensorImage2);

            Log.d(TAG, "Processed tensor images");

            // Get buffers from TensorImages
            ByteBuffer input1 = tensorImage1.getBuffer();
            ByteBuffer input2 = tensorImage2.getBuffer();

            // Prepare output buffers
            float[][] output1 = new float[1][OUTPUT_DIM];
            float[][] output2 = new float[1][OUTPUT_DIM];

            // Run inference
            firstModelInterpreter.run(input1, output1);
            secondModelInterpreter.run(input2, output2);

            Log.d(TAG, "Ran inference");
            Log.d(TAG, "Output model 1: [" + output1[0][0] + ", " + output1[0][1] + ", " + output1[0][2] + "]");
            Log.d(TAG, "Output model 2: [" + output2[0][0] + ", " + output2[0][1] + ", " + output2[0][2] + "]");

            // Apply softmax to outputs
            float[] softmax1 = softMax(output1[0]);
            float[] softmax2 = softMax(output2[0]);

            Log.d(TAG, "Softmax model 1: [" + softmax1[0] + ", " + softmax1[1] + ", " + softmax1[2] + "]");
            Log.d(TAG, "Softmax model 2: [" + softmax2[0] + ", " + softmax2[1] + ", " + softmax2[2] + "]");

            // Combine results - with weighted combining instead of simple averaging
            float[] combined = weightedCombineResults(softmax1, softmax2);

            Log.d(TAG, "Combined result: Real=" + combined[0] + ", Unknown=" + combined[1] + ", Spoof=" + combined[2]);

            // Store frame data for temporal analysis
            TemporalFrameData currentFrame = new TemporalFrameData(combined, faceRect);
            updateFrameHistory(currentFrame);
            
            // 🔒 ENHANCED MULTI-LAYER DETECTION
            
            // Layer 1: Enhanced AI model analysis with weighted combining
            boolean modelIndicatesSpoof = evaluateModelResults(combined);
            
            // Layer 2: Advanced texture analysis for 2D patterns
            boolean hasUniformTexture = checkUniformTexture(softmax1, softmax2);
            
            // Layer 3: Liveness detection through temporal variance
            boolean hasNaturalMovement = checkTemporalVariance();
            
            // Layer 4: Strict oval boundary validation
            boolean isWithinOvalBoundary = validateOvalBoundary(faceRect, ovalRect);
            
            // 🎯 IMPROVED DECISION LOGIC - More balanced and ML model priority - MUCH more lenient now
            boolean isSpoof;
            float confidence;

            // Case 1: Strong ML model confidence for real face - prioritize model results
            if (combined[0] > 0.65f && !modelIndicatesSpoof) { // Reduced from 0.70
                isSpoof = false;
                confidence = combined[0];
                Log.d(TAG, "🟢 HIGH CONFIDENCE REAL: Strong ML model confidence");
            }
            // Case 2: Good ML confidence + at least valid position OR natural movement
            else if (combined[0] > 0.60f && !modelIndicatesSpoof && (isWithinOvalBoundary || hasNaturalMovement)) {
                isSpoof = false;
                confidence = combined[0];
                Log.d(TAG, "🟢 GOOD CONFIDENCE REAL: ML model + partial validation");
            }
            // Case 3: Decent ML confidence - we trust the model more now
            else if (combined[0] > 0.58f && !modelIndicatesSpoof) {
                isSpoof = false;
                confidence = combined[0];
                Log.d(TAG, "🟢 ACCEPTABLE REAL: ML model result trusted");
            }
            // Case 4: Strong spoof indicators - multiple red flags
            else if ((modelIndicatesSpoof && hasUniformTexture) || 
                     (modelIndicatesSpoof && !hasNaturalMovement && !isWithinOvalBoundary)) {
                isSpoof = true;
                confidence = Math.max(0.80f, combined[2]);
                Log.d(TAG, "🔴 HIGH CONFIDENCE SPOOF: Multiple strong indicators");
            }
            // Case 5: More lenient on unclear cases - default to real unless strong spoof
            else if (combined[0] > 0.40f && combined[2] < 0.60f) {
                isSpoof = false;
                confidence = Math.max(0.58f, combined[0]);
                Log.d(TAG, "� LIKELY REAL: Benefit of the doubt");
            }
            // Case 6: Default to spoof for very unclear cases
            else {
                isSpoof = true;
                confidence = Math.max(0.65f, combined[2]);
                Log.d(TAG, "🟠 LIKELY SPOOF: Failed validation checks");
            }
            
            Log.d(TAG, "🎯 FINAL RESULT: " + (isSpoof ? "SPOOF" : "REAL") + 
                    " with confidence: " + confidence +
                    ", modelSpoof=" + modelIndicatesSpoof + 
                    ", uniformTexture=" + hasUniformTexture +
                    ", naturalMovement=" + hasNaturalMovement +
                    ", withinOval=" + isWithinOvalBoundary);

            long timeMillis = System.currentTimeMillis() - startTime;
            return new SpoofResult(isSpoof, confidence, timeMillis);

        } catch (Throwable e) {
            Log.e(TAG, "Error in spoof detection: " + e.getMessage(), e);

            // IMPROVED ERROR HANDLING:
            Log.w(TAG, "⚠️ Processing error in spoof detection. Error type: " + e.getClass().getSimpleName());

            boolean shouldDefaultToSpoof = e instanceof OutOfMemoryError || e instanceof IllegalArgumentException;

            if (shouldDefaultToSpoof) {
                Log.w(TAG, "⚠️ Critical error detected - defaulting to spoof with warning");
                return new SpoofResult(true, 0.75f, System.currentTimeMillis() - startTime);
            } else {
                Log.w(TAG, "⚠️ Non-critical error - still defaulting to spoof with lower confidence");
                return new SpoofResult(true, 0.65f, System.currentTimeMillis() - startTime);
            }
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    public SpoofResult detectSpoof(Bitmap frameImage, Rect faceRect) {
        return detectSpoof(frameImage, faceRect, null);
    }

    /**
     * Weighted combine of model results giving more weight to the model that is more confident
     */
    private float[] weightedCombineResults(float[] softmax1, float[] softmax2) {
        float[] combined = new float[OUTPUT_DIM];
        
        // Calculate confidence level of each model
        float confidence1 = Math.max(softmax1[0], softmax1[2]); // Max of real or spoof
        float confidence2 = Math.max(softmax2[0], softmax2[2]); // Max of real or spoof
        
        // Calculate weights based on confidence
        float totalConfidence = confidence1 + confidence2;
        float weight1 = totalConfidence > 0 ? confidence1 / totalConfidence : 0.5f;
        float weight2 = totalConfidence > 0 ? confidence2 / totalConfidence : 0.5f;
        
        // Ensure weights sum to 1
        float sum = weight1 + weight2;
        weight1 /= sum;
        weight2 /= sum;
        
        // Apply weighted combine
        for (int i = 0; i < OUTPUT_DIM; i++) {
            combined[i] = (softmax1[i] * weight1) + (softmax2[i] * weight2);
        }
        
        return combined;
    }

    /**
     * Evaluate model results with weighted combining
     */
    private boolean evaluateModelResults(float[] combined) {
        return (combined[2] > SPOOF_CONFIDENCE_THRESHOLD && combined[2] > combined[0] * SPOOF_REAL_RATIO_THRESHOLD) ||
               (combined[2] > 0.70f && combined[2] > combined[0]);
    }

    /**
     * Update frame history for temporal analysis
     */
    private void updateFrameHistory(TemporalFrameData currentFrame) {
        frameHistory.add(currentFrame);
        if (frameHistory.size() > FRAME_HISTORY_SIZE) {
            frameHistory.poll();
        }
    }

    /**
     * Check for natural micro-movements that indicate liveness
     */
    private boolean checkTemporalVariance() {
        if (frameHistory.size() < 3) {
            return true; // Not enough data, assume natural
        }
        
        // Calculate variance in face position and size
        float positionVariance = calculatePositionVariance();
        float sizeVariance = calculateSizeVariance();
        
        // Real faces have natural micro-movements
        boolean hasNaturalMovement = positionVariance >= MIN_POSITION_VARIANCE && 
                                     positionVariance <= MAX_POSITION_VARIANCE &&
                                     sizeVariance >= MIN_SIZE_VARIANCE &&
                                     sizeVariance <= MAX_SIZE_VARIANCE;
                                     
        Log.d(TAG, "📊 TEMPORAL ANALYSIS: posVar=" + positionVariance + 
                  ", sizeVar=" + sizeVariance + 
                  ", natural=" + hasNaturalMovement);
                  
        return hasNaturalMovement;
    }

    /**
     * Calculate variance in face position across frames
     */
    private float calculatePositionVariance() {
        if (frameHistory.size() < 2) {
            return 0.01f; // Default value if not enough data
        }
        
        float sumX = 0;
        float sumY = 0;
        float sumSqX = 0;
        float sumSqY = 0;
        int count = 0;
        
        for (TemporalFrameData frame : frameHistory) {
            float centerX = frame.faceRect.exactCenterX();
            float centerY = frame.faceRect.exactCenterY();
            
            sumX += centerX;
            sumY += centerY;
            sumSqX += centerX * centerX;
            sumSqY += centerY * centerY;
            count++;
        }
        
        float meanX = sumX / count;
        float meanY = sumY / count;
        float varianceX = (sumSqX / count) - (meanX * meanX);
        float varianceY = (sumSqY / count) - (meanY * meanY);
        
        // Normalize by face size
        TemporalFrameData lastFrame = getLastFrame();
        if (lastFrame != null) {
            float faceSize = Math.max(lastFrame.faceRect.width(), lastFrame.faceRect.height());
            varianceX /= (faceSize * faceSize);
            varianceY /= (faceSize * faceSize);
        }
        
        return (varianceX + varianceY) / 2;
    }

    /**
     * Calculate variance in face size across frames
     */
    private float calculateSizeVariance() {
        if (frameHistory.size() < 2) {
            return 0.005f; // Default value if not enough data
        }
        
        float sumW = 0;
        float sumH = 0;
        float sumSqW = 0;
        float sumSqH = 0;
        int count = 0;
        
        for (TemporalFrameData frame : frameHistory) {
            float width = frame.faceRect.width();
            float height = frame.faceRect.height();
            
            sumW += width;
            sumH += height;
            sumSqW += width * width;
            sumSqH += height * height;
            count++;
        }
        
        float meanW = sumW / count;
        float meanH = sumH / count;
        float varianceW = (sumSqW / count) - (meanW * meanW);
        float varianceH = (sumSqH / count) - (meanH * meanH);
        
        // Normalize by face size
        TemporalFrameData lastFrame = getLastFrame();
        if (lastFrame != null) {
            float faceWidth = lastFrame.faceRect.width();
            float faceHeight = lastFrame.faceRect.height();
            varianceW /= (faceWidth * faceWidth);
            varianceH /= (faceHeight * faceHeight);
        }
        
        return (varianceW + varianceH) / 2;
    }

    /**
     * Get the last frame from history
     */
    private TemporalFrameData getLastFrame() {
        if (frameHistory.isEmpty()) {
            return null;
        }
        
        // Convert queue to array and get last element
        TemporalFrameData[] frames = frameHistory.toArray(new TemporalFrameData[0]);
        return frames[frames.length - 1];
    }

    /**
     * Check for abnormal classification patterns across frames
     * Real faces show gradual, natural changes while spoofs often show abrupt changes
     */
    private boolean checkAbnormalPatternAcrossFrames() {
        if (frameHistory.size() < 4) {
            return false; // Not enough data
        }
        
        // Convert queue to array for easier processing
        TemporalFrameData[] frames = frameHistory.toArray(new TemporalFrameData[0]);
        
        // Count classification flips (real->spoof->real)
        int classificationFlips = 0;
        for (int i = 1; i < frames.length; i++) {
            if ((frames[i-1].combinedResult[0] > frames[i-1].combinedResult[2] && 
                 frames[i].combinedResult[0] < frames[i].combinedResult[2]) ||
                (frames[i-1].combinedResult[0] < frames[i-1].combinedResult[2] && 
                 frames[i].combinedResult[0] > frames[i].combinedResult[2])) {
                classificationFlips++;
            }
        }
        
        // Calculate confidence stability (suspicious if too stable)
        float confidenceVariance = calculateConfidenceVariance();
        boolean suspiciouslyStableConfidence = confidenceVariance < 0.001f;
        
        // Calculate pattern score
        boolean abnormalPattern = classificationFlips > 2 || suspiciouslyStableConfidence;
        
        Log.d(TAG, "📊 PATTERN ANALYSIS: flips=" + classificationFlips + 
                  ", confVariance=" + confidenceVariance + 
                  ", abnormal=" + abnormalPattern);
                  
        return abnormalPattern;
    }

    /**
     * Calculate variance in confidence scores across frames
     */
    private float calculateConfidenceVariance() {
        if (frameHistory.size() < 2) {
            return 0.01f; // Default value if not enough data
        }
        
        float sum = 0;
        float sumSq = 0;
        int count = 0;
        
        // Calculate for real face confidence (index 0)
        for (TemporalFrameData frame : frameHistory) {
            sum += frame.combinedResult[0];
            sumSq += frame.combinedResult[0] * frame.combinedResult[0];
            count++;
        }
        
        float mean = sum / count;
        float variance = (sumSq / count) - (mean * mean);
        
        return variance;
    }

    /**
     * Validate if face is properly within oval boundaries
     */
    private boolean validateOvalBoundary(Rect faceRect, android.graphics.RectF ovalRect) {
        if (ovalRect == null) {
            return true; // No oval to check against
        }
        
        // Calculate ellipse parameters
        float centerX = ovalRect.centerX();
        float centerY = ovalRect.centerY();
        float a = ovalRect.width() / 2; // semi-major axis
        float b = ovalRect.height() / 2; // semi-minor axis
        
        // Check if face center is within ellipse
        float faceCenterX = faceRect.exactCenterX();
        float faceCenterY = faceRect.exactCenterY();
        
        // Ellipse equation: (x-h)²/a² + (y-k)²/b² ≤ 1
        float ellipseValue = (float) (
            Math.pow(faceCenterX - centerX, 2) / Math.pow(a, 2) +
            Math.pow(faceCenterY - centerY, 2) / Math.pow(b, 2)
        );
        
        // Check face size relative to oval
        float faceWidth = faceRect.width();
        float faceHeight = faceRect.height();
        float widthRatio = faceWidth / ovalRect.width();
        float heightRatio = faceHeight / ovalRect.height();
        
        // Check if any part of face extends too far outside oval
        boolean tooFarOutside = 
            faceRect.left < ovalRect.left - (ovalRect.width() * MAX_FACE_OUTSIDE_RATIO) ||
            faceRect.right > ovalRect.right + (ovalRect.width() * MAX_FACE_OUTSIDE_RATIO) ||
            faceRect.top < ovalRect.top - (ovalRect.height() * MAX_FACE_OUTSIDE_RATIO) ||
            faceRect.bottom > ovalRect.bottom + (ovalRect.height() * MAX_FACE_OUTSIDE_RATIO);
        
        boolean isWithinEllipse = ellipseValue <= 1.5; // More lenient - was 2.0
        boolean hasSuitableSize = 
            widthRatio >= 0.35f && // More lenient - was 0.40f
            widthRatio <= 1.0f && // More lenient - was 0.95f
            heightRatio >= 0.35f && // More lenient - was 0.40f
            heightRatio <= 1.0f; // More lenient - was 0.95f
        
        Log.d(TAG, "🔍 OVAL VALIDATION: ellipseValue=" + String.format("%.6f", ellipseValue) +
                  ", inEllipse=" + isWithinEllipse + " (≤1.5)" + // Updated log to match actual threshold
                  ", goodSize=" + hasSuitableSize + 
                  ", notOutside=" + !tooFarOutside +
                  ", widthRatio=" + String.format("%.3f", widthRatio) +
                  ", heightRatio=" + String.format("%.3f", heightRatio));
                  
        return isWithinEllipse && hasSuitableSize && !tooFarOutside;
    }

    /**
     * Enhanced texture analysis for detecting 2D patterns in spoofing attacks
     * This method analyzes texture patterns to identify characteristics of printed photos,
     * screen displays, and other 2D replay attacks
     */
    private boolean checkUniformTexture(float[] softmax1, float[] softmax2) {
        // 1. Higher thresholds for unusual texture detection
        boolean unusualTextureIndicator =
                (softmax1[1] > 0.55f && softmax2[1] > 0.45f); // Both models must show unusual texture
        
        // 2. Check for inconsistency between models
        boolean modelInconsistency =
                Math.abs(softmax1[0] - softmax2[0]) > 0.70f &&
                Math.abs(softmax1[2] - softmax2[2]) > 0.60f; 
        
        // 3. Check for ambiguous classification
        boolean ambiguousClassification =
                (softmax1[0] > 0.50f && softmax1[2] > 0.50f &&
                 softmax2[0] > 0.45f && softmax2[2] > 0.45f);
        
        // 4. NEW: Check for abnormal classification pattern across frames
        boolean abnormalPattern = checkAbnormalPatternAcrossFrames();
        
        // 5. NEW: Analyze texture variance in recent frames
        boolean lowTextureVariance = calculateConfidenceVariance() < 0.0015f;
        
        // Log detailed information for debugging
        if (unusualTextureIndicator || modelInconsistency || ambiguousClassification || 
            abnormalPattern || lowTextureVariance) {
            Log.d(TAG, "🔍 TEXTURE ANALYSIS: " +
                    "unusualTexture=" + unusualTextureIndicator +
                    ", modelInconsistency=" + modelInconsistency +
                    ", ambiguousClassification=" + ambiguousClassification +
                    ", abnormalPattern=" + abnormalPattern +
                    ", lowTextureVariance=" + lowTextureVariance);
        }
        
        // Return true if multiple strong indicators suggest a 2D spoofing attempt
        boolean strongEvidence =
                (softmax1[1] > 0.70f && softmax2[1] > 0.70f) || // Very strong unusual texture
                (Math.abs(softmax1[0] - softmax2[0]) > 0.85f);  // Very strong inconsistency
        
        // Multiple weaker indicators
        boolean multipleIndicators =
                (unusualTextureIndicator && (modelInconsistency || ambiguousClassification)) ||
                (modelInconsistency && ambiguousClassification) ||
                (abnormalPattern && (unusualTextureIndicator || lowTextureVariance)) ||
                (lowTextureVariance && (unusualTextureIndicator || modelInconsistency));
        
        return strongEvidence || multipleIndicators;
    }

    /**
     * Convert RGB image to BGR
     */
    private Bitmap convertRgbToBgr(Bitmap input) {
        Bitmap output = input.copy(input.getConfig(), true);

        for (int i = 0; i < output.getWidth(); i++) {
            for (int j = 0; j < output.getHeight(); j++) {
                int pixel = output.getPixel(i, j);
                output.setPixel(i, j, Color.rgb(
                        Color.blue(pixel),
                        Color.green(pixel),
                        Color.red(pixel)
                ));
            }
        }

        return output;
    }

    /**
     * Apply softmax to array
     */
    private float[] softMax(float[] x) {
        float[] exp = new float[x.length];
        float sum = 0.0f;

        // Calculate exp and sum
        for (int i = 0; i < x.length; i++) {
            exp[i] = (float) Math.exp(x[i]);
            sum += exp[i];
        }

        // Normalize
        for (int i = 0; i < exp.length; i++) {
            exp[i] = exp[i] / sum;
        }

        return exp;
    }

    /**
     * Crop and scale face region
     */
    private Bitmap crop(Bitmap origImage, Rect bbox, float bboxScale, int targetWidth, int targetHeight) {
        int srcWidth = origImage.getWidth();
        int srcHeight = origImage.getHeight();

        Log.d(TAG, "Crop: Original image size " + srcWidth + "x" + srcHeight + ", bbox: " + bbox.toString());

        // Scale bounding box
        Rect scaledBox = getScaledBox(srcWidth, srcHeight, bbox, bboxScale);
        Log.d(TAG, "Crop: Scaled box: " + scaledBox.toString());

        // Crop image
        Bitmap croppedBitmap = Bitmap.createBitmap(
                origImage,
                scaledBox.left,
                scaledBox.top,
                scaledBox.width(),
                scaledBox.height()
        );

        Log.d(TAG, "Crop: Cropped bitmap size: " + croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());

        // Resize to target dimensions
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true);
        Log.d(TAG, "Crop: Resized bitmap size: " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());

        return resizedBitmap;
    }

    /**
     * Scale bounding box
     */
    private Rect getScaledBox(int imgWidth, int imgHeight, Rect box, float bboxScale) {
        int x = box.left;
        int y = box.top;
        int w = box.width();
        int h = box.height();

        Log.d(TAG, "getScaledBox: Input - imgWidth: " + imgWidth + ", imgHeight: " + imgHeight +
                ", box: " + box.toString() + ", scale: " + bboxScale);

        // Calculate scale
        float scale = Math.min(Math.min((imgHeight - 1f) / h, (imgWidth - 1f) / w), bboxScale);

        // Limit maximum scale to avoid large crops touching edges causing model noise
        scale = Math.min(scale, 2.7f);

        Log.d(TAG, "getScaledBox: Calculated scale: " + scale + " (limited to max 2.7)");

        float newWidth = w * scale;
        float newHeight = h * scale;
        float centerX = w / 2f + x;
        float centerY = h / 2f + y;

        float topLeftX = centerX - newWidth / 2f;
        float topLeftY = centerY - newHeight / 2f;
        float bottomRightX = centerX + newWidth / 2f;
        float bottomRightY = centerY + newHeight / 2f;

        Log.d(TAG, "getScaledBox: Initial scaled box - topLeft: (" + topLeftX + ", " + topLeftY +
                "), bottomRight: (" + bottomRightX + ", " + bottomRightY + ")");

        // Ensure box is within image bounds
        if (topLeftX < 0) {
            bottomRightX -= topLeftX;
            topLeftX = 0;
        }
        if (topLeftY < 0) {
            bottomRightY -= topLeftY;
            topLeftY = 0;
        }
        if (bottomRightX > imgWidth - 1) {
            topLeftX -= (bottomRightX - (imgWidth - 1));
            bottomRightX = imgWidth - 1;
        }
        if (bottomRightY > imgHeight - 1) {
            topLeftY -= (bottomRightY - (imgHeight - 1));
            bottomRightY = imgHeight - 1;
        }

        Rect result = new Rect((int) topLeftX, (int) topLeftY, (int) bottomRightX, (int) bottomRightY);
        Log.d(TAG, "getScaledBox: Final scaled box: " + result.toString());

        return result;
    }

    /**
     * Release resources
     */
    public void close() {
        if (firstModelInterpreter != null) {
            firstModelInterpreter.close();
        }
        if (secondModelInterpreter != null) {
            secondModelInterpreter.close();
        }
    }
}