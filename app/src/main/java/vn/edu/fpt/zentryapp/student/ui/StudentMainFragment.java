package vn.edu.fpt.zentryapp.student.ui;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (backCallback != null) {
            backCallback.remove();
        }
        binding = null;
    }
}
