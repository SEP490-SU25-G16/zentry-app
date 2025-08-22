//package vn.edu.fpt.zentryapp.faceid.util;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Matrix;
//import android.graphics.Rect;
//import android.graphics.YuvImage;
//import android.util.Log;
//
//import androidx.annotation.Nullable;
//
//
//import com.chaos.view.BuildConfig;
//
//import java.io.ByteArrayOutputStream;
//import java.nio.ByteBuffer;
//
///**
// * Canonical, device-safe converter from YUV_420_888 to RGB Bitmap.
// * Handles interleaved UV with pixelStride==2 and applies rotation if provided.
// */
//public final class YuvToRgbConverter {
//
//	private YuvToRgbConverter() {}
//
//	@Nullable
//	public static Bitmap convert(ImageProxy proxy, int rotationDegrees) {
//		// �� DEBUG FLAG 1: Kiểm tra input ImageProxy
//
//			Log.d("DEBUG_BITMAP", "Input ImageProxy - Format: " + proxy.getFormat() +
//					", Size: " + proxy.getWidth() + "x" + proxy.getHeight() +
//					", Rotation: " + rotationDegrees);
//
//		android.media.Image image = proxy.getImage();
//		if (image == null || image.getFormat() != android.graphics.ImageFormat.YUV_420_888) {
//			// 🔧 DEBUG FLAG 2: Log lỗi format
//
//				Log.e("DEBUG_BITMAP", "Invalid image format or null image");
//
//			return null;
//		}
//
//		int width = image.getWidth();
//		int height = image.getHeight();
//		android.media.Image.Plane[] planes = image.getPlanes();
//		ByteBuffer yBuffer = planes[0].getBuffer();
//		ByteBuffer uBuffer = planes[1].getBuffer();
//		ByteBuffer vBuffer = planes[2].getBuffer();
//
//		byte[] nv21 = new byte[width * height * 3 / 2];
//		int ySize = Math.min(yBuffer.remaining(), width * height);
//		yBuffer.get(nv21, 0, ySize);
//
//		int uvPos = width * height;
//		int uPixelStride = planes[1].getPixelStride();
//		int vPixelStride = planes[2].getPixelStride();
//		int uSize = uBuffer.remaining();
//		int vSize = vBuffer.remaining();
//
//		if (uPixelStride == 1 && vPixelStride == 1) {
//			for (int i = 0; i < Math.min(uSize, vSize) && uvPos < nv21.length - 1; i++) {
//				nv21[uvPos++] = vBuffer.get(i);
//				nv21[uvPos++] = uBuffer.get(i);
//			}
//		} else {
//			int uvSamples = Math.min(uSize / Math.max(1, uPixelStride), vSize / Math.max(1, vPixelStride));
//			for (int i = 0; i < uvSamples && uvPos < nv21.length - 1; i++) {
//				nv21[uvPos++] = vBuffer.get(i * vPixelStride);
//				nv21[uvPos++] = uBuffer.get(i * uPixelStride);
//			}
//		}
//
//		YuvImage yuv = new YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null);
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		yuv.compressToJpeg(new Rect(0, 0, width, height), 90, out);
//		byte[] imageBytes = out.toByteArray();
//		Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
//
//			Log.d("DEBUG_BITMAP", "Output Bitmap - Size: " + bmp.getWidth() + "x" + bmp.getHeight() +
//					", Config: " + bmp.getConfig() + ", HasAlpha: " + bmp.hasAlpha());
//
//		if (bmp == null) return null;
//		if (rotationDegrees == 0) return bmp;
//
//		try {
//			Matrix m = new Matrix();
//			m.postRotate(rotationDegrees);
//			Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
//			if (rotated != bmp) bmp.recycle();
//			return rotated;
//		} catch (Exception ignored) {
//			return bmp;
//		}
//
//	}
//}
//
//
