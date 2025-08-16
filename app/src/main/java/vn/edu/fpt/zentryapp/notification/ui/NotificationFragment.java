package vn.edu.fpt.zentryapp.notification.ui;

import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.notification.adapter.NotificationPagerAdapter;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;
import vn.edu.fpt.zentryapp.student.ui.setting.StudentSettingNotificationFragment;

public class NotificationFragment extends Fragment {

    private ImageView btnBack, btnSettings;
    private TextView tvGoToHistory;
    private TabLayout tabNotification;
    private ViewPager2 viewPagerNotification;
    private View emptyStateView;
    private NotificationViewModel viewModel;

    private final String[] tabTitles = new String[]{"All", "Unread"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View bindings
        tabNotification = view.findViewById(R.id.tabNotification);
        viewPagerNotification = view.findViewById(R.id.viewPagerNotification);
        emptyStateView = view.findViewById(R.id.notificationEmptyState);
        tvGoToHistory = view.findViewById(R.id.tvNotificationHistory);
        btnBack = view.findViewById(R.id.btnBack);
        btnSettings = view.findViewById(R.id.btnSettings);

        // Initialize ViewModel
        // Scope theo fragment để tránh share giữa Lecturer/Student
        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        // Set adapter for ViewPager2
        NotificationPagerAdapter pagerAdapter = new NotificationPagerAdapter(this);
        viewPagerNotification.setAdapter(pagerAdapter);

        // Link TabLayout with ViewPager2
        new TabLayoutMediator(tabNotification, viewPagerNotification,
                (tab, position) -> {
                    tab.setText(tabTitles[position]);
                }).attach();

        // Navigation actions
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        btnSettings.setOnClickListener(v -> {
            // Use FragmentManager for manual navigation - more reliable
            navigateToStudentSettingNotificationFragment();
        });
        tvGoToHistory.setOnClickListener(v ->
                Toast.makeText(getContext(), "Navigate to historical notifications...", Toast.LENGTH_SHORT).show());

        // Load initial data from API with userId and context
        String userId = vn.edu.fpt.zentryapp.auth.client.AuthManager.getInstance(requireContext()).getCurrentUserId();
        viewModel.loadNotifications(userId, requireContext());
        
        // Đánh dấu tất cả thông báo là đã seen khi vào màn hình notification
        viewModel.markAllAsSeen(requireContext());
        
        // 🔧 IMPROVED: Register broadcast receiver with better lifecycle management
        registerNotificationReceiver();
    }
    
    // 🔧 NEW: Separate method to register broadcast receiver
    private void registerNotificationReceiver() {
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager localBroadcastManager = 
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext());
            
            // Unregister first to avoid duplicates
            try {
                localBroadcastManager.unregisterReceiver(reloadReceiver);
            } catch (Exception e) {
                // Receiver not registered yet, ignore
            }
            
            // Register receiver
            localBroadcastManager.registerReceiver(reloadReceiver, 
                new android.content.IntentFilter("vn.edu.fpt.zentryapp.NOTIFICATIONS_UPDATED"));
            
            Log.d("NotificationFragment", "✅ Broadcast receiver registered successfully");
            Log.d("NotificationFragment", "📡 Listening for: vn.edu.fpt.zentryapp.NOTIFICATIONS_UPDATED");
            
        } catch (Exception e) {
            Log.e("NotificationFragment", "❌ Failed to register broadcast receiver", e);
        }
    }

    // 🔧 IMPROVED: Broadcast receiver to reload notifications immediately
    private final BroadcastReceiver reloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            Log.d("NotificationFragment", "📢 Received notification update broadcast");
            Log.d("NotificationFragment", "🔍 Intent action: " + intent.getAction());
            Log.d("NotificationFragment", "🔍 Intent extras: " + intent.getExtras());
            
            // Extract broadcast details
            String userId = intent.getStringExtra("userId");
            String notificationType = intent.getStringExtra("notificationType");
            String notificationTitle = intent.getStringExtra("notificationTitle");
            String notificationBody = intent.getStringExtra("notificationBody");
            long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
            
            Log.d("NotificationFragment", "📋 Broadcast details:");
            Log.d("NotificationFragment", "  - UserId: " + userId);
            Log.d("NotificationFragment", "  - Type: " + notificationType);
            Log.d("NotificationFragment", "  - Title: " + notificationTitle);
            Log.d("NotificationFragment", "  - Body: " + notificationBody);
            Log.d("NotificationFragment", "  - Timestamp: " + timestamp);
            
            // Force refresh notifications in real-time
            if (userId != null) {
                Log.d("NotificationFragment", "🔄 Triggering real-time notification refresh");
                
                // Use post to ensure UI thread execution
                requireView().post(() -> {
                    try {
                        // 🔧 IMPROVED: Use forceRefresh instead of forceRefreshNotifications
                        viewModel.forceRefresh(userId, requireContext());
                        Log.d("NotificationFragment", "✅ Real-time refresh triggered successfully");
                        
                        // 🔧 FIX: No need to manually refresh tabs - they will observe data changes automatically
                        Log.d("NotificationFragment", "ℹ️ Tabs will auto-refresh by observing data changes");
                        
                    } catch (Exception e) {
                        Log.e("NotificationFragment", "❌ Error during real-time refresh", e);
                    }
                });
                
                // Show toast for Face ID requests
                if ("FACE_VERIFICATION_REQUEST".equalsIgnoreCase(notificationType)) {
                    String message = "🎭 " + (notificationTitle != null ? notificationTitle : "Face ID verification requested");
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w("NotificationFragment", "⚠️ No userId in broadcast, cannot refresh");
            }
        }
    };

    // 🔧 NEW: Manual refresh method for testing
    public void manualRefreshNotifications() {
        try {
            Log.d("NotificationFragment", "🔄 Manual refresh triggered");
            
            String userId = vn.edu.fpt.zentryapp.auth.client.AuthManager.getInstance(requireContext()).getCurrentUserId();
            if (userId != null) {
                viewModel.forceRefresh(userId, requireContext());
                Toast.makeText(requireContext(), "Refreshing notifications...", Toast.LENGTH_SHORT).show();
            } else {
                Log.w("NotificationFragment", "⚠️ No user ID available for manual refresh");
                Toast.makeText(requireContext(), "No user ID available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("NotificationFragment", "❌ Error during manual refresh", e);
            Toast.makeText(requireContext(), "Refresh failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 🔧 NEW: Test method to manually trigger broadcast receiver
    private void testBroadcastReceiver() {
        try {
            Log.d("NotificationFragment", "🧪 Testing broadcast receiver manually");
            
            // Create test intent
            Intent testIntent = new Intent("vn.edu.fpt.zentryapp.NOTIFICATIONS_UPDATED");
            testIntent.putExtra("userId", "test_user_id");
            testIntent.putExtra("notificationType", "TEST_NOTIFICATION");
            testIntent.putExtra("notificationTitle", "Test Notification");
            testIntent.putExtra("notificationBody", "This is a test notification");
            testIntent.putExtra("timestamp", System.currentTimeMillis());
            
            // Send test broadcast
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                    .sendBroadcast(testIntent);
            
            Log.d("NotificationFragment", "✅ Test broadcast sent successfully");
            Toast.makeText(requireContext(), "Test broadcast sent", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e("NotificationFragment", "❌ Error sending test broadcast", e);
            Toast.makeText(requireContext(), "Test broadcast failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("NotificationFragment", "🔄 onResume: Re-registering broadcast receiver");
        
        // Re-register receiver when fragment resumes
        registerNotificationReceiver();
        
        // 🔧 NEW: In-app polling to refresh periodically (no FCM)
        startInAppPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("NotificationFragment", "⏸️ onPause: Unregistering broadcast receiver");
        
        // Stop polling
        stopInAppPolling();
        
        // Unregister receiver when fragment pauses
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(reloadReceiver);
        } catch (Exception e) {
            Log.e("NotificationFragment", "Error unregistering receiver in onPause", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("NotificationFragment", "🗑️ onDestroyView: Cleaning up broadcast receiver");
        
        // Cleanup broadcast receiver
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(reloadReceiver);
        } catch (Exception e) {
            Log.e("NotificationFragment", "Error unregistering receiver in onDestroyView", e);
        }
    }

    // Alternative navigation method using FragmentManager
    private void navigateToStudentSettingNotificationFragment() {
        try {
            Log.d("NotificationFragment", "Navigating to StudentSettingNotificationFragment");
            
            // Tạo fragment với source là NotificationFragment
            Fragment settingsFragment = StudentSettingNotificationFragment.newInstance(
                    StudentSettingNotificationFragment.SOURCE_NOTIFICATION);
            
            // Sử dụng FragmentTransaction để replace và add to back stack
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(((ViewGroup)getView().getParent()).getId(), settingsFragment)
                    .addToBackStack(null) // Sử dụng null để thêm vào back stack mặc định
                    .commit();
                    
        } catch (Exception e) {
            Log.e("NotificationFragment", "Navigation error", e);
            Toast.makeText(getContext(), "Không thể mở cài đặt thông báo", Toast.LENGTH_SHORT).show();
        }
    }

    // 🔧 NEW: Simple in-app polling (every 8s)
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                String userId = vn.edu.fpt.zentryapp.auth.client.AuthManager.getInstance(requireContext()).getCurrentUserId();
                if (userId != null) {
                    Log.d("NotificationFragment", "⏱️ Polling refresh...");
                    viewModel.forceRefresh(userId, requireContext());
                }
            } catch (Exception e) {
                Log.e("NotificationFragment", "Polling error", e);
            } finally {
                // Schedule next
                viewPagerNotification.postDelayed(pollingRunnable, 8000);
            }
        }
    };

    private void startInAppPolling() {
        // Start only once
        viewPagerNotification.removeCallbacks(pollingRunnable);
        viewPagerNotification.postDelayed(pollingRunnable, 8000);
    }

    private void stopInAppPolling() {
        viewPagerNotification.removeCallbacks(pollingRunnable);
    }
}

