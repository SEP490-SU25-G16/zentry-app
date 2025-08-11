package vn.edu.fpt.zentryapp.student.ui.report;

import android.os.Bundle;
import android.util.Log;
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

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentReportListSessionBinding;
import vn.edu.fpt.zentryapp.student.adapter.StudentListReportSessionAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentReport;

public class StudentReportListSessionFragment extends Fragment {

    private FragmentStudentReportListSessionBinding binding;
    private StudentReportListSessionViewModel viewModel;
    private StudentListReportSessionAdapter studentListReportSessionAdapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentReportListSessionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Get StudentReport from arguments
        StudentReport studentReport = null;
        if (getArguments() != null) {
            studentReport = (StudentReport) getArguments().getSerializable("studentReport");
        }

        if (studentReport == null) {
            Toast.makeText(requireContext(), "No course data found", Toast.LENGTH_SHORT).show();
            navController.navigateUp();
            return;
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentReportListSessionViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager, studentReport);

        setupRecyclerView();
        setupToolbar();
        observeViewModel();
    }

    private void setupRecyclerView() {
        studentListReportSessionAdapter = new StudentListReportSessionAdapter();
        // Students can't click on sessions (view-only), so no click listener needed

        binding.rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessions.setAdapter(studentListReportSessionAdapter);
    }

    private void setupToolbar() {
        binding.ivStudentReportListSessionBack.setOnClickListener(v ->
                navController.navigateUp());
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe sessions data
        viewModel.sessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                studentListReportSessionAdapter.setSessions(sessions);

                boolean isEmpty = sessions.isEmpty();
                binding.rvSessions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                Log.d("StudentReportListSession", "Loaded " + sessions.size() + " sessions");
            }
        });

        // Observe course info
        viewModel.courseInfo().observe(getViewLifecycleOwner(), courseInfo -> {
            if (courseInfo != null) {
                binding.tvStudentReportListSessionGrade.setText(courseInfo.getSectionCode());
                binding.tvStudentReportListSessionSubject.setText(courseInfo.getCourseName());
                binding.tvStudentReportListSessionCount.setText(
                        courseInfo.getAttendedSessions() + "/" + courseInfo.getTotalSessions() + " Sessions"
                );
            }
        });

        // Observe success messages
        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d("SessionList", message);
            }
        });

        // Observe error messages
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                Log.e("SessionList", "Error: " + error);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshData();
        }
    }
}
