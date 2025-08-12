package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceIdRequestCreateResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Data")
    private FaceIdRequestData data;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaceIdRequestData {
        @SerializedName("RequestId")
        private String requestId;

        @SerializedName("SessionId")
        private String sessionId;

        @SerializedName("LecturerId")
        private String lecturerId;

        @SerializedName("ClassSectionId")
        private String classSectionId;

        @SerializedName("ExpiresAt")
        private String expiresAt;
    }
}
