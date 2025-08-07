package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundResultDto {
    @SerializedName("RoundId")
    private String roundId;

    @SerializedName("RoundNumber")
    private int roundNumber;

    @SerializedName("StartTime")
    private String startTime; // yyyy-MM-dd HH:mm:ss format

    @SerializedName("EndTime")
    private String endTime; // yyyy-MM-dd HH:mm:ss format

    @SerializedName("Status")
    private String status;

    @SerializedName("StudentsAttendance")
    private List<StudentAttendanceDto> studentsAttendance = new ArrayList<>();

    // Helper methods
    public int getTotalStudents() {
        return studentsAttendance != null ? studentsAttendance.size() : 0;
    }

    public int getAttendedCount() {
        if (studentsAttendance == null) return 0;
        return (int) studentsAttendance.stream().mapToLong(s -> s.isAttended() ? 1 : 0).sum();
    }
}

