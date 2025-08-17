package vn.edu.fpt.zentryapp.student.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentMainBinding;
import vn.edu.fpt.zentryapp.helper.BottomNavigationHelper;

public class StudentMainFragment extends Fragment {

    private FragmentStudentMainBinding binding;
    private BottomNavigationHelper navigationHelper;
    private OnBackPressedCallback backCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo BottomNavigationHelper
        navigationHelper = new BottomNavigationHelper(
                getChildFragmentManager(),
                R.id.student_nav_host_fragment,
                binding.bottomNavigationStudent
        );

        navigationHelper.addTab(
                R.id.navigation_home,
                R.navigation.nav_graph_tab_home_student
        );
        navigationHelper.addTab(
                R.id.navigation_schedule,
                R.navigation.nav_graph_tab_schedule_student
        );
        navigationHelper.addTab(
                R.id.navigation_report,
                R.navigation.nav_graph_tab_report_student
        );
        navigationHelper.addTab(
                R.id.navigation_profile,
                R.navigation.nav_graph_tab_setting_student
        );

        navigationHelper.selectInitialTab(R.id.navigation_home);

        // ✅ NEW: Check for pending Face ID verification and handle it
        checkAndHandlePendingVerification();

        // Đăng ký callback xử lý Back hệ thống
        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!navigationHelper.handleBackPress()) {
                    requireActivity().finish();
                }
            }
        };
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);
    }

    // ✅ NEW: Method to hide bottom navigation bar (for Face ID fragments)
    public void hideBottomNavigation() {
        if (binding != null && binding.bottomNavigationStudent != null) {
            binding.bottomNavigationStudent.setVisibility(View.GONE);
            
            // Remove bottom padding from fragment container to use full screen
            if (binding.studentNavHostFragment != null) {
                binding.studentNavHostFragment.setPadding(
                    binding.studentNavHostFragment.getPaddingLeft(),
                    binding.studentNavHostFragment.getPaddingTop(),
                    binding.studentNavHostFragment.getPaddingRight(),
                    0 // Remove bottom padding
                );
            }
        }
    }

    // ✅ NEW: Method to show bottom navigation bar (when returning from Face ID fragments)
    public void showBottomNavigation() {
        if (binding != null && binding.bottomNavigationStudent != null) {
            binding.bottomNavigationStudent.setVisibility(View.VISIBLE);
            
            // Restore bottom padding for fragment container
            if (binding.studentNavHostFragment != null) {
                binding.studentNavHostFragment.setPadding(
                    binding.studentNavHostFragment.getPaddingLeft(),
                    binding.studentNavHostFragment.getPaddingTop(),
                    binding.studentNavHostFragment.getPaddingRight(),
                    56 // Restore 56dp bottom padding
                );
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (backCallback != null) {
            backCallback.remove();
        }
        binding = null;
    }
    
    // ✅ NEW: Check for pending Face ID verification and navigate to settings tab
    private void checkAndHandlePendingVerification() {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("face_verification", android.content.Context.MODE_PRIVATE);
            String requestId = prefs.getString("pending_request_id", null);
            String sessionId = prefs.getString("pending_session_id", null);
            long timestamp = prefs.getLong("pending_timestamp", 0);
            
            // Check if we have pending verification (within last 30 seconds)
            if (requestId != null && sessionId != null && 
                (System.currentTimeMillis() - timestamp) < 30000) {
                
                android.util.Log.d("StudentMainFragment", "🔗 Found pending Face ID verification: " + requestId);
                
                // Clear the stored args
                prefs.edit().clear().apply();
                
                // ✅ NEW: Launch dedicated Activity instead of navigating to fragment
                // This ensures full-screen experience without navbar (consistent with register/update)
                try {
                    Intent verifyIntent = new Intent(requireContext(), 
                            vn.edu.fpt.zentryapp.faceid.ui.setting.StudentSettingVerifyFaceIdActivity.class);
                    verifyIntent.putExtra("requestId", requestId);
                    verifyIntent.putExtra("sessionId", sessionId);
                    
                    // Add flags to ensure proper navigation
                    verifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    startActivity(verifyIntent);
                    
                    android.util.Log.d("StudentMainFragment", "✅ Successfully launched Face ID verification Activity");
                } catch (Exception e) {
                    android.util.Log.e("StudentMainFragment", "❌ Failed to launch Face ID verification Activity", e);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("StudentMainFragment", "❌ Error checking pending verification", e);
        }
    }
}
