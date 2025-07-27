package vn.edu.fpt.zentryapp.auth.models;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    @SerializedName("Id")
    private String accountId;
    @SerializedName("Email")
    private String email;
    @SerializedName("Role")
    private String role;
}