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
import vn.edu.fpt.zentryapp.student.adapter.ScheduleAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleSession;

public class StudentScheduleFragment extends Fragment implements ScheduleAdapter.OnScheduleClickListener {

    private FragmentStudentScheduleBinding binding;
    private StudentScheduleViewModel viewModel;
    private ScheduleAdapter scheduleAdapter;
    private NavController navController;
    private AuthManager authManager;

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
        authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        scheduleAdapter = new ScheduleAdapter(authManager);
        scheduleAdapter.setOnScheduleClickListener(this);

        binding.rvSchedules.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSchedules.setAdapter(scheduleAdapter);
    }

    private void setupClickListeners() {
        // Calendar click
        binding.tvStudentScheduleCalendar.setOnClickListener(v ->
                navController.navigate(R.id.action_studentSchedule_to_calendar)
        );

//        // See All click
//        binding.tvStudentScheduleSeeAll.setOnClickListener(v -> {
//            Toast.makeText(requireContext(), "See All clicked", Toast.LENGTH_SHORT).show();
//            // TODO: Navigate to full schedule list
//        });

        // Notification click
        binding.btnStudentScheduleNotification.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Notification clicked", Toast.LENGTH_SHORT).show();
            // TODO: Open notification screen/dialog
        });
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.schedules().observe(getViewLifecycleOwner(), schedules -> {
            if (schedules != null) {
                scheduleAdapter.setSchedules(schedules);

                boolean isEmpty = schedules.isEmpty();
                binding.rvSchedules.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.greeting().observe(getViewLifecycleOwner(), greeting -> {
            if (greeting != null) {
                binding.tvStudentScheduleGreeting.setText(greeting);
            }
        });

        viewModel.subGreeting().observe(getViewLifecycleOwner(), subGreeting -> {
            if (subGreeting != null) {
                binding.tvStudentScheduleSubGreeting.setText(subGreeting);
            }
        });

        viewModel.userProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {

            }
        });

        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d("StudentSchedule", message);
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
                .setMessage("Unable to load schedules. Would you like to retry?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    viewModel.loadSchedules();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onScheduleClick(StudentScheduleSession studentScheduleSession) {
        // 🔧 PASS data qua Bundle để tránh call API lấy thông tin cơ bản
        Bundle args = new Bundle();

        //   "CourseId": "ff128e22-09a6-4a7f-bda9-71600f5d2d54",
        //            "CourseCode": "CS673",
        //            "CourseName": "Introduction to Computer Science",

        // Core session info
        args.putString("sessionId", studentScheduleSession.getSessionId());
        args.putString("courseCode", studentScheduleSession.getCourseCode());
        args.putString("courseName", studentScheduleSession.getClassName());
        args.putString("sectionCode", studentScheduleSession.getGrade());
        args.putString("room", studentScheduleSession.getRoom());
        args.putString("lecturer", studentScheduleSession.getLecturer());

        // Timing info
        args.putString("startTime", studentScheduleSession.getStartTime());
        args.putString("endTime", studentScheduleSession.getEndTime());
        args.putString("dayOfWeek", studentScheduleSession.getDayOfWeek());

        navController.navigate(R.id.action_studentSchedule_to_classDetail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
