package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DailyScheduleResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private List<ClassSectionData> data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

    // Getters
    public boolean isSuccess() { return success; }
    public List<ClassSectionData> getData() { return data; }
    public String getError() { return error; }
    public String getMessage() { return message; }
}
