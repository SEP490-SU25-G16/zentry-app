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
    public static class ScannedDevice {
        private final String androidId;
        private final int rssi;
    }

    @AllArgsConstructor
    @Getter
    public static class AttendanceSubmission {
        private final String submitterDeviceAndroidId;
        private final String sessionId;
        private final List<ScannedDevice> scannedDevices;
        private final String timestamp;
    }
}