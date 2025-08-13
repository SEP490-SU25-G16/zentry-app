package vn.edu.fpt.zentryapp.student.data.model.responsedto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class StudentCalendarDayDto {
    @SerializedName("Date")
    private String date;

    @SerializedName("Classes")
    private List<StudentCalendarClassDto> classes;
}
