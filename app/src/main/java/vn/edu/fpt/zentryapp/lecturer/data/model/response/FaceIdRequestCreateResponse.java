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

    // Some backends may return the data fields at the root instead of under Data
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

    @SerializedName("TotalRecipients")
    private Integer totalRecipients;

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

    /**
     * Treat responses as success if either the standard flag is true,
     * or if a requestId is present at the root or inside Data.
     */
    public boolean isEffectiveSuccess() {
        if (success) return true;
        if (data != null && data.getRequestId() != null && !data.getRequestId().isEmpty()) return true;
        return requestId != null && !requestId.isEmpty();
    }
}
