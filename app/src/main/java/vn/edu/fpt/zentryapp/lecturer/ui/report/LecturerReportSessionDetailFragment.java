package vn.edu.fpt.zentryapp.lecturer.ui.report;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
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

import com.google.android.material.button.MaterialButton;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerReportSessionDetailBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.LecturerReportListStudentOnSessionAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.OverviewSession;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Student;

public class LecturerReportSessionDetailFragment extends Fragment implements LecturerReportListStudentOnSessionAdapter.OnAttendanceEditListener {

    private FragmentLecturerReportSessionDetailBinding binding;
    private LecturerReportSessionDetailViewModel viewModel;
    private LecturerReportListStudentOnSessionAdapter studentAdapter;
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

        // Get OverviewSession from arguments
        OverviewSession sessionInfo = null;
        if (getArguments() != null) {
            sessionInfo = (OverviewSession) getArguments().getSerializable("sessionInfo");
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerReportSessionDetailViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());

        if (sessionInfo != null) {
            viewModel.initWithSessionData(requireContext(), authManager, sessionInfo);
        } else {
            // Fallback to sessionId if sessionInfo is not available
            String sessionId = getArguments() != null ? getArguments().getString("sessionId", "") : "";
            viewModel.init(requireContext(), authManager, sessionId);
        }

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        studentAdapter = new LecturerReportListStudentOnSessionAdapter();
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
                binding.tvSessionDetailSessionNumber.setText(sessionInfo.getSessionTitle());
                binding.tvSessionDetailStudentCount.setText(sessionInfo.getTotalStudents() + " Students");
                binding.tvSessionDetailAttendanceCount.setText(sessionInfo.getAttendanceSummary());

                // Show/hide edit warning based on 24h rule
                boolean canEdit = sessionInfo.canEditAttendance();
                studentAdapter.setCanEditAttendance(canEdit);

                if (canEdit) {
                    binding.tvEditTimeWarning.setText("⚠️ You can edit attendance within 24 hours of session");
                    binding.tvEditTimeWarning.setTextColor(0xFF4CAF50); // Green
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
        showAttendanceUpdateConfirmation(student);
    }

    private void showAttendanceUpdateConfirmation(Student student) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_update_attendance_confirmation);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Get dialog views
        TextView tvStudentName = dialog.findViewById(R.id.tv_student_name);
        TextView tvStudentCode = dialog.findViewById(R.id.tv_student_code);
        TextView tvCurrentStatus = dialog.findViewById(R.id.tv_current_status);
        TextView tvNewStatus = dialog.findViewById(R.id.tv_new_status);
        TextView tvMessage = dialog.findViewById(R.id.tv_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

        // Set student info
        tvStudentName.setText(student.getDisplayName());
        tvStudentCode.setText(student.getStudentCode());

        // Set current and new status
        String currentStatus = student.isPresent() ? "Present" : "Absent";
        String newStatus = student.isPresent() ? "Absent" : "Present";

        tvCurrentStatus.setText(currentStatus);
        tvNewStatus.setText(newStatus);

        // Set status colors
        if (student.isPresent()) {
            // Currently Present -> Will be Absent
            tvCurrentStatus.setTextColor(0xFF059669); // Green
            tvCurrentStatus.setBackgroundColor(0xFFECFDF5); // Light green background
            tvNewStatus.setTextColor(0xFFDC2626); // Red
            tvNewStatus.setBackgroundColor(0xFFFEF2F2); // Light red background
        } else {
            // Currently Absent -> Will be Present
            tvCurrentStatus.setTextColor(0xFFDC2626); // Red
            tvCurrentStatus.setBackgroundColor(0xFFFEF2F2); // Light red background
            tvNewStatus.setTextColor(0xFF059669); // Green
            tvNewStatus.setBackgroundColor(0xFFECFDF5); // Light green background
        }

        // Set message
        String message = String.format("Change %s's attendance from %s to %s?",
                student.getDisplayName(), currentStatus, newStatus);
        tvMessage.setText(message);

        // Set button listeners
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            // Show loading and call API
            viewModel.toggleStudentAttendance(student);
        });

        dialog.setCancelable(true);
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
