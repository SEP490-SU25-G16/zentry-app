package vn.edu.fpt.zentryapp.student.ui.schedule;

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
import vn.edu.fpt.zentryapp.databinding.FragmentStudentScheduleBinding;
import vn.edu.fpt.zentryapp.student.adapter.StudentScheduleClassSectionAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleClassSection;

public class StudentScheduleFragment extends Fragment implements StudentScheduleClassSectionAdapter.OnSessionActionListener {

    private static final String TAG = "StudentScheduleFragment";

    private FragmentStudentScheduleBinding binding;
    private StudentScheduleViewModel viewModel;
    private StudentScheduleClassSectionAdapter scheduleAdapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentScheduleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentScheduleViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        AuthManager authManager = AuthManager.getInstance(requireContext());
        scheduleAdapter = new StudentScheduleClassSectionAdapter(authManager);

        scheduleAdapter.setOnSessionActionListener(this);

        binding.rvStudentSchedules.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStudentSchedules.setAdapter(scheduleAdapter);
    }


    private void setupClickListeners() {
        // Calendar click
        binding.tvStudentScheduleCalendar.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Calendar clicked", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to calendar
        });

        // Notification click
        binding.ivStudentNotification.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Notification clicked", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to notifications
        });

        // Search functionality
        binding.ivStudentSearch.setOnClickListener(v -> {
            String query = binding.etStudentSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                // TODO: Implement search
                Toast.makeText(requireContext(), "Searching: " + query, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModel() {
        // Loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Sessions data
        viewModel.sessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                scheduleAdapter.setSessions(sessions);

                // Show/hide empty state
                boolean isEmpty = sessions.isEmpty();
                binding.rvStudentSchedules.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                Log.d(TAG, "Updated UI with " + sessions.size() + " sessions");
            }
        });

        // Error handling
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Implement OnSessionActionListener interface
    @Override
    public void onSessionClick(StudentScheduleClassSection session) {
        // Navigate to class detail with session object
        Bundle args = new Bundle();
        args.putSerializable("session", session);

        navController.navigate(R.id.action_studentSchedule_to_classDetail, args);

        Log.d(TAG, "Navigating to class detail for session: " + session.getSessionId());
    }

    @Override
    public void onJoinSession(StudentScheduleClassSection session) {
        Log.d(TAG, "Student joined session: " + session.getSessionId());
        //TODO: vào màn hình detail nếu thành công
        // Refresh data after joining
        viewModel.refreshSessions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
