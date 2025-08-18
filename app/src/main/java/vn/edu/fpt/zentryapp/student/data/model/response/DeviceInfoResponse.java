package vn.edu.fpt.zentryapp.student.data.model.response;

public class DeviceInfoResponse {
    public boolean Success;
    public DeviceInfo Data;
    public String Error;
    public String Message;

    public static class DeviceInfo {
        public String DeviceId;
        public String UserId;
        public String UserFullName;
        public String UserEmail;
        public String DeviceName;
        public String AndroidId;
        public String DeviceToken;
        public String Platform;
        public String OsVersion;
        public String Model;
        public String Manufacturer;
        public String AppVersion;
        public String PushNotificationToken;
        public String CreatedAt;
        public String UpdatedAt;
        public String LastVerifiedAt;
        public String Status;
    }
}
