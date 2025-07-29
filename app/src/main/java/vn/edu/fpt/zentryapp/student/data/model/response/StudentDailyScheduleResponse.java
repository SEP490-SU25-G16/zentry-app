package vn.edu.fpt.zentryapp.student.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class StudentDailyScheduleResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private List<StudentClassSectionData> data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

    // Getters
    public boolean isSuccess() { return success; }
    public List<StudentClassSectionData> getData() { return data; }
    public String getError() { return error; }
    public String getMessage() { return message; }
}

