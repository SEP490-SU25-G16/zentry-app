package vn.edu.fpt.zentryapp.faceid.ui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
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
import androidx.lifecycle.LifecycleOwner;

import vn.edu.fpt.zentryapp.BuildConfig;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom camera view for face capture with continuous frame analysis
 */
public class CameraView extends FrameLayout {
    private static final String TAG = "CameraView";
    
    // Switched to natario CameraView as the rendering & capture engine
    private com.otaliastudios.cameraview.CameraView natarioView;
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
        // Create and add natario CameraView
        natarioView = new com.otaliastudios.cameraview.CameraView(context);
        natarioView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        // Configure defaults
        natarioView.setFacing(Facing.FRONT);
        natarioView.setMode(Mode.PICTURE);
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

                            // Mirror horizontally for front camera to keep consistency with previous pipeline
                            Matrix mirrorMatrix = new Matrix();
                            mirrorMatrix.preScale(-1.0f, 1.0f);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mirrorMatrix, true);

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
    
    // (Removed legacy CameraX binding completely)
    
    /**
     * Capture a photo
     */
    public void capturePhoto(CaptureCallback callback) {
        try {
            natarioView.addCameraListener(new CameraListener() {
                @Override
                public void onPictureTaken(@NonNull PictureResult result) {
                    try {
                        result.toBitmap(bitmap -> {
                            try {
                                if (bitmap == null) {
                                    post(() -> callback.onError("Failed to convert picture to bitmap"));
                                    return;
                                }
                                Matrix mirrorMatrix = new Matrix();
                                mirrorMatrix.preScale(-1.0f, 1.0f);
                                Bitmap mirrored = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mirrorMatrix, true);
                                post(() -> callback.onCaptured(mirrored));
                            } catch (Exception e) {
                                Log.e(TAG, "Error handling picture bitmap", e);
                                post(() -> callback.onError("Error handling picture: " + e.getMessage()));
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "toBitmap failed", e);
                        post(() -> callback.onError("Failed to process captured image: " + e.getMessage()));
                    }
                }
            });
            natarioView.takePictureSnapshot();
        } catch (Exception e) {
            Log.e(TAG, "Photo capture failed", e);
            post(() -> callback.onError("Failed to capture image: " + e.getMessage()));
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
        try {
            if (natarioView != null) {
                natarioView.close();
            }
        } catch (Exception ignored) {}
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