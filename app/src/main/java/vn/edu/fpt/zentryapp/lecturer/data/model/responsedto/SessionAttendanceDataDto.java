package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class SessionAttendanceDataDto {
    @SerializedName("SessionInfo")
    private SessionInfoDto sessionInfo;

    @SerializedName("Students")
    private List<StudentAttendanceDto> students;
}
