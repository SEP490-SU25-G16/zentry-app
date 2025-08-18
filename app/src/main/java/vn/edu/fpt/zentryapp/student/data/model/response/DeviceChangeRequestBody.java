package vn.edu.fpt.zentryapp.student.data.model.response;

public class DeviceChangeRequestBody {
    public String userId;
    public String reason;
    public String deviceName;
    public String androidId;
    public String platform;
    public String osVersion;
    public String model;
    public String manufacturer;
    public String appVersion;
    public String pushNotificationToken;

    public DeviceChangeRequestBody(
            String userId, String reason, String deviceName, String androidId, String platform,
            String osVersion, String model, String manufacturer, String appVersion, String pushNotificationToken
    ) {
        this.userId = userId;
        this.reason = reason;
        this.deviceName = deviceName;
        this.androidId = androidId;
        this.platform = platform;
        this.osVersion = osVersion;
        this.model = model;
        this.manufacturer = manufacturer;
        this.appVersion = appVersion;
        this.pushNotificationToken = pushNotificationToken;
    }
}
