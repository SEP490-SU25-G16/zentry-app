package vn.edu.fpt.zentryapp.faceid.ui.setting.action;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdService;

/**
 * Encapsulates the Face ID registration call to the FaceIdService.
 * Keeps Fragment free from direct service call to improve testability and reuse.
 */
public class FaceRegistrationAction {

    public interface Callback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    /**
     * Execute registration using the provided service.
     */
    public void execute(@NonNull FaceIdService faceIdService,
                        @NonNull Bitmap bitmap,
                        @NonNull Rect faceRect,
                        RectF ovalRect,
                        @NonNull String userId,
                        @NonNull Callback callback) {
        faceIdService.captureAndRegisterFace(
                bitmap,
                faceRect,
                ovalRect,
                userId,
                new FaceIdService.FaceIdCallback() {
                    @Override
                    public void onSuccess(String message) {
                        callback.onSuccess(message);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        callback.onFailure(errorMessage);
                    }
                }
        );
    }
}


