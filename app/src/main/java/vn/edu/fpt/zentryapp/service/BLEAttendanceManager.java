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
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class BLEAttendanceManager {
    private static final String TAG = "BLEAttendanceManager";
    private static final String SERVICE_UUID = "0000180F-0000-1000-8000-00805F9B34FB";

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeAdvertiser advertiser;
    private final BluetoothLeScanner scanner;
    private final Gson gson = new Gson();

    private AdvertiseCallback advertiseCallback;
    private ScanCallback scanCallback;

    public BLEAttendanceManager(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.advertiser = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeAdvertiser() : null;
        this.scanner = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeScanner() : null;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    public void startAdvertising(AttendanceModels.BLEAdvertiseData data,
                                 AttendanceCallbacks.BLEOperationCallback callback) {
        if (advertiser == null) {
            callback.onFailure("BLE Advertiser not available");
            return;
        }

        this.advertiseCallback = createAdvertiseCallback(callback);

        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData advertiseData = buildAdvertiseData(data);

        advertiser.startAdvertising(settings, advertiseData, advertiseCallback);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void startScanning(String targetRoom,
                              AttendanceCallbacks.DeviceDetectionCallback callback) {
        if (scanner == null) {
            callback.onDeviceLost("BLE Scanner not available");
            return;
        }

        this.scanCallback = createScanCallback(targetRoom, callback);
        ScanSettings settings = buildScanSettings();
        scanner.startScan(null, settings, scanCallback);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    public void stopAdvertising() {
        Optional.ofNullable(advertiser)
                .ifPresent(adv -> adv.stopAdvertising(advertiseCallback));
    }

    private ScanSettings buildScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)  // Quét nhanh nhất
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setReportDelay(0)  // Report ngay lập tức
                .build();
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void stopScanning() {
        Optional.ofNullable(scanner)
                .ifPresent(scan -> scan.stopScan(scanCallback));
    }

    private AdvertiseCallback createAdvertiseCallback(AttendanceCallbacks.BLEOperationCallback callback) {
        return new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "BLE Advertising started successfully");
                callback.onSuccess();
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "BLE Advertising failed: " + errorCode);
                callback.onFailure("Advertising failed with code: " + errorCode);
            }
        };
    }

    private ScanCallback createScanCallback(String targetRoom,
                                            AttendanceCallbacks.DeviceDetectionCallback callback) {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                processScannedDevice(result, targetRoom, callback);
            }
        };
    }

    private void processScannedDevice(ScanResult result,
                                      String targetRoom,
                                      AttendanceCallbacks.DeviceDetectionCallback callback) {
        extractBLEData(result)
                // chỉ quan tâm đến đúng room
                .filter(d -> targetRoom.equals(d.getRoomName()))
                .map(d -> {
                    // deviceId = MAC, studentId tạm lấy MAC (hoặc map sang studentId nếu có)
                    return new AttendanceModels.ScannedDevice(
                            d.getMac()
                    );
                })
                .ifPresent(callback::onDeviceDetected);
    }


    private Optional<AttendanceModels.BLEAdvertiseData> extractBLEData(ScanResult result) {
        ParcelUuid uuid = ParcelUuid.fromString(SERVICE_UUID);
        byte[] raw = result.getScanRecord().getServiceData(uuid);
        if (raw == null) return Optional.empty();

        String payload = new String(raw, StandardCharsets.UTF_8);
        String[] parts = payload.split("\\|");
        if (parts.length < 2) return Optional.empty();

        String mac = parts[0];
        String room = parts[1];
        return Optional.of(new AttendanceModels.BLEAdvertiseData(mac, room));
    }


    private AdvertiseSettings buildAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0)
                .build();
    }

    private AdvertiseData buildAdvertiseData(AttendanceModels.BLEAdvertiseData data) {
        // Format: MAC|RoomName (VD: "AA:BB:CC:DD:EE:FF|SE1750")
        String deviceMac = bluetoothAdapter.getAddress(); // Lấy MAC address
        String compactData = deviceMac + "|" + data.getRoomName();
        byte[] dataBytes = compactData.getBytes(StandardCharsets.UTF_8);

        Log.d(TAG, "Advertising data: " + compactData);
        Log.d(TAG, "Data size: " + dataBytes.length + " bytes (limit: 31)");

        if (dataBytes.length > 31) {
            Log.e(TAG, "Data too large: " + dataBytes.length + " bytes");
            // Fallback to room only
            return buildFallbackAdvertiseData(data.getRoomName());
        }

        return new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceData(ParcelUuid.fromString(SERVICE_UUID), dataBytes)
                .build();
    }


    private AdvertiseData buildFallbackAdvertiseData(String room) {
        Log.d(TAG, "Using fallback: room only");
        return new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceData(ParcelUuid.fromString(SERVICE_UUID),
                        room.getBytes(StandardCharsets.UTF_8))
                .build();
    }

}