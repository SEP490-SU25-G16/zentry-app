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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    private final ExecutorService mlWorker = Executors.newSingleThreadExecutor();
    private final AtomicReference<Bitmap> latestFrame = new AtomicReference<>(null);
    private final AtomicBoolean workerActive = new AtomicBoolean(false);
    private volatile int analysisStride = 1; // soft FPS cap: analyze every N frames
    
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
            
            // Cấu hình phân tích hình ảnh với độ phân giải phù hợp
            // Không set OUTPUT_IMAGE_FORMAT để sử dụng format mặc định (YUV_420_888)
            imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(new Size(640, 480)) // Độ phân giải phù hợp cho face detection
                    .build();
            
            // Store callback for worker delivery
            final FrameAnalysisCallback analysisCallback = frameCallback;
            imageAnalysis.setAnalyzer(executor, image -> {
                frameCount++;
                
                // Log mỗi 30 frames (khoảng 1 giây)
                if (frameCount % 30 == 0) {
                    Log.d(TAG, "Processing frame #" + frameCount + ", format: " + image.getFormat() + 
                            ", size: " + image.getWidth() + "x" + image.getHeight());
                }
                
                // Soft FPS cap: process only every Nth frame
                if (analysisStride > 1 && (frameCount % analysisStride) != 0) {
                    image.close();
                    return;
                }
                
                try {
                    
                    // Log thông tin về định dạng ảnh (chỉ log lần đầu tiên)
                    if (!hasLoggedImageInfo) {
                        logImageInfo(image);
                        hasLoggedImageInfo = true;
                    }
                    
                    try {
                        Log.d(TAG, "Converting frame #" + frameCount + " to bitmap");
                        
                        // Avoid JPEG path; if the downstream needs Bitmap, consider faster YUV->RGB. Here pass through MPImage via callback if supported.
                        Bitmap bitmap = imageToBitmap(image);
                        
                        if (bitmap == null) {
                            Log.e(TAG, "Failed to convert frame #" + frameCount + " to bitmap - bitmap is null");

                            return;
                        }
                        
                        Log.d(TAG, "Successfully converted frame #" + frameCount + " to bitmap: " + 
                                bitmap.getWidth() + "x" + bitmap.getHeight());
                        
                        // Apply rotation if needed
                        int rotationDegrees = image.getImageInfo().getRotationDegrees();
                        if (rotationDegrees != 0) {
                            Log.d(TAG, "Applying rotation: " + rotationDegrees + " degrees");
                            Matrix matrix = new Matrix();
                            matrix.postRotate(rotationDegrees);
                            bitmap = Bitmap.createBitmap(
                                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        }
                        
                        // Mirror image for front camera
                        Matrix matrix = new Matrix();
                        matrix.preScale(-1.0f, 1.0f);
                        bitmap = Bitmap.createBitmap(
                                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        
                        Log.d(TAG, "Final bitmap for frame #" + frameCount + ": " + 
                                bitmap.getWidth() + "x" + bitmap.getHeight());
                        
                        // Coalesce frames for worker to keep freshest
                        Bitmap previous = latestFrame.getAndSet(bitmap);
                        if (previous != null && previous != bitmap && !previous.isRecycled()) {
                            try { previous.recycle(); } catch (Exception ignore) {}
                        }
                        if (workerActive.compareAndSet(false, true)) {
                            mlWorker.execute(() -> drainWorkerQueue(analysisCallback));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing frame #" + frameCount, e);

                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error analyzing frame #" + frameCount, e);

                } finally {
                    image.close();
                }
            });

            // Warm default stride based on device load could be updated elsewhere
            setAnalysisStride(1);
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
    
    private void drainWorkerQueue(@Nullable FrameAnalysisCallback analysisCallback) {
        try {
            while (true) {
                Bitmap toProcess = latestFrame.getAndSet(null);
                if (toProcess == null) break;
                final Bitmap deliver = toProcess;
                post(() -> {
                    try {
                        // Deliver on UI thread; heavy ML should be in worker callbacks downstream
                        if (analysisCallback != null) analysisCallback.onFrameAnalyzed(deliver);
                    } catch (Exception e) {
                        Log.e(TAG, "Error delivering frame to callback", e);
                    }
                });
            }
        } finally {
            workerActive.set(false);
            if (latestFrame.get() != null && workerActive.compareAndSet(false, true)) {
                mlWorker.execute(() -> drainWorkerQueue(null));
            }
        }
    }

    /**
     * Adjust analysis stride (process every Nth frame). Use >1 to reduce load.
     */
    public void setAnalysisStride(int stride) {
        if (stride < 1) stride = 1;
        this.analysisStride = stride;
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
                    // Convert ImageProxy to Bitmap
                    Bitmap bitmap = imageToBitmap(imageProxy);
                    
                    // Apply rotation if needed
                    int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                    if (rotationDegrees != 0) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotationDegrees);
                        bitmap = Bitmap.createBitmap(
                                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }
                    
                    // Mirror image for front camera
                    Matrix matrix = new Matrix();
                    matrix.preScale(-1.0f, 1.0f);
                    bitmap = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    
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
     * Convert ImageProxy to Bitmap
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private Bitmap imageToBitmap(ImageProxy image) {
        try {
            // Lấy định dạng ảnh
            int format = image.getFormat();
            Log.d(TAG, "imageToBitmap: Image format = " + format + " (RGBA_8888=" + ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888 + ")");
            
            // Thử chuyển đổi từ Image đối tượng TRƯỚC (ưu tiên cao nhất)
            android.media.Image mediaImage = image.getImage();
            if (mediaImage != null) {
                int mediaFormat = mediaImage.getFormat();
                Log.d(TAG, "imageToBitmap: MediaImage format = " + mediaFormat + 
                        " (YUV_420_888=" + android.graphics.ImageFormat.YUV_420_888 + 
                        ", NV16=" + android.graphics.ImageFormat.NV16 + 
                        ", NV21=" + android.graphics.ImageFormat.NV21 + ")");
                
                // Thử các định dạng YUV
                if (mediaFormat == android.graphics.ImageFormat.YUV_420_888 || 
                    mediaFormat == android.graphics.ImageFormat.NV21 ||
                    mediaFormat == android.graphics.ImageFormat.NV16) {
                    Log.d(TAG, "imageToBitmap: Trying YUV conversion for format " + mediaFormat);
                    Bitmap bitmap = imageToBitmapUsingYUV(mediaImage);
                    if (bitmap != null) {
                        Log.d(TAG, "imageToBitmap: YUV conversion successful");
                        return bitmap;
                    } else {
                        Log.w(TAG, "imageToBitmap: YUV conversion failed");
                    }
                }
                
                // Nếu không phải YUV, thử chuyển đổi raw data
                Log.d(TAG, "imageToBitmap: Trying generic YUV conversion for unknown format " + mediaFormat);
                Bitmap bitmap = imageToBitmapUsingGenericYUV(mediaImage);
                if (bitmap != null) {
                    Log.d(TAG, "imageToBitmap: Generic YUV conversion successful");
                    return bitmap;
                } else {
                    Log.w(TAG, "imageToBitmap: Generic YUV conversion failed");
                }
            } else {
                Log.w(TAG, "imageToBitmap: MediaImage is null");
            }
            
            // Thử phương pháp RGBA
            if (format == ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) {
                Log.d(TAG, "imageToBitmap: Trying RGBA conversion");
                Bitmap bitmap = rgbaImageProxyToBitmap(image);
                if (bitmap != null) {
                    Log.d(TAG, "imageToBitmap: RGBA conversion successful");
                    return bitmap;
                } else {
                    Log.w(TAG, "imageToBitmap: RGBA conversion failed");
                }
            }
            
            // Thử phương pháp JPEG
            if (image.getPlanes().length > 0) {
                Log.d(TAG, "imageToBitmap: Trying JPEG/byte array conversion");
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                
                Log.d(TAG, "imageToBitmap: Byte array size = " + bytes.length);
                
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    Log.d(TAG, "imageToBitmap: JPEG conversion successful");
                    return bitmap;
                } else {
                    Log.w(TAG, "imageToBitmap: JPEG conversion failed");
                }
            } else {
                Log.w(TAG, "imageToBitmap: No planes available");
            }
            
            // Nếu tất cả các phương pháp đều thất bại, tạo bitmap trống
            Log.w(TAG, "imageToBitmap: All conversion methods failed, creating empty bitmap");
            int width = image.getWidth() > 0 ? image.getWidth() : 640;
            int height = image.getHeight() > 0 ? image.getHeight() : 480;
            Bitmap fallbackBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Log.d(TAG, "imageToBitmap: Created fallback bitmap: " + width + "x" + height);
            return fallbackBitmap;
        } catch (Exception e) {
            Log.e(TAG, "imageToBitmap: Exception occurred", e);
            Bitmap errorBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
            Log.d(TAG, "imageToBitmap: Created error bitmap: 640x480");
            return errorBitmap;
        }
    }
    
    /**
     * Log thông tin về định dạng ảnh từ camera
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void logImageInfo(ImageProxy image) {
        try {
            Log.d(TAG, "Image info: " + image.getWidth() + "x" + image.getHeight() + 
                    ", format=" + image.getFormat() + 
                    ", cropRect=" + image.getCropRect());
            
            if (image.getPlanes() != null && image.getPlanes().length > 0) {
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
            
            // Nếu pixelStride = 4 (RGBA_8888), chúng ta có thể tạo bitmap trực tiếp
            if (pixelStride == 4) {
                Log.d(TAG, "rgbaImageProxyToBitmap: Using direct copy method (pixelStride=4)");
                try {
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    buffer.rewind();
                    bitmap.copyPixelsFromBuffer(buffer);
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