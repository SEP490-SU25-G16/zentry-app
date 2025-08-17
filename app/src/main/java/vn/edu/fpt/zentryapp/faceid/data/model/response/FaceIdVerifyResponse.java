package vn.edu.fpt.zentryapp.faceid.data.model.response;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class FaceIdVerifyResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Message")
    private String message;

    @SerializedName("Similarity")
    private Float similarity;

    @SerializedName("VerifiedAt")
    private String verifiedAt;

    @SerializedName("RequestStatus")
    private String requestStatus;

}

