package vn.edu.fpt.zentryapp.faceid.data.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import lombok.Getter;

@Getter
public class FaceIdRequestStatusResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Message")
    private String message;

    @SerializedName("Error")
    private String error;

    @SerializedName("Data")
    private Data data;

    public static class Data {
        @SerializedName("RequestId")
        private String requestId;

        @SerializedName("SessionId")
        private String sessionId;

        @SerializedName("Status")
        private String status;

        @SerializedName("ExpiresAt")
        private String expiresAt;

        @SerializedName("Threshold")
        private Float threshold;

        @SerializedName("VerifiedUserIds")
        private List<String> verifiedUserIds;

        public String getRequestId() { return requestId; }
        public String getSessionId() { return sessionId; }
        public String getStatus() { return status; }
        public String getExpiresAt() { return expiresAt; }
        public Float getThreshold() { return threshold; }
        public List<String> getVerifiedUserIds() { return verifiedUserIds; }
    }
}

