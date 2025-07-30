package vn.edu.fpt.zentryapp.student.ui.setting.state;

/**
 * Face Registration State Machine
 * Quản lý tất cả trạng thái của quá trình đăng ký Face ID
 */
public enum FaceRegistrationState {
    // Khởi tạo và setup
    INITIALIZING("Initializing camera..."),
    READY("Position your face in the oval"),
    
    // Face detection states
    NO_FACE("Look at the camera"),
    MULTIPLE_FACES("Only one face should be visible"),
    FACE_DETECTED("Face detected"),
    FACE_REAL("Face verified as real"),
    
    // Positioning states
    FACE_TOO_FAR("Move closer to camera"),
    FACE_TOO_CLOSE("Move away from camera"),
    FACE_NOT_CENTERED("Center your face in oval"),
    
    // Stabilization states
    FACE_STABILIZING("Hold still..."),
    FACE_STABLE("Perfect! Processing..."),
    
    // Spoof detection
    FACE_SPOOFED("Spoof detected! Use real face"),
    SPOOF_SUSPECTED("Please ensure you're using a real face"),
    
    // Processing states
    CAPTURING("Capturing face..."),
    PROCESSING("Registering your face..."),
    
    // Final states
    SUCCESS("Face ID registered successfully!"),
    
    // Error states
    FAILED_NETWORK("Network error occurred"),
    FAILED_SPOOF("Spoof detection failed"),
    FAILED_CAMERA("Camera error"),
    FAILED_PERMISSION("Camera permission denied"),
    FAILED_OTHER("Registration failed"),
    
    // Timeout states
    TIMEOUT_DETECTION("Face detection timeout"),
    TIMEOUT_REGISTRATION("Registration timeout");
    
    private final String defaultMessage;
    
    FaceRegistrationState(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
    
    /**
     * Kiểm tra xem state có phải là final state không
     */
    public boolean isFinalState() {
        return this == SUCCESS || 
               this == FAILED_NETWORK || 
               this == FAILED_SPOOF ||
               this == FAILED_CAMERA ||
               this == FAILED_PERMISSION ||
               this == FAILED_OTHER ||
               this == TIMEOUT_DETECTION ||
               this == TIMEOUT_REGISTRATION;
    }
    
    /**
     * Kiểm tra xem state có phải là error state không
     */
    public boolean isErrorState() {
        return this == FAILED_NETWORK ||
               this == FAILED_SPOOF ||
               this == FAILED_CAMERA ||
               this == FAILED_PERMISSION ||
               this == FAILED_OTHER ||
               this == TIMEOUT_DETECTION ||
               this == TIMEOUT_REGISTRATION ||
               this == FACE_SPOOFED;
    }
    
    /**
     * Kiểm tra xem state có phải là processing state không
     */
    public boolean isProcessingState() {
        return this == CAPTURING ||
               this == PROCESSING ||
               this == FACE_STABILIZING;
    }
}