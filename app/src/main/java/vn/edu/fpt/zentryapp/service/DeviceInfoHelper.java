package vn.edu.fpt.zentryapp.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

public class DeviceInfoHelper {
    private static final String TAG = "DeviceInfoHelper";

    /**
     * Tạo MAC address từ Android ID
     */
    @SuppressLint("HardwareIds")
    public static String generateMacAddress(Context context) {
        try {
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            byte[] idBytes = generateIdBytes(androidId);

            // Format thành XX:XX:XX:XX:XX:XX
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < idBytes.length; i++) {
                sb.append(String.format("%02X", idBytes[i]));
                if (i < idBytes.length - 1) sb.append(":");
            }

            String macAddress = sb.toString();
            Log.d(TAG, "Generated MAC address: " + macAddress);
            return macAddress;
        } catch (Exception e) {
            Log.e(TAG, "Error generating MAC address", e);
            return "00:00:00:00:00:00"; // Fallback
        }
    }

    /**
     * Hash input string thành 6 bytes để làm MAC address
     */
    private static byte[] generateIdBytes(String input) {
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
     * Lấy tên thiết bị thân thiện
     */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    /**
     * Capitalize string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : str.toCharArray()) {
            if (capitalizeNext && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Lấy platform
     */
    public static String getPlatform() {
        return "Android";
    }

    /**
     * Lấy OS version
     */
    public static String getOsVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * Lấy model thiết bị
     */
    public static String getModel() {
        return Build.MODEL;
    }

    /**
     * Lấy manufacturer
     */
    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * Lấy app version
     */
    public static String getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version", e);
            return "1.0.0"; // Fallback
        }
    }

    /**
     * Generate push notification token
     */
    public static String generatePushNotificationToken(Context context) {
        try {
            // Tạo unique token từ Android ID + timestamp
            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String input = androidId + "_" + timestamp;

            // Hash để tạo token
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            String token = sb.toString().substring(0, 32); // Lấy 32 ký tự đầu
            Log.d(TAG, "Generated push token: " + token);
            return token;

        } catch (Exception e) {
            Log.e(TAG, "Error generating push token", e);
            // Fallback sử dụng UUID
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
