package vn.edu.fpt.zentryapp.student.ui.schedule.tabs;

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
import vn.edu.fpt.zentryapp.databinding.FragmentFinalAttendanceBinding;
import vn.edu.fpt.zentryapp.databinding.FragmentFinalAttendanceStudentBinding;

public class FinalAttendanceFragment extends Fragment {

    private FragmentFinalAttendanceStudentBinding binding;
    private FinalAttendanceViewModel viewModel;
    private String classId;

    public static FinalAttendanceFragment newInstance(String classId) {
        FinalAttendanceFragment fragment = new FinalAttendanceFragment();
        Bundle args = new Bundle();
        args.putString("classId", classId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            classId = getArguments().getString("classId");
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
        viewModel.init(authManager, classId);

        setupClickListener();
        observeViewModel();
    }

    private void setupClickListener() {
        binding.cardMyAttendance.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Attendance details", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.myAttendance().observe(getViewLifecycleOwner(), attendance -> {
            if (attendance != null) {
                binding.tvTotalSessions.setText("Total Sessions: " + attendance.getTotalSessions());
                binding.tvAttendedSessions.setText("Attended: " + attendance.getAttendedSessions());
                binding.tvAbsentSessions.setText("Absent: " + attendance.getAbsentSessions());
                binding.tvAttendancePercentage.setText(String.format("%.1f%%", attendance.getAttendancePercentage()));
                binding.tvAttendanceStatus.setText(attendance.getAttendanceGrade());
                binding.tvAttendanceStatus.setTextColor(attendance.getAttendanceColor());
                binding.tvAttendancePercentage.setTextColor(attendance.getAttendanceColor());
            }
        });

        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d("FinalAttendance", message);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
