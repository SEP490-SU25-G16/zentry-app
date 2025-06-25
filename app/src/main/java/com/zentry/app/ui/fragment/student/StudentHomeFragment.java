package com.zentry.app.ui.fragment.student;

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

public class StudentHomeFragment extends Fragment {
    private TextView tvWelcome;
    private Button btnLogout;
    private Button btnViewLectures;
    private Button btnViewAttendance;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_home, container, false);

        initViews(view);
        initViewModel();
        setupClickListeners();
        loadUserInfo();

        return view;
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tv_welcome);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnViewLectures = view.findViewById(R.id.btn_view_lectures);
        btnViewAttendance = view.findViewById(R.id.btn_view_attendance);
    }

    private void initViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> handleLogout());
        btnViewLectures.setOnClickListener(v -> handleViewLectures());
        btnViewAttendance.setOnClickListener(v -> handleViewAttendance());
    }

    private void loadUserInfo() {
        // Lấy thông tin user từ token hoặc SharedPreferences
        authViewModel.getUserToken().observe(getViewLifecycleOwner(), tokenModel -> {
            if (tokenModel != null) {
                String welcomeMessage = "Welcome, Student!";
                // Nếu có thông tin user name trong token
                // welcomeMessage = "Welcome, " + tokenModel.getUserName() + "!";
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

    private void handleViewLectures() {
        // Navigate to lectures list for student
        Toast.makeText(getContext(), "View Lectures feature coming soon!", Toast.LENGTH_SHORT).show();

        // Implement navigation to lectures fragment
        // NavHostFragment.findNavController(this)
        //     .navigate(R.id.action_studentHome_to_lecturesList);
    }

    private void handleViewAttendance() {
        // Navigate to attendance history for student
        Toast.makeText(getContext(), "View Attendance feature coming soon!", Toast.LENGTH_SHORT).show();

        // Implement navigation to attendance fragment
        // NavHostFragment.findNavController(this)
        //     .navigate(R.id.action_studentHome_to_attendanceHistory);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Verify user is still authenticated and has student role
        if (!authViewModel.isLoggedIn() || !authViewModel.isStudent()) {
            AreaManager.logout(this);
        }
    }
}