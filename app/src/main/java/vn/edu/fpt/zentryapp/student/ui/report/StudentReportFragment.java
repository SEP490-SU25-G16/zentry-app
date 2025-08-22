package vn.edu.fpt.zentryapp.student.ui.report;

import android.annotation.SuppressLint;
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
import vn.edu.fpt.zentryapp.databinding.FragmentStudentReportBinding;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;
import vn.edu.fpt.zentryapp.student.adapter.StudentReportAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentReport;

public class StudentReportFragment extends Fragment implements StudentReportAdapter.OnReportClickListener {

    private FragmentStudentReportBinding binding;
    private StudentReportViewModel viewModel;
    private StudentReportAdapter reportAdapter;
    private NavController navController;
    private NotificationViewModel notificationViewModel;
    private static final String TAG = "StudentReportFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel với context
        viewModel = new ViewModelProvider(this).get(StudentReportViewModel.class);
        notificationViewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
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
        setupRefreshListener();
    }

    // 🔧 NEW: Broadcast receiver for real-time notification updates
    private final BroadcastReceiver notificationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            Log.d(TAG, "📢 StudentReport: Received notification update broadcast");
            
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

    private void setupClickListeners() {
        // Notification button click handler
        binding.btnStudentReportNotification.setOnClickListener(v -> {
            try {
                if (navController.getCurrentDestination() != null && 
                    navController.getCurrentDestination().getAction(R.id.action_studentReport_to_notification) != null) {
                    navController.navigate(R.id.action_studentReport_to_notification);
                } else {
                    // Fallback navigation if action isn't available
                    Log.w(TAG, "Navigation action not available, using fallback");
                    Toast.makeText(requireContext(), "Navigating to notifications…", Toast.LENGTH_SHORT).show();
                    
                    // Try to find notificationFragment by ID
                    navController.navigate(R.id.notificationFragment);
                }
            } catch (Exception e) {
                Log.e(TAG, "Navigation error: ", e);
                Toast.makeText(requireContext(), "Notification feature is under development", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRefreshListener() {
        // Add pull-to-refresh functionality if SwipeRefreshLayout exists
        // binding.swipeRefresh.setOnRefreshListener(() -> {
        //     viewModel.refreshReports();
        // });
    }
    
    private void setupSearchBar() {
        binding.etStudentSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Use ViewModel search instead of adapter filter
                viewModel.searchReports(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });

        binding.ivStudentSearch.setOnClickListener(v -> {
            String query = binding.etStudentSearch.getText().toString();
            viewModel.searchReports(query);
        });
    }

    private void setupRecyclerView() {
        reportAdapter = new StudentReportAdapter();
        reportAdapter.setOnReportClickListener(this);

        binding.rvReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReports.setAdapter(reportAdapter);
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            // Stop refresh animation if SwipeRefreshLayout exists
            // if (binding.swipeRefresh != null) {
            //     binding.swipeRefresh.setRefreshing(isLoading);
            // }
        });

        // Observe reports data
        viewModel.reports().observe(getViewLifecycleOwner(), reports -> {
            if (reports != null) {
                reportAdapter.setReports(reports);

                boolean isEmpty = reports.isEmpty();
                binding.rvReports.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                Log.d("StudentReportFragment", "Loaded " + reports.size() + " reports");
            }
        });

        // Observe errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                Log.e("StudentReportFragment", "Error: " + error);
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

    @Override
    public void onReportClick(StudentReport report) {
        Log.d("StudentReportFragment", "Report clicked: " + report.getCourseName());

        // Show class details in toast (for debugging)
        @SuppressLint("DefaultLocale") String classInfo = String.format("%s\n%s\n%s\n%.1f%% Attendance",
                report.getClassName(),
                report.getClassInfo(),
                report.getLecturerDisplayName(),
                report.getAttendanceRate());

        // Pass StudentReport object via Serializable
        Bundle args = new Bundle();
        args.putSerializable("studentReport", report);

        navController.navigate(R.id.action_studentReport_to_listSession, args);
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

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        if (viewModel != null) {
            viewModel.refreshReports();
        }
    }
}
