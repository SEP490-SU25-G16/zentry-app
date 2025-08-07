package vn.edu.fpt.zentryapp.student.ui.schedule;

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
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Loading: " + isLoading);
        });

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
        binding.tvStudentId.setText("ID: " + attendanceData.getStudentId().substring(0, 8).toUpperCase());

        // ✅ Update attendance statistics
        updateAttendanceStatistics(attendanceData);

        // ✅ Update final status button
        updateFinalStatusButton(attendanceData);

        Log.d(TAG, "Updated UI - Student: " + attendanceData.getFullName() +
                ", Status: " + attendanceData.getSessionStatus() +
                ", Percentage: " + attendanceData.getFinalAttendancePercentage() + "%");
    }

    private void updateAttendanceStatistics(StudentFinalAttendanceDto attendanceData) {
        // ✅ Update attended rounds
        binding.tvAttendedRounds.setText(String.valueOf(attendanceData.getAttendedRoundsCount()));

        // ✅ Update missed rounds
        int missedRounds = attendanceData.getTotalRounds() - attendanceData.getAttendedRoundsCount();
        binding.tvMissedRounds.setText(String.valueOf(missedRounds));

        // ✅ Update attendance percentage (circular progress)
        int percentage = (int) Math.round(attendanceData.getFinalAttendancePercentage());
        binding.tvAttendancePercentage.setText(percentage + "%");

        // ✅ Update total rounds display
        String totalDisplay = String.format("Total: %d/%d",
                attendanceData.getAttendedRoundsCount(),
                attendanceData.getTotalRounds());
        binding.tvTotalRounds.setText(totalDisplay);

        // ✅ Update percentage circle color based on attendance
        if (percentage >= 80) {
            // Green for good attendance (80%+)
            binding.tvAttendancePercentage.getParent(); // MaterialCardView parent will keep green background
        } else if (percentage >= 50) {
            // Could add orange/yellow for medium attendance if needed
        } else {
            // Could add red background for low attendance if needed
        }
    }

    private void updateFinalStatusButton(StudentFinalAttendanceDto attendanceData) {
        String status;
        String backgroundColor;

        String sessionStatus = attendanceData.getSessionStatus().toLowerCase();

        // ✅ Check if Active session has ended
        boolean isActiveSessionEnded = false;
        if ("active".equals(sessionStatus) && session != null) {
            Date currentTime = new Date();
            Date endTime = session.getEndTimeAsDate();

            if (endTime != null) {
                isActiveSessionEnded = currentTime.after(endTime);
                Log.d(TAG, String.format("Active session check: CurrentTime=%s, EndTime=%s, HasEnded=%s",
                        currentTime, endTime, isActiveSessionEnded));
            }
        }

        // ✅ Determine status and color
        switch (sessionStatus) {
            case "active":
                if (isActiveSessionEnded) {
                    // Active session has ended - check attendance
                    if (attendanceData.getFinalAttendancePercentage() >= 80.0) {
                        status = "Attended";
                        backgroundColor = "#10B981"; // Green
                    } else {
                        status = "Absent";
                        backgroundColor = "#EF4444"; // Red
                    }
                } else {
                    // Active and still ongoing
                    status = "Ongoing";
                    backgroundColor = "#F59E0B"; // Orange
                }
                break;

            case "completed":
                // Session completed - check attendance percentage
                if (attendanceData.getFinalAttendancePercentage() >= 80.0) {
                    status = "Attended";
                    backgroundColor = "#10B981"; // Green
                } else {
                    status = "Absent";
                    backgroundColor = "#EF4444"; // Red
                }
                break;

            case "missed":
                // Session was missed
                status = "Missed";
                backgroundColor = "#EF4444"; // Red
                break;

            default:
                // Fallback
                status = "Unknown";
                backgroundColor = "#64748B"; // Gray
                break;
        }

        // ✅ Update final status button
        binding.btnFinalAttendanceStatus.setText(status);
        binding.btnFinalAttendanceStatus.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor(backgroundColor))
        );

        Log.d(TAG, String.format("Final status updated: %s (Session: %s, IsActiveEnded: %s, Percentage: %.1f%%)",
                status, attendanceData.getSessionStatus(), isActiveSessionEnded,
                attendanceData.getFinalAttendancePercentage()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
