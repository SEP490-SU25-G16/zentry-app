package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CalculateAttendanceResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private CalculateAttendanceData data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;
}