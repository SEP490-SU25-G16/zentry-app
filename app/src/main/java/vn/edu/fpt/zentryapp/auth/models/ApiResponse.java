package vn.edu.fpt.zentryapp.auth.models;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private T data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;
}
