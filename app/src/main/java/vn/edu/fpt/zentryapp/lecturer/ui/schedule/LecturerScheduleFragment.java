package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

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

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerScheduleBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.LecturerScheduleClassSectionAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleClassSection;

public class LecturerScheduleFragment extends Fragment implements LecturerScheduleClassSectionAdapter.OnSessionActionListener {

    private static final String TAG = "LecturerScheduleFragment";

    private FragmentLecturerScheduleBinding binding;
    private LecturerScheduleViewModel viewModel;
    private LecturerScheduleClassSectionAdapter sessionAdapter;
    private NavController navController;
    private AuthManager authManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerScheduleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);
        authManager = AuthManager.getInstance(requireContext());

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerScheduleViewModel.class);
        viewModel.init(requireContext(), authManager);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        sessionAdapter = new LecturerScheduleClassSectionAdapter(authManager);
        sessionAdapter.setOnSessionActionListener(this);

        binding.rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessions.setAdapter(sessionAdapter);
    }

    private void setupClickListeners() {
        // Calendar navigation
        binding.tvScheduleCalendar.setOnClickListener(v ->
                navController.navigate(R.id.action_schedule_to_calendar)
        );
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe sessions
        viewModel.todaySessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                sessionAdapter.setSessions(sessions);
                Log.d(TAG, "Updated sessions list: " + sessions.size() + " items");
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
    public void onSessionClick(LecturerScheduleClassSection session) {
        // Navigate to session detail for Active (ongoing), Completed
        String status = session.getSessionStatus();
        if ("Active".equals(status) || "Completed".equals(status)) {
            navigateToSessionDetail(session);
        } else {
            Toast.makeText(requireContext(), "Session details not available yet", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStartSession(LecturerScheduleClassSection session) {
        // Call ViewModel to start session via API
        viewModel.startSession(session);
    }
    private void navigateToSessionDetail(LecturerScheduleClassSection session) {
        try {
            Bundle args = new Bundle();
            args.putSerializable("session", session);
            navController.navigate(R.id.action_schedule_to_classDetail, args);
            Log.d(TAG, "Navigating to session detail: " + session.getSessionId());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cannot open session details", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Navigation error", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
