package vn.edu.fpt.zentryapp.student.ui.report;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import vn.edu.fpt.zentryapp.databinding.FragmentStudentReportListSessionBinding;
import vn.edu.fpt.zentryapp.student.adapter.SessionAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.Session;

public class StudentReportListSessionFragment extends Fragment implements SessionAdapter.OnSessionClickListener {

    private FragmentStudentReportListSessionBinding binding;
    private StudentReportListSessionViewModel viewModel;
    private SessionAdapter sessionAdapter;
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
        sessionAdapter = new SessionAdapter();
//        sessionAdapter.setOnSessionClickListener(this);

        binding.rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessions.setAdapter(sessionAdapter);
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
                sessionAdapter.setSessions(sessions);

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

        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();

                if (error.contains("network") || error.contains("connection")) {
                    showRetryDialog();
                }
            }
        });
    }

    private void showRetryDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Connection Error")
                .setMessage("Unable to load sessions. Would you like to retry?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    String courseId = getArguments() != null ?
                            getArguments().getString("courseId", "MATH101") : "MATH101";
                    viewModel.loadSessions(courseId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onSessionClick(Session session) {
//        Toast.makeText(requireContext(), "Clicked: " + session.getTitle(), Toast.LENGTH_SHORT).show();
//
//        Bundle args = new Bundle();
//        args.putString("sessionId", session.getId());
//        args.putString("sessionTitle", session.getTitle());
//        navController.navigate(R.id.action_listSession_to_sessionDetail, args);
//
//        viewModel.onSessionClicked(session);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
