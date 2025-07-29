package vn.edu.fpt.zentryapp.service;

public interface AttendanceCallbacks {

    interface BLEOperationCallback {
        void onSuccess();

        void onFailure(String error);
    }

    interface DeviceDetectionCallback {
        void onDeviceDetected(AttendanceModels.ScannedDevice device);

        void onDeviceLost(String deviceId);
    }

    interface AttendanceSubmissionCallback {
        void onSubmissionSuccess(AttendanceModels.AttendanceSubmission submission);

        void onSubmissionFailure(int roundNumber, String error);
    }
     interface CalculateAttendanceCallback {
        void onCalculateSuccess(String roundId, int attendedCount, String message);
        void onCalculateFailure(String roundId, String error);
    }
}