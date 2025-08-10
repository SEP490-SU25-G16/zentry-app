package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class EndSessionResponse {
    @SerializedName("Success")  // ✅ Capital S
    private boolean success;

    @SerializedName("Data")     // ✅ Capital D
    private EndSessionData data;

    @SerializedName("Error")    // ✅ Capital E
    private String error;

    @SerializedName("Message")  // ✅ Capital M
    private String message;
    public static class EndSessionData {
        @SerializedName("SessionId")
        private String sessionId;

        @SerializedName("Status")
        private String status;

        @SerializedName("EndTime")
        private String endTime;

        @SerializedName("UpdatedAt")
        private String updatedAt;

        @SerializedName("ActualRoundsCompleted")
        private int actualRoundsCompleted;

        @SerializedName("RoundsFinalized")
        private int roundsFinalized;
    }
}
