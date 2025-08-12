package vn.edu.fpt.zentryapp.faceid.ui.setting.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceSpoofDetector;
import vn.edu.fpt.zentryapp.faceid.ui.setting.detection.SpoofDetectionManager;

/**
 * Thin adapter around SpoofDetectionManager to standardize interface for reuse across flows.
 */
public class SpoofDetectionController {

    public interface Callback {
        void onResult(@NonNull SpoofDetectionManager.SpoofDetectionResult result);
    }

    private final SpoofDetectionManager manager;

    public SpoofDetectionController(@NonNull SpoofDetectionManager manager) {
        this.manager = manager;
    }

    public static SpoofDetectionController create(@NonNull Context context,
                                                  @NonNull Object faceSpoofDetector) {
        SpoofDetectionManager m = new SpoofDetectionManager((FaceSpoofDetector) faceSpoofDetector, context);
        return new SpoofDetectionController(m);
    }

    public void setOvalBoundary(@Nullable RectF ovalRect) {
        manager.setOvalBoundary(ovalRect);
    }

    public void resetLivenessState() { manager.resetLivenessState(); }

    public void markLivenessSuccess() { manager.markLivenessSuccess(); }

    public void analyzeFrame(@NonNull Bitmap bitmap,
                             @NonNull Rect faceRect,
                             @NonNull Callback callback) {
        manager.analyzeFrame(bitmap, faceRect, callback::onResult);
    }
}


