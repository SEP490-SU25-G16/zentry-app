package vn.edu.fpt.zentryapp.student.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentFinalAttendanceResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private StudentFinalAttendanceDto data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;
}
