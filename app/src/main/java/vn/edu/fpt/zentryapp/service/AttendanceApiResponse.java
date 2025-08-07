package vn.edu.fpt.zentryapp.service;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AttendanceApiResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

}
