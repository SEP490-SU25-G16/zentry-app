package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;

@Getter
public class AttendanceResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private List<FinalAttendance> data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

}