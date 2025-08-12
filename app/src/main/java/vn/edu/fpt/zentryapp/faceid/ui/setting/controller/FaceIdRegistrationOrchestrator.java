package vn.edu.fpt.zentryapp.faceid.ui.setting.controller;

import androidx.lifecycle.LifecycleOwner;

import vn.edu.fpt.zentryapp.faceid.ui.setting.controller.CameraPipelineController.FrameConsumer;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdService;
import android.graphics.Rect;
import android.graphics.RectF;
import vn.edu.fpt.zentryapp.faceid.ui.setting.state.FaceRegistrationStateManager;
import vn.edu.fpt.zentryapp.faceid.ui.setting.state.FaceRegistrationState;

/**
 * Orchestrator that wires the camera pipeline and verification sub-controllers.
 * Phase 1: delegate only camera start/stop to avoid behavior changes.
 * Next phases can route frame processing and state transitions through this class.
 */
public class FaceIdRegistrationOrchestrator {

    private final CameraPipelineController cameraController;
    private FrameConsumer frameConsumer;

    // Optional dependencies for later phases (kept for DI, not used yet to avoid behavior change)
    public enum OperationMode { REGISTER, UPDATE, VERIFY }
    private OperationMode mode = OperationMode.REGISTER;
    private SpoofDetectionController spoofController;
    private LivenessVerificationController livenessController;
    private FaceAnalysisController analysisController;
    private FaceIdServiceProvider faceIdServiceProvider;
    private OvalRectProvider ovalRectProvider;
    private RegistrationAction registrationAction;
    private FaceRegistrationStateManager stateManager;
    private BooleanProvider livenessVerifiedProvider;

    public interface FaceIdServiceProvider { FaceIdService get(); }
    public interface OvalRectProvider { RectF get(); }
    public interface RegistrationAction { void run(); }
    public interface BooleanProvider { boolean get(); }

    // Optional: allow routing when base already provides faceRect and flags
    public boolean routeProcessedFrame(android.graphics.Bitmap bitmap,
                                       Rect faceRect,
                                       boolean isValidPosition,
                                       boolean isSpoof,
                                       String statusMessage) {
        // When analyzing: analysis consumes
        if (analysisController != null && analysisController.isAnalyzing()) {
            FaceIdService svc = faceIdServiceProvider != null ? faceIdServiceProvider.get() : null;
            RectF oval = ovalRectProvider != null ? ovalRectProvider.get() : null;
            if (svc != null) {
                analysisController.processFrame(svc, bitmap, oval);
                return true;
            }
        }
        // Otherwise, let default flow continue
        return false;
    }

    public FaceIdRegistrationOrchestrator(CameraPipelineController cameraController) {
        this.cameraController = cameraController;
    }

    public void setFrameConsumer(FrameConsumer consumer) {
        this.frameConsumer = consumer;
    }

    public void startCamera(LifecycleOwner lifecycleOwner, FrameConsumer consumer) {
        if (cameraController == null) return;
        // store delegate so we can evolve orchestration later without changing callers
        setFrameConsumer(consumer);
        cameraController.start(lifecycleOwner, bitmap -> {
            // 1) Liveness challenge routing (safe): if in LIVENESS_CHALLENGE, feed to liveness controller and return
            if (stateManager != null && livenessController != null &&
                    stateManager.getCurrentState() == FaceRegistrationState.LIVENESS_CHALLENGE) {
                FaceIdService svc = faceIdServiceProvider != null ? faceIdServiceProvider.get() : null;
                RectF oval = ovalRectProvider != null ? ovalRectProvider.get() : null;
                if (svc != null && oval != null) {
                    svc.processContinuousFrame(bitmap, oval, new FaceIdService.ContinuousProcessingCallback() {
                        @Override
                        public void onFaceDetected(Rect boundingBox, boolean isSpoof, float spoofScore) {
                            livenessController.processFrame(bitmap, boundingBox);
                        }

                        @Override
                        public void onNoFaceDetected() {
                            stateManager.transitionTo(FaceRegistrationState.NO_FACE, "Look at the camera");
                        }

                        @Override
                        public void onMultipleFacesDetected() {
                            stateManager.transitionTo(FaceRegistrationState.MULTIPLE_FACES,
                                    "Only one person should be visible");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // no-op: keep errors handled upstream if needed
                        }
                    });
                    return;
                }
            }

            // 2) Analysis routing (safe): if analyzing, feed to analysis controller and return
            if (analysisController != null && analysisController.isAnalyzing()) {
                FaceIdService svc = faceIdServiceProvider != null ? faceIdServiceProvider.get() : null;
                RectF oval = ovalRectProvider != null ? ovalRectProvider.get() : null;
                if (svc != null) {
                    analysisController.processFrame(svc, bitmap, oval);
                    return;
                }
            }

            // 3) Fallback to fragment consumer (original pipeline)
            if (faceIdServiceProvider != null && ovalRectProvider != null && spoofController != null) {
                FaceIdService svc = faceIdServiceProvider.get();
                RectF oval = ovalRectProvider.get();
                if (svc != null && oval != null) {
                    svc.processContinuousFrame(bitmap, oval, new FaceIdService.ContinuousProcessingCallback() {
                        @Override
                        public void onFaceDetected(Rect boundingBox, boolean isSpoof, float spoofScore) {
                            // Delegate spoof decision; if liveness needed or spoofed, handle here; otherwise fallback
                            spoofController.analyzeFrame(bitmap, boundingBox, result -> {
                                if (result.triggerLivenessChallenge) {
                                    if (stateManager != null) {
                                        stateManager.transitionTo(FaceRegistrationState.LIVENESS_CHALLENGE, result.explanation);
                                    }
                                    return; // do not forward
                                }
                                if (result.isSpoof) {
                                    if (stateManager != null) {
                                        stateManager.transitionTo(FaceRegistrationState.FACE_SPOOFED, result.explanation);
                                    }
                                    return; // do not forward
                                }
                                // Proceed: decide to start analysis when conditions are right, otherwise forward
                                boolean livenessVerified = livenessVerifiedProvider != null && livenessVerifiedProvider.get();
                                if (analysisController != null && faceIdServiceProvider != null && ovalRectProvider != null) {
                                    // Heuristic: let fragment still handle stability guidance; we only trigger analysis
                                    if (!analysisController.isAnalyzing() && livenessVerified) {
                                        analysisController.startAnalysis(faceIdServiceProvider.get(), ovalRectProvider.get(), livenessVerified);
                                        return; // analysis will take over routing next frames
                                    }
                                }
                                if (frameConsumer != null) frameConsumer.onFrame(bitmap);
                            });
                        }

                        @Override
                        public void onNoFaceDetected() {
                            if (stateManager != null) {
                                stateManager.transitionTo(FaceRegistrationState.NO_FACE, "Look at the camera");
                            }
                        }

                        @Override
                        public void onMultipleFacesDetected() {
                            if (stateManager != null) {
                                stateManager.transitionTo(FaceRegistrationState.MULTIPLE_FACES, "Only one person should be visible");
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // no-op
                        }
                    });
                    return;
                }
            }
            // Default forward
            if (frameConsumer != null) frameConsumer.onFrame(bitmap);
        });
    }

    public void stopCamera() {
        if (cameraController == null) return;
        cameraController.stop();
    }

    // ---- Dependency injection (optional) ----
    public FaceIdRegistrationOrchestrator setMode(OperationMode mode) { this.mode = mode; return this; }
    public FaceIdRegistrationOrchestrator withSpoof(SpoofDetectionController spoof) { this.spoofController = spoof; return this; }
    public FaceIdRegistrationOrchestrator withLiveness(LivenessVerificationController liveness) { this.livenessController = liveness; return this; }
    public FaceIdRegistrationOrchestrator withAnalysis(FaceAnalysisController analysis) { this.analysisController = analysis; return this; }
    public FaceIdRegistrationOrchestrator withService(FaceIdServiceProvider provider) { this.faceIdServiceProvider = provider; return this; }
    public FaceIdRegistrationOrchestrator withOval(OvalRectProvider provider) { this.ovalRectProvider = provider; return this; }
    public FaceIdRegistrationOrchestrator withRegistrationAction(RegistrationAction action) { this.registrationAction = action; return this; }
    public FaceIdRegistrationOrchestrator withStateManager(FaceRegistrationStateManager sm) { this.stateManager = sm; return this; }
    public FaceIdRegistrationOrchestrator withLivenessVerified(BooleanProvider provider) { this.livenessVerifiedProvider = provider; return this; }
}


