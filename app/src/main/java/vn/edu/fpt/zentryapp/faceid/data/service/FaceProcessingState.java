package vn.edu.fpt.zentryapp.faceid.data.service;

/**
 * Enum representing the different states of face processing for zero-touch recognition
 */
public enum FaceProcessingState {
    INITIALIZING,    // Models and camera are initializing
    LIVENESS_CHALLENGE, // Liveness detection challenge
    READY,           // Ready to detect faces
    NO_FACE,         // No face detected in frame
    MULTIPLE_FACES,  // Multiple faces detected in frame
    FACE_DETECTED,   // Single face detected but not yet verified
    FACE_SPOOFED,    // Face detected but identified as spoofed
    FACE_REAL,       // Face detected and verified as real
    FACE_STABILIZING,// Face is real and being tracked for stability
    FACE_STABLE,     // Face has been stable for required duration
    PROCESSING,      // Face is being processed for registration
    SUCCESS,         // Face registration successful
    ERROR            // Error occurred during processing
} 