package vn.edu.fpt.zentryapp.student.ui.schedule;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import android.content.BroadcastReceiver;
import android.content.Intent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Date;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentScheduleClassDetailBinding;
import vn.edu.fpt.zentryapp.student.data.model.response.ScheduleDetailDto;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleClassSection;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentFinalAttendanceDto;

public class StudentScheduleClassDetailFragment extends Fragment {

    private static final String TAG = "StudentClassDetail";

    private FragmentStudentScheduleClassDetailBinding binding;
    private StudentScheduleClassDetailViewModel viewModel;
    private NavController navController;
    private StudentScheduleClassSection session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentScheduleClassDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Get session object from arguments
        if (getArguments() != null) {
            session = (StudentScheduleClassSection) getArguments().getSerializable("session");
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentScheduleClassDetailViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager, session);

        setupUI();
        setupClickListeners();
        observeViewModel();
        
        // 🔧 NEW: Register broadcast receiver for real-time notification updates
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(notificationUpdateReceiver, new android.content.IntentFilter("vn.edu.fpt.zentryapp.NOTIFICATIONS_UPDATED"));
    }
    
    // 🔧 NEW: Broadcast receiver for real-time notification updates
    private final BroadcastReceiver notificationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            Log.d(TAG, "📢 StudentClassDetail: Received notification update broadcast");
            
            // ✅ NEW: Check if this is a session end notification and handle BLE service stopping
            // This serves as a fallback when FCM fails due to network restrictions
            String notificationBody = intent.getStringExtra("notificationBody");
            if (notificationBody != null && notificationBody.contains("Tiết học đã kết thúc sớm")) {
                Log.d(TAG, "🛑 Received session end notification, checking if BLE service needs to be stopped");
                stopBLEAttendanceServiceIfNeeded();
            }
        }
    };

    private void setLoading(boolean loading) {
        binding.flStudentScheduleClassDetailLoadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void setupUI() {
        // Set basic info from session object if available
        if (session != null) {
            // Course info
            String courseDisplay = session.getCourseName() + " - " + session.getSectionCode();
            binding.tvStudentScheduleClassDetailSubject.setText(courseDisplay);

            // Class info - format time display
            String timeDisplay = formatTimeDisplay(session);
            binding.tvStudentScheduleClassDetailDuration.setText(timeDisplay);

            // Room info
            String roomDisplay = session.getBuildingRoomDisplay();
            binding.tvStudentScheduleClassDetailRoom.setText(roomDisplay);

            // Will be updated from API
            binding.tvStudentScheduleClassDetailStudentCount.setText("Loading...");
        }
    }

    private String formatTimeDisplay(StudentScheduleClassSection session) {
        try {
            String dayTime = session.getDayOfWeek() + " " +
                    session.getStartTime().substring(0, 5) + " - " +
                    session.getEndTime().substring(0, 5);
            return dayTime;
        } catch (Exception e) {
            return "Schedule not available";
        }
    }

    private void setupClickListeners() {
        // Back button
        binding.ivStudentScheduleClassDetailBack.setOnClickListener(v ->
                navController.navigateUp());

        // Notification button
        binding.btnStudentScheduleClassDetailNotification.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show();
        });

        // Claim Request button
        binding.btnStudentScheduleClassDetailClaimRequest.setOnClickListener(v -> {
            // TODO: Implement claim request functionality
            Toast.makeText(requireContext(), "Claim Request clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        // Loading state
        // Show/hide overlay
        viewModel.isLoading().observe(getViewLifecycleOwner(), this::setLoading);

        viewModel.classSectionDetail().observe(getViewLifecycleOwner(), scheduleDetail -> {
            if (scheduleDetail != null) {
                updateScheduleDetailUI(scheduleDetail);
            }
        });

        // Student final attendance
        viewModel.studentFinalAttendance().observe(getViewLifecycleOwner(), attendanceData -> {
            if (attendanceData != null) {
                updateStudentAttendanceUI(attendanceData);
            }
        });

        // Error handling
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Method để update UI với schedule detail data
    private void updateScheduleDetailUI(ScheduleDetailDto scheduleDetail) {
        // Update course name với data từ API
        String courseDisplay = scheduleDetail.getFormattedCourseDisplay();
        binding.tvStudentScheduleClassDetailSubject.setText(courseDisplay);

        // Update student count từ API
        String studentCountDisplay = scheduleDetail.getFormattedStudentCount();
        binding.tvStudentScheduleClassDetailStudentCount.setText(studentCountDisplay);

        // Update duration từ API
        String durationDisplay = scheduleDetail.getFormattedDuration();
        binding.tvStudentScheduleClassDetailDuration.setText(durationDisplay);

        // Update room display từ API
        String roomDisplay = scheduleDetail.getFormattedRoomDisplay();
        binding.tvStudentScheduleClassDetailRoom.setText(roomDisplay);
    }

    private void updateStudentAttendanceUI(StudentFinalAttendanceDto attendanceData) {
        // ✅ Update student info section
        binding.tvStudentName.setText(attendanceData.getFullName());
        binding.tvStudentId.setText("ID: " + attendanceData.getStudentCode());

        // ✅ Update attendance statistics
        updateAttendanceStatistics(attendanceData);

        // ✅ Update final status button
        updateFinalStatusButton(attendanceData);

        Log.d(TAG, "Updated UI - Student: " + attendanceData.getFullName() +
                ", Status: " + attendanceData.getSessionStatus() +
                ", Percentage: " + attendanceData.getFinalAttendancePercentage() + "%");
    }

    private void updateAttendanceStatistics(StudentFinalAttendanceDto attendanceData) {
        Log.d(TAG, "=== ATTENDANCE DATA DEBUG ===");
        Log.d(TAG, "AttendedRoundsCount: " + attendanceData.getAttendedRoundsCount());
        Log.d(TAG, "TotalRounds: " + attendanceData.getTotalRounds());
        Log.d(TAG, "MissedRoundsCount: " + attendanceData.getMissedRoundsCount());
        Log.d(TAG, "FinalAttendancePercentage: " + attendanceData.getFinalAttendancePercentage());
        Log.d(TAG, "FinalStatus: " + attendanceData.getFinalStatus());
        // ✅ Update attended rounds
        binding.tvAttendedRounds.setText(attendanceData.getAttendedRoundsCount() + "");

        // ✅ Update missed rounds
        int missedRounds = attendanceData.getTotalRounds() - attendanceData.getAttendedRoundsCount();
        binding.tvMissedRounds.setText(String.valueOf(missedRounds));

        // ✅ Update attendance percentage (circular progress)
        int percentage = (int) Math.round(attendanceData.getFinalAttendancePercentage());
        binding.tvAttendancePercentage.setText(percentage + "%");

        // ✅ Update total rounds display
        @SuppressLint("DefaultLocale") String totalDisplay = String.format("Total: %d/%d",
                attendanceData.getAttendedRoundsCount(),
                attendanceData.getTotalRounds());
        binding.tvTotalRounds.setText(totalDisplay);
    }

    private void updateFinalStatusButton(StudentFinalAttendanceDto attendanceData) {
        String status = attendanceData.getFinalStatus(); // "Attended", "Absent", "Future" (hoặc giá trị khác)
        String displayStatus; // Hiển thị ra nút
        String backgroundColor;

        if (status == null) {
            displayStatus = "Unknown";
            backgroundColor = "#64748B"; // Gray
        } else {
            switch (status.toLowerCase()) {
                case "attended":
                    displayStatus = "Attended";
                    backgroundColor = "#10B981"; // Green
                    break;
                case "present": // TH nếu backend dùng "Present"
                    displayStatus = "Present";
                    backgroundColor = "#10B981"; // Green
                    break;
                case "absent":
                    displayStatus = "Absent";
                    backgroundColor = "#EF4444"; // Red
                    break;
                case "future":   // Tương lai, chưa tới buổi này
                    displayStatus = "Future";
                    backgroundColor = "#AAAAAA"; // Gray
                    break;
                default:
                    displayStatus = capitalizeFirst(status); // Nếu có giá trị lạ
                    backgroundColor = "#64748B"; // Gray
                    break;
            }
        }

        binding.btnFinalAttendanceStatus.setText(displayStatus);
        binding.btnFinalAttendanceStatus.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor(backgroundColor))
        );
        // Nếu muốn disable nút khi future:
        binding.btnFinalAttendanceStatus.setEnabled(!"future".equalsIgnoreCase(status));

        Log.d(TAG, String.format("Final status updated: %s (FinalStatus: %s)", displayStatus, status));
    }

    private String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return "";
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
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
}
