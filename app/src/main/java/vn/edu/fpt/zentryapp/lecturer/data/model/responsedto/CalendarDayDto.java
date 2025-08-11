package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CalendarDayDto {
    @SerializedName("Date")
    private String date;

    @SerializedName("Classes")
    private List<CalendarClassDto> classes;
}
