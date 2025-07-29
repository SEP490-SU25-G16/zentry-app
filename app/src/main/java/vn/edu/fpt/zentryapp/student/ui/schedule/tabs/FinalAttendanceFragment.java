package vn.edu.fpt.zentryapp.student.ui.schedule.tabs;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentFinalAttendanceStudentBinding;

public class FinalAttendanceFragment extends Fragment {

    private FragmentFinalAttendanceStudentBinding binding;
    private FinalAttendanceViewModel viewModel;
    private String sessionId; // 🔧 ĐỔI từ classId thành sessionId

    // 🔧 CẬP NHẬT newInstance để nhận sessionId
    public static FinalAttendanceFragment newInstance(String sessionId) {
        FinalAttendanceFragment fragment = new FinalAttendanceFragment();
        Bundle args = new Bundle();
        args.putString("sessionId", sessionId); // 🔧 ĐỔI key
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sessionId = getArguments().getString("sessionId"); // 🔧 ĐỔI key
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFinalAttendanceStudentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(FinalAttendanceViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());

        // 🔧 TRUYỀN context và sessionId
        viewModel.init(requireContext(), authManager, sessionId);

        setupClickListener();
        observeViewModel();
    }

    private void setupClickListener() {
        // Optional: Add click listeners if needed
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Optional: show loading indicator
            // binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.myAttendance().observe(getViewLifecycleOwner(), attendance -> {
            if (attendance != null) {
                binding.tvTotalRounds.setText("Total Rounds: " + attendance.getTotalSessions());
                binding.tvAttendedSessions.setText("Attended: " + attendance.getAttendedSessions());
                binding.tvAbsentSessions.setText("Absent: " + attendance.getAbsentSessions());
                binding.tvAttendancePercentage.setText(String.format("%.1f%%", attendance.getAttendancePercentage()));
                binding.tvAttendanceStatus.setText(attendance.getAttendanceGrade());
                binding.tvAttendanceStatus.setTextColor(attendance.getAttendanceColor());
                binding.tvAttendancePercentage.setTextColor(attendance.getAttendanceColor());

                Log.d("FinalAttendance", "My attendance loaded: " + attendance.getAttendancePercentage() + "%");
            }
        });

        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                Log.e("FinalAttendance", "Error: " + error);
            }
        });

        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Log.d("FinalAttendance", "Success: " + message);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
