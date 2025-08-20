package vn.edu.fpt.zentryapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;
import vn.edu.fpt.zentryapp.service.AttendanceModels;
import vn.edu.fpt.zentryapp.service.ManualAttendanceSyncService;
import vn.edu.fpt.zentryapp.service.NetworkStateManager;
import vn.edu.fpt.zentryapp.service.OfflineSubmissionManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ManualAttendanceSyncService syncService;
    private ActivityResultLauncher<String[]> blePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // ➕ SETUP PERMISSION LAUNCHER TRƯỚC KHI REQUEST
        setupBLEPermissionLauncher();

        // ➕ REQUEST PERMISSIONS NGAY SAU KHI SETUP UI
        requestBLEPermissionsIfNeeded();
        // ✅ INITIALIZE SYNC SERVICE
        initializeSyncService();

        // ✅ AUTO SYNC CACHED SUBMISSIONS
        autoSyncCachedSubmissions();

        // No need to modify navigation - the default navigation in nav_graph_root.xml
        // already starts with the login screen (loginFragment)

        // Handle deep link intents for Navigation
        try {
            androidx.navigation.fragment.NavHostFragment host = (androidx.navigation.fragment.NavHostFragment)
                    getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (host != null) {
                androidx.navigation.NavController navController = host.getNavController();
                navController.handleDeepLink(getIntent());
                
                // ✅ NEW: Xử lý navigation về student settings từ Face ID success
                String navigateTo = getIntent().getStringExtra("navigate_to");
                if ("student_settings".equals(navigateTo)) {
                    android.util.Log.d("MainActivity", "✅ Navigating to student settings from Face ID success");
                    // Navigate đến student settings
                    try {
                        navController.navigate(vn.edu.fpt.zentryapp.R.id.nav_graph_student);
                    } catch (Exception e) {
                        android.util.Log.w("MainActivity", "⚠️ Failed to navigate to student settings", e);
                    }
                }
                
                // ✅ NEW: Xử lý Face ID verification deeplink
                handleFaceIdVerificationDeepLink(getIntent());
            }
        } catch (Exception ignored) {}
        android.util.Log.d("MainActivity", "Using default navigation starting with login screen");
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        try {
            androidx.navigation.fragment.NavHostFragment host = (androidx.navigation.fragment.NavHostFragment)
                    getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (host != null) {
                androidx.navigation.NavController navController = host.getNavController();
                navController.handleDeepLink(intent);
                
                // ✅ NEW: Xử lý navigation về student settings từ Face ID success
                String navigateTo = intent.getStringExtra("navigate_to");
                if ("student_settings".equals(navigateTo)) {
                    android.util.Log.d("MainActivity", "✅ Navigating to student settings from Face ID success (onNewIntent)");
                    // Navigate đến student settings
                    try {
                        navController.navigate(vn.edu.fpt.zentryapp.R.id.nav_graph_student);
                    } catch (Exception e) {
                        android.util.Log.w("MainActivity", "⚠️ Failed to navigate to student settings", e);
                    }
                }
                
                // ✅ NEW: Xử lý Face ID verification deeplink
                handleFaceIdVerificationDeepLink(intent);
            }
        } catch (Exception ignored) {}
    }
    // ✅ NEW: Initialize sync service
    private void initializeSyncService() {
        syncService = new ManualAttendanceSyncService(this);
        android.util.Log.d("MainActivity", "📋 Sync service initialized");
    }

    // ✅ NEW: Auto sync cached submissions khi vào app
    private void autoSyncCachedSubmissions() {
        // Check if có cached submissions
        if (!syncService.needsSync()) {
            android.util.Log.d("MainActivity", "✅ No cached submissions to sync");
            return;
        }

        // Check network availability
        NetworkStateManager networkManager = new NetworkStateManager(this);
        if (!networkManager.isNetworkAvailable()) {
            android.util.Log.w("MainActivity", "📵 No network - cached submissions will sync later");
            return;
        }

        // Get summary info
        OfflineSubmissionManager.CachedSubmissionSummary summary = syncService.getSyncSummary();
        android.util.Log.d("MainActivity", "🔄 Found " + summary.totalCount + " cached submissions - starting auto sync");

        // ✅ SILENT AUTO SYNC
        syncService.syncAllCachedSubmissions(new OfflineSubmissionManager.ManualSyncCallback() {
            @Override
            public void onSyncStarted(int totalSubmissions) {
                android.util.Log.d("MainActivity", "🚀 Auto sync started: " + totalSubmissions + " submissions");
            }

            @Override
            public void onSubmissionSynced(AttendanceModels.AttendanceSubmission submission, int remaining) {
                android.util.Log.d("MainActivity", "✅ Synced: " + submission.getSessionId() + " (" + remaining + " remaining)");
            }

            @Override
            public void onSubmissionFailed(AttendanceModels.AttendanceSubmission submission, String error, int remaining) {
                android.util.Log.w("MainActivity", "❌ Sync failed: " + submission.getSessionId() + " - " + error);
            }

            @Override
            public void onSyncCompleted(int successful, int failed) {
                if (successful > 0) {
                    android.util.Log.d("MainActivity", "🎉 Auto sync completed: " + successful + " successful, " + failed + " failed");

                    // ✅ OPTIONAL: Show silent toast (không intrusive)
                    if (successful > 0) {
                        Toast.makeText(MainActivity.this,
                                "📤 Synced " + successful + " attendance record" + (successful > 1 ? "s" : ""),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    android.util.Log.w("MainActivity", "⚠️ Auto sync completed but no successes");
                }
            }
        });
    }

    // ✅ NEW: Auto sync lại khi user quay về app (onResume)
    @Override
    protected void onResume() {
        super.onResume();

        // ✅ RETRY SYNC khi user quay lại app (maybe có network rồi)
        if (syncService != null && syncService.needsSync()) {
            android.util.Log.d("MainActivity", "🔄 App resumed - checking for pending syncs");

            // Delay một chút để app settle
            new android.os.Handler().postDelayed(() -> {
                autoSyncCachedSubmissions();
            }, 1000);
        }
    }

    // ➕ SETUP PERMISSION LAUNCHER
    private void setupBLEPermissionLauncher() {
        blePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    StringBuilder deniedPerms = new StringBuilder();

                    for (String permission : result.keySet()) {
                        Boolean granted = result.get(permission);
                        if (granted == null || !granted) {
                            allGranted = false;
                            deniedPerms.append(permission).append(" ");
                        }
                    }

//                    if (allGranted) {
//                        android.util.Log.d("MainActivity", "✅ All BLE permissions granted");
//                        Toast.makeText(this, "✅ BLE permissions granted - Ready for attendance!",
//                                Toast.LENGTH_SHORT).show();
//                    } else {
//                        android.util.Log.w("MainActivity", "❌ Some permissions denied: " + deniedPerms);
//                        Toast.makeText(this, "⚠️ Some BLE permissions denied. Features may be limited.",
//                                Toast.LENGTH_LONG).show();
//                    }
                }
        );
    }

    // ➕ CHECK VÀ REQUEST PERMISSIONS
    private void requestBLEPermissionsIfNeeded() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Always need Location for BLE scanning
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Android 12+ BLE permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            android.util.Log.d("MainActivity", "Requesting " + permissionsNeeded.size() + " BLE permissions");

            // Request permissions
            blePermissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
        } else {
            android.util.Log.d("MainActivity", "✅ All BLE permissions already granted");
        }
    }

    // ➕ PUBLIC METHOD để các Fragment khác check permissions
    public boolean hasBLEPermissions() {
        // Check Location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Check Android 12+ BLE permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    // ➕ PUBLIC METHOD để Fragment request permissions lại
    public void requestBLEPermissions() {
        requestBLEPermissionsIfNeeded();
    }

    // ✅ NEW: Xử lý Face ID verification deeplink với expiration check
    private void handleFaceIdVerificationDeepLink(android.content.Intent intent) {
        if (intent == null) return;
        
        android.net.Uri data = intent.getData();
        if (data != null && "zentry".equals(data.getScheme()) && "face-verify".equals(data.getHost())) {
            android.util.Log.d("MainActivity", "🔗 Handling Face ID verification deeplink: " + data);
            
            String requestId = data.getQueryParameter("requestId");
            String sessionId = data.getQueryParameter("sessionId");
            String expiresAt = data.getQueryParameter("expiresAt");
            
            if (requestId != null && sessionId != null) {
                // ✅ NEW: Validate expiration before proceeding
                if (!isRequestExpired(expiresAt)) {
                    Log.d("MainActivity", "✅ Face ID verification request: " + requestId + " for session: " + sessionId);
                    
                    // Navigate to student settings with verification args
                    try {
                        androidx.navigation.fragment.NavHostFragment host = (androidx.navigation.fragment.NavHostFragment)
                                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                        if (host != null) {
                            androidx.navigation.NavController navController = host.getNavController();
                            
                            // Navigate to student settings first
                            navController.navigate(vn.edu.fpt.zentryapp.R.id.nav_graph_student);
                            
                            // Store the verification args for later use when settings tab is ready
                            storeVerificationArgs(requestId, sessionId, expiresAt);
                            
                            // The actual navigation to Face ID fragment will be handled by StudentMainFragment
                            // after the settings tab is initialized
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "❌ Failed to navigate to student settings", e);
                    }
                } else {
                    // ✅ NEW: Show error for expired request
                    Log.w("MainActivity", "⏰ Face ID verification request expired: " + requestId);
                }
            } else {
                Log.w("MainActivity", "⚠️ Missing requestId or sessionId in deeplink");
            }
        }
    }
    
    // ✅ NEW: Store verification args for later use
    private void storeVerificationArgs(String requestId, String sessionId, String expiresAt) {
        // Store in SharedPreferences or use a static variable for now
        android.content.SharedPreferences prefs = getSharedPreferences("face_verification", MODE_PRIVATE);
        prefs.edit()
                .putString("pending_request_id", requestId)
                .putString("pending_session_id", sessionId)
                .putString("pending_expires_at", expiresAt)
                .putLong("pending_timestamp", System.currentTimeMillis())
                .apply();
        
        Log.d("MainActivity", "💾 Stored verification args for later navigation");
    }
    
    // ✅ NEW: Check if Face ID request is expired
    private boolean isRequestExpired(String expiresAt) {
        if (expiresAt == null || expiresAt.isEmpty()) {
            android.util.Log.w("MainActivity", "⚠️ No expiration timestamp provided, treating as expired for security");
            return true; // Treat as expired if no timestamp provided
        }
        
        try {
            // Parse ISO 8601 timestamp (e.g., "2024-01-01T12:00:00Z")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date expirationDate = sdf.parse(expiresAt);
            
            if (expirationDate == null) {
                Log.w("MainActivity", "⚠️ Failed to parse expiration timestamp: " + expiresAt);
                return true; // Treat as expired if parsing fails
            }
            
            long currentTime = System.currentTimeMillis();
            long expirationTime = expirationDate.getTime();
            
            // Add 5-minute buffer for network delays and processing time
            long bufferTime = 5 * 60 * 1000; // 5 minutes in milliseconds
            
            boolean isExpired = currentTime > (expirationTime + bufferTime);
            
            if (isExpired) {
                Log.d("MainActivity", "⏰ Request expired: " + expiresAt);
            } else {
                Log.d("MainActivity", "✅ Request still valid: " + expiresAt + " (expires in " + ((expirationTime + bufferTime - currentTime) / 1000) + "s)");
            }
            
            return isExpired;
            
        } catch (ParseException e) {
            Log.e("MainActivity", "❌ Error parsing expiration timestamp: " + expiresAt, e);
            return true; // Treat as expired if parsing fails
        }
    }

}
