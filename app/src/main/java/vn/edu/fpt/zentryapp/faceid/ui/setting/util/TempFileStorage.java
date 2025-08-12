package vn.edu.fpt.zentryapp.faceid.ui.setting.util;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility to persist bitmaps in app cache for short-term use (e.g., success screens).
 */
public final class TempFileStorage {
    private TempFileStorage() {}

    public static String saveBitmapToTempFile(Context context, Bitmap bitmap, String subDir) throws IOException {
        if (context == null) throw new IllegalStateException("Context is null");
        File tempDir = new File(context.getCacheDir(), subDir != null ? subDir : "face_registration");
        if (!tempDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tempDir.mkdirs();
        }

        File tempFile = new File(tempDir, "face_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        }
        return tempFile.getAbsolutePath();
    }
}


