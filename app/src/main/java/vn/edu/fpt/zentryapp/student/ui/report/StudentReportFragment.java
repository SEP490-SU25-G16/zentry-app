package vn.edu.fpt.zentryapp.student.ui.report;

import android.annotation.SuppressLint;
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
import vn.edu.fpt.zentryapp.databinding.FragmentStudentReportBinding;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;
import vn.edu.fpt.zentryapp.student.adapter.StudentReportAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentReport;

public class StudentReportFragment extends Fragment implements StudentReportAdapter.OnReportClickListener {

    private FragmentStudentReportBinding binding;
    private StudentReportViewModel viewModel;
    private StudentReportAdapter reportAdapter;
    private NavController navController;
    private NotificationViewModel notificationViewModel;
    private static final String TAG = "StudentReportFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel với context
        viewModel = new ViewModelProvider(this).get(StudentReportViewModel.class);
        notificationViewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);
        
        // Load notifications để có dữ liệu cho badge
        notificationViewModel.loadNotifications();

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
        setupSearchBar();
        setupRefreshListener();
    }

    private void setupClickListeners() {
        // Notification button click handler
        binding.btnStudentReportNotification.setOnClickListener(v -> {
            try {
                if (navController.getCurrentDestination() != null && 
                    navController.getCurrentDestination().getAction(R.id.action_studentReport_to_notification) != null) {
                    navController.navigate(R.id.action_studentReport_to_notification);
                } else {
                    // Fallback navigation if action isn't available
                    Log.w(TAG, "Navigation action not available, using fallback");
                    Toast.makeText(requireContext(), "Đang chuyển đến thông báo...", Toast.LENGTH_SHORT).show();
                    
                    // Try to find notificationFragment by ID
                    navController.navigate(R.id.notificationFragment);
                }
            } catch (Exception e) {
                Log.e(TAG, "Navigation error: ", e);
                Toast.makeText(requireContext(), "Chức năng thông báo đang được phát triển", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRefreshListener() {
        // Add pull-to-refresh functionality if SwipeRefreshLayout exists
        // binding.swipeRefresh.setOnRefreshListener(() -> {
        //     viewModel.refreshReports();
        // });
    }
    
    private void setupSearchBar() {
        binding.etStudentSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Use ViewModel search instead of adapter filter
                viewModel.searchReports(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });

        binding.ivStudentSearch.setOnClickListener(v -> {
            String query = binding.etStudentSearch.getText().toString();
            viewModel.searchReports(query);
        });
    }

    private void setupRecyclerView() {
        reportAdapter = new StudentReportAdapter();
        reportAdapter.setOnReportClickListener(this);

        binding.rvReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReports.setAdapter(reportAdapter);
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            // Stop refresh animation if SwipeRefreshLayout exists
            // if (binding.swipeRefresh != null) {
            //     binding.swipeRefresh.setRefreshing(isLoading);
            // }
        });

        // Observe reports data
        viewModel.reports().observe(getViewLifecycleOwner(), reports -> {
            if (reports != null) {
                reportAdapter.setReports(reports);

                boolean isEmpty = reports.isEmpty();
                binding.rvReports.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                Log.d("StudentReportFragment", "Loaded " + reports.size() + " reports");
            }
        });

        // Observe errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                Log.e("StudentReportFragment", "Error: " + error);
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
    public void onReportClick(StudentReport report) {
        Log.d("StudentReportFragment", "Report clicked: " + report.getCourseName());

        // Show class details in toast (for debugging)
        @SuppressLint("DefaultLocale") String classInfo = String.format("%s\n%s\n%s\n%.1f%% Attendance",
                report.getClassName(),
                report.getClassInfo(),
                report.getLecturerDisplayName(),
                report.getAttendanceRate());

        // Pass StudentReport object via Serializable
        Bundle args = new Bundle();
        args.putSerializable("studentReport", report);

        navController.navigate(R.id.action_studentReport_to_listSession, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        if (viewModel != null) {
            viewModel.refreshReports();
        }
    }
}
