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
import vn.edu.fpt.zentryapp.lecturer.adapter.LecturerReportClassSectionAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerReportClassSection;
import vn.edu.fpt.zentryapp.lecturer.adapter.SessionAdapter;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ClassroomInfo;

/**
 * Màn hình này hiển thị list report các classroom/course với attendance percentage
 * Khi click vào một classroom sẽ navigate đến danh sách sessions chi tiết của lớp đó
 */
public class LecturerReportFragment extends Fragment implements LecturerReportClassSectionAdapter.OnLecturerReportClassSectionClickListener {

    private FragmentLecturerReportBinding binding;
    private LecturerReportViewModel viewModel;
    private LecturerReportClassSectionAdapter adapter;
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

        // Initialize ViewModel với context
        viewModel = new ViewModelProvider(this).get(LecturerReportViewModel.class);
        notificationViewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.initWithContext(requireContext(), authManager);

        // Load notifications để có dữ liệu cho badge
        notificationViewModel.loadNotifications();

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new LecturerReportClassSectionAdapter();
        adapter.setOnClassroomClickListener(this);

        binding.rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessions.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Notification button
        binding.btnNotification.setOnClickListener(v -> {
            // Navigate đến NotificationFragment (cùng fragment với student)
            try {
                if (navController.getCurrentDestination() != null && 
                    navController.getCurrentDestination().getAction(R.id.action_lecturerReport_to_notification) != null) {
                    navController.navigate(R.id.action_lecturerReport_to_notification);
                } else {
                    // Fallback navigation if action isn't available
                    android.util.Log.w("LecturerReport", "Navigation action not available, using fallback");

                    // Try to find notificationFragment by ID
                    navController.navigate(R.id.notificationFragment);
                }
            } catch (Exception e) {
            }
        });
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.rvSessions.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        // Observe classrooms data
        viewModel.classrooms().observe(getViewLifecycleOwner(), classrooms -> {
            if (classrooms != null) {
                adapter.setClassrooms(classrooms);
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
    public void onClassroomClick(LecturerReportClassSection classroom) {
        // Tạo ClassroomInfo object để pass qua Serializable
        ClassroomInfo classroomInfo = new ClassroomInfo(
                classroom.getClassId(),
                classroom.getCourseName(),
                classroom.getCourseCode(),
                classroom.getSectionCode(),
                classroom.getClassName(),
                classroom.getStudentCount(),
                classroom.getCompletedSessions(),
                classroom.getTotalSessions(),
                classroom.getAttendancePercentage()
        );

        // Navigate với Serializable object
        Bundle args = new Bundle();
        args.putSerializable("classroomInfo", classroomInfo);

        navController.navigate(R.id.action_report_to_listSession, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
