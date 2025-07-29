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
import vn.edu.fpt.zentryapp.lecturer.adapter.ScheduleSessionAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleSession;

public class LecturerScheduleFragment extends Fragment implements ScheduleSessionAdapter.OnSessionActionListener {

    private FragmentLecturerScheduleBinding binding;
    private LecturerScheduleViewModel viewModel;
    private ScheduleSessionAdapter sessionAdapter;
    private LecturerScheduleSession currentStartingSession = null;
    private NavController navController;
    AuthManager authManager;

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

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerScheduleViewModel.class);
        authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);
        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        sessionAdapter = new ScheduleSessionAdapter(authManager);
        sessionAdapter.setOnSessionActionListener(this);

        binding.rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessions.setAdapter(sessionAdapter);
    }

    private void setupClickListeners() {
        // Calendar navigation
        binding.tvScheduleCalendar.setOnClickListener(v ->
                navController.navigate(R.id.action_schedule_to_calendar)
        );

        // See all classes
//        binding.tvScheduleSeeAll.setOnClickListener(v -> {
//            Toast.makeText(requireContext(), "See all classes feature", Toast.LENGTH_SHORT).show();
//            // TODO: Navigate to full schedule view
//        });

        // Notification button
//        binding.btnScheduleNotification.setOnClickListener(v -> {
//            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show();
//            // TODO: Navigate to notifications
//        });
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
            }
        });

        // Observe greeting
        viewModel.greeting().observe(getViewLifecycleOwner(), greeting -> {
            if (greeting != null) {
                binding.tvScheduleGreeting.setText(greeting);
            }
        });

        // Observe current date
        viewModel.currentDate().observe(getViewLifecycleOwner(), date -> {
            if (date != null) {
                // binding.tvCurrentDate.setText("Today, " + date);
            }
        });

        // Observe user profile
        viewModel.userProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                // Update avatar
                // binding.ivScheduleAvatar.setImageResource(R.drawable.ic_launcher_foreground);
            }
        });

        // Observe errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.isStartingSession().observe(getViewLifecycleOwner(), this::handleStartingSessionState);

        viewModel.startSessionSuccess().observe(getViewLifecycleOwner(), this::handleStartSessionSuccess);
    }

    /**
     * 🔧 Handle starting session loading state
     */
    private void handleStartingSessionState(Boolean isStarting) {
        if (isStarting != null) {
            Log.d("LecturerSchedule", "Starting session: " + isStarting);
        }
    }

    /**
     * 🔧 Handle start session success với reference chính xác
     */
    private void handleStartSessionSuccess(String successMessage) {
        if (successMessage != null && !successMessage.trim().isEmpty()) {
            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();

            // 🔧 Navigate đến detail của session vừa start
            if (currentStartingSession != null) {
                // Update status local trước khi navigate
                currentStartingSession.setStatus("Active");
                currentStartingSession.setCanViewDetail(true);

                // Navigate to detail
                navigateToSessionDetail(currentStartingSession);

                // Clear reference
                currentStartingSession = null;
            }

            // Clear message
            viewModel.clearStartSessionSuccess();

            Log.d("LecturerSchedule", "Auto-navigated to session detail after successful start");
        }
    }


    @Override
    public void onSessionClick(LecturerScheduleSession session) {
        if (session == null) {
            Toast.makeText(requireContext(), "Invalid session", Toast.LENGTH_SHORT).show();
            return;
        }

        if (session.isCanViewDetail()) {
            navigateToSessionDetail(session);
        } else {
            Toast.makeText(requireContext(),
                    "Session details not available yet", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStartSession(LecturerScheduleSession session) {
        if (session == null) {
            Toast.makeText(requireContext(), "Invalid session", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔧 LƯU reference của session đang start
        currentStartingSession = session;

        // 🔧 GỌI ViewModel để start session via API
        viewModel.startSessionViaAPI(session);
    }
    /**
     * 🔧 Navigate to session detail screen với đầy đủ thông tin
     */
    private void navigateToSessionDetail(LecturerScheduleSession session) {
        try {
            Bundle args = new Bundle();
            args.putString("sessionId", session.getSessionId());
            args.putString("courseCode", session.getCourseCode());
            args.putString("courseName", session.getCourseName());
            args.putString("room", session.getRoom());
            args.putLong("startTime", session.getStartTime().getTime());
            args.putLong("endTime", session.getEndTime().getTime());
            args.putString("status", session.getStatus()); // 🔧 THÊM status để detail screen biết trạng thái

            navController.navigate(R.id.action_schedule_to_classDetail, args);

            Log.d("LecturerSchedule", "Navigating to session detail: " + session.getSessionId());

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cannot open session details", Toast.LENGTH_SHORT).show();
            Log.e("LecturerSchedule", "Navigation error", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
