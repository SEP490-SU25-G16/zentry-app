package vn.edu.fpt.zentryapp.student.ui.schedule;

import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentScheduleBinding;
import vn.edu.fpt.zentryapp.student.adapter.StudentScheduleClassSectionAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleClassSection;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;


public class StudentScheduleFragment extends Fragment implements StudentScheduleClassSectionAdapter.OnSessionActionListener {

    private static final String TAG = "StudentScheduleFragment";

    private FragmentStudentScheduleBinding binding;
    private StudentScheduleViewModel viewModel;
    private StudentScheduleClassSectionAdapter scheduleAdapter;
    private NotificationViewModel notificationViewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentScheduleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentScheduleViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        notificationViewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);
        viewModel.init(requireContext(), authManager);

        // Load notifications từ API để có dữ liệu cho badge
        String userId = authManager.getCurrentUserId();
        if (userId != null) {
            notificationViewModel.loadNotifications(userId, requireContext());
        }

        // 🔧 NEW: Register broadcast receiver for real-time notification updates
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(notificationUpdateReceiver, new android.content.IntentFilter("vn.edu.fpt.zentryapp.NOTIFICATIONS_UPDATED"));

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
        setupSearchBar();
    }

    // 🔧 NEW: Broadcast receiver for real-time notification updates
    private final BroadcastReceiver notificationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            Log.d(TAG, "📢 StudentSchedule: Received notification update broadcast");
            
            // Force refresh notification count for badge
            String userId = intent.getStringExtra("userId");
            if (userId != null) {
                Log.d(TAG, "🔄 Refreshing notification count for badge update");
                // 🔧 FIX: Use forceRefresh instead of forceRefreshNotifications
                notificationViewModel.forceRefresh(userId, requireContext());
                
                // ✅ NEW: Check if this is a session end notification and handle BLE service stopping
                // This serves as a fallback when FCM fails due to network restrictions
                String notificationBody = intent.getStringExtra("notificationBody");
                if (notificationBody != null && notificationBody.contains("Tiết học đã kết thúc sớm")) {
                    Log.d(TAG, "🛑 Received session end notification, checking if BLE service needs to be stopped");
                    stopBLEAttendanceServiceIfNeeded();
                }
                
                // 🔧 FIX: Ensure unseenCount observer is set up for immediate badge update
                Log.d(TAG, "ℹ️ Badge will be updated automatically by existing observer");
            }
        }
    };

    private void setupSearchBar() {
        // Realtime search khi gõ
        binding.etStudentSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Click icon search
        binding.ivStudentSearch.setOnClickListener(v -> {
            String query = binding.etStudentSearch.getText().toString().trim();
            scheduleAdapter.filter(query);
        });
    }


    private void setupRecyclerView() {
        AuthManager authManager = AuthManager.getInstance(requireContext());
        scheduleAdapter = new StudentScheduleClassSectionAdapter(authManager);

        scheduleAdapter.setOnSessionActionListener(this);

        binding.rvStudentSchedules.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStudentSchedules.setAdapter(scheduleAdapter);
    }


    private void setupClickListeners() {
        // Calendar click
        binding.tvStudentScheduleCalendar.setOnClickListener(v -> {
            navController.navigate(R.id.action_studentSchedule_to_calendar);
        });

        // Notification click - using shared navigation handler
        binding.ivStudentNotification.setOnClickListener(v -> {
            try {
                if (navController.getCurrentDestination() != null && 
                    navController.getCurrentDestination().getAction(R.id.action_studentSchedule_to_notification) != null) {
                    navController.navigate(R.id.action_studentSchedule_to_notification);
                } else {
                    // Fallback navigation if action isn't available
                    android.util.Log.w(TAG, "Navigation action not available, using fallback");
                    Toast.makeText(requireContext(), "Navigating to notifications…", Toast.LENGTH_SHORT).show();
                    
                    // Try to find notificationFragment by ID
                    navController.navigate(R.id.notificationFragment);
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Navigation error: ", e);
                Toast.makeText(requireContext(), "Notification feature is under development", Toast.LENGTH_SHORT).show();
            }
        });

        // Search functionality
        binding.ivStudentSearch.setOnClickListener(v -> {
            String query = binding.etStudentSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                // TODO: Implement search
                Toast.makeText(requireContext(), "Searching: " + query, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void observeViewModel() {
        // Loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Sessions data
        viewModel.sessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                scheduleAdapter.setSessions(sessions);

                // Show/hide empty state
                boolean isEmpty = sessions.isEmpty();
                binding.rvStudentSchedules.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                Log.d(TAG, "Updated UI with " + sessions.size() + " sessions");
            }
        });

        // Error handling
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // Observe notification unseen count để hiển thị badge
        notificationViewModel.getUnseenCount().observe(getViewLifecycleOwner(), unseenCount -> {
            if (unseenCount != null && unseenCount > 0) {
                binding.tvNotificationBadge.setVisibility(View.VISIBLE);
                binding.tvNotificationBadge.setText(String.valueOf(unseenCount));
            } else {
                binding.tvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }

    // Implement OnSessionActionListener interface
    @Override
    public void onSessionClick(StudentScheduleClassSection session) {
        // Navigate to class detail with session object
        Bundle args = new Bundle();
        args.putSerializable("session", session);

        navController.navigate(R.id.action_studentSchedule_to_classDetail, args);

        Log.d(TAG, "Navigating to class detail for session: " + session.getSessionId());
    }

    @Override
    public void onJoinSession(StudentScheduleClassSection session) {
        Log.d(TAG, "Student joined session: " + session.getSessionId());
        //TODO: vào màn hình detail nếu thành công
        // Refresh data after joining
        viewModel.refreshSessions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // 🔧 NEW: Unregister broadcast receiver
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(notificationUpdateReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering notification receiver", e);
        }
        
        binding = null;
    }
    
    // ✅ NEW: Method to stop BLE attendance service when session ends (fallback for FCM failures)
    private void stopBLEAttendanceServiceIfNeeded() {
        try {
            // Check if BLE service is already stopped via FCM
            if (!vn.edu.fpt.zentryapp.notification.push.FcmMessagingService.isBLEServiceStopped()) {
                Intent serviceIntent = new Intent(requireContext(), vn.edu.fpt.zentryapp.service.BLEAttendanceService.class);
                serviceIntent.setAction("STOP_ATTENDANCE");
                requireContext().startService(serviceIntent);
                Log.d(TAG, "✅ Fragment: Sent STOP_ATTENDANCE intent to BLE service (FCM fallback)");
            } else {
                Log.d(TAG, "ℹ️ Fragment: BLE service already stopped by FCM, no action needed");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Fragment: Error stopping BLE service", e);
        }
    }
}
