package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

// CalculateAttendanceResponse.java
public class CalculateAttendanceResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private CalculateAttendanceData data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

    // Getters
    public boolean isSuccess() { return success; }
    public CalculateAttendanceData getData() { return data; }
    public String getError() { return error; }
    public String getMessage() { return message; }
}