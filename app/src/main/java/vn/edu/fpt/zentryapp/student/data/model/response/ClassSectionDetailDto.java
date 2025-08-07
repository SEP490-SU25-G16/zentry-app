package vn.edu.fpt.zentryapp.student.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassSectionDetailDto {
    @SerializedName("CourseName")
    private String courseName;

    @SerializedName("EnrolledStudentsCount")
    private int enrolledStudentsCount;

    @SerializedName("DurationInMinutes")
    private int durationInMinutes;

    @SerializedName("Building")
    private String building;

    @SerializedName("ClassSectionId")
    private String classSectionId;

    // Helper method để format duration
    public String getFormattedDuration() {
        if (durationInMinutes < 60) {
            return durationInMinutes + " mins";
        } else {
            int hours = durationInMinutes / 60;
            int mins = durationInMinutes % 60;
            if (mins == 0) {
                return hours + (hours == 1 ? " hour" : " hours");
            } else {
                return hours + (hours == 1 ? " hour " : " hours ") + mins + " mins";
            }
        }
    }

    // Helper method để format student count
    public String getFormattedStudentCount() {
        return enrolledStudentsCount + (enrolledStudentsCount == 1 ? " Student" : " Students");
    }
}
