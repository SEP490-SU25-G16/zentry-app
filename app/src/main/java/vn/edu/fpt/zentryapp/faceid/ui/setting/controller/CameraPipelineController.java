package vn.edu.fpt.zentryapp.faceid.ui.setting.controller;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import vn.edu.fpt.zentryapp.faceid.ui.components.CameraView;

/**
 * Thin controller to manage CameraView start/stop and route frames to a consumer.
 * This helps reuse the camera pipeline across Register/Update/Verify flows.
 */
public class CameraPipelineController {

    public interface FrameConsumer {
        void onFrame(Bitmap bitmap);
    }

    private final CameraView cameraView;

    public CameraPipelineController(CameraView cameraView) {
        this.cameraView = cameraView;
    }

    public void start(LifecycleOwner lifecycleOwner, @Nullable FrameConsumer consumer) {
        if (cameraView == null) return;
        cameraView.startCamera(lifecycleOwner, bitmap -> {
            if (consumer != null) {
                consumer.onFrame(bitmap);
            }
        });
    }

    public void stop() {
        if (cameraView == null) return;
        cameraView.stopCamera();
    }
}


