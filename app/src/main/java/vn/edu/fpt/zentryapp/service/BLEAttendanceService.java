package vn.edu.fpt.zentryapp.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;

import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import vn.edu.fpt.zentryapp.MainActivity;
import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleSession;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleSession;

public class BLEAttendanceService extends Service {
    private static final String TAG = "BLEAttendanceService";
    private static final String CHANNEL_ID = "BLE_ATTENDANCE_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;

    // 🔧 SIMPLE broadcast constants
    public static final String ACTION_ATTENDANCE_CALCULATED = "vn.edu.fpt.zentryapp.ATTENDANCE_CALCULATED";
    public static final String EXTRA_SESSION_ID = "sessionId";

    // Core components
    private BLEAttendanceManager bleManager;
    private AttendanceRoundScheduler roundScheduler;
    private AttendanceSubmissionHandler submissionHandler;

    // State management
    private final Map<String, AttendanceModels.ScannedDevice> detectedDevices = new ConcurrentHashMap<>();
    private AttendanceModels.BLEAdvertiseData currentAdvertiseData;
    private String sessionId;
    private String room;
    private String userId;
    private AttendanceCalculateHandler calculateHandler; // 🔧 THÊM
    private String userRole; // 🔧 THÊM để track role

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "=== BLE ATTENDANCE SERVICE CREATING ===");
        createNotificationChannel();

        // Initialize components
        bleManager = new BLEAttendanceManager(this);
        submissionHandler = new AttendanceSubmissionHandler(this);
        calculateHandler = new AttendanceCalculateHandler(this);
        Log.d(TAG, "All components initialized successfully");
        Log.d(TAG, "BLE Manager: " + (bleManager != null ? "Ready" : "Failed"));
        Log.d(TAG, "Submission Handler: " + (submissionHandler != null ? "Ready" : "Failed"));
        Log.d(TAG, "Calculate Handler: " + (calculateHandler != null ? "Ready" : "Failed"));
        Log.d(TAG, "======================================");
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "=== SERVICE START COMMAND ===");
        Log.d(TAG, "Intent: " + (intent != null ? "Available" : "NULL"));
        Log.d(TAG, "Flags: " + flags);
        Log.d(TAG, "Start ID: " + startId);
        if (intent == null) {
            Log.w(TAG, "Intent is null, returning START_STICKY");
            return START_STICKY;
        }

        String action = intent.getAction();
        Log.d(TAG, "Action: " + action);
        if (!"START_ATTENDANCE".equals(action)) {
            Log.w(TAG, "Invalid action, stopping service");
            stopAttendanceService();
            return START_STICKY;
        }

        // Extract data from intent
        String userId = intent.getStringExtra("userId");
        String userRole = intent.getStringExtra("userRole");

        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "User Role: " + userRole);
        // rounds thì vẫn getParcelableArrayListExtra / getSerializableExtra như trước
        @SuppressWarnings("unchecked")
        List<AttendanceModels.AttendanceRound> rounds =
                (List<AttendanceModels.AttendanceRound>) intent.getSerializableExtra("rounds");
        Log.d(TAG, "Rounds: " + (rounds != null ? rounds.size() + " rounds" : "NULL"));

        if (rounds != null) {
            for (int i = 0; i < rounds.size(); i++) {
                AttendanceModels.AttendanceRound round = rounds.get(i);
                Log.d(TAG, "  Round " + (i + 1) + ": " + round.getRoundNumber());
            }
        }
        // 2) Tạo sessionId + room tuỳ theo role
        String sessionId;
        String room;

        if ("STUDENT".equals(userRole)) {
            Log.d(TAG, "Processing STUDENT session data...");
            StudentScheduleSession studentScheduleSession = (StudentScheduleSession) intent.getSerializableExtra("session");
            if (studentScheduleSession != null) {
                sessionId = studentScheduleSession.getSessionId();
                room = studentScheduleSession.getRoom();
                Log.d(TAG, "Student session - ID: " + sessionId + ", Room: " + room);
            } else {
                Log.e(TAG, "Student session is NULL!");
                return START_STICKY;
            }
        } else {
            Log.d(TAG, "Processing LECTURER session data...");
            LecturerScheduleSession session = (LecturerScheduleSession) intent.getSerializableExtra("session");
            if (session != null) {
                sessionId = session.getSessionId();
                room = session.getRoom();
                Log.d(TAG, "Lecturer session - ID: " + sessionId + ", Room: " + room);
            } else {
                Log.e(TAG, "Lecturer session is NULL!");
                return START_STICKY;
            }
        }

        Log.d(TAG, "Final session data - ID: " + sessionId + ", Room: " + room);
        Log.d(TAG, "Starting attendance service...");

        startAttendanceService(userId, sessionId, room, rounds);

        Log.d(TAG, "==============================");
        return START_STICKY;
    }


    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void startAttendanceService(
            String userId,
            String sessionId,
            String room,
            List<AttendanceModels.AttendanceRound> rounds) {

        Log.d(TAG, "=== STARTING ATTENDANCE SERVICE ===");
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Session ID: " + sessionId);
        Log.d(TAG, "Room: " + room);
        Log.d(TAG, "Rounds count: " + (rounds != null ? rounds.size() : 0));

        this.sessionId = sessionId;
        this.room = room;
        this.userId = userId;
        @SuppressLint("HardwareIds") String deviceMac = getDeviceMacFromAndroidId();
        currentAdvertiseData = new AttendanceModels.BLEAdvertiseData(
                deviceMac,  // 🔧 THÊM MAC address
                room        // roomName
        );
        Log.d(TAG, "Device MAC: " + deviceMac);
        Log.d(TAG, "Created advertise data: " + currentAdvertiseData.toString());

        // Start foreground service
        Log.d(TAG, "Starting foreground service...");
        startForeground(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "Foreground service started");

        // Start BLE operations
        Log.d(TAG, "Starting BLE operations...");
        startBLEOperations();

        // Schedule rounds
        Log.d(TAG, "Creating round scheduler...");
        roundScheduler = new AttendanceRoundScheduler(
                rounds,
                this::executeRound,
                this::calculateRound,
                this::onAllRoundsComplete
        );

        Log.d(TAG, "Starting round scheduler...");
        roundScheduler.start();

        Log.d(TAG, "✅ Attendance service started successfully for session: " + sessionId);
        Log.d(TAG, "===================================");
    }

    /**
     * 🔧 LẤY MAC TỪ ANDROID ID (THAY THẾ CHO bluetoothAdapter.getAddress())
     */
    @SuppressLint("HardwareIds")
    private String getDeviceMacFromAndroidId() {
        Log.d(TAG, "=== GETTING MAC FROM ANDROID ID ===");

        try {
            String androidId = android.provider.Settings.Secure.getString(
                    this.getContentResolver(), // Hoặc context.getContentResolver() nếu trong class khác
                    android.provider.Settings.Secure.ANDROID_ID
            );

            if (androidId == null || androidId.isEmpty()) {
                Log.w(TAG, "⚠️ Android ID is null or empty, using fallback");
                return "00:00:00:00:00:00";
            }

            Log.d(TAG, "📱 Android ID: " + androidId);
            Log.d(TAG, "📏 Android ID length: " + androidId.length());

            // Đảm bảo có đủ 12 ký tự hex cho MAC (6 bytes = 12 hex chars)
            String hexString = androidId;
            if (hexString.length() < 12) {
                // Nếu thiếu, lặp lại Android ID để đủ 12 ký tự
                while (hexString.length() < 12) {
                    hexString += androidId;
                }
            }

            // Lấy 12 ký tự đầu và format thành MAC
            String macHex = hexString.substring(0, 12).toUpperCase();

            // Chèn dấu ":" để tạo format MAC chuẩn
            StringBuilder fakeMac = new StringBuilder();
            for (int i = 0; i < macHex.length(); i += 2) {
                if (i > 0) {
                    fakeMac.append(":");
                }
                fakeMac.append(macHex.substring(i, i + 2));
            }

            String result = fakeMac.toString();
            Log.d(TAG, "✅ Generated MAC from Android ID: " + result);

            return result;

        } catch (Exception ex) {
            Log.e(TAG, "❌ Exception while generating MAC from Android ID", ex);
            return "00:00:00:00:00:00";
        }
    }


    private void calculateRound(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "=== CALCULATING ROUND ===");
        Log.d(TAG, "Round number: " + round.getRoundNumber());
        Log.d(TAG, "User role: " + userRole);
        Log.d(TAG, "Round ID: " + round.getRoundId());

        if (!"LECTURER".equals(userRole)) {
            Log.d(TAG, "Skipping calculate for non-lecturer role: " + userRole);
            return;
        }

        Log.d(TAG, "Calculating attendance for round " + round.getRoundNumber());

        // Lấy roundId từ round object (cần thêm field này vào AttendanceModels.AttendanceRound)
        String roundId = round.getRoundId();

        calculateHandler.calculateRoundAttendance(sessionId, roundId,
                new AttendanceCallbacks.CalculateAttendanceCallback() {
                    @Override
                    public void onCalculateSuccess(String roundId, int attendedCount, String message) {
                        Log.d(TAG, "✅ CALCULATION SUCCESS");
                        Log.d(TAG, "  Round ID: " + roundId);
                        Log.d(TAG, "  Attended count: " + attendedCount);
                        Log.d(TAG, "  Message: " + message);
                        Log.d(TAG, "  Round number: " + round.getRoundNumber());

                        sendAttendanceCalculatedBroadcast();
                    }

                    @Override
                    public void onCalculateFailure(String roundId, String error) {
                        Log.e(TAG, "❌ CALCULATION FAILED");
                        Log.e(TAG, "  Round ID: " + roundId);
                        Log.e(TAG, "  Round number: " + round.getRoundNumber());
                        Log.e(TAG, "  Error: " + error);
                    }
                });
    }

    /**
     * 🔧 SIMPLE broadcast - chỉ notify có update
     */
    private void sendAttendanceCalculatedBroadcast() {
        Intent broadcastIntent = new Intent(ACTION_ATTENDANCE_CALCULATED);
        broadcastIntent.putExtra(EXTRA_SESSION_ID, sessionId);

        androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this)
                .sendBroadcast(broadcastIntent);

        Log.d(TAG, "📢 Broadcasted attendance calculated for session: " + sessionId);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void startBLEOperations() {
        Log.d(TAG, "=== STARTING BLE OPERATIONS ===");

        // Start advertising
        Log.d(TAG, "Starting BLE advertising...");
        bleManager.startAdvertising(currentAdvertiseData, new AttendanceCallbacks.BLEOperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ BLE Advertising started successfully in service");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "❌ BLE Advertising failed in service: " + error);
            }
        });


        // Start scanning
        Log.d(TAG, "Starting BLE scanning for room: " + room);
        bleManager.startScanning(room, new AttendanceCallbacks.DeviceDetectionCallback() {
            @Override
            public void onDeviceDetected(AttendanceModels.ScannedDevice device) {
                handleDeviceDetected(device);
            }

            @Override
            public void onDeviceLost(String deviceId) {
                handleDeviceLost(deviceId);
            }
        });

        Log.d(TAG, "BLE operations started");
        Log.d(TAG, "===============================");
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private void executeRound(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "=== EXECUTING ROUND ===");
        Log.d(TAG, "Round number: " + round.getRoundNumber());
        Log.d(TAG, "Round ID: " + round.getRoundId());
        Log.d(TAG, "Is last round: " + round.isLastRound());
        Log.d(TAG, "Execution time: " + round.getExecutionTime());

        // Collect current detected devices
        List<AttendanceModels.ScannedDevice> currentDevices = new ArrayList<>(detectedDevices.values());
        Log.d(TAG, "Current detected devices: " + currentDevices.size());

        for (int i = 0; i < currentDevices.size(); i++) {
            AttendanceModels.ScannedDevice device = currentDevices.get(i);
            Log.d(TAG, "  Device " + (i + 1) + ": " + device.getMacAddress() + " (RSSI: " + device.getRssi() + " dBm)");
        }
        @SuppressLint("HardwareIds")
        String deviceMac = getDeviceMacFromAndroidId();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                .format(new Date());

        Log.d(TAG, "Submitter MAC: " + deviceMac);
        Log.d(TAG, "Session ID: " + sessionId);
        Log.d(TAG, "Timestamp: " + timestamp);

        AttendanceModels.AttendanceSubmission submission = new AttendanceModels.AttendanceSubmission(
                deviceMac,
                sessionId,
                currentDevices,
                timestamp
        );

        Log.d(TAG, "Created submission: " + submission.toString());
        Log.d(TAG, "Submitting attendance...");

        submissionHandler.submitAttendance(submission, new AttendanceCallbacks.AttendanceSubmissionCallback() {
            @Override
            public void onSubmissionSuccess(AttendanceModels.AttendanceSubmission submission) {
                Log.d(TAG, "✅ SUBMISSION SUCCESS");
                Log.d(TAG, "  Session ID: " + submission.getSessionId());
                Log.d(TAG, "  Submitted devices: " + submission.getScannedDevices().size());
                Log.d(TAG, "  Submitter MAC: " + submission.getSubmitterDeviceMacAddress());
                Log.d(TAG, "  Timestamp: " + submission.getTimestamp());

                // Clear detected devices after successful submission
                int previousSize = detectedDevices.size();
                detectedDevices.clear();
                Log.d(TAG, "Cleared " + previousSize + " detected devices from cache");
            }

            @Override
            public void onSubmissionFailure(int roundNumber, String error) {
                Log.e(TAG, "❌ SUBMISSION FAILED");
                Log.e(TAG, "  Round number: " + roundNumber);
                Log.e(TAG, "  Error: " + error);
                Log.e(TAG, "  Detected devices count: " + detectedDevices.size());
            }
        });


        // If last round, stop advertising
        if (round.isLastRound()) {
            Log.d(TAG, "🏁 Last round completed, stopping advertising...");
            bleManager.stopAdvertising();
            Log.d(TAG, "Advertising stopped after last round");
        }

        Log.d(TAG, "====================");
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void onAllRoundsComplete() {
        Log.d(TAG, "=== ALL ROUNDS COMPLETED ===");
        Log.d(TAG, "Session ID: " + sessionId);
        Log.d(TAG, "Final detected devices: " + detectedDevices.size());
        Log.d(TAG, "Stopping attendance service...");

        stopAttendanceService();

        Log.d(TAG, "============================");
    }

    private void handleDeviceDetected(AttendanceModels.ScannedDevice device) {
        Log.d(TAG, "📱 DEVICE DETECTED");
        Log.d(TAG, "  MAC: " + device.getMacAddress());
        Log.d(TAG, "  RSSI: " + device.getRssi() + " dBm");

        boolean isNewDevice = !detectedDevices.containsKey(device.getMacAddress());
        detectedDevices.put(device.getMacAddress(), device);

        Log.d(TAG, "  Status: " + (isNewDevice ? "NEW" : "UPDATED"));
        Log.d(TAG, "  Total detected: " + detectedDevices.size());

        if (isNewDevice) {
            Log.d(TAG, "  🆕 First time detecting this device");
        } else {
            Log.d(TAG, "  🔄 Updated existing device");
        }
    }

    private void handleDeviceLost(String deviceId) {
        Log.d(TAG, "📱 DEVICE LOST");
        Log.d(TAG, "  Device ID: " + deviceId);

        boolean wasPresent = detectedDevices.containsKey(deviceId);
        detectedDevices.remove(deviceId);

        Log.d(TAG, "  Was present: " + wasPresent);
        Log.d(TAG, "  Remaining detected: " + detectedDevices.size());
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void stopAttendanceService() {
        Log.d(TAG, "=== STOPPING ATTENDANCE SERVICE ===");

        // Stop BLE operations
        Log.d(TAG, "Stopping BLE operations...");
        bleManager.stopAdvertising();
        bleManager.stopScanning();
        Log.d(TAG, "BLE operations stopped");

        // Stop scheduler
        if (roundScheduler != null) {
            Log.d(TAG, "Stopping round scheduler...");
            roundScheduler.stop();
            Log.d(TAG, "Round scheduler stopped");
        } else {
            Log.w(TAG, "Round scheduler was null");
        }

        // Stop foreground service
        Log.d(TAG, "Stopping foreground service...");
        stopForeground(true);
        stopSelf();
        Log.d(TAG, "Service stopped");

        Log.d(TAG, "✅ Attendance service stopped completely");
        Log.d(TAG, "===================================");
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Điểm danh BLE đang hoạt động")
                .setContentText("Đang phát tín hiệu cho phòng: " + room)
                .setSmallIcon(R.drawable.ic_bluetooth)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Đang điểm danh",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Đang trong quá trình điểm danh và theo dõi bằng BLE");

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}