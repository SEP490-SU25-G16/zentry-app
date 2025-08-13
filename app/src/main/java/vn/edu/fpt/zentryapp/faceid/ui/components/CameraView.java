package vn.edu.fpt.zentryapp.faceid.ui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom camera view for face capture with continuous frame analysis
 */
public class CameraView extends FrameLayout {
    private static final String TAG = "CameraView";

    private PreviewView previewView;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private AtomicBoolean processingFrame = new AtomicBoolean(false);

    // Biến để log info chỉ một lần
    private boolean hasLoggedImageInfo = false;
    private int frameCount = 0;

    public interface CaptureCallback {
        void onCaptured(Bitmap bitmap);
        void onError(String message);
    }

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
        // Create and add PreviewView
        previewView = new PreviewView(context);
        addView(previewView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
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
        Log.d(TAG, "Starting camera with frame analysis: " + (frameCallback != null));

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(getContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(lifecycleOwner, frameCallback);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    /**
     * Bind camera use cases
     */
    private void bindCameraUseCases(LifecycleOwner lifecycleOwner, @Nullable FrameAnalysisCallback frameCallback) {
        Log.d(TAG, "Binding camera use cases, frameCallback: " + (frameCallback != null));

        // Get screen metrics
        int rotation = previewView.getDisplay().getRotation();

        // Log device information for debugging
        String deviceInfo = DeviceSpecificOptimizer.getDeviceInfoString();
        Log.d(TAG, "Device info: " + deviceInfo);

        // CameraSelector - Front camera for face capture
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        // Preview use case
        Preview preview = new Preview.Builder()
                .setTargetRotation(rotation)
                .build();

        // ImageCapture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(rotation)
                .build();

        // ImageAnalysis use case (if callback provided)
        if (frameCallback != null) {
            Log.d(TAG, "Setting up ImageAnalysis with YUV format (default)");

            // Get optimal resolution based on device
            Size targetResolution = DeviceSpecificOptimizer.getOptimalResolutionForDevice();
            Log.d(TAG, "Using target resolution: " + targetResolution.getWidth() + "x" + targetResolution.getHeight());

            // Cấu hình phân tích hình ảnh với độ phân giải phù hợp
            // Không set OUTPUT_IMAGE_FORMAT để sử dụng format mặc định (YUV_420_888)
            // Đã bỏ RGBA_8888 để tăng tính tương thích với nhiều thiết bị
            imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(targetResolution) // Sử dụng độ phân giải tối ưu cho thiết bị
                    .build();

            imageAnalysis.setAnalyzer(executor, image -> {
                frameCount++;

                // Log mỗi 10 frames thay vì 30 để có thêm thông tin debug
                if (frameCount % 10 == 0) {
                    Log.d(TAG, "Processing frame #" + frameCount + ", format: " + image.getFormat() +
                            ", size: " + image.getWidth() + "x" + image.getHeight());
                }

                // Skip if we're still processing a previous frame
                if (processingFrame.get()) {
                    if (frameCount % 10 == 0) {
                        Log.d(TAG, "Skipping frame #" + frameCount + " - still processing previous frame");
                    }
                    image.close();
                    return;
                }

                try {
                    processingFrame.set(true);

                    // Log thông tin về định dạng ảnh (luôn log cho 5 frame đầu tiên)
                    if (!hasLoggedImageInfo || frameCount <= 5) {
                        logImageInfo(image);
                        hasLoggedImageInfo = true;
                    }

                    try {
                        if (frameCount % 10 == 0) {
                            Log.d(TAG, "Converting frame #" + frameCount + " to bitmap");
                        }

                        // Convert to bitmap using enhanced converter
                        Bitmap bitmap = EnhancedImageConverter.convertToBitmap(image);

                        if (bitmap == null) {
                            Log.e(TAG, "Failed to convert frame #" + frameCount + " to bitmap - bitmap is null");
                            processingFrame.set(false);
                            return;
                        }

                        if (frameCount % 10 == 0) {
                            Log.d(TAG, "Successfully converted frame #" + frameCount + " to bitmap: " +
                                    bitmap.getWidth() + "x" + bitmap.getHeight());
                        }

                        // No extra rotation or mirroring here; imageToBitmap already returns rotated

                        if (frameCount % 10 == 0) {
                            Log.d(TAG, "Final bitmap for frame #" + frameCount + ": " +
                                    bitmap.getWidth() + "x" + bitmap.getHeight());
                        }

                        // Return bitmap via callback
                        final Bitmap finalBitmap = bitmap;
                        post(() -> {
                            try {
                                if (frameCount % 10 == 0) {
                                    Log.d(TAG, "Calling frameCallback for frame #" + frameCount);
                                }
                                frameCallback.onFrameAnalyzed(finalBitmap);
                            } catch (Exception e) {
                                Log.e(TAG, "Error in frame callback: " + e.getMessage(), e);
                            } finally {
                                processingFrame.set(false);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing frame #" + frameCount + ": " + e.getMessage(), e);
                        processingFrame.set(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error analyzing frame #" + frameCount + ": " + e.getMessage(), e);
                    processingFrame.set(false);
                } finally {
                    image.close();
                }
            });
        } else {
            Log.d(TAG, "No frame callback provided - ImageAnalysis not set up");
        }

        // Unbind previous use cases
        cameraProvider.unbindAll();

        try {
            // Bind use cases to camera
            if (frameCallback != null && imageAnalysis != null) {
                Log.d(TAG, "Binding camera with Preview, ImageCapture, and ImageAnalysis");
                camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis);
            } else {
                Log.d(TAG, "Binding camera with Preview and ImageCapture only");
                camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture);
            }

            // Connect preview to previewView
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            Log.d(TAG, "Camera binding successful");
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    /**
     * Get optimal resolution based on device model
     * Different devices perform better with different resolutions
     */
    private Size getOptimalResolutionForDevice() {
        String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
        String model = android.os.Build.MODEL.toLowerCase();

        // Default resolution for most devices
        Size defaultResolution = new Size(640, 480);

        // Lower resolution for lower-end devices
        Size lowResolution = new Size(320, 240);

        // Higher resolution for high-end devices
        Size highResolution = new Size(1280, 720);

        Log.d(TAG, "Selecting resolution for " + manufacturer + " " + model);

        // Samsung devices
        if (manufacturer.contains("samsung")) {
            if (model.contains("a10") || model.contains("a20") || model.contains("a30")) {
                Log.d(TAG, "Using low resolution for Samsung A series");
                return lowResolution;
            } else if (model.contains("s10") || model.contains("s20") || model.contains("note")) {
                Log.d(TAG, "Using high resolution for Samsung flagship");
                return highResolution;
            }
        }

        // Xiaomi devices - tend to need lower resolution
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
            Log.d(TAG, "Using low resolution for Xiaomi device");
            return lowResolution;
        }

        // Oppo devices
        if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
            Log.d(TAG, "Using standard resolution for Oppo device");
            return defaultResolution;
        }

        // LG devices
        if (manufacturer.contains("lg")) {
            if (model.contains("g7") || model.contains("thinq")) {
                Log.d(TAG, "Using standard resolution for LG G7 ThinQ");
                return defaultResolution;
            }
        }

        Log.d(TAG, "Using default resolution for unrecognized device");
        return defaultResolution;
    }

    /**
     * Capture a photo
     */
    public void capturePhoto(CaptureCallback callback) {
        if (imageCapture == null) {
            callback.onError("Camera not initialized");
            return;
        }

        // Create image capture listener
        ImageCapture.OnImageCapturedCallback imageCapturedCallback = new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                try {
                    // Convert ImageProxy to Bitmap using enhanced converter
                    Bitmap bitmap = EnhancedImageConverter.convertToBitmap(imageProxy);

                    // No extra rotation or mirroring here; imageToBitmap already returns rotated

                    // Return bitmap
                    final Bitmap finalBitmap = bitmap;
                    imageProxy.close();

                    // Post to main thread
                    post(() -> callback.onCaptured(finalBitmap));
                } catch (Exception e) {
                    Log.e(TAG, "Error processing captured image", e);
                    imageProxy.close();
                    post(() -> callback.onError("Failed to process captured image: " + e.getMessage()));
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed", exception);
                post(() -> callback.onError("Failed to capture image: " + exception.getMessage()));
            }
        };

        // Capture the image
        imageCapture.takePicture(executor, imageCapturedCallback);
    }

    /**
     * Add a new emergency fallback method using Android's ImageReader
     * This can work better on some devices with non-standard camera implementations
     */
    private Bitmap emergencyFallbackConversion(ImageProxy image) {
        try {
            // Log that we're attempting emergency fallback
            Log.d(TAG, "Using emergency fallback conversion for device: " +
                    android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);

            android.media.Image mediaImage = image.getImage();
            if (mediaImage == null) {
                Log.e(TAG, "Emergency fallback: MediaImage is null");
                return null;
            }

            // Try a different YUV conversion approach
            int width = mediaImage.getWidth();
            int height = mediaImage.getHeight();
            int rotation = image.getImageInfo().getRotationDegrees();

            // Get YUV planes
            android.media.Image.Plane[] planes = mediaImage.getPlanes();
            if (planes.length < 3) {
                Log.e(TAG, "Emergency fallback: Not enough planes: " + planes.length);
                return null;
            }

            // More robust YUV to RGB conversion that may work on problematic devices
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            Log.d(TAG, "Emergency fallback: YUV buffer sizes: Y=" + ySize +
                    ", U=" + uSize + ", V=" + vSize);

            // Create byte arrays for each plane
            byte[] yBytes = new byte[ySize];
            byte[] uBytes = new byte[uSize];
            byte[] vBytes = new byte[vSize];

            yBuffer.get(yBytes);
            uBuffer.get(uBytes);
            vBuffer.get(vBytes);

            // Calculate pixel strides and row strides
            int yPixelStride = planes[0].getPixelStride();
            int yRowStride = planes[0].getRowStride();
            int uPixelStride = planes[1].getPixelStride();
            int uRowStride = planes[1].getRowStride();
            int vPixelStride = planes[2].getPixelStride();
            int vRowStride = planes[2].getRowStride();

            Log.d(TAG, "Emergency fallback: Y pixelStride=" + yPixelStride + ", rowStride=" + yRowStride);
            Log.d(TAG, "Emergency fallback: U pixelStride=" + uPixelStride + ", rowStride=" + uRowStride);
            Log.d(TAG, "Emergency fallback: V pixelStride=" + vPixelStride + ", rowStride=" + vRowStride);

            // Create NV21 format byte array (which is compatible with YuvImage)
            byte[] nv21 = new byte[width * height * 3 / 2];

            // Copy Y plane
            int yPos = 0;
            for (int i = 0; i < height; i++) {
                int srcPos = i * yRowStride;
                for (int j = 0; j < width; j++) {
                    if (srcPos + j * yPixelStride < ySize) {
                        nv21[yPos++] = yBytes[srcPos + j * yPixelStride];
                    }
                }
            }

            // Copy UV planes
            int uvPos = width * height;
            for (int i = 0; i < height / 2; i++) {
                int uSrcRow = i * uRowStride;
                int vSrcRow = i * vRowStride;

                for (int j = 0; j < width / 2; j++) {
                    int uSrcPos = uSrcRow + j * uPixelStride;
                    int vSrcPos = vSrcRow + j * vPixelStride;

                    if (vSrcPos < vSize && uSrcPos < uSize) {
                        nv21[uvPos++] = vBytes[vSrcPos];  // V first in NV21
                        nv21[uvPos++] = uBytes[uSrcPos];  // U second in NV21
                    }
                }
            }

            // Convert NV21 to JPEG using YuvImage
            YuvImage yuvImage = new YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, out);

            // Decode JPEG to bitmap
            byte[] jpegData = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);

            if (bitmap == null) {
                Log.e(TAG, "Emergency fallback: Failed to create bitmap from JPEG data");
                return null;
            }

            Log.d(TAG, "Emergency fallback conversion successful: " +
                    bitmap.getWidth() + "x" + bitmap.getHeight());

            // Apply rotation if needed
            return applyRotation(bitmap, rotation);
        } catch (Exception e) {
            Log.e(TAG, "Emergency fallback conversion failed", e);
            return null;
        }
    }

    /**
     * Convert YUV_420_888 to NV21 format for compatibility
     */
    private byte[] yuv420ToNv21(android.media.Image image, int width, int height) {
        try {
            android.media.Image.Plane[] planes = image.getPlanes();
            byte[] nv21 = new byte[width * height * 3 / 2];

            // Copy Y plane
            ByteBuffer yBuffer = planes[0].getBuffer();
            int ySize = yBuffer.remaining();
            yBuffer.get(nv21, 0, Math.min(ySize, width * height));

            // Interleave U and V planes
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();
            int uPixelStride = planes[1].getPixelStride();
            int vPixelStride = planes[2].getPixelStride();
            int uvRowStride = planes[1].getRowStride();

            int uvPos = width * height;

            for (int row = 0; row < height / 2; row++) {
                for (int col = 0; col < width / 2; col++) {
                    int uvIndex = col * uPixelStride + row * uvRowStride;
                    nv21[uvPos++] = vBuffer.get(uvIndex); // V
                    nv21[uvPos++] = uBuffer.get(uvIndex); // U
                }
            }

            return nv21;
        } catch (Exception e) {
            Log.e(TAG, "yuv420ToNv21: Error converting YUV to NV21", e);
            return null;
        }
    }

    private Bitmap applyRotation(Bitmap src, int rotationDegrees) {
        if (rotationDegrees == 0) return src;
        try {
            Matrix m = new Matrix();
            m.postRotate(rotationDegrees);
            Bitmap out = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
            if (out != src) src.recycle();
            return out;
        } catch (Exception ignored) {
            return src;
        }
    }

    /**
     * Log thông tin về định dạng ảnh từ camera
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void logImageInfo(ImageProxy image) {
        try {
            String deviceInfo = "Device: " + android.os.Build.MANUFACTURER + " " +
                    android.os.Build.MODEL + ", Android " + android.os.Build.VERSION.RELEASE;

            Log.d(TAG, "Device info: " + deviceInfo);
            Log.d(TAG, "Image info: " + image.getWidth() + "x" + image.getHeight() +
                    ", format=" + image.getFormat() +
                    ", rotation=" + image.getImageInfo().getRotationDegrees() +
                    ", cropRect=" + image.getCropRect());

            // Get format name for better diagnostics
            String formatName = "Unknown";
            if (image.getFormat() == android.graphics.ImageFormat.YUV_420_888) {
                formatName = "YUV_420_888";
            } else if (image.getFormat() == android.graphics.ImageFormat.NV21) {
                formatName = "NV21";
            } else if (image.getFormat() == android.graphics.ImageFormat.JPEG) {
                formatName = "JPEG";
            } else if (image.getFormat() == ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) {
                formatName = "RGBA_8888";
            }
            Log.d(TAG, "Image format name: " + formatName);

            if (image.getPlanes() != null && image.getPlanes().length > 0) {
                Log.d(TAG, "Number of planes: " + image.getPlanes().length);

                for (int i = 0; i < image.getPlanes().length; i++) {
                    ImageProxy.PlaneProxy plane = image.getPlanes()[i];
                    Log.d(TAG, "Plane " + i + ": pixelStride=" + plane.getPixelStride() +
                            ", rowStride=" + plane.getRowStride() +
                            ", bufferSize=" + plane.getBuffer().remaining());
                }
            }

            android.media.Image mediaImage = image.getImage();
            if (mediaImage != null) {
                Log.d(TAG, "MediaImage format: " + mediaImage.getFormat() +
                        ", timestamp=" + mediaImage.getTimestamp());

                android.media.Image.Plane[] planes = mediaImage.getPlanes();
                if (planes != null) {
                    Log.d(TAG, "MediaImage has " + planes.length + " planes");

                    for (int i = 0; i < planes.length; i++) {
                        android.media.Image.Plane plane = planes[i];
                        Log.d(TAG, "MediaImage Plane " + i + ": pixelStride=" + plane.getPixelStride() +
                                ", rowStride=" + plane.getRowStride() +
                                ", bufferSize=" + plane.getBuffer().remaining());
                    }
                }
            }

            // Log camera state and available formats
            try {
                String cameraInfo = "Camera rotation: " + previewView.getDisplay().getRotation();
                Log.d(TAG, cameraInfo);

                Log.d(TAG, "CameraView current state summary: " +
                        "imageAnalysis=" + (imageAnalysis != null) +
                        ", imageCapture=" + (imageCapture != null) +
                        ", camera=" + (camera != null) +
                        ", cameraProvider=" + (cameraProvider != null));
            } catch (Exception e) {
                Log.w(TAG, "Could not log camera state", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging image info", e);
        }
    }

    /**
     * Convert RGBA_8888 format ImageProxy to Bitmap
     */
    private Bitmap rgbaImageProxyToBitmap(ImageProxy image) {
        try {
            Log.d(TAG, "rgbaImageProxyToBitmap: Starting RGBA conversion");

            if (image.getPlanes().length == 0) {
                Log.e(TAG, "rgbaImageProxyToBitmap: No planes available");
                return null;
            }

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            int pixelStride = image.getPlanes()[0].getPixelStride();
            int rowStride = image.getPlanes()[0].getRowStride();
            int width = image.getWidth();
            int height = image.getHeight();

            Log.d(TAG, "rgbaImageProxyToBitmap: " + width + "x" + height +
                    ", pixelStride=" + pixelStride + ", rowStride=" + rowStride +
                    ", bufferSize=" + buffer.remaining());

            // Nếu pixelStride = 4 (RGBA_8888) và không có padding (rowStride == width*4), có thể tạo bitmap trực tiếp
            if (pixelStride == 4 && rowStride == width * 4) {
                Log.d(TAG, "rgbaImageProxyToBitmap: Using direct copy method (pixelStride=4)");
                try {
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    buffer.rewind();
                    bitmap.copyPixelsFromBuffer(buffer);
                    // Apply rotation if needed
                    int rotationDegrees = 0;
                    try { rotationDegrees = image.getImageInfo().getRotationDegrees(); } catch (Exception ignored) {}
                    if (rotationDegrees != 0) {
                        Matrix m = new Matrix();
                        m.postRotate(rotationDegrees);
                        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, true);
                        if (rotated != bitmap) bitmap.recycle();
                        return rotated;
                    }
                    Log.d(TAG, "rgbaImageProxyToBitmap: Direct copy successful");
                    return bitmap;
                } catch (Exception e) {
                    Log.e(TAG, "rgbaImageProxyToBitmap: Direct copy failed", e);
                    // Fall through to manual method
                }
            }

            // Nếu không, chúng ta cần sao chép dữ liệu theo cách thủ công
            Log.d(TAG, "rgbaImageProxyToBitmap: Using manual copy method (pixelStride=" + pixelStride + ")");
            int bufferSize = buffer.remaining();
            byte[] data = new byte[bufferSize];
            buffer.get(data);

            Log.d(TAG, "rgbaImageProxyToBitmap: Read " + data.length + " bytes from buffer");

            // Tạo bitmap trống
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // Sao chép từng pixel
            int[] pixels = new int[width * height];
            int offset = 0;
            int successfulPixels = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = 0;
                    int bufferIndex = y * rowStride + x * pixelStride;

                    if (bufferIndex + 3 < data.length) {
                        // RGBA -> ARGB
                        int r = data[bufferIndex] & 0xff;
                        int g = data[bufferIndex + 1] & 0xff;
                        int b = data[bufferIndex + 2] & 0xff;
                        int a = data[bufferIndex + 3] & 0xff;

                        pixel = (a << 24) | (r << 16) | (g << 8) | b;
                        successfulPixels++;
                    }

                    pixels[offset++] = pixel;
                }
            }

            Log.d(TAG, "rgbaImageProxyToBitmap: Processed " + successfulPixels + "/" + (width * height) + " pixels");

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            // Apply rotation if needed
            int rotationDegrees = 0;
            try { rotationDegrees = image.getImageInfo().getRotationDegrees(); } catch (Exception ignored) {}
            if (rotationDegrees != 0) {
                Matrix m = new Matrix();
                m.postRotate(rotationDegrees);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, true);
                if (rotated != bitmap) bitmap.recycle();
                Log.d(TAG, "rgbaImageProxyToBitmap: Manual copy successful (rotated)");
                return rotated;
            }
            Log.d(TAG, "rgbaImageProxyToBitmap: Manual copy successful");
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "rgbaImageProxyToBitmap: Exception occurred", e);
            return null;
        }
    }

    /**
     * Convert YUV_420_888 Image to Bitmap
     */
    private Bitmap imageToBitmapUsingYUV(android.media.Image image) {
        try {
            Log.d(TAG, "imageToBitmapUsingYUV: Format = " + image.getFormat());

            int width = image.getWidth();
            int height = image.getHeight();

            // Lấy các planes
            android.media.Image.Plane[] planes = image.getPlanes();
            if (planes.length < 3) {
                Log.e(TAG, "imageToBitmapUsingYUV: Not enough planes: " + planes.length);
                return null;
            }

            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            // Log thông tin về planes
            Log.d(TAG, "imageToBitmapUsingYUV: Y plane - stride=" + planes[0].getRowStride() + ", pixelStride=" + planes[0].getPixelStride());
            Log.d(TAG, "imageToBitmapUsingYUV: U plane - stride=" + planes[1].getRowStride() + ", pixelStride=" + planes[1].getPixelStride());
            Log.d(TAG, "imageToBitmapUsingYUV: V plane - stride=" + planes[2].getRowStride() + ", pixelStride=" + planes[2].getPixelStride());

            // Tính toán kích thước dữ liệu
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            Log.d(TAG, "imageToBitmapUsingYUV: Buffer sizes - Y=" + ySize + ", U=" + uSize + ", V=" + vSize);

            // Tạo mảng NV21 (YUV420SP)
            byte[] nv21 = new byte[width * height * 3 / 2];

            // Sao chép Y plane
            int yActualSize = Math.min(ySize, width * height);
            yBuffer.get(nv21, 0, yActualSize);
            Log.d(TAG, "imageToBitmapUsingYUV: Copied Y data: " + yActualSize + " bytes");

            // Sao chép U và V xen kẽ để tạo UV plane
            int uvPos = width * height;
            int uPixelStride = planes[1].getPixelStride();
            int vPixelStride = planes[2].getPixelStride();

            // Xử lý theo pixel stride
            if (uPixelStride == 1 && vPixelStride == 1) {
                // Planar format
                Log.d(TAG, "imageToBitmapUsingYUV: Processing planar format");
                for (int i = 0; i < Math.min(uSize, vSize) && uvPos < nv21.length - 1; i++) {
                    nv21[uvPos++] = vBuffer.get(i);
                    nv21[uvPos++] = uBuffer.get(i);
                }
            } else {
                // Semi-planar hoặc interleaved format
                Log.d(TAG, "imageToBitmapUsingYUV: Processing semi-planar format (uPixelStride=" + uPixelStride + ", vPixelStride=" + vPixelStride + ")");
                int uvSamples = Math.min(uSize / uPixelStride, vSize / vPixelStride);
                for (int i = 0; i < uvSamples && uvPos < nv21.length - 1; i++) {
                    nv21[uvPos++] = vBuffer.get(i * vPixelStride);
                    nv21[uvPos++] = uBuffer.get(i * uPixelStride);
                }
            }

            Log.d(TAG, "imageToBitmapUsingYUV: UV data copied, total NV21 size: " + nv21.length);

            // Chuyển đổi YUV sang RGB
            YuvImage yuvImage = new YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, out);
            byte[] imageBytes = out.toByteArray();

            Log.d(TAG, "imageToBitmapUsingYUV: JPEG size: " + imageBytes.length);

            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap != null) {
                Log.d(TAG, "imageToBitmapUsingYUV: Successfully created bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            } else {
                Log.e(TAG, "imageToBitmapUsingYUV: Failed to decode JPEG");
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "imageToBitmapUsingYUV: Exception occurred", e);
            return null;
        }
    }

    /**
     * Convert generic YUV format Image to Bitmap
     */
    private Bitmap imageToBitmapUsingGenericYUV(android.media.Image image) {
        try {
            Log.d(TAG, "imageToBitmapUsingGenericYUV: Format = " + image.getFormat());

            int width = image.getWidth();
            int height = image.getHeight();

            // Lấy các planes
            android.media.Image.Plane[] planes = image.getPlanes();
            if (planes.length == 0) {
                Log.e(TAG, "imageToBitmapUsingGenericYUV: No planes available");
                return null;
            }

            Log.d(TAG, "imageToBitmapUsingGenericYUV: " + width + "x" + height + ", planes=" + planes.length);

            // Nếu chỉ có 1 plane, có thể là grayscale hoặc packed format
            if (planes.length == 1) {
                Log.d(TAG, "imageToBitmapUsingGenericYUV: Single plane format");
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();

                Log.d(TAG, "imageToBitmapUsingGenericYUV: pixelStride=" + pixelStride + ", rowStride=" + rowStride);

                // Nếu là packed format (NV21/NV16), thử xử lý
                if (pixelStride == 1) {
                    // Đây có thể là NV21 hoặc format tương tự
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);

                    Log.d(TAG, "imageToBitmapUsingGenericYUV: Data size=" + data.length + ", expected=" + (width * height * 3 / 2));

                    // Thử tạo YuvImage với NV21
                    try {
                        YuvImage yuvImage = new YuvImage(data, android.graphics.ImageFormat.NV21, width, height, null);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
                        byte[] imageBytes = out.toByteArray();

                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        if (bitmap != null) {
                            Log.d(TAG, "imageToBitmapUsingGenericYUV: NV21 conversion successful");
                            return bitmap;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "imageToBitmapUsingGenericYUV: NV21 conversion failed", e);
                    }
                }
            }

            // Nếu có 3 planes, thử xử lý như YUV420
            if (planes.length >= 3) {
                Log.d(TAG, "imageToBitmapUsingGenericYUV: Multi-plane format, trying YUV420 conversion");

                ByteBuffer yBuffer = planes[0].getBuffer();
                ByteBuffer uBuffer = planes[1].getBuffer();
                ByteBuffer vBuffer = planes[2].getBuffer();

                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();

                Log.d(TAG, "imageToBitmapUsingGenericYUV: Y=" + ySize + ", U=" + uSize + ", V=" + vSize);

                // Tạo mảng NV21
                byte[] nv21 = new byte[width * height * 3 / 2];

                // Sao chép Y
                yBuffer.get(nv21, 0, Math.min(ySize, width * height));

                // Sao chép U và V xen kẽ
                int uvPos = width * height;
                int uStep = Math.max(1, planes[1].getPixelStride());
                int vStep = Math.max(1, planes[2].getPixelStride());

                for (int i = 0; i < Math.min(uSize / uStep, vSize / vStep) && uvPos < nv21.length - 1; i++) {
                    if (i * vStep < vSize) {
                        nv21[uvPos++] = vBuffer.get(i * vStep);
                    }
                    if (i * uStep < uSize && uvPos < nv21.length) {
                        nv21[uvPos++] = uBuffer.get(i * uStep);
                    }
                }

                // Chuyển đổi sang RGB
                try {
                    YuvImage yuvImage = new YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
                    byte[] imageBytes = out.toByteArray();

                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    if (bitmap != null) {
                        Log.d(TAG, "imageToBitmapUsingGenericYUV: Multi-plane conversion successful");
                        return bitmap;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "imageToBitmapUsingGenericYUV: Multi-plane conversion failed", e);
                }
            }

            Log.w(TAG, "imageToBitmapUsingGenericYUV: All methods failed");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "imageToBitmapUsingGenericYUV: Exception occurred", e);
            return null;
        }
    }

    /**
     * Stop camera and release resources
     */
    public void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}