package vn.edu.fpt.zentryapp.faceid.data.service;

public final class FaceDecisionInput {
    public final FaceDecisionEngine.FaceDetectionResult detection;
    public final FaceDecisionEngine.SpoofDetectionResult spoof;
    public final FaceDecisionEngine.OvalValidationResult oval;
    public final boolean livenessVerifiedRecently;
    public final boolean straightGaze;
    public final FaceIdConfig.Scenario scenario;

    public FaceDecisionInput(
            FaceDecisionEngine.FaceDetectionResult detection,
            FaceDecisionEngine.SpoofDetectionResult spoof,
            FaceDecisionEngine.OvalValidationResult oval,
            boolean livenessVerifiedRecently,
            boolean straightGaze,
            FaceIdConfig.Scenario scenario
    ) {
        this.detection = detection;
        this.spoof = spoof;
        this.oval = oval;
        this.livenessVerifiedRecently = livenessVerifiedRecently;
        this.straightGaze = straightGaze;
        this.scenario = scenario;
    }
}