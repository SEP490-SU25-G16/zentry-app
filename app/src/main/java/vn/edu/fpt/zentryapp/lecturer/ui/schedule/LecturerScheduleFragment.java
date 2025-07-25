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

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerScheduleBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.ScheduleSessionAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ScheduleSession;

public class LecturerScheduleFragment extends Fragment implements ScheduleSessionAdapter.OnSessionActionListener {

    private FragmentLecturerScheduleBinding binding;
    private LecturerScheduleViewModel viewModel;
    private ScheduleSessionAdapter sessionAdapter;
    private NavController navController;

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
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        sessionAdapter = new ScheduleSessionAdapter();
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
    }

    @Override
    public void onSessionClick(ScheduleSession session) {
        if (session.isCanViewDetail()) {
            // Navigate to session detail
            Bundle args = new Bundle();
            args.putString("sessionId", session.getSessionId());
            args.putString("courseCode", session.getCourseCode());
            args.putString("courseName", session.getCourseName());
            args.putString("className", session.getClassName());

            // Navigate to existing session detail screen
            navController.navigate(R.id.action_schedule_to_classDetail, args);
        } else {
            Toast.makeText(requireContext(),
                    "Session detail not available yet", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStartInstantClick(ScheduleSession session) {
        if (session.isCanStartInstant()) {
            // Handle start instant class
            viewModel.startInstantClass(session);

            Toast.makeText(requireContext(),
                    "Starting instant class: " + session.getCourseName(), Toast.LENGTH_SHORT).show();

            // TODO: Navigate to teaching interface or attendance screen
            // Bundle args = new Bundle();
            // args.putString("sessionId", session.getSessionId());
            // navController.navigate(R.id.action_schedule_to_instantClass, args);
        } else {
            Toast.makeText(requireContext(),
                    "Cannot start this session at the moment", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
