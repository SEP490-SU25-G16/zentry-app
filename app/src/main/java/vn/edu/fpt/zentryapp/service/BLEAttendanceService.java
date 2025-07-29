package vn.edu.fpt.zentryapp.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        createNotificationChannel();

        // Initialize components
        bleManager = new BLEAttendanceManager(this);
        submissionHandler = new AttendanceSubmissionHandler(this);
        calculateHandler = new AttendanceCalculateHandler(this);
        Log.d(TAG, "BLE Attendance Service created");
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (!"START_ATTENDANCE".equals(action)) {
            stopAttendanceService();
            return START_STICKY;
        }

        // 1) Lấy chung các extras
        String userId = intent.getStringExtra("userId");
        String userRole = intent.getStringExtra("userRole");
        // rounds thì vẫn getParcelableArrayListExtra / getSerializableExtra như trước
        @SuppressWarnings("unchecked")
        List<AttendanceModels.AttendanceRound> rounds =
                (List<AttendanceModels.AttendanceRound>) intent.getSerializableExtra("rounds");

        // 2) Tạo sessionId + room tuỳ theo role
        String sessionId;
        String room;
        if ("STUDENT".equals(userRole)) {
            // Sinh viên gửi lên object Schedule (vd: vn.edu.fpt.zentryapp.student.data.model.response.Schedule)
            StudentScheduleSession studentScheduleSession = (StudentScheduleSession) intent.getSerializableExtra("session");
            sessionId = studentScheduleSession.getSessionId();
            room = studentScheduleSession.getRoom(); // hoặc schedule.getRoomName()
        } else {
            // Giảng viên gửi lên object ScheduleSession
            LecturerScheduleSession session = (LecturerScheduleSession) intent.getSerializableExtra("session");
            sessionId = session.getSessionId();
            room = session.getRoom();
        }

        // 3) Khởi chạy service chung
        startAttendanceService(userId, sessionId, room, rounds);
        return START_STICKY;
    }


    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void startAttendanceService(
            String userId,
            String sessionId,
            String room,
            List<AttendanceModels.AttendanceRound> rounds) {
        this.sessionId = sessionId;
        this.room = room;
        this.userId = userId;
        @SuppressLint("HardwareIds") String deviceMac = BluetoothAdapter.getDefaultAdapter().getAddress();
        currentAdvertiseData = new AttendanceModels.BLEAdvertiseData(
                deviceMac,  // 🔧 THÊM MAC address
                room        // roomName
        );

        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        // Start BLE operations
        startBLEOperations();

        // Schedule rounds
        roundScheduler = new AttendanceRoundScheduler(
                rounds,
                this::executeRound,           // execution callback
                this::calculateRound,         // 🔧 THÊM calculate callback
                this::onAllRoundsComplete     // completion callback
        );
        roundScheduler.start();

        Log.d(TAG, "Attendance service started for session: " + sessionId);
    }

    private void calculateRound(AttendanceModels.AttendanceRound round) {
        // 🔧 CHỈ LECTURER mới calculate
        if (!"LECTURER".equals(userRole)) {
            Log.d(TAG, "Skipping calculate for role: " + userRole);
            return;
        }

        Log.d(TAG, "Calculating attendance for round " + round.getRoundNumber());

        // Lấy roundId từ round object (cần thêm field này vào AttendanceModels.AttendanceRound)
        String roundId = round.getRoundId(); // 🔧 CẦN THÊM field này

        calculateHandler.calculateRoundAttendance(sessionId, roundId,
                new AttendanceCallbacks.CalculateAttendanceCallback() {
                    @Override
                    public void onCalculateSuccess(String roundId, int attendedCount, String message) {
                        Log.d(TAG, "✅ Round " + round.getRoundNumber() + " calculated: " +
                                attendedCount + " students attended");

                        // 🔧 SIMPLE broadcast - chỉ thông báo có update
                        sendAttendanceCalculatedBroadcast();
                    }

                    @Override
                    public void onCalculateFailure(String roundId, String error) {
                        Log.e(TAG, "❌ Failed to calculate round " + round.getRoundNumber() + ": " + error);
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
        // Start advertising
        bleManager.startAdvertising(currentAdvertiseData, new AttendanceCallbacks.BLEOperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "BLE Service: Advertising initialized successfully");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "BLE Advertising failed: " + error);
            }
        });

        // Start scanning
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
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private void executeRound(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "Executing round " + round.getRoundNumber());

        // Collect current detected devices
        List<AttendanceModels.ScannedDevice> currentDevices = new ArrayList<>(detectedDevices.values());

        // TODO: Get data MAC and userId here to submit
        @SuppressLint("HardwareIds")
        String deviceMac = BluetoothAdapter.getDefaultAdapter().getAddress();

        // 🔧 SỬA: Format Date thành String timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                .format(new Date());
        // Tạo submission - bỏ userId vì API không cần
        AttendanceModels.AttendanceSubmission submission = new AttendanceModels.AttendanceSubmission(
                deviceMac,      // submitterDeviceMacAddress
                sessionId,      // sessionId
                currentDevices, // scannedDevices
                timestamp            // timestamp
        );

        // 🔧 API call đã được chuyển vào AttendanceSubmissionHandler
        submissionHandler.submitAttendance(submission, new AttendanceCallbacks.AttendanceSubmissionCallback() {
            @Override
            public void onSubmissionSuccess(AttendanceModels.AttendanceSubmission submission) {
                Log.d(TAG, "✅ Attendance submitted successfully for session: " + submission.getSessionId());
                Log.d(TAG, "Submitted " + submission.getScannedDevices().size() + " scanned devices");

                // Clear detected devices after successful submission
                detectedDevices.clear();
                Log.d(TAG, "Cleared detected devices cache");
            }

            @Override
            public void onSubmissionFailure(int roundNumber, String error) {
                Log.e(TAG, "❌ Round " + roundNumber + " submission failed: " + error);
                // Optional: Có thể thêm retry logic hoặc error handling
                // Ví dụ: lưu submission để retry sau
                // saveFailedSubmissionForRetry(submission);

                // Hoặc thông báo cho user
                // showErrorNotification("Attendance submission failed: " + error);
            }
        });


        // If last round, stop advertising
        if (round.isLastRound()) {
            bleManager.stopAdvertising();
            Log.d(TAG, "Stopped advertising after last round");
        }
    }


    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void onAllRoundsComplete() {
        Log.d(TAG, "All rounds completed, stopping service");
        stopAttendanceService();
    }

    private void handleDeviceDetected(AttendanceModels.ScannedDevice device) {
        detectedDevices.put(device.getMacAddress(), device); // Cập nhật method name
        Log.d(TAG, "Device detected: " + device.getMacAddress() + " with RSSI: " + device.getRssi() +
                ", Total: " + detectedDevices.size());
    }

    private void handleDeviceLost(String deviceId) {
        detectedDevices.remove(deviceId);
        Log.d(TAG, "Device lost: " + deviceId + ", Total: " + detectedDevices.size());
    }

    private void cleanupOldDevices() {
        detectedDevices.clear();
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void stopAttendanceService() {
        // Stop BLE operations
        bleManager.stopAdvertising();
        bleManager.stopScanning();

        // Stop scheduler
        if (roundScheduler != null) {
            roundScheduler.stop();
        }

        // Stop foreground service
        stopForeground(true);
        stopSelf();

        Log.d(TAG, "Attendance service stopped");
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