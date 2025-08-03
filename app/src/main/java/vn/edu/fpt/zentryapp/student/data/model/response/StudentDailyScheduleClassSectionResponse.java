package vn.edu.fpt.zentryapp.student.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StudentDailyScheduleClassSectionResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private List<StudentScheduleClassSection> data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

}

