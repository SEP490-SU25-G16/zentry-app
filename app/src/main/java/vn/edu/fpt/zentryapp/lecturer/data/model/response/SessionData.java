package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

public class SessionData {
    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("SessionNumber")
    private int sessionNumber;

    @SerializedName("Status")
    private String status;

    @SerializedName("StartTime")
    private String startTime;

    @SerializedName("EndTime")
    private String endTime;

    // Getters
    public String getSessionId() { return sessionId; }
    public int getSessionNumber() { return sessionNumber; }
    public String getStatus() { return status; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
}
