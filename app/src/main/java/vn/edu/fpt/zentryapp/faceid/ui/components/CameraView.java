package vn.edu.fpt.zentryapp.faceid.ui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.Audio;
import androidx.lifecycle.LifecycleOwner;

import vn.edu.fpt.zentryapp.BuildConfig;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import android.os.Handler;
import android.os.Looper;

/**
 * Custom camera view for face capture with continuous frame analysis
 */
public class CameraView extends FrameLayout {
    private static final String TAG = "CameraView";
    
    // Switched to natario CameraView as the rendering & capture engine
    private com.otaliastudios.cameraview.CameraView natarioView;
    private AtomicBoolean processingFrame = new AtomicBoolean(false);
    private final Handler testFrameHandler = new Handler(Looper.getMainLooper());
    private Runnable activeTestFrameRunnable;
    private boolean testFrameInjectionEnabled = false;
    private Bitmap testFrameBitmap;
    private Bitmap preparedTestFrameBitmap; // mirrored & prepared to match pipeline
    private int testFrameIntervalMs = 66; // ~15 FPS by default

    // Quality gate configuration (high impact first)
    private boolean qualityGateEnabled = true;
    // Acceptable luminance mean range on Y channel (0..255)
    private int minLumaMean = 60;
    private int maxLumaMean = 200;
    // Minimum dynamic range (maxY - minY) on Y channel
    private float minDynamicRange = 40f;
    // Minimum blur metric (variance of Laplacian on downscaled grayscale)
    private float minLaplacianVariance = 120f;

    private int frameCount = 0;

    
    public interface FrameAnalysisCallback {
        void onFrameAnalyzed(Bitmap bitmap);
    }
    
    public CameraView(@NonNull Context context) {
        super(context);
        init(context);
    }
    
    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        // Create and add natario CameraView
        natarioView = new com.otaliastudios.cameraview.CameraView(context);
        natarioView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        // Configure defaults
        natarioView.setFacing(Facing.FRONT);
        natarioView.setMode(Mode.PICTURE);
        // Disable audio to avoid requiring RECORD_AUDIO permission
        natarioView.setAudio(Audio.OFF);
        addView(natarioView);
    }
    
    /**
     * Start the camera
     * @param lifecycleOwner Lifecycle owner
     */
    public void startCamera(LifecycleOwner lifecycleOwner) {
        startCamera(lifecycleOwner, null);
    }
    
    /**
     * Start the camera with frame analysis
     * @param lifecycleOwner Lifecycle owner
     * @param frameCallback Callback for frame analysis
     */
    public void startCamera(LifecycleOwner lifecycleOwner, @Nullable FrameAnalysisCallback frameCallback) {
        Log.d(TAG, "Starting natario CameraView with frame analysis: " + (frameCallback != null));

        try {
            // If test frame injection is enabled, bypass real camera and feed constant frames
            if (testFrameInjectionEnabled && preparedTestFrameBitmap != null && frameCallback != null) {
                Log.d(TAG, "Test frame injection mode enabled. Skipping real camera.");
                // Initialize coordinate mapping for test frames (front preview mirrored, bitmap mirrored)
                try {
                    boolean isPreviewMirrored = true;
                    boolean isBitmapMirrored = true; // preparedTestFrameBitmap already mirrored
                    vn.edu.fpt.zentryapp.faceid.util.CoordinateMapper.getInstance().updateMappingWithPolicy(
                            getWidth(), getHeight(), preparedTestFrameBitmap.getWidth(), preparedTestFrameBitmap.getHeight(),
                            isPreviewMirrored, isBitmapMirrored
                    );
                } catch (Exception ignored) {}
                startTestFrameLoop(bitmap -> {
                    if (!passesQualityGate(bitmap)) {
                        return;
                    }
                    frameCallback.onFrameAnalyzed(bitmap);
                });
                return;
            }

            // Attach to lifecycle if supported
            try {
                natarioView.setLifecycleOwner(lifecycleOwner);
            } catch (Throwable ignored) {}

            if (frameCallback != null) {
                natarioView.clearFrameProcessors();
                natarioView.addFrameProcessor(new FrameProcessor() {
                    @Override
                    public void process(@NonNull Frame frame) {
                        frameCount++;
                        if (processingFrame.get()) {
                            if (frameCount % 30 == 0) {
                                Log.d(TAG, "Skipping frame #" + frameCount + " - still processing previous frame");
                            }
                            return;
                        }

                        processingFrame.set(true);
                        try {
                            int width = frame.getSize().getWidth();
                            int height = frame.getSize().getHeight();
                            Bitmap bitmap = frameToBitmap(frame, width, height);
                            if (bitmap == null) {
                                processingFrame.set(false);
                                return;
                            }

                            // Rotate to user orientation if needed
                            try {
                                int rotationToUser = frame.getRotationToUser();
                                if (rotationToUser != 0) {
                                    Matrix rot = new Matrix();
                                    rot.postRotate(rotationToUser);
                                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rot, true);
                                }
                            } catch (Throwable ignored) {}

                            // Mirror horizontally for front camera to keep consistency with previous pipeline
                            Matrix mirrorMatrix = new Matrix();
                            mirrorMatrix.preScale(-1.0f, 1.0f);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mirrorMatrix, true);

                            // Quality gate check
                            if (!passesQualityGate(bitmap)) {
                                processingFrame.set(false);
                                return;
                            }

                            // Update coordinate mapping using standardized policy
                            try {
                                boolean isPreviewMirrored = true;
                                boolean isBitmapMirrored = true;
                                vn.edu.fpt.zentryapp.faceid.util.CoordinateMapper.getInstance().updateMappingWithPolicy(
                                        getWidth(), getHeight(), bitmap.getWidth(), bitmap.getHeight(),
                                        isPreviewMirrored, isBitmapMirrored
                                );
                            } catch (Exception ignored) {}

                            final Bitmap finalBitmap = bitmap;
                            post(() -> {
                                try {
                                    frameCallback.onFrameAnalyzed(finalBitmap);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error delivering analyzed frame", e);
                                } finally {
                                    processingFrame.set(false);
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing natario frame", e);
                            processingFrame.set(false);
                        }
                    }
                });
            } else {
                natarioView.clearFrameProcessors();
            }

            natarioView.open();
            Log.d(TAG, "natario CameraView opened");
        } catch (Exception e) {
            Log.e(TAG, "Error starting natario CameraView", e);
        }
    }


    public void stopCamera() {
        try {
            if (natarioView != null) {
                natarioView.close();
            }
            // Stop any active test frame loop
            if (activeTestFrameRunnable != null) {
                testFrameHandler.removeCallbacks(activeTestFrameRunnable);
                activeTestFrameRunnable = null;
            }
        } catch (Exception ignored) {}
    }

    // ---------------------------
    // Test Frame Injection APIs
    // ---------------------------

    /**
     * Enable test frame injection with the provided bitmap. Frames will be delivered at the given fps.
     * Call before startCamera().
     */
    public void setTestFrameInjection(@NonNull Bitmap testFrame, int targetFps) {
        this.testFrameInjectionEnabled = true;
        this.testFrameBitmap = testFrame;
        this.preparedTestFrameBitmap = prepareBitmapForPipeline(testFrame);
        setTestFrameFps(targetFps);
    }

    /** Disable test frame injection and return to real camera input. */
    public void disableTestFrameInjection() {
        this.testFrameInjectionEnabled = false;
        this.testFrameBitmap = null;
        this.preparedTestFrameBitmap = null;
        if (activeTestFrameRunnable != null) {
            testFrameHandler.removeCallbacks(activeTestFrameRunnable);
            activeTestFrameRunnable = null;
        }
    }

    /** Optional: set the FPS used when injecting test frames. */
    public void setTestFrameFps(int targetFps) {
        if (targetFps <= 0) targetFps = 15;
        this.testFrameIntervalMs = Math.max(10, 1000 / targetFps);
    }

    /** Convenience: load a bitmap from file path for test injection. Returns true on success. */
    public boolean setTestFrameFromFile(@NonNull String filePath, int targetFps) {
        try {
            Bitmap bmp = BitmapFactory.decodeFile(filePath);
            if (bmp == null) {
                Log.e(TAG, "Failed to decode test frame from file: " + filePath);
                return false;
            }
            setTestFrameInjection(bmp, targetFps);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error loading test frame from file", e);
            return false;
        }
    }

    /** Convenience: load a bitmap from a Uri using the given context. Returns true on success. */
    public boolean setTestFrameFromUri(@NonNull Context context, @NonNull Uri uri, int targetFps) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return false;
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) return false;
            setTestFrameInjection(bmp, targetFps);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error loading test frame from uri", e);
            return false;
        }
    }

    private void startTestFrameLoop(@NonNull FrameAnalysisCallback frameCallback) {
        if (preparedTestFrameBitmap == null) return;
        // Ensure previous loop is stopped
        if (activeTestFrameRunnable != null) {
            testFrameHandler.removeCallbacks(activeTestFrameRunnable);
        }
        activeTestFrameRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // Deliver the same prepared bitmap to mimic camera pipeline
                    frameCallback.onFrameAnalyzed(preparedTestFrameBitmap);
                } catch (Exception e) {
                    Log.e(TAG, "Error delivering test frame", e);
                } finally {
                    // Schedule next frame if still enabled
                    if (testFrameInjectionEnabled && preparedTestFrameBitmap != null) {
                        testFrameHandler.postDelayed(this, testFrameIntervalMs);
                    }
                }
            }
        };
        testFrameHandler.post(activeTestFrameRunnable);
        Log.d(TAG, "Test frame loop started at interval: " + testFrameIntervalMs + "ms");
    }

    // ---------------------------
    // Quality Gate
    // ---------------------------

    private boolean passesQualityGate(@NonNull Bitmap bitmap) {
        if (!qualityGateEnabled) return true;
        try {
            // Downscale for speed
            int targetW = 320;
            int targetH = Math.max(1, bitmap.getHeight() * targetW / Math.max(1, bitmap.getWidth()));
            Bitmap small = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true);

            // Compute Y channel stats and Laplacian variance on grayscale
            int[] pixels = new int[small.getWidth() * small.getHeight()];
            small.getPixels(pixels, 0, small.getWidth(), 0, 0, small.getWidth(), small.getHeight());

            int minY = 255, maxY = 0;
            long sumY = 0;
            // Simple grayscale Laplacian (3x3 kernel)
            float lapVar = 0f;
            // First compute grayscale buffer
            int w = small.getWidth();
            int h = small.getHeight();
            int[] gray = new int[w * h];
            for (int i = 0; i < pixels.length; i++) {
                int c = pixels[i];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = (c) & 0xFF;
                int y = (int)(0.299f * r + 0.587f * g + 0.114f * b);
                gray[i] = y;
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                sumY += y;
            }
            float meanY = (float) sumY / (float) gray.length;
            float dynamicRange = (float)(maxY - minY);

            // Laplacian
            long sumLap = 0;
            long sumLapSq = 0;
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    int cIdx = y * w + x;
                    int lap = -gray[cIdx - w] - gray[cIdx - 1] + 4 * gray[cIdx] - gray[cIdx + 1] - gray[cIdx + w];
                    sumLap += lap;
                    sumLapSq += (long) lap * (long) lap;
                }
            }
            int n = (w - 2) * (h - 2);
            if (n <= 0) return true; // trivial accept
            float meanLap = (float) sumLap / (float) n;
            float varLap = (float) sumLapSq / (float) n - meanLap * meanLap;

            boolean okLuma = meanY >= minLumaMean && meanY <= maxLumaMean;
            boolean okRange = dynamicRange >= minDynamicRange;
            boolean okBlur = varLap >= minLaplacianVariance;

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "QualityGate -> meanY=" + meanY + ", range=" + dynamicRange + ", lapVar=" + varLap
                        + " | okLuma=" + okLuma + ", okRange=" + okRange + ", okBlur=" + okBlur);
            }

            return okLuma && okRange && okBlur;
        } catch (Exception e) {
            Log.w(TAG, "Quality gate failed open due to exception", e);
            return true; // fail-open to avoid blocking capture
        }
    }
    /** Prepare a bitmap to match the normal pipeline (mirror for front camera). */
    private Bitmap prepareBitmapForPipeline(@NonNull Bitmap source) {
        try {
            Matrix mirrorMatrix = new Matrix();
            mirrorMatrix.preScale(-1.0f, 1.0f);
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), mirrorMatrix, true);
        } catch (Exception e) {
            Log.w(TAG, "Failed to prepare test frame bitmap, returning original", e);
            return source;
        }
    }

    // Convert natario Frame to Bitmap (NV21/YUV -> JPEG -> Bitmap)
    private Bitmap frameToBitmap(Frame frame, int width, int height) {
        try {
            Object dataObj = frame.getData();
            byte[] nv21;
            if (dataObj instanceof byte[]) {
                nv21 = (byte[]) dataObj;
            } else if (dataObj instanceof java.nio.ByteBuffer) {
                java.nio.ByteBuffer buffer = (java.nio.ByteBuffer) dataObj;
                nv21 = new byte[buffer.remaining()];
                buffer.get(nv21);
            } else {
                Log.w(TAG, "Unsupported frame data type: " + (dataObj != null ? dataObj.getClass() : "null"));
                return null;
            }

            YuvImage yuv = new YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 90, out);
            byte[] jpeg = out.toByteArray();
            return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        } catch (Exception e) {
            Log.e(TAG, "frameToBitmap failed", e);
            return null;
        }
    }
} 