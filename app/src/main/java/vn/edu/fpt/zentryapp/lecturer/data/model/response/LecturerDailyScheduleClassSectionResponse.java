package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Getter
@NoArgsConstructor
public class LecturerDailyScheduleClassSectionResponse {

    @SerializedName("Success")
    private boolean success;

    @SerializedName("Message")
    private String message;

    @SerializedName("Error")
    private String error;

    @SerializedName("Data")
    private List<LecturerScheduleClassSection> data;

}
