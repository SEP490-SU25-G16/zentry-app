package vn.edu.fpt.zentryapp.lecturer.ui.schedule;


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

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerScheduleCalendarBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.LecturerCalendarClassSectionAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CalendarSession;

public class LecturerScheduleCalendarFragment extends Fragment implements LecturerCalendarClassSectionAdapter.OnLecturerCalendarClassSectionListener {

    private FragmentLecturerScheduleCalendarBinding binding;
    private LecturerScheduleCalendarViewModel viewModel;
    private LecturerCalendarClassSectionAdapter sessionAdapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerScheduleCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerScheduleCalendarViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager);

        setupRecyclerView();
        setupCalendarListener();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        sessionAdapter = new LecturerCalendarClassSectionAdapter();
        sessionAdapter.setOnSessionClickListener(this);

        binding.rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessions.setAdapter(sessionAdapter);
    }

    private void setupCalendarListener() {
        // Listen for date selection changes
        binding.calendarView.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            // Load sessions for selected date
            viewModel.loadSessionsForDate(year, month, dayOfMonth);
        });
    }

    private void setupClickListeners() {
        // Back button
        binding.ivScheduleCalendarBack.setOnClickListener(v -> navController.navigateUp());
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe sessions
        viewModel.sessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                sessionAdapter.setSessions(sessions);
            }
        });


        // Observe has sessions for date
        viewModel.hasSessionsForDate().observe(getViewLifecycleOwner(), hasSessions -> {
            if (hasSessions != null) {
                binding.rvSessions.setVisibility(hasSessions ? View.VISIBLE : View.GONE);
                binding.llNoSessions.setVisibility(hasSessions ? View.GONE : View.VISIBLE);
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
    public void onSessionClick(CalendarSession session) {
        Toast.makeText(requireContext(),
                "Session: " + session.getCourseName() + " at " + session.getStartTimeDisplay(),
                Toast.LENGTH_SHORT).show();

        // TODO: Navigate to session detail if needed
        // Bundle args = new Bundle();
        // args.putString("sessionId", session.getSessionId());
        // navController.navigate(R.id.action_calendar_to_sessionDetail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
