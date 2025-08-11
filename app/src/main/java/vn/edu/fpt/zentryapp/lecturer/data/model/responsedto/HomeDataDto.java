package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class HomeDataDto {
    @SerializedName("NextSessions")
    private List<NextSessionDto> nextSessions;

    @SerializedName("WeeklyOverview")
    private List<WeeklyOverviewDto> weeklyOverview;

}
