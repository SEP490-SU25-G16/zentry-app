package vn.edu.fpt.zentryapp.faceid.utils;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Lightweight YUV_420_888 to ARGB converter without JPEG path.
 * Reuses the provided output Bitmap buffer (must be ARGB_8888 and match image size).
 */
public final class YuvToRgbConverter {
    private static final String TAG = "YuvToRgbConverter";

    public void yuvToRgb(Image image, Bitmap output) {
        if (image == null || output == null) return;
        if (image.getFormat() != android.graphics.ImageFormat.YUV_420_888) {
            Log.w(TAG, "Unsupported image format: " + image.getFormat());
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (output.getWidth() != width || output.getHeight() != height) {
            throw new IllegalArgumentException("Output bitmap size must match image size");
        }
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuf = planes[0].getBuffer();
        ByteBuffer uBuf = planes[1].getBuffer();
        ByteBuffer vBuf = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        int[] argb = new int[width * height];
        int pos = 0;
        for (int y = 0; y < height; y++) {
            int pY = yRowStride * y;
            int uvRow = uvRowStride * (y >> 1);
            for (int x = 0; x < width; x++) {
                int uvCol = (x >> 1) * uvPixelStride;
                int Y = yBuf.get(pY + x) & 0xff;
                int U = uBuf.get(uvRow + uvCol) & 0xff;
                int V = vBuf.get(uvRow + uvCol) & 0xff;

                // Convert YUV to RGB
                int C = Y - 16;
                int D = U - 128;
                int E = V - 128;
                if (C < 0) C = 0;
                int R = (298 * C + 409 * E + 128) >> 8;
                int G = (298 * C - 100 * D - 208 * E + 128) >> 8;
                int B = (298 * C + 516 * D + 128) >> 8;
                R = R < 0 ? 0 : Math.min(255, R);
                G = G < 0 ? 0 : Math.min(255, G);
                B = B < 0 ? 0 : Math.min(255, B);
                argb[pos++] = 0xff000000 | (R << 16) | (G << 8) | B;
            }
        }
        output.setPixels(argb, 0, width, 0, 0, width, height);
    }
}
