package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceDto {
    @SerializedName("StudentId")
    private String studentId;

    @SerializedName("FullName")
    private String fullName;

    @SerializedName("IsAttended")
    private boolean isAttended;

    @SerializedName("AttendedTime")
    private String attendedTime; // yyyy-MM-dd HH:mm:ss format

    // Helper methods
    public String getDisplayName() {
        return fullName;
    }

    public String getAttendanceStatus() {
        return isAttended ? "Present" : "Absent";
    }
}
