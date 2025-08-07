package vn.edu.fpt.zentryapp.student.data.model.request;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {
    @SerializedName("userId")
    private String userId;

    @SerializedName("androidId")
    private String androidId;

    @SerializedName("deviceName")
    private String deviceName;

    @SerializedName("platform")
    private String platform;

    @SerializedName("osVersion")
    private String osVersion;

    @SerializedName("model")
    private String model;

    @SerializedName("manufacturer")
    private String manufacturer;

    @SerializedName("appVersion")
    private String appVersion;

    @SerializedName("pushNotificationToken")
    private String pushNotificationToken;
}
