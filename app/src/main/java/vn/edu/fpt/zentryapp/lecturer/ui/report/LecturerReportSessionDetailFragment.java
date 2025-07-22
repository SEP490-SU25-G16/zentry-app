package vn.edu.fpt.zentryapp.lecturer.ui.report;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerReportSessionDetailBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.StudentAttendanceAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Student;

public class LecturerReportSessionDetailFragment extends Fragment implements StudentAttendanceAdapter.OnAttendanceEditListener {

    private FragmentLecturerReportSessionDetailBinding binding;
    private LecturerReportSessionDetailViewModel viewModel;
    private StudentAttendanceAdapter studentAdapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerReportSessionDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Get arguments from previous fragment
        String sessionId = getArguments() != null ? getArguments().getString("sessionId", "") : "";
        String courseCode = getArguments() != null ? getArguments().getString("courseCode", "") : "";
        String courseName = getArguments() != null ? getArguments().getString("courseName", "") : "";
        String className = getArguments() != null ? getArguments().getString("className", "") : "";
        int sessionNumber = getArguments() != null ? getArguments().getInt("sessionNumber", 1) : 1;

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerReportSessionDetailViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager, sessionId, courseCode, courseName, className, sessionNumber);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        studentAdapter = new StudentAttendanceAdapter();
        studentAdapter.setOnAttendanceEditListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rvStudents.setLayoutManager(layoutManager);
        binding.rvStudents.setAdapter(studentAdapter);

        // Add divider between items
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), layoutManager.getOrientation());
        binding.rvStudents.addItemDecoration(divider);
    }

    private void setupClickListeners() {
        // Back button
        binding.ivSessionDetailBack.setOnClickListener(v -> navController.navigateUp());
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe session info
        viewModel.sessionInfo().observe(getViewLifecycleOwner(), sessionInfo -> {
            if (sessionInfo != null) {
                binding.tvSessionDetailGrade.setText(sessionInfo.getGradeDisplay());
                binding.tvSessionDetailSubject.setText(sessionInfo.getCourseName());
                binding.tvSessionDetailSessionNumber.setText(sessionInfo.getSessionTitle());
                binding.tvSessionDetailStudentCount.setText(sessionInfo.getTotalStudents() + " Students");
                binding.tvSessionDetailAttendanceCount.setText(sessionInfo.getAttendanceSummary());

                // Show/hide edit warning based on 24h rule
                boolean canEdit = sessionInfo.canEditAttendance();
                studentAdapter.setCanEditAttendance(canEdit);

                if (canEdit) {
                    binding.tvEditTimeWarning.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEditTimeWarning.setText("⚠️ Editing disabled - 24 hours have passed");
                    binding.tvEditTimeWarning.setTextColor(0xFFE53935); // Red
                    binding.tvEditTimeWarning.setVisibility(View.VISIBLE);
                }
            }
        });

        // Observe students
        viewModel.students().observe(getViewLifecycleOwner(), students -> {
            if (students != null) {
                studentAdapter.setStudents(students);
            }
        });

        // Observe attendance updates
        viewModel.attendanceUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (Boolean.TRUE.equals(updated)) {
                Toast.makeText(requireContext(), "Attendance updated", Toast.LENGTH_SHORT).show();
            }
        });

        // Observe errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onEditAttendance(Student student) {
        // Toggle attendance status
        viewModel.toggleStudentAttendance(student);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
