package vn.edu.fpt.zentryapp.service;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class AttendanceModels {

    @AllArgsConstructor
    @Getter
    public static class AttendanceRound implements Serializable {
        private static final long serialVersionUID = 1L;
        private String roundId;
        private final Date executionTime;
        private final int roundNumber;
        private final boolean isLastRound;
    }

    @Getter
    @AllArgsConstructor
    public static class BLEAdvertiseData {
        private final String macAddress;
        private final String roomName;
    }


    @Getter
    @AllArgsConstructor
    public static class ScannedDevice {
        private final String deviceId;
        private final int rssi;
    }

    @AllArgsConstructor
    @Getter
    public static class AttendanceSubmission {
        private final String submitterDeviceMacAddress;
        private final String sessionId;
        private final List<ScannedDevice> scannedDevices;
        private final String timestamp;
        public AttendanceSubmission(String submitterDeviceMacAddress, String userId,
                                    String sessionId, List<ScannedDevice> scannedDevices, Date timestamp) {
            this.submitterDeviceMacAddress = submitterDeviceMacAddress;
            this.sessionId = sessionId;
            this.scannedDevices = scannedDevices;
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(timestamp);
        }
    }
}