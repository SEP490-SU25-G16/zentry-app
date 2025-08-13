package vn.edu.fpt.zentryapp.faceid.data.model.response;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for Face ID API operations
 */
public class FaceIdResponse {
    
    @SerializedName(value = "success", alternate = {"Success"})
    private boolean success;
    
    @SerializedName(value = "message", alternate = {"Message"})
    private String message;
    
    @SerializedName(value = "timestamp", alternate = {"Timestamp"})
    private String timestamp;
    
    @SerializedName("similarity")
    private float similarity;
    
    public FaceIdResponse() {
    }
    
    public FaceIdResponse(boolean success, String message, String timestamp, float similarity) {
        this.success = success;
        this.message = message;
        this.timestamp = timestamp;
        this.similarity = similarity;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public float getSimilarity() {
        return similarity;
    }
    
    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }
} 