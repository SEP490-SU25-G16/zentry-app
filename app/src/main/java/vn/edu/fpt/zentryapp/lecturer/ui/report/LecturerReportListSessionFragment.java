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

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerReportListSessionBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.LecturerListSessionDetailAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.OverviewSession;

public class LecturerReportListSessionFragment extends Fragment implements LecturerListSessionDetailAdapter.OnSessionDetailClickListener {

    private FragmentLecturerReportListSessionBinding binding;
    private LecturerReportListSessionViewModel viewModel;
    private LecturerListSessionDetailAdapter sessionAdapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerReportListSessionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Get data from arguments
        String courseCode = getArguments() != null ? getArguments().getString("courseCode", "") : "";
        String className = getArguments() != null ? getArguments().getString("className", "") : "";
        String courseName = getArguments() != null ? getArguments().getString("courseName", "") : "";

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerReportListSessionViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager, courseCode, className, courseName);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        sessionAdapter = new LecturerListSessionDetailAdapter();
        sessionAdapter.setOnSessionDetailClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rvSessions.setLayoutManager(layoutManager);
        binding.rvSessions.setAdapter(sessionAdapter);

        // Add divider
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), layoutManager.getOrientation());
        binding.rvSessions.addItemDecoration(divider);
    }

    private void setupClickListeners() {
        // Back button
        binding.ivReportBack.setOnClickListener(v -> navController.navigateUp());
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe course info
        viewModel.courseInfo().observe(getViewLifecycleOwner(), courseInfo -> {
            if (courseInfo != null) {
                binding.tvReportGrade.setText(courseInfo.getGradeDisplay());
                binding.tvReportSubject.setText(courseInfo.getCourseName());
                binding.tvReportStudentCount.setText(courseInfo.getStudentCount());
                binding.tvReportSessionCount.setText(courseInfo.getSessionProgress());
            }
        });

        // Observe sessions
        viewModel.sessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                sessionAdapter.setSessions(sessions);
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
    public void onSessionDetailClick(OverviewSession session) {
        Toast.makeText(requireContext(),
                "Opening " + session.getSessionTitle(), Toast.LENGTH_SHORT).show();

        Bundle args = new Bundle();
        args.putString("sessionId", session.getSessionId());
        args.putInt("sessionNumber", session.getSessionNumber());

        navController.navigate(R.id.action_listSession_to_sessionDetail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
