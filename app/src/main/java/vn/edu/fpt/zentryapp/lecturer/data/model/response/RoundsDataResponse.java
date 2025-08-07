package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;

@Getter
public class RoundsDataResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private List<RoundDetail> data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;
}