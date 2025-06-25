package com.zentry.app.ui.fragment.lecture;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.zentry.app.R;
import com.zentry.app.viewmodel.AuthViewModel;
import com.zentry.app.navigation.AreaManager;

public class LectureHomeFragment extends Fragment {
    private TextView tvWelcome;
    private Button btnLogout;
    private Button btnCreateLecture;
    private Button btnManageAttendance;
    private Button btnViewReports;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lecture_home, container, false);

        initViews(view);
        initViewModel();
        setupClickListeners();
        loadUserInfo();

        return view;
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tv_welcome);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnCreateLecture = view.findViewById(R.id.btn_create_lecture);
        btnManageAttendance = view.findViewById(R.id.btn_manage_attendance);
        btnViewReports = view.findViewById(R.id.btn_view_reports);
    }

    private void initViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> handleLogout());
        btnCreateLecture.setOnClickListener(v -> handleCreateLecture());
        btnManageAttendance.setOnClickListener(v -> handleManageAttendance());
        btnViewReports.setOnClickListener(v -> handleViewReports());
    }

    private void loadUserInfo() {
        // Lấy thông tin user từ token hoặc SharedPreferences
        authViewModel.getUserToken().observe(getViewLifecycleOwner(), tokenModel -> {
            if (tokenModel != null) {
                String welcomeMessage = "Welcome, Lecturer!";
                // Nếu có thông tin user name trong token
                // welcomeMessage = "Welcome, Prof. " + tokenModel.getUserName() + "!";
                tvWelcome.setText(welcomeMessage);
            }
        });
    }

    private void handleLogout() {
        // Clear user session
        authViewModel.logout();

        // Navigate back to sign in
        AreaManager.logout(this);

        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    private void handleCreateLecture() {
        // Navigate to create lecture form
        Toast.makeText(getContext(), "Create Lecture feature coming soon!", Toast.LENGTH_SHORT).show();

        // Implement navigation to create lecture fragment
        // NavHostFragment.findNavController(this)
        //     .navigate(R.id.action_lectureHome_to_createLecture);
    }

    private void handleManageAttendance() {
        // Navigate to attendance management
        Toast.makeText(getContext(), "Manage Attendance feature coming soon!", Toast.LENGTH_SHORT).show();

        // Implement navigation to attendance management fragment
        // NavHostFragment.findNavController(this)
        //     .navigate(R.id.action_lectureHome_to_manageAttendance);
    }

    private void handleViewReports() {
        // Navigate to reports and analytics
        Toast.makeText(getContext(), "View Reports feature coming soon!", Toast.LENGTH_SHORT).show();

        // Implement navigation to reports fragment
        // NavHostFragment.findNavController(this)
        //     .navigate(R.id.action_lectureHome_to_reports);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Verify user is still authenticated and has lecturer role
        if (!authViewModel.isLoggedIn() || !authViewModel.isLecturer()) {
            AreaManager.logout(this);
        }
    }
}