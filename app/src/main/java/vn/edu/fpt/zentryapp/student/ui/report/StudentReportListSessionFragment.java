package vn.edu.fpt.zentryapp.student.ui.report;

import android.os.Bundle;
import android.util.Log;

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

public class StudentReportListSessionFragment extends Fragment  {

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

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentReportListSessionViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());

        // Get courseId from arguments or use default
        String courseId = getArguments() != null ?
                getArguments().getString("courseId", "MATH101") : "MATH101";

        viewModel.init(authManager, courseId);

        setupRecyclerView();
        setupToolbar();
        observeViewModel();
    }

    private void setupRecyclerView() {
        studentListReportSessionAdapter = new StudentListReportSessionAdapter();
//        sessionAdapter.setOnSessionClickListener(this);

        binding.rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessions.setAdapter(studentListReportSessionAdapter);
    }

    private void setupToolbar() {
        binding.ivStudentReportListSessionBack.setOnClickListener(v ->
                requireActivity().onBackPressed());
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.sessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                studentListReportSessionAdapter.setSessions(sessions);

                boolean isEmpty = sessions.isEmpty();
                binding.rvSessions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.courseInfo().observe(getViewLifecycleOwner(), courseInfo -> {
            if (courseInfo != null) {
                binding.tvStudentReportListSessionGrade.setText(courseInfo.getGrade());
                binding.tvStudentReportListSessionSubject.setText(courseInfo.getCourseName());
                binding.tvStudentReportListSessionCount.setText(courseInfo.getSessionCountText());
            }
        });

        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d("SessionList", message);
            }
        });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
