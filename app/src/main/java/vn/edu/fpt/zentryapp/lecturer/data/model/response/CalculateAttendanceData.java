package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

// CalculateAttendanceData.java
public class CalculateAttendanceData {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Message")
    private String message;

    @SerializedName("AttendedCount")
    private int attendedCount;

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public int getAttendedCount() { return attendedCount; }
}