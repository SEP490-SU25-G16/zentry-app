package vn.edu.fpt.zentryapp.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class AttendanceModels {

    @AllArgsConstructor
    @Getter
    public static class AttendanceRound implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Date timestamp;
        private final int roundNumber;
        private final boolean isLastRound;
    }

    @Getter
    @AllArgsConstructor
    public static class BLEAdvertiseData {
        private final String mac;
        private final String roomName;
    }


    @Getter
    @AllArgsConstructor
    public static class ScannedDevice {
        private final String MAC;
    }

    @AllArgsConstructor
    @Getter
    public static class AttendanceSubmission {
        private final String submitterMac;
        private final String submitterUserId;
        private final String sessionId;
        private final List<ScannedDevice> scannedDevices;
        private final Date timestamp;
    }
}