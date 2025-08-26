package vn.edu.fpt.zentryapp.lecturer.ui.schedule.tabs;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import vn.edu.fpt.zentryapp.databinding.FragmentLecturerAttendanceBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.LecturerAttendanceAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Attendance;
import vn.edu.fpt.zentryapp.lecturer.ui.schedule.LecturerScheduleClassDetailFragment;

public class LecturerAttendanceFragment extends Fragment implements LecturerAttendanceAdapter.OnStudentClickListener {

    private FragmentLecturerAttendanceBinding binding;
    private LecturerAttendanceAdapter attendanceAdapter;
    private String sessionId;
    private GestureDetector gestureDetector;

    // ✅ Track current display mode
    private boolean isShowingFinalAttendance = true;
    private int currentRoundNumber = -1;

    public static LecturerAttendanceFragment newInstance(String sessionId) {
        LecturerAttendanceFragment fragment = new LecturerAttendanceFragment();
        Bundle args = new Bundle();
        args.putString("sessionId", sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerAttendanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            sessionId = getArguments().getString("sessionId", "");
        }

        setupRecyclerView();
        setupDoubleTapDetection();
    }

    private void setupRecyclerView() {
        attendanceAdapter = new LecturerAttendanceAdapter();
        attendanceAdapter.setOnStudentClickListener(this);

        binding.rvAttendance.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAttendance.setAdapter(attendanceAdapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDoubleTapDetection() {
        gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Double tap detected - notify parent to load final attendance
                if (getParentFragment() instanceof LecturerScheduleClassDetailFragment) {
                    // This will be handled by the main fragment's double tap detection
                }
                return true;
            }
        });

        binding.getRoot().setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }

    // ✅ Method cho final attendance (từ main fragment)
    public void updateAttendanceData(List<Attendance> attendanceList) {
        if (attendanceAdapter != null) {
            attendanceAdapter.setAttendanceList(attendanceList);

            isShowingFinalAttendance = true;
            currentRoundNumber = -1;
            updateHeaderText(attendanceList);
        }
    }

    // ✅ Method cho round-specific attendance (từ round click)
    public void updateRoundAttendanceData(List<Attendance> attendanceList, int roundNumber) {
        if (attendanceAdapter != null) {
            attendanceAdapter.setAttendanceList(attendanceList);

            // ✅ Update header cho round attendance
            isShowingFinalAttendance = false;
            currentRoundNumber = roundNumber;
            updateHeaderText(attendanceList);
        }
    }

    // ✅ Method để update header text
    @SuppressLint("DefaultLocale")
    private void updateHeaderText(List<Attendance> attendanceList) {
        if (binding == null || attendanceList == null || attendanceList.isEmpty()) {
            binding.tvAttendanceHeader.setText("No attendance data");
            return;
        }

        // Tính số sinh viên attended
        int totalStudents = attendanceList.size();
        int attendedStudents = 0;

        for (Attendance student : attendanceList) {
            // ✅ SỬA: Check status String thay vì boolean
            if ("Present".equalsIgnoreCase(student.getStatus())) {
                attendedStudents++;
            }
        }

        // Format header text
        String headerText;
        if (isShowingFinalAttendance) {
            headerText = String.format("%d/%d students attended - Final Result",
                    attendedStudents, totalStudents);
        } else {
            headerText = String.format("%d students joined - Round %d",
                    totalStudents, currentRoundNumber);
        }

        binding.tvAttendanceHeader.setText(headerText);
    }


    @Override
    public void onStudentClick(Attendance student) {
        android.widget.Toast.makeText(requireContext(),
                student.getStudentName() + " - " + student.getAttendanceStatus(),
                android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
