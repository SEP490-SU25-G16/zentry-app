package vn.edu.fpt.zentryapp.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import vn.edu.fpt.zentryapp.MainActivity;
import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleClassSection;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleClassSection;

/**
 * BLE Attendance Service - Quản lý attendance tracking qua Bluetooth Low Energy
 *
 * Chức năng chính:
 * 1. Advertise device ID và room info liên tục
 * 2. Scan device theo lịch trình (1 giây mỗi round)
 * 3. Submit attendance data khi có lệnh từ AttendanceRoundScheduler
 * 4. Calculate attendance (chỉ cho lecturer)
 */
public class BLEAttendanceService extends Service {
    private static final String TAG = "BLEAttendanceService";
    private static final String CHANNEL_ID = "BLE_ATTENDANCE_CHANNEL";
    public static final String ACTION_ATTENDANCE_CALCULATED = "vn.edu.fpt.zentryapp.ATTENDANCE_CALCULATED";
    public static final String EXTRA_SESSION_ID = "sessionId";
    private static final int NOTIFICATION_ID = 1001;

    // ======= BLE CONSTANTS =======
    private static final int COMPANY_ID = 0x1234; // Manufacturer ID cho BLE advertising
    private static final int ROOM_BYTES_MAX = 10; // Tăng từ 4 lên 10 để hỗ trợ room name dài
    private static final long SCAN_DURATION_MS = 3000; // Scan trong 1 giây mỗi round

    // ======= BLE COMPONENTS =======
    private byte[] idBytes; // Device ID đã hash thành 6 bytes
    private AdvertiseSettings advertiseSettings;
    private AdvertiseCallback advCallback;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    // ======= STATE MANAGEMENT =======
    // Map lưu devices được phát hiện trong round hiện tại
    private final Map<String, AttendanceModels.ScannedDevice> detectedDevices = new ConcurrentHashMap<>();
    // Map theo dõi thời gian phát hiện device cuối cùng (không cần thiết nữa với scan theo lịch)
    private final Map<String, Long> deviceLastSeen = new ConcurrentHashMap<>();

    // ======= SESSION DATA =======
    private String sessionId; // ID phiên học
    private String roomName; // ID phòng học
    private String userId; // ID người dùng
    private String userRole; // STUDENT hoặc LECTURER
    private String deviceId; // Device ID formatted (XX:XX:XX:XX:XX:XX)

    // ======= CORE COMPONENTS =======
    private AttendanceRoundScheduler roundScheduler; // Quản lý lịch trình các round
    private AttendanceSubmissionHandler submissionHandler; // Xử lý submit attendance
    private AttendanceCalculateHandler calculateHandler; // Xử lý calculate attendance

    @SuppressLint("ForegroundServiceType")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "=== BLE SERVICE CREATING ===");

        // Tạo device ID duy nhất từ ANDROID_ID
        generateDeviceId();

        // Cấu hình BLE advertising settings
        setupAdvertiseSettings();

        // Cấu hình BLE scanner callback
        setupScanner();

        // Khởi tạo các handler xử lý attendance
        submissionHandler = new AttendanceSubmissionHandler(this);
        calculateHandler = new AttendanceCalculateHandler(this);

        // Tạo notification channel cho foreground service
        createNotificationChannel();
        Log.d(TAG, "Service created successfully with device ID: " + deviceId);
    }

    /**
     * Tạo device ID duy nhất từ ANDROID_ID và hash thành 6 bytes
     */
    private void generateDeviceId() {
        try {
            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            this.idBytes = generateIdBytes(androidId);
            // Format thành XX:XX:XX:XX:XX:XX để dễ đọc
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < idBytes.length; i++) {
                sb.append(String.format("%02X", idBytes[i]));
                if (i < idBytes.length - 1) sb.append(":");
            }
            this.deviceId = sb.toString();

            Log.d(TAG, "Generated device ID: " + deviceId);
        } catch (Exception e) {
            Log.e(TAG, "Error generating device ID", e);
            this.deviceId = "00:00:00:00:00:00"; // Fallback ID
        }
    }

    /**
     * Hash input string thành 6 bytes để làm device ID
     */
    private byte[] generateIdBytes(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(hash, 6); // Lấy 6 bytes đầu
        } catch (Exception e) {
            // Fallback nếu SHA-256 không available
            byte[] raw = input.getBytes(StandardCharsets.UTF_8);
            return Arrays.copyOf(raw, Math.min(6, raw.length));
        }
    }

    /**
     * Cấu hình BLE advertising settings cho performance tối ưu
     */
    private void setupAdvertiseSettings() {
        advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) // Advertise nhanh
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) // Công suất cao
                .setConnectable(false) // Không cần kết nối
                .build();

        // Callback để theo dõi trạng thái advertising
        advCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "🟢 Advertising started successfully");
            }
            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "🔴 Advertising failed: " + getAdvertiseErrorMessage(errorCode));
            }
        };
    }

    /**
     * Cấu hình BLE scanner callback để xử lý kết quả scan
     */
    private void setupScanner() {
        scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                processScanResult(result);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "🔴 Scan failed: " + getScanErrorMessage(errorCode));
            }
        };
    }

    /**
     * Xử lý kết quả scan từ một device
     */
    private void processScanResult(ScanResult result) {

        ScanRecord rec = result.getScanRecord();
        if (rec == null) {
            return;
        }

        // Tìm Manufacturer Data
        byte[] payload = rec.getManufacturerSpecificData().get(COMPANY_ID);
        if (payload == null) {
            return;
        }

        if (payload.length < idBytes.length) {
            Log.d(TAG, "  Payload too short: " + payload.length + " < " + idBytes.length);
            return;
        }

        // Parse data
        byte[] deviceBytes = Arrays.copyOfRange(payload, 0, idBytes.length);
        byte[] roomBytes = Arrays.copyOfRange(payload, idBytes.length, payload.length);

        String advertisedRoom;
        if (roomBytes.length >= 4) {
            byte[] first4Bytes = Arrays.copyOf(roomBytes, 4);
            advertisedRoom = new String(first4Bytes, StandardCharsets.UTF_8);
        } else {
            advertisedRoom = new String(roomBytes, StandardCharsets.UTF_8);
        }

        // ✅ FIXED: Lấy 4 ký tự đầu của my room để compare
        String myRoomPrefix = roomName.length() >= 4 ? roomName.substring(0, 4) : roomName;

     //   Log.d(TAG, "  Advertised room (4 chars): '" + advertisedRoom + "'");
      //  Log.d(TAG, "  My room (4 chars): '" + myRoomPrefix + "'");

        // ✅ Compare 4 ký tự đầu của cả 2 bên
        if (!advertisedRoom.equals(myRoomPrefix)) {
        //    Log.d(TAG, "  Room mismatch, ignoring");
            return;
        }

        // Format device ID
        StringBuilder sb = new StringBuilder();
        for (byte b : deviceBytes) sb.append(String.format("%02X:", b));
        String scannedDeviceId = sb.substring(0, sb.length() - 1);

        // ✅ FIXED: Sử dụng đúng variable name
        long now = System.currentTimeMillis();
        deviceLastSeen.put(scannedDeviceId, now);

        Log.d(TAG, "  Device ID: " + scannedDeviceId);
        Log.d(TAG, "  RSSI: " + result.getRssi() + " dBm");
        Log.d(TAG, "✅ Device accepted!");

        // ✅ FIXED: Store detected device với đúng variable name
        AttendanceModels.ScannedDevice device =
                new AttendanceModels.ScannedDevice(scannedDeviceId, result.getRssi());
        detectedDevices.put(scannedDeviceId, device);
    }

    @SuppressLint("ForegroundServiceType")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "=== SERVICE START COMMAND ===");

        // Validate intent
        if (intent == null || !"START_ATTENDANCE".equals(intent.getAction())) {
            Log.w(TAG, "Invalid intent, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Extract session data từ intent
        extractSessionData(intent);

        // Extract rounds data (đã được convert sang UTC+7 ở mapping layer)
        @SuppressWarnings("unchecked")
        List<AttendanceModels.AttendanceRound> rounds =
                (List<AttendanceModels.AttendanceRound>) intent.getSerializableExtra("rounds");

        Log.d(TAG, "Session ID: " + sessionId);
        Log.d(TAG, "Room: " + roomName);
        Log.d(TAG, "User: " + userId + " (" + userRole + ")");
        Log.d(TAG, "Rounds: " + (rounds != null ? rounds.size() : 0));

        // Khởi động attendance service
        startAttendanceService(rounds);

        return START_STICKY; // Service sẽ được restart nếu bị kill
    }

    /**
     * Extract session data từ intent dựa trên user role
     */
    private void extractSessionData(Intent intent) {
        userId = intent.getStringExtra("userId");
        userRole = intent.getStringExtra("userRole");

        if ("STUDENT".equals(userRole)) {
            StudentScheduleClassSection session = (StudentScheduleClassSection) intent.getSerializableExtra("session");
            if (session != null) {
                sessionId = session.getSessionId();
                roomName = session.getRoom();
            }
        } else {
            LecturerScheduleClassSection session = (LecturerScheduleClassSection) intent.getSerializableExtra("session");
            if (session != null) {
                sessionId = session.getSessionId();
                roomName = session.getRoomName();
            }
        }
    }

    /**
     * Khởi động attendance service với rounds data
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void startAttendanceService(List<AttendanceModels.AttendanceRound> rounds) {
        Log.d(TAG, "=== STARTING ATTENDANCE SERVICE ===");

        // Start làm foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        // Khởi động BLE operations (chỉ advertising, chưa scan)
        startBLEOperations();

        // Khởi tạo và start round scheduler
        if (rounds != null && !rounds.isEmpty()) {
            roundScheduler = new AttendanceRoundScheduler(
                    rounds,
                    this::executeRound,      // Callback khi đến giờ execute round
                    this::calculateRound,    // Callback khi đến giờ calculate round
                    this::onAllRoundsComplete // Callback khi tất cả rounds hoàn thành
            );
            roundScheduler.start();
            Log.d(TAG, "Round scheduler started with " + rounds.size() + " rounds");
        } else {
            Log.w(TAG, "No rounds provided, running in continuous mode");
        }

        Log.d(TAG, "✅ Attendance service started successfully");
    }

    /**
     * Khởi động BLE operations - CHỈ ADVERTISING, KHÔNG SCAN LIÊN TỤC
     * Scan sẽ được thực hiện theo lịch trình khi executeRound() được gọi
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void startBLEOperations() {
        Log.d(TAG, "=== STARTING BLE OPERATIONS ===");

        // Chỉ start advertising, KHÔNG start scanning liên tục
        startAdvertising();

        Log.d(TAG, "BLE advertising started, scanning will be scheduled by rounds");
    }

    /**
     * Bắt đầu BLE advertising để broadcast device ID và room info
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private void startAdvertising() {
        // Chuẩn bị room data, truncate nếu quá dài
        byte[] roomBytes = roomName.getBytes(StandardCharsets.UTF_8);
        if (roomBytes.length > ROOM_BYTES_MAX) {
            roomBytes = Arrays.copyOf(roomBytes, ROOM_BYTES_MAX);
            Log.w(TAG, "Room truncated from '" + roomName + "' to '" +
                    new String(roomBytes, StandardCharsets.UTF_8) + "'");
        }

        // Tạo payload = device ID (6 bytes) + room name (≤10 bytes)
        byte[] advertPayload = new byte[idBytes.length + roomBytes.length];
        System.arraycopy(idBytes, 0, advertPayload, 0, idBytes.length);
        System.arraycopy(roomBytes, 0, advertPayload, idBytes.length, roomBytes.length);

        Log.d(TAG, "Advertising payload size: " + advertPayload.length + " bytes");
        Log.d(TAG, "Room: '" + new String(roomBytes, StandardCharsets.UTF_8) + "'");

        // Stop advertising cũ nếu có
        if (advertiser != null) {
            advertiser.stopAdvertising(advCallback);
        }

        // Start advertising mới
        advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        if (advertiser != null) {
            AdvertiseData data = new AdvertiseData.Builder()
                    .addManufacturerData(COMPANY_ID, advertPayload)
                    .setIncludeDeviceName(false) // Không cần device name
                    .build();
            advertiser.startAdvertising(advertiseSettings, data, advCallback);
            Log.d(TAG, "🟢 Advertising started for room: " + roomName);
        } else {
            Log.e(TAG, "❌ BLE Advertiser not available");
        }
    }

    // ======= SCHEDULED SCANNING (CHỈ KHI CẦN THIẾT) =======

    /**
     * Bắt đầu scan có thời gian giới hạn (1 giây)
     * Được gọi bởi executeRound() khi đến thời gian attendance
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScheduledScan() {
        Log.d(TAG, "🔍 STARTING SCHEDULED SCAN (1 second)");

        if (scanner == null) {
            Log.e(TAG, "❌ Scanner not available");
            return;
        }

        // Clear kết quả scan cũ cho round mới
        detectedDevices.clear();
        deviceLastSeen.clear();

        // Cấu hình scan settings cho performance cao
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Scan nhanh
                .setReportDelay(0) // Báo kết quả ngay lập tức
                .build();

        // Bắt đầu scan
        scanner.startScan(null, scanSettings, scanCallback);
        Log.d(TAG, "✅ Scheduled scan started");

        // Tự động stop scan sau SCAN_DURATION_MS (1 giây)
        new Handler(Looper.getMainLooper()).postDelayed(this::stopScheduledScan, SCAN_DURATION_MS);
    }

    /**
     * Dừng scheduled scan và log kết quả
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void stopScheduledScan() {
        if (scanner != null) {
            scanner.stopScan(scanCallback);
            Log.d(TAG, "🛑 Scheduled scan stopped after " + SCAN_DURATION_MS + "ms");
            Log.d(TAG, "📊 Scan results: " + detectedDevices.size() + " devices found");
        }
    }

    /**
     * Broadcast scan result cho UI components (nếu cần)
     */
    private void broadcastScanResult(String deviceId, int rssi, String room) {
        Intent intent = new Intent("vn.edu.fpt.zentryapp.SCAN_RESULT");
        intent.putExtra("id", deviceId);
        intent.putExtra("rssi", rssi);
        intent.putExtra("room", room);
        sendBroadcast(intent);
    }

    // ======= ROUND EXECUTION (ĐƯỢC GỌI BỞI SCHEDULER) =======

    /**
     * Execute attendance round - được gọi bởi AttendanceRoundScheduler
     * Timeline: Round execution time + 10s = scan time
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void executeRound(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "=== EXECUTING ROUND " + round.getRoundNumber() + " ===");

        // Bắt đầu scan 1 giây để collect devices
        startScheduledScan();

        // Đợi scan hoàn thành (1s + 100ms buffer) rồi mới process results
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            performRoundExecution(round);
        }, SCAN_DURATION_MS + 100); // +100ms buffer để đảm bảo scan đã dừng
    }

    /**
     * Xử lý kết quả scan và submit attendance
     */
    private void performRoundExecution(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "📝 PROCESSING SCAN RESULTS FOR ROUND " + round.getRoundNumber());

        // Lấy top devices theo RSSI từ kết quả scan vừa rồi
        List<AttendanceModels.ScannedDevice> topDevices = getTopDevicesByRssi(detectedDevices.size());

        Log.d(TAG, "Round " + round.getRoundNumber() + ": " + topDevices.size() + " devices detected");
        for (AttendanceModels.ScannedDevice device : topDevices) {
            Log.d(TAG, "  " + device.getMacAddress() + " RSSI: " + device.getRssi());
        }

        String timestamp = createTimestamp();

        // Tạo attendance submission object
        AttendanceModels.AttendanceSubmission submission = new AttendanceModels.AttendanceSubmission(
                deviceId, sessionId, topDevices, timestamp);

        // Submit attendance qua API
        submissionHandler.submitAttendance(submission, new AttendanceCallbacks.AttendanceSubmissionCallback() {
            @Override
            public void onSubmissionSuccess(AttendanceModels.AttendanceSubmission submission) {
                Log.d(TAG, "✅ Round " + round.getRoundNumber() + " submitted successfully");
            }

            @Override
            public void onSubmissionFailure(int roundNumber, String error) {
                Log.e(TAG, "❌ Round " + roundNumber + " submission failed: " + error);
            }
        });
    }

    /**
     * Tạo UTC timestamp đúng format cho API
     */
    private String createTimestamp() {
        return createTimestamp(new Date());
    }

    private String createTimestamp(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = format.format(date);

        Log.d(TAG, "Formatted timestamp: " + timestamp);
        return timestamp;
    }

    /**
     * Calculate attendance round - chỉ lecturer mới thực hiện
     * Được gọi 30s sau khi scan round hoàn thành
     */
    private void calculateRound(AttendanceModels.AttendanceRound round) {
        // Chỉ lecturer mới calculate attendance
        if (!"LECTURER".equals(userRole)) return;

        Log.d(TAG, "🧮 CALCULATING ROUND " + round.getRoundNumber());

        // Gọi API calculate attendance cho round này
        calculateHandler.calculateRoundAttendance(sessionId, round.getRoundId(),
                new AttendanceCallbacks.CalculateAttendanceCallback() {
                    @Override
                    public void onCalculateSuccess(String roundId, int attendedCount, String message) {
                        Log.d(TAG, "✅ Round " + round.getRoundNumber() + " calculated: " +
                                attendedCount + " students attended");
                        // Broadcast để thông báo UI
                        sendAttendanceCalculatedBroadcast();
                    }

                    @Override
                    public void onCalculateFailure(String roundId, String error) {
                        Log.e(TAG, "❌ Round " + round.getRoundNumber() + " calculate failed: " + error);
                    }
                });
    }

    // ======= UTILITY METHODS =======

    /**
     * Lấy top N devices theo RSSI cao nhất
     */
    private List<AttendanceModels.ScannedDevice> getTopDevicesByRssi(int count) {
        List<AttendanceModels.ScannedDevice> allDevices = new ArrayList<>(detectedDevices.values());
        // Sort theo RSSI giảm dần (RSSI cao hơn = gần hơn)
        allDevices.sort((a, b) -> Integer.compare(b.getRssi(), a.getRssi()));

        // Lấy top N devices
        List<AttendanceModels.ScannedDevice> topDevices = new ArrayList<>();
        for (int i = 0; i < Math.min(allDevices.size(), count); i++) {
            topDevices.add(allDevices.get(i));
        }
        return topDevices;
    }

    /**
     * Broadcast thông báo attendance đã được calculate
     */
    private void sendAttendanceCalculatedBroadcast() {
        Intent broadcastIntent = new Intent(ACTION_ATTENDANCE_CALCULATED);
        broadcastIntent.putExtra(EXTRA_SESSION_ID, sessionId);

        // Send local broadcast
        androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this).sendBroadcast(broadcastIntent);

        Log.d(TAG, "📢 Sent attendance calculated broadcast for session: " + sessionId);
    }
    /**
     * Callback khi tất cả rounds hoàn thành
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void onAllRoundsComplete() {
        Log.d(TAG, "=== ALL ROUNDS COMPLETED ===");
        stopAttendanceService();
    }

    // ======= ERROR HELPERS =======

    /**
     * Convert BLE advertise error code thành human-readable message
     */
    private String getAdvertiseErrorMessage(int errorCode) {
        switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED: return "Already started";
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE: return "Data too large";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED: return "Feature unsupported";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR: return "Internal error";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS: return "Too many advertisers";
            default: return "Unknown error (" + errorCode + ")";
        }
    }

    /**
     * Convert BLE scan error code thành human-readable message
     */
    private String getScanErrorMessage(int errorCode) {
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED: return "Already started";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED: return "App registration failed";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED: return "Feature unsupported";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR: return "Internal error";
            default: return "Unknown error (" + errorCode + ")";
        }
    }

    // ======= SERVICE CLEANUP =======

    /**
     * Dừng attendance service và cleanup tất cả resources
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    private void stopAttendanceService() {
        Log.d(TAG, "=== STOPPING ATTENDANCE SERVICE ===");

        // Dừng BLE operations
        if (advertiser != null) {
            advertiser.stopAdvertising(advCallback);
            Log.d(TAG, "✅ Advertising stopped");
        }
        if (scanner != null) {
            scanner.stopScan(scanCallback);
            Log.d(TAG, "✅ Scanning stopped");
        }

        // Dừng scheduler
        if (roundScheduler != null) {
            roundScheduler.stop();
            Log.d(TAG, "✅ Round scheduler stopped");
        }

        // Clear data
        detectedDevices.clear();
        deviceLastSeen.clear();

        // Stop foreground service
        stopForeground(true);
        stopSelf();

        Log.d(TAG, "✅ Service stopped completely");
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public void onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYING ===");
        super.onDestroy();

        // Đảm bảo cleanup nếu service bị destroy đột ngột
        if (advertiser != null) advertiser.stopAdvertising(advCallback);
        if (scanner != null) scanner.stopScan(scanCallback);
        if (roundScheduler != null) roundScheduler.stop();
    }

    /**
     * Xử lý khi user swipe away app từ recent apps
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task removed, stopping service");
        stopForeground(true);
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    // ======= NOTIFICATION FOR FOREGROUND SERVICE =======

    /**
     * Tạo notification cho foreground service
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BLE Attendance Active")
                .setContentText("Room: " + roomName + " | Status: Running")
                .setSmallIcon(R.drawable.ic_bluetooth)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Không thể swipe away
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /**
     * Tạo notification channel (Android 8.0+)
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "BLE Attendance Service",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("BLE attendance tracking service");
        channel.setShowBadge(false);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Service không hỗ trợ binding
    }
}
