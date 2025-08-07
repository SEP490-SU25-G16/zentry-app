package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

// SessionStartData.java (nếu API trả về data)
public class SessionStartData {
    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("Status")
    private String status;

    @SerializedName("StartedAt")
    private String startedAt;

    // Getters
    public String getSessionId() { return sessionId; }
    public String getStatus() { return status; }
    public String getStartedAt() { return startedAt; }
}