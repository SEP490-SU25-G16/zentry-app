package vn.edu.fpt.zentryapp.student.data.model.response;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class UserProfileDto {
    @SerializedName("Id")
    private String id;

    @SerializedName("FullName")
    private String fullName;

    @SerializedName("Email")
    private String email;

    @SerializedName("HasFaceId")
    private boolean hasFaceId;
}


