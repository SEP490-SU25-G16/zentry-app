package vn.edu.fpt.zentryapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ➕ THÊM PERMISSION LAUNCHER
    private ActivityResultLauncher<String[]> blePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ➕ SETUP PERMISSION LAUNCHER TRƯỚC KHI REQUEST
        setupBLEPermissionLauncher();

        // ➕ REQUEST PERMISSIONS NGAY SAU KHI SETUP UI
        requestBLEPermissionsIfNeeded();

        // No need to modify navigation - the default navigation in nav_graph_root.xml
        // already starts with the login screen (loginFragment)

        // Log that we're using the default navigation
        android.util.Log.d("MainActivity", "Using default navigation starting with login screen");
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

                    if (allGranted) {
                        android.util.Log.d("MainActivity", "✅ All BLE permissions granted");
                        Toast.makeText(this, "✅ BLE permissions granted - Ready for attendance!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        android.util.Log.w("MainActivity", "❌ Some permissions denied: " + deniedPerms);
                        Toast.makeText(this, "⚠️ Some BLE permissions denied. Features may be limited.",
                                Toast.LENGTH_LONG).show();
                    }
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

            // Show explanation toast
            Toast.makeText(this, "📱 Requesting BLE permissions for attendance features...",
                    Toast.LENGTH_SHORT).show();

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
}
