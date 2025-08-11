package vn.edu.fpt.zentryapp.student.data.model.responsedto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StudentSessionsDataDto {
    @SerializedName("Sessions")
    private List<StudentSessionDto> sessions;
}
