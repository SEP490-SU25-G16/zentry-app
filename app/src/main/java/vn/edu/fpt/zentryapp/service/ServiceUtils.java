package vn.edu.fpt.zentryapp.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceUtils {

    /**
     * Kiểm tra xem service có đang chạy không
     */
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kill service nếu nó đang chạy
     */
    public static void killServiceIfRunning(Context context, Class<?> serviceClass) {
        if (isServiceRunning(context, serviceClass)) {
            Log.d("ServiceUtils", "🛑 Service is running, killing it...");

            // Method 1: Send STOP action
            Intent stopIntent = new Intent(context, serviceClass);
            stopIntent.setAction("STOP_ATTENDANCE");
            context.startService(stopIntent);

            // Method 2: Force stop service (backup)
            context.stopService(new Intent(context, serviceClass));

            Log.d("ServiceUtils", "✅ Service kill commands sent");
        } else {
            Log.d("ServiceUtils", "ℹ️ Service is not running");
        }
    }
}
