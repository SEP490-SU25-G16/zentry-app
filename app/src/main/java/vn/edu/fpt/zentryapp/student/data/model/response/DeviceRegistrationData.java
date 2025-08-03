package vn.edu.fpt.zentryapp.student.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationData {
    @SerializedName("DeviceId")
    private String deviceId;

    @SerializedName("UserId")
    private String userId;

    @SerializedName("DeviceToken")
    private String deviceToken;

    @SerializedName("MacAddress")
    private String macAddress;

    @SerializedName("CreatedAt")
    private String createdAt;
}
