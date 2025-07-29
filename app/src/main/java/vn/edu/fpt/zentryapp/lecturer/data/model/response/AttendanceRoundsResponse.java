package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AttendanceRoundsResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private List<AttendanceRoundData> data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

    // Getters
    public boolean isSuccess() { return success; }
    public List<AttendanceRoundData> getData() { return data; }
    public String getError() { return error; }
    public String getMessage() { return message; }
}