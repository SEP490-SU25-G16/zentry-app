package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class ApiResponseDto<T> {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private T data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

}


