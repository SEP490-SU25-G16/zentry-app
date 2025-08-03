package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CalculateAttendanceData {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Message")
    private String message;

    @SerializedName("AttendedCount")
    private int attendedCount;

}