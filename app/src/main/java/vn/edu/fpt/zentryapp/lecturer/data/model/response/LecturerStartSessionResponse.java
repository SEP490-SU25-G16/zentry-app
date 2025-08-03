package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class LecturerStartSessionResponse {
    // Getters
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

    @SerializedName("Data")
    private SessionStartData data;

}