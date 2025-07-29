package vn.edu.fpt.zentryapp.service;

import com.google.gson.annotations.SerializedName;

public class AttendanceApiResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

    // Getters
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public String getMessage() { return message; }
}
