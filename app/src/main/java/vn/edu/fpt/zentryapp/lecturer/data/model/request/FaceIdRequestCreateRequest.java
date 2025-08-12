package vn.edu.fpt.zentryapp.lecturer.data.model.request;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceIdRequestCreateRequest {
    @SerializedName("lecturerId")
    private String lecturerId;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("classSectionId")
    private String classSectionId;

    @SerializedName("expiresInMinutes")
    private int expiresInMinutes;

    @SerializedName("title")
    private String title;

    @SerializedName("body")
    private String body;
}
