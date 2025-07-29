package vn.edu.fpt.zentryapp.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class BLEAttendanceManager {
    private static final String TAG = "BLEAttendanceManager";
    private static final String SERVICE_UUID = "0000180F-0000-1000-8000-00805F9B34FB";

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeAdvertiser advertiser;
    private final BluetoothLeScanner scanner;
    private AdvertiseCallback advertiseCallback;
    private ScanCallback scanCallback;
    private Context context;

    public BLEAttendanceManager(Context context) {
        this.context = context;
        Log.d(TAG, "=== INITIALIZING BLE ATTENDANCE MANAGER ===");

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        Log.d(TAG, "BluetoothAdapter: " + (bluetoothAdapter != null ? "Available" : "NULL"));

        this.advertiser = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeAdvertiser() : null;
        this.scanner = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeScanner() : null;

        Log.d(TAG, "BLE Advertiser: " + (advertiser != null ? "Available" : "NULL"));
        Log.d(TAG, "BLE Scanner: " + (scanner != null ? "Available" : "NULL"));
        Log.d(TAG, "Service UUID: " + SERVICE_UUID);
        Log.d(TAG, "============================================");
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    public void startAdvertising(AttendanceModels.BLEAdvertiseData data,
                                 AttendanceCallbacks.BLEOperationCallback callback) {
        Log.d(TAG, "=== STARTING BLE ADVERTISING ===");
        Log.d(TAG, "Advertise data: " + data.toString());
        Log.d(TAG, "Room name: " + data.getRoomName());
        Log.d(TAG, "MAC address: " + data.getMacAddress());

        if (advertiser == null) {
            Log.e(TAG, "❌ BLE Advertiser not available");
            callback.onFailure("BLE Advertiser not available");
            return;
        }

        this.advertiseCallback = createAdvertiseCallback(callback);

        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData advertiseData = buildAdvertiseData(data);

        Log.d(TAG, "Starting advertiser with settings...");
        advertiser.startAdvertising(settings, advertiseData, advertiseCallback);
        Log.d(TAG, "Advertiser start command sent");
        Log.d(TAG, "=================================");
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void startScanning(String targetRoom,
                              AttendanceCallbacks.DeviceDetectionCallback callback) {
        Log.d(TAG, "=== STARTING BLE SCANNING ===");
        Log.d(TAG, "Target room: " + targetRoom);
        Log.d(TAG, "Service UUID to scan: " + SERVICE_UUID);

        if (scanner == null) {
            Log.e(TAG, "❌ BLE Scanner not available");
            callback.onDeviceLost("BLE Scanner not available");
            return;
        }

        this.scanCallback = createScanCallback(targetRoom, callback);
        ScanSettings settings = buildScanSettings();

        Log.d(TAG, "Scan settings: " + settings.toString());
        Log.d(TAG, "Starting scanner...");
        scanner.startScan(null, settings, scanCallback);
        Log.d(TAG, "Scanner start command sent");
        Log.d(TAG, "=============================");
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    public void stopAdvertising() {
        Log.d(TAG, "=== STOPPING BLE ADVERTISING ===");
        Optional.ofNullable(advertiser)
                .ifPresent(adv -> {
                    Log.d(TAG, "Stopping advertiser...");
                    adv.stopAdvertising(advertiseCallback);
                    Log.d(TAG, "Advertiser stopped");
                });
        Log.d(TAG, "=================================");
    }

    private ScanSettings buildScanSettings() {
        Log.d(TAG, "Building scan settings:");
        Log.d(TAG, "  Scan mode: LOW_LATENCY");
        Log.d(TAG, "  Callback type: ALL_MATCHES");
        Log.d(TAG, "  Report delay: 0ms");

        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setReportDelay(0)
                .build();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void stopScanning() {
        Log.d(TAG, "=== STOPPING BLE SCANNING ===");
        Optional.ofNullable(scanner)
                .ifPresent(scan -> {
                    Log.d(TAG, "Stopping scanner...");
                    scan.stopScan(scanCallback);
                    Log.d(TAG, "Scanner stopped");
                });
        Log.d(TAG, "==============================");
    }

    private AdvertiseCallback createAdvertiseCallback(AttendanceCallbacks.BLEOperationCallback callback) {
        return new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "🟢 BLE Advertising started successfully");
                Log.d(TAG, "Settings in effect: " + settingsInEffect.toString());
                Log.d(TAG, "  Mode: " + settingsInEffect.getMode());
                Log.d(TAG, "  TX Power: " + settingsInEffect.getTxPowerLevel());
                Log.d(TAG, "  Timeout: " + settingsInEffect.getTimeout());
                callback.onSuccess();
            }

            @Override
            public void onStartFailure(int errorCode) {
                String errorMsg = getAdvertiseErrorMessage(errorCode);
                Log.e(TAG, "🔴 BLE Advertising failed: " + errorCode + " - " + errorMsg);
                callback.onFailure("Advertising failed with code: " + errorCode + " - " + errorMsg);
            }
        };
    }

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

    private ScanCallback createScanCallback(String targetRoom,
                                            AttendanceCallbacks.DeviceDetectionCallback callback) {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.v(TAG, "🔍 Scan result received:");
                Log.v(TAG, "  Device: " + result.getDevice().getAddress());
                Log.v(TAG, "  RSSI: " + result.getRssi() + " dBm");
                Log.v(TAG, "  Callback type: " + callbackType);

                processScannedDevice(result, targetRoom, callback);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.d(TAG, "📦 Batch scan results: " + results.size() + " devices");
                for (ScanResult result : results) {
                    processScannedDevice(result, targetRoom, callback);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                String errorMsg = getScanErrorMessage(errorCode);
                Log.e(TAG, "🔴 Scan failed: " + errorCode + " - " + errorMsg);
                callback.onDeviceLost("Scan failed: " + errorMsg);
            }
        };
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

    private void processScannedDevice(ScanResult result,
                                      String targetRoom,
                                      AttendanceCallbacks.DeviceDetectionCallback callback) {
        Log.v(TAG, "📱 Processing scanned device: " + result.getDevice().getAddress());

        extractBLEData(result)
                .filter(d -> {
                    Log.v(TAG, "  Extracted room: '" + d.getRoomName() + "', Target: '" + targetRoom + "'");
                    boolean match = targetRoom.equals(d.getRoomName());
                    Log.v(TAG, "  Room match: " + match);
                    return match;
                })
                .map(d -> {
                    int rssi = result.getRssi();
                    Log.d(TAG, "  Creating ScannedDevice - MAC: " + d.getMacAddress() + ", RSSI: " + rssi);
                    return new AttendanceModels.ScannedDevice(
                            d.getMacAddress(),
                            rssi
                    );
                })
                .ifPresent(device -> {
                    Log.d(TAG, "✅ Device detected and accepted: " + device.getMacAddress() +
                            " with RSSI: " + device.getRssi() + " dBm");
                    callback.onDeviceDetected(device);
                });
    }

    private Optional<AttendanceModels.BLEAdvertiseData> extractBLEData(ScanResult result) {
        Log.v(TAG, "🔬 Extracting BLE data from scan result...");

        if (result.getScanRecord() == null) {
            Log.w(TAG, "  No scan record available");
            return Optional.empty();
        }

        ParcelUuid uuid = ParcelUuid.fromString(SERVICE_UUID);
        byte[] raw = result.getScanRecord().getServiceData(uuid);

        if (raw == null) {
            Log.v(TAG, "  No service data found for UUID: " + SERVICE_UUID);
            return Optional.empty();
        }

        Log.d(TAG, "  Raw data length: " + raw.length + " bytes");

        String payload = new String(raw, StandardCharsets.UTF_8);
        Log.d(TAG, "  Payload: '" + payload + "'");

        String[] parts = payload.split("\\|");
        Log.d(TAG, "  Split parts: " + parts.length);

        if (parts.length < 2) {
            Log.w(TAG, "  Invalid payload format (expected: MAC|Room)");
            return Optional.empty();
        }

        String mac = parts[0];
        String room = parts[1];
        Log.d(TAG, "  Extracted - MAC: '" + mac + "', Room: '" + room + "'");

        return Optional.of(new AttendanceModels.BLEAdvertiseData(mac, room));
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        Log.d(TAG, "Building advertise settings:");
        Log.d(TAG, "  Mode: LOW_LATENCY");
        Log.d(TAG, "  TX Power: MEDIUM");
        Log.d(TAG, "  Connectable: false");
        Log.d(TAG, "  Timeout: 0 (indefinite)");

        return new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0)
                .build();
    }

    private AdvertiseData buildAdvertiseData(AttendanceModels.BLEAdvertiseData data) {
        Log.d(TAG, "=== BUILDING ADVERTISE DATA ===");

        @SuppressLint("HardwareIds") String deviceMac = getDeviceMacFromAndroidId();
        String compactData = deviceMac + "|" + data.getRoomName();
        byte[] dataBytes = compactData.getBytes(StandardCharsets.UTF_8);

        Log.d(TAG, "Device MAC: " + deviceMac);
        Log.d(TAG, "Room name: " + data.getRoomName());
        Log.d(TAG, "Compact data: '" + compactData + "'");
        Log.d(TAG, "Data size: " + dataBytes.length + " bytes (limit: 31)");

        if (dataBytes.length > 31) {
            Log.e(TAG, "❌ Data too large: " + dataBytes.length + " bytes");
        }

        Log.d(TAG, "✅ Data size OK, building advertise data...");

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceData(ParcelUuid.fromString(SERVICE_UUID), dataBytes)
                .build();

        Log.d(TAG, "Advertise data built successfully");
        Log.d(TAG, "===============================");

        return advertiseData;
    }

    /**
     * 🔧 LẤY MAC TỪ ANDROID ID (THAY THẾ CHO bluetoothAdapter.getAddress())
     */
    @SuppressLint("HardwareIds")
    private String getDeviceMacFromAndroidId() {
        Log.d(TAG, "=== GETTING MAC FROM ANDROID ID ===");

        try {
            String androidId = android.provider.Settings.Secure.getString(
                    context.getContentResolver(),
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


}
