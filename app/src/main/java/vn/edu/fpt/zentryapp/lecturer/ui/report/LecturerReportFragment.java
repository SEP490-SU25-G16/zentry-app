package vn.edu.fpt.zentryapp.lecturer.ui.report;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerReportBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.LecturerReportClassSectionAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerReportClassSection;

/**
 * Màn hình này hiển thị list report các classroom/course với attendance percentage
 * Khi click vào một classroom sẽ navigate đến danh sách sessions chi tiết của lớp đó
 */
public class LecturerReportFragment extends Fragment implements LecturerReportClassSectionAdapter.OnLecturerReportClassSectionClickListener {

    private FragmentLecturerReportBinding binding;
    private LecturerReportViewModel viewModel;
    private LecturerReportClassSectionAdapter adapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerReportViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new LecturerReportClassSectionAdapter();
        adapter.setOnClassroomClickListener(this);

        binding.rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessions.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Notification button
        binding.btnNotification.setOnClickListener(v -> {
            // TODO: Navigate to notifications screen
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            // Optional: Show/hide RecyclerView during loading
            binding.rvSessions.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        // Observe classrooms data
        viewModel.classrooms().observe(getViewLifecycleOwner(), classrooms -> {
            if (classrooms != null) {
                adapter.setClassrooms(classrooms);
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
    public void onClassroomClick(LecturerReportClassSection classroom) {
        Toast.makeText(requireContext(),
                "Classroom clicked: " + classroom.getCourseName(),
                Toast.LENGTH_SHORT).show();

        // Navigate to sessions list of this classroom
        Bundle args = new Bundle();
        args.putString("courseName", classroom.getCourseName());
        args.putString("classInfo", classroom.getClassInfo());
        args.putInt("studentCount", classroom.getStudentCount());
        args.putDouble("attendancePercentage", classroom.getAttendancePercentage());

        navController.navigate(R.id.action_report_to_listSession, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
