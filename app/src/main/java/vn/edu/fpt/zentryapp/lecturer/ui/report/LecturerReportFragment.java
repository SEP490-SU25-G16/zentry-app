package vn.edu.fpt.zentryapp.lecturer.ui.report;

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
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerReportBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.SessionAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Session;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;

/**
 * Màn hình này gần giống với màn hình Home nhưng cái này là nó hiện thị ra list report session hôm nay
 * ở dạng demo, không phải là chi tiết để khác biệt so với màn hình home là overview cho cả kỳ
 * <p>
 * Màn hình này khi ấn vào thì sẽ thấy được danh sách các cái session trong lớp đó chi tiết, có thể sắp xếp
 * theo chiều mới nhất (ấn cho nhanh)
 */

public class LecturerReportFragment extends Fragment implements SessionAdapter.OnSessionClickListener {

    private FragmentLecturerReportBinding binding;
    private LecturerReportViewModel viewModel;
    private NotificationViewModel notificationViewModel;
    private SessionAdapter sessionAdapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerReportViewModel.class);
        notificationViewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager);
        
        // Load notifications để có dữ liệu cho badge
        notificationViewModel.loadNotifications();

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        sessionAdapter = new SessionAdapter();
        sessionAdapter.setOnSessionClickListener(this);

        binding.rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessions.setAdapter(sessionAdapter);
    }

    private void setupClickListeners() {
        // Notification button
        binding.btnNotification.setOnClickListener(v -> {
            // TODO: Navigate to notifications screen
        });
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
                binding.tvHomeGreeting.setText(greeting);
            }
        });

        // Observe current date
        viewModel.currentDate().observe(getViewLifecycleOwner(), date -> {
            if (date != null) {
//                binding.tvCurrentDate.setText("Today, " + date);
            }
        });

        // Observe user profile
        viewModel.userProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                android.util.Log.d("LecturerReport",
                        "User loaded: " + profile.getName() + " (" + profile.getRole() + ")");
            }
        });

        // Observe errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        
        // Observe notification unseen count để hiển thị badge
        notificationViewModel.getUnseenCount().observe(getViewLifecycleOwner(), unseenCount -> {
            if (unseenCount != null && unseenCount > 0) {
                binding.tvNotificationBadge.setVisibility(View.VISIBLE);
                binding.tvNotificationBadge.setText(String.valueOf(unseenCount));
            } else {
                binding.tvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onSessionClick(Session session) {
        Toast.makeText(requireContext(), "Session clicked: " + session.getCourseName(), Toast.LENGTH_SHORT).show();

        // Navigate to list all sessions of this course and class
        Bundle args = new Bundle();
        args.putString("courseCode", session.getCourseCode());
        args.putString("className", session.getClassName());
        args.putString("courseName", session.getCourseName());
        navController.navigate(R.id.action_report_to_listSession, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}