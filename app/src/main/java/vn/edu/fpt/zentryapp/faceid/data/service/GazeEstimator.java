package vn.edu.fpt.zentryapp.faceid.data.service;

import android.content.Context;
import android.graphics.Bitmap;

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

/**
 * Gaze direction estimator using TensorFlow Lite model
 * Based on the GAZEL model architecture
 */
public class GazeEstimator {
    private static final String TAG = "GazeEstimator";

    // TensorFlow Lite model parameters
    private static final String MODEL_FILE = "gazel_shared_ver9.tflite";
    private static final int INPUT_SIZE = 64; // Model input size (square)
    private static final int FLOAT_BYTES = 4; // Size of float in bytes

    // Input tensor indices (will be determined during model inspection)
    private int imageInputIndex = 0;
    private int faceposInputIndex = 1;
    
    // Map to store all input tensor indices by name
    private Map<String, Integer> inputTensorIndices = new HashMap<>();

    // Interpreter and associated objects
    private Interpreter interpreter;
    private ByteBuffer inputBuffer;
    private float[][] outputBuffer; // [1][2] - x,y gaze coordinates (normalized -1 to 1)

    // Last estimated gaze
    private float gazeX = 0; // -1 (left) to 1 (right)
    private float gazeY = 0; // -1 (up) to 1 (down)

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
    private float emaAlpha = 0.4f; // smoothing factor

    // Head pose blending weight
    private float headPoseWeight = 0.2f; // reduced from 0.3 to avoid over-correction

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
                // Initialize input and output buffers
                inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * FLOAT_BYTES);
                inputBuffer.order(ByteOrder.nativeOrder());
                outputBuffer = new float[1][2]; // x,y gaze direction

                Log.d(TAG, "Buffers initialized, loading TensorFlow Lite model...");

                // Initialize TensorFlow Lite with the gaze model
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
                
                // Store the tensor indices based on name for backward compatibility
                if (tensorName.equals("facepos")) {
                    faceposInputIndex = i;
                    Log.d(TAG, "Found facepos tensor at index " + i);
                } else {
                    // Assume any non-facepos tensor is for the image
                    imageInputIndex = i;
                    Log.d(TAG, "Found image input tensor at index " + i);
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
     * Estimate gaze direction from eye images
     *
     * @param leftEyeImage  Left eye image bitmap
     * @param rightEyeImage Right eye image bitmap
     * @param headPose      Head pose angles [pitch, roll, yaw]
     * @return True if gaze was successfully estimated
     */
    public boolean estimateGaze(Bitmap leftEyeImage, Bitmap rightEyeImage, float[] headPose) {
        try {
            if (!isInitialized) {
                Log.w(TAG, "TensorFlow Lite interpreter not initialized, falling back to simulation");
                return simulateGazeEstimation(headPose);
            }

            // Check if input images are valid
            if (leftEyeImage == null || rightEyeImage == null) {
                Log.w(TAG, "Eye images are null, falling back to simulation");
                return simulateGazeEstimation(headPose);
            }

            // Choose eye dynamically in single-input mode by head pose/quality
            Bitmap primaryEye = leftEyeImage;
            if (headPose != null && headPose.length >= 3) {
                float yaw = headPose[2];
                // If turning right (positive yaw), prioritize right eye; if left, prioritize left eye
                if (yaw > 0) primaryEye = rightEyeImage;
            }

            // Process chosen eye
            preprocessImage(primaryEye);

            // Check if interpreter is null
            if (interpreter == null) {
                Log.w(TAG, "TensorFlow Lite interpreter is null, falling back to simulation");
                return simulateGazeEstimation(headPose);
            }

            // Check if model has multiple inputs
            int inputTensorCount = interpreter.getInputTensorCount();
            if (inputTensorCount > 1) {
                // Model has multiple inputs - use runForMultipleInputsOutputs
                Log.d(TAG, "Model has " + inputTensorCount + " input tensors, using runForMultipleInputsOutputs");

                // Create input array (Object[] for the TensorFlow Lite API)
                Object[] inputs = new Object[inputTensorCount];

                // Initialize all inputs to avoid null values
                for (int i = 0; i < inputTensorCount; i++) {
                    String tensorName = interpreter.getInputTensor(i).name();
                    int[] shape = interpreter.getInputTensor(i).shape();
                    
                    inputs[i] = createTensorBufferForName(tensorName, shape, leftEyeImage, rightEyeImage, headPose);
                    Log.d(TAG, "Added input data to index " + i + " (tensor: " + tensorName + ")");
                }

                // Create output map
                Map<Integer, Object> outputMap = new HashMap<>();
                outputMap.put(0, outputBuffer);

                // Run inference with multiple inputs (using correct signature: Object[], Map)
                interpreter.runForMultipleInputsOutputs(inputs, outputMap);
            } else {
                // Model has single input - use regular run method
                Log.d(TAG, "Model has single input tensor, using regular run method");
                inputBuffer.rewind();
                interpreter.run(inputBuffer, outputBuffer);
            }

            // Extract gaze coordinates from the output (range -1 to 1)
            gazeX = outputBuffer[0][0];
            gazeY = outputBuffer[0][1];

            // Apply front camera mirroring if needed
            if (frontCameraMirrored) {
                gazeX = -gazeX;
            }

            // Adjust gaze based on head pose
            if (headPose != null && headPose.length >= 3) {
                adjustGazeWithHeadPose(headPose);
            }

            // Smooth gaze with EMA to stabilize thresholds
            emaGazeX = emaAlpha * gazeX + (1 - emaAlpha) * emaGazeX;
            emaGazeY = emaAlpha * gazeY + (1 - emaAlpha) * emaGazeY;

            // Update gaze history
            updateGazeHistory();

            // Check if looking away from the screen
            checkLookingAway();

            // Log the result
            Log.d(TAG, "Gaze direction: (" + gazeX + ", " + gazeY + "), " +
                    (isLookingAtScreen ? "Looking at screen" : "Looking away"));

            // Notify callback if available (use smoothed values)
            if (callback != null) {
                callback.onGazeUpdate(emaGazeX, emaGazeY, isLookingAtScreen);
                callback.onLookingAway(isLookingAway);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error during model inference", e);
            Log.d(TAG, "Debug info - Image input index: " + imageInputIndex + ", Facepos input index: " + faceposInputIndex);
            // Fall back to simulation if model inference fails
            return simulateGazeEstimation(headPose);
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

            // Log the result
            Log.d(TAG, "Simulated gaze direction: (" + gazeX + ", " + gazeY + "), " +
                    (isLookingAtScreen ? "Looking at screen" : "Looking away"));

            // Notify callback if available
            if (callback != null) {
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
     * Preprocess image for the neural network
     */
    private void preprocessImage(Bitmap eyeImage) {
        try {
            Log.d(TAG, "Preprocessing eye image for model input: " + eyeImage.getWidth() + "x" + eyeImage.getHeight());

            // Reset input buffer
            inputBuffer.rewind();

            // Resize the image if needed
            Bitmap resizedImage = eyeImage;
            if (eyeImage.getWidth() != INPUT_SIZE || eyeImage.getHeight() != INPUT_SIZE) {
                Log.d(TAG, "Resizing eye image to " + INPUT_SIZE + "x" + INPUT_SIZE);
                resizedImage = Bitmap.createScaledBitmap(eyeImage, INPUT_SIZE, INPUT_SIZE, true);
            }

            // Convert bitmap to float array and normalize
            int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
            resizedImage.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

            for (int pixel : pixels) {
                // Extract RGB values
                float r = ((pixel >> 16) & 0xFF) / 255.0f;
                float g = ((pixel >> 8) & 0xFF) / 255.0f;
                float b = (pixel & 0xFF) / 255.0f;

                // Normalize to [-1, 1]
                r = (r - 0.5f) * 2.0f;
                g = (g - 0.5f) * 2.0f;
                b = (b - 0.5f) * 2.0f;

                // Add to input buffer
                inputBuffer.putFloat(r);
                inputBuffer.putFloat(g);
                inputBuffer.putFloat(b);
            }

            // If we created a new bitmap, recycle it
            if (resizedImage != eyeImage) {
                resizedImage.recycle();
            }

            Log.d(TAG, "Image preprocessing completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error preprocessing image: " + e.getMessage(), e);
        }
    }

    /**
     * Create appropriate buffer for a tensor based on its name, shape and dataType
     */
    private ByteBuffer createTensorBuffer(String tensorName, int[] shape) {
        int totalElements = 1;
        for (int dim : shape) {
            totalElements *= dim;
        }
        
        int bufferSize = totalElements * FLOAT_BYTES;
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.order(ByteOrder.nativeOrder());
        
        Log.d(TAG, "Created buffer for tensor " + tensorName + " with shape " + Arrays.toString(shape) + 
              " (size: " + bufferSize + " bytes)");
        
        return buffer;
    }
    
    /**
     * Create appropriate buffer for a tensor based on its name and shape
     */
    private ByteBuffer createTensorBufferForName(String tensorName, int[] shape, Bitmap leftEyeImage, Bitmap rightEyeImage, float[] headPose) {
        // Determine tensor type based on name
        if (tensorName.equals("facepos")) {
            // Head pose tensor - 2 float values (yaw, pitch)
            ByteBuffer buffer = ByteBuffer.allocateDirect(2 * FLOAT_BYTES);
            buffer.order(ByteOrder.nativeOrder());
            if (headPose != null && headPose.length >= 3) {
                buffer.putFloat(headPose[2]); // yaw (left/right)
                buffer.putFloat(headPose[0]); // pitch (up/down)
            } else {
                buffer.putFloat(0.0f); // yaw
                buffer.putFloat(0.0f); // pitch
            }
            buffer.rewind();
            return buffer;
        } else if (tensorName.equals("left_eye") || tensorName.equals("right_eye")) {
            // Image tensor - preprocess image data
            Bitmap eyeImage = tensorName.equals("left_eye") ? leftEyeImage : rightEyeImage;
            return preprocessImageForTensor(eyeImage, tensorName, shape);
        } else if (tensorName.contains("_top") || tensorName.contains("_bottom") || 
                   tensorName.contains("_left") || tensorName.contains("_right")) {
            // Landmark coordinate tensor - 2 float values (x, y coordinates)
            ByteBuffer buffer = ByteBuffer.allocateDirect(2 * FLOAT_BYTES);
            buffer.order(ByteOrder.nativeOrder());
            // Use default coordinates (center of eye region)
            buffer.putFloat(0.0f); // x coordinate
            buffer.putFloat(0.0f); // y coordinate
            buffer.rewind();
            Log.d(TAG, "Created landmark buffer for tensor " + tensorName + " with default coordinates");
            return buffer;
        } else {
            // Unknown tensor type - create buffer with zeros
            ByteBuffer buffer = createTensorBuffer(tensorName, shape);
            buffer.rewind();
            Log.d(TAG, "Created default buffer for unknown tensor " + tensorName);
            return buffer;
        }
    }
    
    /**
     * Preprocess image for a specific tensor based on its requirements
     */
    private ByteBuffer preprocessImageForTensor(Bitmap eyeImage, String tensorName, int[] shape) {
        try {
            Log.d(TAG, "Preprocessing image for tensor " + tensorName + " with shape " + Arrays.toString(shape));
            
            // Create buffer for this tensor
            ByteBuffer tensorBuffer = createTensorBuffer(tensorName, shape);
            
            // Determine image format based on shape
            int expectedHeight = shape[1];
            int expectedWidth = shape[2];
            int expectedChannels = shape.length > 3 ? shape[3] : 1; // Default to 1 channel if not specified
            
            Log.d(TAG, "Expected format: " + expectedWidth + "x" + expectedHeight + "x" + expectedChannels);
            
            // Resize image if needed
            Bitmap resizedImage = eyeImage;
            if (eyeImage.getWidth() != expectedWidth || eyeImage.getHeight() != expectedHeight) {
                Log.d(TAG, "Resizing image to " + expectedWidth + "x" + expectedHeight);
                resizedImage = Bitmap.createScaledBitmap(eyeImage, expectedWidth, expectedHeight, true);
            }
            
            // Convert bitmap to float array based on expected format
            int[] pixels = new int[expectedWidth * expectedHeight];
            resizedImage.getPixels(pixels, 0, expectedWidth, 0, 0, expectedWidth, expectedHeight);
            
            for (int pixel : pixels) {
                if (expectedChannels == 1) {
                    // Grayscale: convert to grayscale and normalize
                    float gray = ((pixel >> 16) & 0xFF) * 0.299f + 
                               ((pixel >> 8) & 0xFF) * 0.587f + 
                               (pixel & 0xFF) * 0.114f;
                    gray = gray / 255.0f;
                    // Normalize to [-1, 1]
                    gray = (gray - 0.5f) * 2.0f;
                    tensorBuffer.putFloat(gray);
                } else if (expectedChannels == 3) {
                    // RGB: extract and normalize each channel
                    float r = ((pixel >> 16) & 0xFF) / 255.0f;
                    float g = ((pixel >> 8) & 0xFF) / 255.0f;
                    float b = (pixel & 0xFF) / 255.0f;
                    
                    // Normalize to [-1, 1]
                    r = (r - 0.5f) * 2.0f;
                    g = (g - 0.5f) * 2.0f;
                    b = (b - 0.5f) * 2.0f;
                    
                    tensorBuffer.putFloat(r);
                    tensorBuffer.putFloat(g);
                    tensorBuffer.putFloat(b);
                }
            }
            
            tensorBuffer.rewind();
            
            // If we created a new bitmap, recycle it
            if (resizedImage != eyeImage) {
                resizedImage.recycle();
            }
            
            Log.d(TAG, "Image preprocessing completed for tensor " + tensorName);
            return tensorBuffer;
            
        } catch (Exception e) {
            Log.e(TAG, "Error preprocessing image for tensor " + tensorName + ": " + e.getMessage(), e);
            // Return a buffer with zeros as fallback
            ByteBuffer fallbackBuffer = createTensorBuffer(tensorName, shape);
            fallbackBuffer.rewind();
            return fallbackBuffer;
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

    


