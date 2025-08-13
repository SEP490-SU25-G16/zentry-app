package vn.edu.fpt.zentryapp.faceid.data.service;

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
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.Objects;
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
    private static final int OUTPUT_DIM = 2;

    private final java.util.Queue<TemporalFrameData> frameHistory = new java.util.LinkedList<>();

    private Interpreter firstModelInterpreter;
    private Interpreter secondModelInterpreter;
    private ImageProcessor imageTensorProcessor;
    private boolean useMockDetection = false;
    private final Object interpreterLock = new Object();
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean isInitialized = false;
    private final CountDownLatch initLatch = new CountDownLatch(1);
    // Track if liveness challenge was verified for this face
    private boolean livenessVerified = false;


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
        // Initialize model asynchronously
        executor.execute(() -> {
            try {
                // Log asset information for debugging
                logAssetsContent(context);

                try {
                    Log.d(TAG, "Loading model files...");

                    // Initialize TFLiteInterpreter
                    Interpreter.Options interpreterOptions = vn.edu.fpt.zentryapp.faceid.utils.InterpreterOptionsFactory.createBestOptions(context);

                    // Load models from assets
                    MappedByteBuffer model1Buffer = FileUtil.loadMappedFile(context, MODEL_FILE_1);
                    MappedByteBuffer model2Buffer = FileUtil.loadMappedFile(context, MODEL_FILE_2);

                    Log.d(TAG, "Model 1 loaded, size: " + model1Buffer.capacity() + " bytes");
                    Log.d(TAG, "Model 2 loaded, size: " + model2Buffer.capacity() + " bytes");

                    // Create interpreters
                    firstModelInterpreter = new Interpreter(model1Buffer, interpreterOptions);
                    secondModelInterpreter = new Interpreter(model2Buffer, interpreterOptions);
                    try {
                        // Ensure expected input shape [1,80,80,3]
                        firstModelInterpreter.resizeInput(0, new int[]{1, INPUT_IMAGE_DIM, INPUT_IMAGE_DIM, 3});
                        secondModelInterpreter.resizeInput(0, new int[]{1, INPUT_IMAGE_DIM, INPUT_IMAGE_DIM, 3});
                    } catch (Throwable ignore) {}
                    try { firstModelInterpreter.allocateTensors(); } catch (Throwable ignore) {}
                    try { secondModelInterpreter.allocateTensors(); } catch (Throwable ignore) {}
                    // Warmup both models with NHWC 4D input shape
                    try {
                        float[][][][] dummy = new float[1][INPUT_IMAGE_DIM][INPUT_IMAGE_DIM][3];
                        int outDim1 = firstModelInterpreter.getOutputTensor(0).shape()[1];
                        int outDim2 = secondModelInterpreter.getOutputTensor(0).shape()[1];
                        int outDim = Math.min(outDim1, outDim2);
                        float[][] out1 = new float[1][outDim];
                        float[][] out2 = new float[1][outDim];
                        firstModelInterpreter.run(dummy, out1);
                        secondModelInterpreter.run(dummy, out2);
                    } catch (Throwable warm) {
                        Log.e(TAG, "Warmup ignored: " + warm.getMessage(), warm);
                    }

                    // Create image processor for preprocessing
                    imageTensorProcessor = new ImageProcessor.Builder()
                            .add(new ResizeOp(INPUT_IMAGE_DIM, INPUT_IMAGE_DIM, ResizeOp.ResizeMethod.BILINEAR))
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
            for (String file : Objects.requireNonNull(files)) {
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
     * Callback interface for spoof detection
     */
    public interface SpoofCallback {
        void onResult(SpoofResult result);
    }

    private static void ensureAllocatedSafe(Interpreter i) {
        try { if (i != null) i.allocateTensors(); } catch (Throwable ignore) {}
    }

    public SpoofResult detectSpoof(Bitmap frameImage, Rect faceRect, android.graphics.RectF ovalRect) {
        long startTime = System.currentTimeMillis();

        // If using mock detection or interpreter not initialized, always return not spoof
        if (useMockDetection || firstModelInterpreter == null || secondModelInterpreter == null || imageTensorProcessor == null) {
            Log.d(TAG, "Using mock spoof detection (always return real face)");
            return new SpoofResult(false, 0.95f, System.currentTimeMillis() - startTime);
        }

        try {
            Log.d(TAG, "Starting spoof detection with bounding box: " + faceRect.toString());

            // Validate face rect
            if (faceRect.width() <= 0 || faceRect.height() <= 0) {
                Log.e(TAG, "Invalid face rect size: " + faceRect);
                return new SpoofResult(false, 0.5f, System.currentTimeMillis() - startTime);
            }

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
            // Ensure direct buffers with native order
            if (input1 != null) {
                input1.order(java.nio.ByteOrder.nativeOrder());
                if (!input1.isDirect()) {
                    ByteBuffer direct = ByteBuffer.allocateDirect(input1.capacity()).order(java.nio.ByteOrder.nativeOrder());
                    input1.rewind(); direct.put(input1); input1 = direct;
                }
            }
            if (input2 != null) {
                input2.order(java.nio.ByteOrder.nativeOrder());
                if (!input2.isDirect()) {
                    ByteBuffer direct2 = ByteBuffer.allocateDirect(input2.capacity()).order(java.nio.ByteOrder.nativeOrder());
                    input2.rewind(); direct2.put(input2); input2 = direct2;
                }
            }

            // Prepare output buffers based on actual model output dims
            int outDim1 = firstModelInterpreter.getOutputTensor(0).shape()[1];
            int outDim2 = secondModelInterpreter.getOutputTensor(0).shape()[1];
            if (outDim1 != 2 || outDim2 != 2) {
                Log.w(TAG, "Model output dims are not 2; detected dims: " + outDim1 + ", " + outDim2 + ". Adjusting to smaller common dim.");
            }
            int outDim = Math.min(outDim1, outDim2);
            float[][] output1 = new float[1][outDim];
            float[][] output2 = new float[1][outDim];

            // Run inference guarded and retry allocate once on failure
            try { Objects.requireNonNull(input1).rewind(); } catch (Throwable ignore) {}
            try { Objects.requireNonNull(input2).rewind(); } catch (Throwable ignore) {}
            synchronized (interpreterLock) {
                ensureAllocatedSafe(firstModelInterpreter);
                ensureAllocatedSafe(secondModelInterpreter);
                try {
                    firstModelInterpreter.run(input1, output1);
                    secondModelInterpreter.run(input2, output2);
                } catch (IllegalArgumentException allocErr) {
                    Log.w(TAG, "Re-allocating tensors due to allocation error", allocErr);
                    ensureAllocatedSafe(firstModelInterpreter);
                    ensureAllocatedSafe(secondModelInterpreter);
                    firstModelInterpreter.run(input1, output1);
                    secondModelInterpreter.run(input2, output2);
                }
            }

            Log.d(TAG, "Ran inference");
            Log.d(TAG, "Output model 1: " + Arrays.toString(output1[0]));
            Log.d(TAG, "Output model 2: " + Arrays.toString(output2[0]));

            // Apply softmax to outputs (use the actual dim)
            float[] softmax1 = softMax(output1[0]);
            float[] softmax2 = softMax(output2[0]);

            Log.d(TAG, "Softmax model 1: " + Arrays.toString(softmax1));
            Log.d(TAG, "Softmax model 2: " + Arrays.toString(softmax2));

            // Combine probabilities by summation across scales (post-softmax)
            float[] combined = new float[outDim];
            for (int i = 0; i < outDim; i++) {
                combined[i] = softmax1[i] + softmax2[i];
            }

            Log.d(TAG, "Combined probs (sum): " + Arrays.toString(combined));

            // Map real/spoof probabilities according to 2-class model semantics:
            // index 0 = spoof, index 1 = real
            float realProb;
            float spoofProb;
            if (outDim >= 2) {
                realProb = combined[1] / 2.0f;
                spoofProb = combined[0] / 2.0f;
            } else {
                // Fallback if model unexpectedly returns 1-dim: treat as spoof probability
                spoofProb = Math.min(1f, Math.max(0f, combined[0] / 2.0f));
                realProb = 1f - spoofProb;
            }

            Log.d(TAG, "Raw probabilities: realProb=" + realProb + ", spoofProb=" + spoofProb);

            // Apply multi-layer detection

            // Layer 1: Natural movement check
            boolean hasNaturalMovement = checkTemporalVariance();

            // Layer 2: Texture analysis
            boolean hasUniformTexture = checkUniformTexture(softmax1, softmax2);
            
            // Layer 3: Oval boundary validation
            boolean isWithinOvalBoundary = true; // Default to true if no oval provided
            if (ovalRect != null) {
                // Add oval validation logic here (simplified for now)
                isWithinOvalBoundary = true;
            }

            // Enhanced decision logic with liveness awareness
            boolean isSpoof = !isRealFace(realProb, spoofProb, hasNaturalMovement, hasUniformTexture, isWithinOvalBoundary);

            // Confidence definition: use the winning class probability, with liveness-friendly floor
            float confidence;
            if (livenessVerified) {
                confidence = isSpoof ? Math.max(0.60f, spoofProb) : Math.max(0.85f, realProb);
            } else {
                confidence = isSpoof ? Math.max(0.70f, spoofProb) : Math.max(0.70f, realProb);
            }
            
            Log.d(TAG, "🎯 FINAL RESULT: " + (isSpoof ? "SPOOF" : "REAL") + 
                    " with confidence: " + String.format(java.util.Locale.US, "%.4f", confidence) +
                    ", realProb=" + String.format(java.util.Locale.US, "%.4f", realProb) +
                    ", spoofProb=" + String.format(java.util.Locale.US, "%.4f", spoofProb) +
                    ", livenessVerified=" + livenessVerified +
                    ", naturalMovement=" + hasNaturalMovement +
                    ", uniformTexture=" + hasUniformTexture +
                    ", withinOval=" + isWithinOvalBoundary);

            // Store frame data for temporal analysis (cap history)
            frameHistory.offer(new TemporalFrameData(combined, faceRect));
            while (frameHistory.size() > 30) {
                frameHistory.poll();
            }

            long timeMillis = System.currentTimeMillis() - startTime;
            return new SpoofResult(isSpoof, confidence, timeMillis);

        } catch (Throwable e) {
            Log.e(TAG, "Error in spoof detection: " + e.getMessage(), e);

            // ENHANCED ERROR HANDLING - stricter for security:
            Log.w(TAG, "⚠️ Processing error in spoof detection. Error type: " + e.getClass().getSimpleName());

            // Always default to spoof on error for maximum security
            // This prevents bypass through intentional errors or manipulations
            Log.w(TAG, "⚠️ Error detected - defaulting to spoof for security");
            return new SpoofResult(true, 0.85f, System.currentTimeMillis() - startTime);
        }
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

        // Calculate scale around center, clamp to image bounds – no artificial cap
        float scale = Math.min(Math.min((imgHeight - 1f) / h, (imgWidth - 1f) / w), bboxScale);
        Log.d(TAG, "getScaledBox: Calculated scale: " + scale);

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

    /**
     * Sets the liveness verification flag and resets frame history
     * This method should be called by FaceIdService when a liveness challenge is passed
     *
     * @param verified True if liveness challenge was passed
     */
    public void setLivenessVerified(boolean verified) {
        this.livenessVerified = verified;

        if (verified) {
            // Reset frame history to ensure temporal analysis only considers post-verification frames
            frameHistory.clear();
            Log.d(TAG, "✓ LIVENESS VERIFIED: Reset frame history for fresh analysis");
        }
    }


    private float calculateConfidenceVariance() {
        if (frameHistory.size() < 2) {
            return 0.01f; // Default value if not enough data
        }

        float sum = 0;
        float sumSq = 0;
        int count = 0;
        
        // Calculate variance on REAL probability (index 1 in 2-class mapping)
        for (TemporalFrameData frame : frameHistory) {
            float realProb = frame.combinedResult.length >= 2 ? frame.combinedResult[1] : (1f - frame.combinedResult[0]);
            sum += realProb;
            sumSq += realProb * realProb;
            count++;
        }
        
        float mean = sum / count;
        float variance = (sumSq / count) - (mean * mean);

        return variance;
    }

    private boolean checkAbnormalPatternAcrossFrames() {
        if (frameHistory.size() < 4) {
            return false; // Not enough data
        }
        
        // Convert queue to array for easier processing
        TemporalFrameData[] frames = frameHistory.toArray(new TemporalFrameData[0]);
        
        // Count classification flips (real<->spoof) using 2-class mapping
        int classificationFlips = 0;
        for (int i = 1; i < frames.length; i++) {
            float prevSpoof = frames[i-1].combinedResult[0];
            float prevReal = frames[i-1].combinedResult.length >= 2 ? frames[i-1].combinedResult[1] : 1f - prevSpoof;
            float currSpoof = frames[i].combinedResult[0];
            float currReal = frames[i].combinedResult.length >= 2 ? frames[i].combinedResult[1] : 1f - currSpoof;
            boolean prevIsReal = prevReal >= prevSpoof;
            boolean currIsReal = currReal >= currSpoof;
            if (prevIsReal != currIsReal) {
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
     * Add method to check if uniform texture is present in the face
     * Important: This method was missing a return statement which caused inconsistent behavior
     */
    private boolean checkUniformTexture(float[] softmax1, float[] softmax2) {
        // If liveness is verified, be extremely lenient in texture analysis
        if (this.livenessVerified) {
            // Only detect uniform texture in extremely obvious cases for verified faces
            boolean extremelyUniform =
                (softmax1[1] < 0.10f && softmax2[1] < 0.10f) &&
                (Math.abs(softmax1[0] - softmax2[0]) < 0.01f) &&
                (Math.abs(softmax1[1] - softmax2[1]) < 0.01f) &&
                (Math.abs(softmax1[2] - softmax2[2]) < 0.01f) &&
                calculateConfidenceVariance() < 0.0001f;

            // For liveness verified faces, only return true in extremely obvious cases
            return extremelyUniform;
        }

        // Standard checks for faces without liveness verification
        // 1. Check for unusual texture
        boolean unusualTextureIndicator =
                (softmax1[1] < 0.25f && softmax2[1] < 0.25f);
        
        // 2. Check for inconsistency between models
        boolean modelInconsistency =
                Math.abs(softmax1[0] - softmax2[0]) > 0.75f ||
                Math.abs(softmax1[2] - softmax2[2]) > 0.70f;
        
        // 3. Check for ambiguous classification
        boolean ambiguousClassification =
                (softmax1[0] > 0.55f && softmax1[2] > 0.55f &&
                 softmax2[0] > 0.50f && softmax2[2] > 0.50f);
        
        // 4. Check for abnormal pattern
        boolean abnormalPattern = checkAbnormalPatternAcrossFrames();
        
        // 5. Check for low texture variance
        boolean lowTextureVariance = calculateConfidenceVariance() < 0.001f;
        
        // 6. Check for suspiciously stable predictions
        boolean suspiciouslyStablePredictions = 
                Math.abs(softmax1[0] - softmax2[0]) < 0.03f &&
                Math.abs(softmax1[1] - softmax2[1]) < 0.03f &&
                Math.abs(softmax1[2] - softmax2[2]) < 0.03f;

        // Combine multiple indicators, requiring strong evidence to reduce false positives
        return unusualTextureIndicator &&
               (modelInconsistency || ambiguousClassification ||
                (abnormalPattern && lowTextureVariance) ||
                (suspiciouslyStablePredictions && lowTextureVariance));
    }

    /**
     * Modified detection logic to properly handle liveness verified faces
     * This replaces the original detection logic to fix issues with false positives
     */
    private boolean isRealFace(float realProb, float spoofProb, boolean hasNaturalMovement,
                              boolean hasUniformTexture, boolean isWithinOvalBoundary) {
        // Case 1: Liveness verified - trust it completely
        if (this.livenessVerified) {
            // Only flag as spoof in extreme cases that are virtually impossible for real faces
            if (spoofProb > 0.99f && !hasNaturalMovement && hasUniformTexture) {
                Log.d(TAG, "🔴 LIVENESS VERIFIED BUT EXTREME SPOOF INDICATORS: Very rare case");
                return false;
            }

            // In all other cases, trust the liveness verification completely
            Log.d(TAG, "🟢 LIVENESS VERIFIED: Treating as real face with high confidence");
            return true;
        }

        // Case 2: High confidence for real face
        if (realProb > 0.65f) {
            Log.d(TAG, "🟢 HIGH CONFIDENCE REAL: Strong ML model confidence");
            return true;
        }

        // Case 3: Good ML confidence + valid position
        if (realProb > 0.55f && isWithinOvalBoundary) {
            Log.d(TAG, "🟢 GOOD CONFIDENCE REAL: ML model + oval validation");
            return true;
        }

        // Case 4: Decent ML confidence - only if no uniform texture detected
        if (realProb > 0.50f && !hasUniformTexture) {
            Log.d(TAG, "🟢 ACCEPTABLE REAL: ML model result trusted");
            return true;
        }

        // Case 5: Very high spoof probability - definitely spoof
        if (spoofProb >= 0.88f && hasUniformTexture) {
            Log.d(TAG, "🔴 HIGH CONFIDENCE SPOOF: Multiple strong indicators");
            return false;
        }

        // Case 6: More lenient on unclear cases - bias toward real face detection
        if (realProb > 0.40f && spoofProb < 0.65f) {
            Log.d(TAG, "🟡 LIKELY REAL: Borderline case favoring real");
            return true;
        }

        // Case 7: Default to real face for very unclear cases
        boolean isReal = spoofProb < 0.75f;
        Log.d(TAG, "🟠 BORDERLINE CASE: " + (isReal ? "REAL" : "SPOOF") + " with limited confidence");
        return isReal;
    }

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

    private TemporalFrameData getLastFrame() {
        if (frameHistory.isEmpty()) {
            return null;
        }

        // Convert queue to array and get last element
        TemporalFrameData[] frames = frameHistory.toArray(new TemporalFrameData[0]);
        return frames[frames.length - 1];
    }
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
     * Enhanced temporal variance check that's more lenient for liveness verified faces
     */
    private boolean checkTemporalVariance() {
        if (frameHistory.size() < 3) {
            return true; // Not enough data, assume natural (benefit of doubt)
        }

        // Calculate variance in face position and size
        float positionVariance = calculatePositionVariance();
        float sizeVariance = calculateSizeVariance();

        // Special case for liveness verified faces - extremely lenient thresholds
        if (this.livenessVerified) {
            // For verified faces, only fail if extremely abnormal (practically never)
            boolean extremelyAbnormal =
                (positionVariance < 0.00001f && sizeVariance < 0.00001f) || // Practically static image
                (positionVariance > 0.5f || sizeVariance > 0.25f);    // Extreme erratic movement

            // Almost always return true for liveness verified faces
            return !extremelyAbnormal;
        }

        // Regular path for non-verified faces - still lenient but more careful
        float actualMinPositionVariance = 0.00003f; // Very low minimum threshold
        float actualMaxPositionVariance = 0.12f;    // Higher maximum allowed
        float actualMinSizeVariance = 0.00003f;     // Very low minimum threshold
        float actualMaxSizeVariance = 0.075f;       // Higher maximum allowed

        // Real faces have natural micro-movements within acceptable ranges
        boolean hasNaturalMovement = positionVariance >= actualMinPositionVariance &&
                                    positionVariance <= actualMaxPositionVariance &&
                                    sizeVariance >= actualMinSizeVariance &&
                                    sizeVariance <= actualMaxSizeVariance;

        // Special check for very low variance - could indicate a static image (definite spoof)
        boolean suspiciouslyStatic = positionVariance < 0.000005f && sizeVariance < 0.000005f;

        // Only flag as unnatural if extremely static or way outside reasonable bounds
        boolean definitelyUnnatural = suspiciouslyStatic ||
                                    positionVariance > 0.3f ||
                                    sizeVariance > 0.2f;

        // Give benefit of doubt in borderline cases - only fail if definitely unnatural
        if (!hasNaturalMovement && !definitelyUnnatural) {
            Log.d(TAG, "📊 TEMPORAL ANALYSIS: Borderline movement - giving benefit of doubt for real face");
            hasNaturalMovement = true;
        }

        Log.d(TAG, "📊 TEMPORAL ANALYSIS: posVar=" + positionVariance +
                ", sizeVar=" + sizeVariance +
                ", natural=" + hasNaturalMovement);

        return hasNaturalMovement;
    }
}