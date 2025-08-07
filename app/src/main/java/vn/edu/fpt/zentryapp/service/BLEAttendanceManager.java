package vn.edu.fpt.zentryapp.service;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BLEAttendanceManager {
    private static final String TAG = "BLEAttendanceManager";

    // 🔧 Manufacturer Data constants
    private static final int COMPANY_ID = 0x1234;
    private static final int ROOM_BYTES_MAX = 4;

    // 🔧 Cleanup mechanism
    private static final long STALE_TIMEOUT_MS = 3000;
    private static final long CLEANUP_INTERVAL_MS = 2000;

    private Handler scanHandler = new Handler(Looper.getMainLooper());
    private final Handler cleanupHandler = new Handler(Looper.getMainLooper());

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeAdvertiser advertiser;
    private final BluetoothLeScanner scanner;
    private AdvertiseCallback advertiseCallback;
    private ScanCallback scanCallback;
    private Context context;

    // 🔧 Device tracking và state management
    private final Map<String, AttendanceModels.ScannedDevice> detectedDevices = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
    private boolean isScanning = false;
    private boolean isAdvertising = false;
    private String currentTargetRoom;

    // 🔧 Device ID từ Android ID
    private byte[] idBytes;
    private String deviceId;

    // 🔧 Operation results
    private boolean lastAdvertiseSuccess = false;
    private String lastAdvertiseError = "";
    private boolean lastScanSuccess = false;
    private String lastScanError = "";

    public BLEAttendanceManager(Context context) {
        this.context = context;
        Log.d(TAG, "=== INITIALIZING BLE ATTENDANCE MANAGER ===");

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        Log.d(TAG, "BluetoothAdapter: " + (bluetoothAdapter != null ? "Available" : "NULL"));

        this.advertiser = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeAdvertiser() : null;
        this.scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        Log.d(TAG, "BLE Advertiser: " + (advertiser != null ? "Available" : "NULL"));
        Log.d(TAG, "BLE Scanner: " + (scanner != null ? "Available" : "NULL"));
        Log.d(TAG, "Company ID: 0x" + Integer.toHexString(COMPANY_ID));

        // Generate device ID
        generateDeviceId();

        Log.d(TAG, "============================================");
    }

    // 🔧 Generate device ID như BleForegroundService
    private void generateDeviceId() {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.idBytes = generateIdBytes(androidId);

        // Format device ID XX:XX:XX:XX:XX:XX
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < idBytes.length; i++) {
            sb.append(String.format("%02X", idBytes[i]));
            if (i < idBytes.length - 1) sb.append(":");
        }
        this.deviceId = sb.toString();

        Log.d(TAG, "Generated device ID: " + deviceId);
    }

    private byte[] generateIdBytes(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(hash, 6);
        } catch (Exception e) {
            byte[] raw = input.getBytes(StandardCharsets.UTF_8);
            return Arrays.copyOf(raw, 6);
        }
    }

    // ======= PUBLIC METHODS - TƯỜNG MINH =======

    /**
     * Bắt đầu advertising cho room cụ thể
     * @param roomName Tên room để advertise
     * @return true nếu bắt đầu thành công, false nếu thất bại
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    public boolean startAdvertising(String roomName) {
        Log.d(TAG, "=== STARTING BLE ADVERTISING ===");
        Log.d(TAG, "Room name: " + roomName);
        Log.d(TAG, "Device ID: " + deviceId);

        if (advertiser == null) {
            Log.e(TAG, "❌ BLE Advertiser not available");
            lastAdvertiseSuccess = false;
            lastAdvertiseError = "BLE Advertiser not available";
            return false;
        }

        if (isAdvertising) {
            Log.w(TAG, "⚠️ Already advertising, stopping previous...");
            stopAdvertising();
        }

        this.advertiseCallback = createAdvertiseCallback();
        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData advertiseData = buildAdvertiseData(roomName);

        Log.d(TAG, "Starting advertiser with settings...");
        try {
            advertiser.startAdvertising(settings, advertiseData, advertiseCallback);
            Log.d(TAG, "✅ Advertiser start command sent");
            Log.d(TAG, "=================================");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception starting advertiser: " + e.getMessage());
            lastAdvertiseSuccess = false;
            lastAdvertiseError = "Exception: " + e.getMessage();
            return false;
        }
    }

    /**
     * Bắt đầu scanning cho room cụ thể
     * @param targetRoom Room cần tìm
     * @return true nếu bắt đầu thành công, false nếu thất bại
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public boolean startScanning(String targetRoom) {
        Log.d(TAG, "=== STARTING BLE SCANNING ===");
        Log.d(TAG, "Target room: " + targetRoom);

        if (scanner == null) {
            Log.e(TAG, "❌ BLE Scanner not available");
            lastScanSuccess = false;
            lastScanError = "BLE Scanner not available";
            return false;
        }

        if (isScanning) {
            Log.w(TAG, "⚠️ Already scanning, stopping previous...");
            stopScanning();
        }

        // Clear previous state
        scanHandler.removeCallbacksAndMessages(null);
        cleanupHandler.removeCallbacks(cleanupTask);
        detectedDevices.clear();
        lastSeen.clear();

        this.currentTargetRoom = targetRoom;
        this.scanCallback = createScanCallback();
        ScanSettings settings = buildScanSettings();

        Log.d(TAG, "Starting scanner...");
        try {
            scanner.startScan(null, settings, scanCallback);
            isScanning = true;

            // Start cleanup task
            cleanupHandler.postDelayed(cleanupTask, CLEANUP_INTERVAL_MS);

            // Auto stop sau 60 giây
            scanHandler.postDelayed(() -> {
                Log.d(TAG, "⏰ Auto-stopping scan after 60 seconds");
                stopScanning();
            }, 60000);

            Log.d(TAG, "✅ Scanner started with cleanup mechanism");
            Log.d(TAG, "=============================");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception starting scanner: " + e.getMessage());
            lastScanSuccess = false;
            lastScanError = "Exception: " + e.getMessage();
            isScanning = false;
            return false;
        }
    }

    /**
     * Dừng advertising
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    public void stopAdvertising() {
        Log.d(TAG, "=== STOPPING BLE ADVERTISING ===");
        if (advertiser != null && advertiseCallback != null && isAdvertising) {
            Log.d(TAG, "Stopping advertiser...");
            advertiser.stopAdvertising(advertiseCallback);
            Log.d(TAG, "Advertiser stopped");
        }
        isAdvertising = false;
        Log.d(TAG, "=================================");
    }

    /**
     * Dừng scanning
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void stopScanning() {
        Log.d(TAG, "=== STOPPING BLE SCANNING ===");

        // Remove callbacks
        scanHandler.removeCallbacksAndMessages(null);
        cleanupHandler.removeCallbacks(cleanupTask);

        if (scanner != null && scanCallback != null && isScanning) {
            Log.d(TAG, "Stopping scanner...");
            scanner.stopScan(scanCallback);
            Log.d(TAG, "Scanner stopped");
        }

        isScanning = false;
        lastSeen.clear();
        Log.d(TAG, "==============================");
    }

    /**
     * Lấy danh sách devices đã được detect
     * @return List of ScannedDevice
     */
    public List<AttendanceModels.ScannedDevice> getDetectedDevices() {
        return new ArrayList<>(detectedDevices.values());
    }

    /**
     * Lấy top N devices theo RSSI mạnh nhất
     * @param count Số lượng devices cần lấy
     * @return List of top devices
     */
    public List<AttendanceModels.ScannedDevice> getTopDevicesByRssi(int count) {
        List<AttendanceModels.ScannedDevice> allDevices = new ArrayList<>(detectedDevices.values());

        // Sort by RSSI (strongest first)
        allDevices.sort((a, b) -> Integer.compare(b.getRssi(), a.getRssi()));

        // Return top N devices
        List<AttendanceModels.ScannedDevice> topDevices = new ArrayList<>();
        for (int i = 0; i < Math.min(allDevices.size(), count); i++) {
            topDevices.add(allDevices.get(i));
        }

        Log.d(TAG, "Returning top " + topDevices.size() + " devices from " + allDevices.size() + " total");
        return topDevices;
    }

    /**
     * Làm sạch devices cũ
     */
    public void cleanupStaleDevices() {
        long now = System.currentTimeMillis();
        boolean removed = false;
        Iterator<Map.Entry<String, Long>> it = lastSeen.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > STALE_TIMEOUT_MS) {
                String deviceId = entry.getKey();
                it.remove();
                detectedDevices.remove(deviceId);
                removed = true;

                Log.d(TAG, "🗑️ Removed stale device: " + deviceId);
            }
        }

        if (removed) {
            Log.d(TAG, "Cleanup completed. Active devices: " + detectedDevices.size());
        }
    }

    // ======= STATUS METHODS =======

    /**
     * Kiểm tra trạng thái advertising
     */
    public boolean isAdvertising() {
        return isAdvertising;
    }

    /**
     * Kiểm tra trạng thái scanning
     */
    public boolean isScanning() {
        return isScanning;
    }

    /**
     * Lấy số lượng devices đã detect
     */
    public int getDetectedDeviceCount() {
        return detectedDevices.size();
    }

    /**
     * Lấy device ID của thiết bị này
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Lấy target room hiện tại
     */
    public String getCurrentTargetRoom() {
        return currentTargetRoom;
    }

    /**
     * Kiểm tra kết quả advertise operation cuối cùng
     */
    public boolean wasLastAdvertiseSuccessful() {
        return lastAdvertiseSuccess;
    }

    /**
     * Lấy error message của advertise operation cuối cùng
     */
    public String getLastAdvertiseError() {
        return lastAdvertiseError;
    }

    /**
     * Kiểm tra kết quả scan operation cuối cùng
     */
    public boolean wasLastScanSuccessful() {
        return lastScanSuccess;
    }

    /**
     * Lấy error message của scan operation cuối cùng
     */
    public String getLastScanError() {
        return lastScanError;
    }

    // ======= PRIVATE IMPLEMENTATION =======

    private AdvertiseSettings buildAdvertiseSettings() {
        Log.d(TAG, "Building advertise settings:");
        Log.d(TAG, "  Mode: LOW_LATENCY");
        Log.d(TAG, "  TX Power: HIGH");
        Log.d(TAG, "  Connectable: false");

        return new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();
    }

    private ScanSettings buildScanSettings() {
        Log.d(TAG, "Building scan settings:");
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();
    }

    private AdvertiseData buildAdvertiseData(String roomName) {
        Log.d(TAG, "=== BUILDING ADVERTISE DATA ===");
        Log.d(TAG, "Room name: " + roomName);

        // Truncate room name
        byte[] roomBytes = roomName.getBytes(StandardCharsets.UTF_8);
        if (roomBytes.length > ROOM_BYTES_MAX) {
            roomBytes = Arrays.copyOf(roomBytes, ROOM_BYTES_MAX);
            String truncatedRoom = new String(roomBytes, StandardCharsets.UTF_8);
            Log.d(TAG, "Room truncated from '" + roomName + "' to '" + truncatedRoom + "'");
        }

        // Build payload = idBytes + roomBytes
        byte[] advertPayload = new byte[idBytes.length + roomBytes.length];
        System.arraycopy(idBytes, 0, advertPayload, 0, idBytes.length);
        System.arraycopy(roomBytes, 0, advertPayload, idBytes.length, roomBytes.length);

        Log.d(TAG, "ID bytes length: " + idBytes.length);
        Log.d(TAG, "Room bytes length: " + roomBytes.length);
        Log.d(TAG, "Total payload length: " + advertPayload.length + " bytes");

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addManufacturerData(COMPANY_ID, advertPayload)
                .setIncludeDeviceName(false)
                .build();

        Log.d(TAG, "Advertise data built successfully");
        Log.d(TAG, "===============================");

        return advertiseData;
    }

    private ScanCallback createScanCallback() {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
               // Log.d(TAG, "🔍 DEVICE FOUND: " + result.getDevice().getAddress() + " RSSI: " + result.getRssi());

                ScanRecord rec = result.getScanRecord();
                if (rec == null) {
                    // Log.d(TAG, "  No scan record");
                    return;
                }

                // Tìm Manufacturer Data
                byte[] payload = rec.getManufacturerSpecificData().get(COMPANY_ID);
                if (payload == null) {
                //    Log.d(TAG, "  No manufacturer data for company ID 0x" + Integer.toHexString(COMPANY_ID));
                    return;
                }

                if (payload.length < idBytes.length) {
                    Log.d(TAG, "  Payload too short: " + payload.length + " < " + idBytes.length);
                    return;
                }

                // Parse data
                byte[] deviceBytes = Arrays.copyOfRange(payload, 0, idBytes.length);
                byte[] roomBytes = Arrays.copyOfRange(payload, idBytes.length, payload.length);

                // ✅ FIXED: Chỉ lấy 4 ký tự đầu của advertised room
                String advertisedRoom;
                if (roomBytes.length >= 4) {
                    byte[] first4Bytes = Arrays.copyOf(roomBytes, 4);
                    advertisedRoom = new String(first4Bytes, StandardCharsets.UTF_8);
                } else {
                    advertisedRoom = new String(roomBytes, StandardCharsets.UTF_8);
                }

                // ✅ FIXED: Lấy 4 ký tự đầu của my room để compare
                String myRoomPrefix = currentTargetRoom.length() >= 4 ? currentTargetRoom.substring(0, 4) : currentTargetRoom;

                Log.d(TAG, "  Advertised room (4 chars): '" + advertisedRoom + "'");
                Log.d(TAG, "  My room (4 chars): '" + myRoomPrefix + "'");

                // ✅ Compare 4 ký tự đầu của cả 2 bên
                if (!advertisedRoom.equals(myRoomPrefix)) {
                    Log.d(TAG, "  Room mismatch, ignoring");
                    return;
                }

                // Format device ID
                StringBuilder sb = new StringBuilder();
                for (byte b : deviceBytes) sb.append(String.format("%02X:", b));
                String scannedDeviceId = sb.substring(0, sb.length() - 1);

                // ✅ FIXED: Sử dụng đúng variable name
                long now = System.currentTimeMillis();
                lastSeen.put(scannedDeviceId, now);

                Log.d(TAG, "  Device ID: " + scannedDeviceId);
                Log.d(TAG, "  RSSI: " + result.getRssi() + " dBm");
                Log.d(TAG, "✅ Device accepted!");

                // ✅ FIXED: Store detected device với đúng variable name
                AttendanceModels.ScannedDevice device =
                        new AttendanceModels.ScannedDevice(scannedDeviceId, result.getRssi());
                detectedDevices.put(scannedDeviceId, device);
            }


            @Override
            public void onScanFailed(int errorCode) {
                String errorMsg = getScanErrorMessage(errorCode);
                Log.e(TAG, "🔴 Scan failed: " + errorCode + " - " + errorMsg);
                lastScanSuccess = false;
                lastScanError = "Scan failed: " + errorMsg;
                isScanning = false;
            }
        };
    }

    private AdvertiseCallback createAdvertiseCallback() {
        return new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "🟢 BLE Advertising started successfully");
                Log.d(TAG, "Settings in effect: " + settingsInEffect.toString());
                lastAdvertiseSuccess = true;
                lastAdvertiseError = "";
                isAdvertising = true;
            }

            @Override
            public void onStartFailure(int errorCode) {
                String errorMsg = getAdvertiseErrorMessage(errorCode);
                Log.e(TAG, "🔴 BLE Advertising failed: " + errorCode + " - " + errorMsg);
                lastAdvertiseSuccess = false;
                lastAdvertiseError = "Advertising failed: " + errorMsg;
                isAdvertising = false;
            }
        };
    }

    // Cleanup task
    private final Runnable cleanupTask = () -> {
        if (!isScanning) return;

        long now = System.currentTimeMillis();
        boolean removed = false;
        Iterator<Map.Entry<String, Long>> it = lastSeen.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > STALE_TIMEOUT_MS) {
                String deviceId = entry.getKey();
                it.remove();
                detectedDevices.remove(deviceId);
                removed = true;

                Log.d(TAG, "🗑️ Device timeout: " + deviceId);
            }
        }

        if (removed) {
            Log.d(TAG, "Cleaned up stale devices. Active: " + detectedDevices.size());
        }

        // Schedule next cleanup
        if (isScanning) {
            cleanupHandler.postDelayed(this.cleanupTask, CLEANUP_INTERVAL_MS);
        }
    };

    private String getAdvertiseErrorMessage(int errorCode) {
        switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                return "Already started";
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "Data too large";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "Feature unsupported";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                return "Internal error";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "Too many advertisers";
            default:
                return "Unknown error";
        }
    }

    private String getScanErrorMessage(int errorCode) {
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "Already started";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "App registration failed";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "Feature unsupported";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "Internal error";
            default:
                return "Unknown error";
        }
    }
}
