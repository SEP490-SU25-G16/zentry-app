package vn.edu.fpt.zentryapp.lecturer.ui;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerMainBinding;
import vn.edu.fpt.zentryapp.helper.BottomNavigationHelper;

public class LecturerMainFragment extends Fragment {

    private static final String TAG = "LecturerMainFragment";

    private FragmentLecturerMainBinding binding;
    private BottomNavigationHelper navigationHelper;
    private OnBackPressedCallback backCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "=== onCreateView started ===");

        try {
            binding = FragmentLecturerMainBinding.inflate(inflater, container, false);
            Log.d(TAG, "Binding inflated successfully");

            Log.d(TAG, "onCreateView completed successfully");
            return binding.getRoot();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "=== onViewCreated started ===");

        try {
            super.onViewCreated(view, savedInstanceState);
            Log.d(TAG, "super.onViewCreated completed");

            // Khởi tạo helper mỗi tab là một stack riêng
            Log.d(TAG, "Initializing BottomNavigationHelper");
            navigationHelper = new BottomNavigationHelper(
                    getChildFragmentManager(),
                    R.id.lecturer_nav_host_fragment,
                    binding.bottomNavigationLecturer
            );
            Log.d(TAG, "BottomNavigationHelper created successfully");

            // Add tabs
            Log.d(TAG, "Adding navigation tabs");

            Log.d(TAG, "Adding HOME tab");
            navigationHelper.addTab(R.id.navigation_home,
                    R.navigation.nav_graph_tab_home_lecturer);

            Log.d(TAG, "Adding SCHEDULE tab");
            navigationHelper.addTab(R.id.navigation_schedule,
                    R.navigation.nav_graph_tab_schedule_lecturer);

            Log.d(TAG, "Adding REPORT tab");
            navigationHelper.addTab(R.id.navigation_report,
                    R.navigation.nav_graph_tab_report_lecturer);

            Log.d(TAG, "Adding PROFILE tab");
            navigationHelper.addTab(R.id.navigation_profile,
                    R.navigation.nav_graph_tab_setting_lecturer);

            Log.d(TAG, "All tabs added successfully");

            // Select initial tab
            Log.d(TAG, "Selecting initial tab: HOME");
            navigationHelper.selectInitialTab(R.id.navigation_home);
            Log.d(TAG, "Initial tab selected successfully");

            // Đăng ký callback xử lý Back hệ thống cho stack hiện tại
            Log.d(TAG, "Setting up back press callback");
            backCallback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    Log.d(TAG, "Back press detected");

                    if (!navigationHelper.handleBackPress()) {
                        Log.d(TAG, "Navigation helper cannot handle back press, finishing activity");
                        requireActivity().finish();
                    } else {
                        Log.d(TAG, "Navigation helper handled back press successfully");
                    }
                }
            };

            requireActivity()
                    .getOnBackPressedDispatcher()
                    .addCallback(getViewLifecycleOwner(), backCallback);

            Log.d(TAG, "Back press callback registered successfully");
            Log.d(TAG, "=== onViewCreated completed successfully ===");

        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "=== onDestroyView started ===");

        try {
            super.onDestroyView();
            Log.d(TAG, "super.onDestroyView completed");

            if (backCallback != null) {
                Log.d(TAG, "Removing back press callback");
                backCallback.remove();
                backCallback = null;
                Log.d(TAG, "Back press callback removed");
            } else {
                Log.d(TAG, "Back press callback was null, no removal needed");
            }

            Log.d(TAG, "Setting binding to null");
            binding = null;

            Log.d(TAG, "=== onDestroyView completed successfully ===");

        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroyView: " + e.getMessage(), e);
        }
    }
}
