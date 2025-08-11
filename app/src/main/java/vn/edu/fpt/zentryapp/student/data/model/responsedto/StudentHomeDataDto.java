package vn.edu.fpt.zentryapp.student.data.model.responsedto;


import com.google.gson.annotations.SerializedName;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StudentHomeDataDto {
    @SerializedName("NextSessions")
    private List<StudentNextSessionDto> nextSessions;

    @SerializedName("WeeklyReview")
    private StudentWeeklyReviewDto weeklyReview;
}
