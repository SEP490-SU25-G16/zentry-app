package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoundsResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private List<RoundData> data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;
}