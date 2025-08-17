package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    @SerializedName("UserId")
    private String userId;
    @SerializedName("Code")
    private String code;

    @SerializedName("AccountId")
    private String accountId;

    @SerializedName("Email")
    private String email;

    @SerializedName("FullName")
    private String fullName;

    @SerializedName("PhoneNumber")
    private String phoneNumber;

    @SerializedName("Role")
    private String role;

    @SerializedName("Status")
    private String status;

    @SerializedName("CreatedAt")
    private String createdAt;

    @SerializedName("HasFaceId")
    private boolean hasFaceId;

    @SerializedName("FaceIdLastUpdated")
    private String faceIdLastUpdated;}
