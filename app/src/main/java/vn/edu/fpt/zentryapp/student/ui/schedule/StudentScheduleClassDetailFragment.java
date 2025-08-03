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


    // ✅ NEW: Method để update UI với schedule detail data
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
        // Update student info
        binding.tvStudentName.setText(attendanceData.getFullName());
        binding.tvStudentId.setText("ID: " + attendanceData.getStudentId());

        // Update attendance status based on session status
        updateAttendanceStatus(attendanceData);

        // Update rounds info in student count field
        String roundsInfo = attendanceData.getAttendedRoundsCount() + "/" +
                attendanceData.getTotalRounds() + " Rounds";
        binding.tvStudentScheduleClassDetailStudentCount.setText(roundsInfo);

        Log.d(TAG, "Updated UI - Student: " + attendanceData.getFullName() +
                ", Status: " + attendanceData.getSessionStatus() +
                ", Percentage: " + attendanceData.getFinalAttendancePercentage() + "%");
    }

    private void updateAttendanceStatus(StudentFinalAttendanceDto attendanceData) {
        String status;
        String textColor;

        // ✅ SIMPLIFIED: Chỉ 3 cases - Active, Completed, Missed
        switch (attendanceData.getSessionStatus().toLowerCase()) {
            case "active":
                // Session đang diễn ra - chưa có kết quả final
                status = "On going";
                textColor = "#F59E0B"; // Orange - đang diễn ra
                break;

            case "completed":
                // Session đã kết thúc - check attendance percentage
                if (attendanceData.getFinalAttendancePercentage() >= 80.0) {
                    status = "Attended";
                    textColor = "#10B981"; // Green - có mặt
                } else {
                    status = "Absent";
                    textColor = "#EF4444"; // Red - vắng mặt
                }
                break;

            case "missed":
                // Session bị miss - không tham gia
                status = "Missed";
                textColor = "#EF4444"; // Red - bỏ lỡ
                break;

            default:
                // Fallback case (shouldn't happen)
                status = "Unknown";
                textColor = "#64748B"; // Gray
                break;
        }

        // ✅ CHỈ set text và color - không set background
        binding.tvStudentAttendanceStatus.setText(status);
        binding.tvStudentAttendanceStatus.setTextColor(Color.parseColor(textColor));

        Log.d(TAG, "Status updated: " + status +
                " (Session: " + attendanceData.getSessionStatus() +
                ", Percentage: " + attendanceData.getFinalAttendancePercentage() + "%)");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
