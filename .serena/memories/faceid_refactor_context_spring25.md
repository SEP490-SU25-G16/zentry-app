Context: Face ID UI refactor (Android). We split StudentSettingRegisterFaceIdFragment (~1800 lines) into SRP modules and introduced an Orchestrator. 

Modules created:
- CameraPipelineController: wraps CameraView start/stop, forwards frames.
- SpoofDetectionController (adapter over SpoofDetectionManager).
- LivenessVerificationController: manages FaceIdEnhancer, liveness UI and callbacks.
- FaceAnalysisController: 5s quality analysis (UI overlay, countdown, thresholds). Fragment no longer handles analysis logic/UI.
- FaceRegistrationAction: wraps FaceIdService.captureAndRegisterFace.
- FaceUpdateAction: wraps FaceIdProcessor.registerFace.
- FaceVerifyAction: wraps FaceIdProcessor.verifyFace.
- TempFileStorage: saves bitmaps to cache.
- ErrorPresenter: standard error/retry dialogs.
- SuccessNavigator: launches success screen.

Orchestrator:
- FaceIdRegistrationOrchestrator routes frames:
  - LIVENESS_CHALLENGE: processContinuousFrame → feed LivenessVerificationController.
  - ANALYZING: feed FaceAnalysisController.
  - NORMAL+SPOOF: processContinuousFrame → SpoofDetectionController; trigger liveness or forward; auto startAnalysis when livenessVerified and not analyzing.
- DI provided: OperationMode, spoof/liveness/analysis controllers, FaceIdService provider, Oval provider (RectF), RegistrationAction, StateManager, livenessVerified provider.

Integration status:
- Register fragment uses CameraPipelineController + Orchestrator and delegates analysis, spoof, liveness routing via orchestrator. TempFileStorage and SuccessNavigator in use. ErrorPresenter replaces AlertDialog usage. Fragment still keeps stability/UI and state updates.
- Update/Verify fragments now use FaceUpdateAction/FaceVerifyAction for actions; pipeline still via BaseFaceIdFragment.

Next steps planned:
1) Extend orchestrator to handle stability/analysis trigger more fully (optional), then remove duplicate startAnalysis triggers in Register fragment.
2) Apply orchestrator to Update/Verify (OperationMode UPDATE/VERIFICATION) for shared pipeline; only action differs.
3) Optional: extract remaining UI status/presentation helpers to StatusPresenter; optional PermissionsHandler.

Goal: Reduce Register fragment to ~300–450 lines; unify pipeline across Register/Update/Verify with SOLID/DRY.