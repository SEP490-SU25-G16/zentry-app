package vn.edu.fpt.zentryapp.student.data.model.responsedto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StudentWeeklyReviewDto {
    @SerializedName("WeekStart")
    private String weekStart;

    @SerializedName("WeekEnd")
    private String weekEnd;

    @SerializedName("Courses")
    private List<WeeklyCourseDto> courses;
}
