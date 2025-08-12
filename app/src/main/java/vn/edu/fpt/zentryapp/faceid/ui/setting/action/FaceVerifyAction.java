package vn.edu.fpt.zentryapp.faceid.ui.setting.action;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vn.edu.fpt.zentryapp.faceid.ui.common.FaceIdProcessingCallback;
import vn.edu.fpt.zentryapp.faceid.ui.common.FaceIdProcessor;

/**
 * Encapsulates Face ID verify action using FaceIdProcessor.
 */
public class FaceVerifyAction {
    public interface Callback {
        void onSuccess(String message, @Nullable Object metadata);
        void onFailure(String errorMessage);
    }

    public void execute(@NonNull FaceIdProcessor processor,
                        @NonNull Bitmap bitmap,
                        @NonNull Rect faceRect,
                        @Nullable RectF ovalRect,
                        @NonNull String userId,
                        @NonNull Callback callback) {
        processor.verifyFace(bitmap, faceRect, ovalRect, userId, new FaceIdProcessingCallback() {
            @Override
            public void onSuccess(String message, Object metadata) {
                callback.onSuccess(message, metadata);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }
}


