package vn.edu.fpt.zentryapp.faceid.ui.common;

/**
 * Callback interface for Face ID processing operations
 */
public interface FaceIdProcessingCallback {
    /**
     * Called when the operation succeeds
     * @param message Success message
     * @param metadata Additional metadata about the operation
     */
    void onSuccess(String message, Object metadata);
    
    /**
     * Called when the operation fails
     * @param errorMessage Error message
     */
    void onFailure(String errorMessage);
}
