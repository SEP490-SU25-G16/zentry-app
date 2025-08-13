package vn.edu.fpt.zentryapp.faceid.ui.components;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Enhanced image converter that uses device-specific optimizations
 * to convert camera frames to bitmaps with better cross-device compatibility.
 */
public class EnhancedImageConverter {
    private static final String TAG = "EnhancedImageConverter";
    
    /**
     * Convert an ImageProxy to Bitmap with device-specific optimizations
     * @param image ImageProxy from camera
     * @return Bitmap or null if conversion fails
     */
    public static Bitmap convertToBitmap(ImageProxy image) {
        try {
            // Get image information
            int width = image.getWidth();
            int height = image.getHeight();
            int format = image.getFormat();
            int rotation = image.getImageInfo().getRotationDegrees();
            
            Log.d(TAG, "Converting image " + width + "x" + height + 
                  ", format=" + format + ", rotation=" + rotation);
            
            // Check if we should use emergency fallback first based on device
            boolean useEmergencyFallbackFirst = DeviceSpecificOptimizer.shouldUseEmergencyFallbackFirst();
            
            // Try emergency fallback first for known problematic devices
            if (useEmergencyFallbackFirst) {
                Bitmap emergencyResult = emergencyFallbackConversion(image);
                if (emergencyResult != null) {
                    Log.d(TAG, "Successfully converted using emergency fallback for " + 
                              android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
                    return emergencyResult;
                }
                // If emergency fallback fails, continue with standard methods
                Log.d(TAG, "Emergency fallback failed, trying standard methods");
            }
            
            // Canonical path: try to use YuvToRgbConverter utility (expects ImageProxy + rotation)
            try {
                Bitmap bmp = vn.edu.fpt.zentryapp.faceid.util.YuvToRgbConverter.convert(image, rotation);
                if (bmp != null) {
                    Log.d(TAG, "Successfully converted using YuvToRgbConverter (ImageProxy)");
                    return bmp;
                }
            } catch (Exception e) {
                Log.w(TAG, "YuvToRgbConverter (ImageProxy) failed: " + e.getMessage());
                // Continue to fallback methods
            }

            // Canonical path 2: try MediaPipe's YuvToRgbConverter if available
            try {
                Bitmap bmp = vn.edu.fpt.zentryapp.faceid.util.YuvToRgbConverter.convert(image, rotation);
                if (bmp != null) {
                    Log.d(TAG, "Successfully converted using MediaPipe YuvToRgbConverter");
                    return bmp;
                }
            } catch (Exception e) {
                Log.w(TAG, "MediaPipe YuvToRgbConverter failed: " + e.getMessage());
                // Continue to fallback methods
            }

            // Fallback 1: RGBA fast path (if truly RGBA)
            if (format == ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) {
                try {
                    Bitmap bmp = rgbaImageProxyToBitmap(image);
                    if (bmp != null) {
                        Log.d(TAG, "Successfully converted using RGBA fast path");
                        return applyRotation(bmp, rotation);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "RGBA fast path failed: " + e.getMessage());
                    // Continue to next fallback
                }
            }

            // Fallback 2: Manual YUV handling with ByteBuffer
            android.media.Image mediaImage = image.getImage();
            if (mediaImage != null && mediaImage.getPlanes() != null && mediaImage.getPlanes().length >= 3) {
                try {
                    // Try YUV to NV21 conversion
                    byte[] nv21 = yuv420ToNv21(mediaImage, width, height);
                    if (nv21 != null) {
                        android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                                nv21, android.graphics.ImageFormat.NV21, width, height, null);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, width, height), 95, out);
                        byte[] jpegData = out.toByteArray();
                        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
                        
                        if (bitmap != null) {
                            Log.d(TAG, "Successfully converted using manual YUV to NV21 conversion");
                            return applyRotation(bitmap, rotation);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Manual YUV conversion failed: " + e.getMessage());
                    // Continue to next fallback
                }
            }

            // Last resort fallback: JPEG decode from first plane
            try {
                if (image.getPlanes().length > 0) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp != null) {
                        Log.d(TAG, "Successfully converted using JPEG fallback");
                        return applyRotation(bmp, rotation);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "JPEG fallback failed: " + e.getMessage());
                // Continue to next fallback
            }
            
            // If we haven't used emergency fallback yet, try it now as a last resort
            if (!useEmergencyFallbackFirst) {
                Bitmap emergencyResult = emergencyFallbackConversion(image);
                if (emergencyResult != null) {
                    Log.d(TAG, "Successfully converted using emergency fallback as last resort");
                    return emergencyResult;
                }
            }
            
            // All conversions failed - log and return null or placeholder
            Log.w(TAG, "All conversion methods failed, returning null");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred during image conversion", e);
            return null;
        }
    }
    
    /**
     * Emergency fallback conversion using a more robust approach
     */
    private static Bitmap emergencyFallbackConversion(ImageProxy image) {
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
            android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                    nv21, android.graphics.ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, width, height), 90, out);
            
            // Decode JPEG to bitmap
            byte[] jpegData = out.toByteArray();
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
            
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
     * Apply rotation to a bitmap
     */
    private static Bitmap applyRotation(Bitmap bitmap, int rotation) {
        if (rotation == 0) {
            return bitmap;
        }
        
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, 
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }
    
    /**
     * Convert RGBA ImageProxy to Bitmap directly
     */
    private static Bitmap rgbaImageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        int pixelStride = image.getPlanes()[0].getPixelStride();
        int rowStride = image.getPlanes()[0].getRowStride();
        int width = image.getWidth();
        int height = image.getHeight();
        
        if (pixelStride == 4 && rowStride == width * 4) {
            // Fast path if the buffer is contiguous
            int[] pixels = new int[width * height];
            buffer.asIntBuffer().get(pixels);
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } else {
            // Slower path for non-contiguous buffers
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            
            int[] pixels = new int[width * height];
            int offset = 0;
            for (int y = 0; y < height; y++) {
                int rowOffset = y * rowStride;
                for (int x = 0; x < width; x++) {
                    int pixelOffset = rowOffset + x * pixelStride;
                    
                    // RGBA to ARGB conversion
                    int r = data[pixelOffset] & 0xFF;
                    int g = data[pixelOffset + 1] & 0xFF;
                    int b = data[pixelOffset + 2] & 0xFF;
                    int a = data[pixelOffset + 3] & 0xFF;
                    
                    pixels[offset++] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        }
    }
    
    /**
     * Convert YUV_420_888 to NV21 format
     */
    private static byte[] yuv420ToNv21(android.media.Image image, int width, int height) {
        android.media.Image.Plane[] planes = image.getPlanes();
        byte[] yPlane = new byte[planes[0].getBuffer().remaining()];
        byte[] uPlane = new byte[planes[1].getBuffer().remaining()];
        byte[] vPlane = new byte[planes[2].getBuffer().remaining()];
        
        planes[0].getBuffer().get(yPlane);
        planes[1].getBuffer().get(uPlane);
        planes[2].getBuffer().get(vPlane);
        
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();
        
        int[] strides = new int[] { yRowStride, uvRowStride, uvRowStride };
        int[] pixelStrides = new int[] { 1, uvPixelStride, uvPixelStride };
        
        // NV21 format: YYYYVUVUVU...
        byte[] nv21 = new byte[width * height * 3 / 2];
        int ySize = width * height;
        
        // Y Plane
        int k = 0;
        for (int i = 0; i < height; i++) {
            int offset = i * yRowStride;
            for (int j = 0; j < width; j++) {
                if (offset + j < yPlane.length) {
                    nv21[k++] = yPlane[offset + j];
                }
            }
        }
        
        // UV Planes
        for (int i = 0; i < height / 2; i++) {
            int vOffset = i * strides[2];
            int uOffset = i * strides[1];
            
            for (int j = 0; j < width / 2; j++) {
                int vIndex = vOffset + j * pixelStrides[2];
                int uIndex = uOffset + j * pixelStrides[1];
                
                if (vIndex < vPlane.length && uIndex < uPlane.length) {
                    nv21[ySize + k++] = vPlane[vIndex]; // V first in NV21
                    nv21[ySize + k++] = uPlane[uIndex]; // U second in NV21
                }
            }
        }
        
        return nv21;
    }
}
