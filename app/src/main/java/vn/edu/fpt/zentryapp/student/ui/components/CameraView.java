package vn.edu.fpt.zentryapp.student.ui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
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

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Custom camera view for face capture
 */
public class CameraView extends FrameLayout {
    private static final String TAG = "CameraView";
    
    private PreviewView previewView;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private final Executor executor = Executors.newSingleThreadExecutor();
    
    public interface CaptureCallback {
        void onCaptured(Bitmap bitmap);
        void onError(String message);
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
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(getContext());
        
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(lifecycleOwner);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }
    
    /**
     * Bind camera use cases
     */
    private void bindCameraUseCases(LifecycleOwner lifecycleOwner) {
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
        
        // Unbind previous use cases
        cameraProvider.unbindAll();
        
        try {
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture);
            
            // Connect preview to previewView
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
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
    private Bitmap imageToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
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