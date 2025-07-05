package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerSettingBinding;

public class LecturerSettingFragment extends Fragment {

    private FragmentLecturerSettingBinding binding;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLecturerSettingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // Xử lý click Profile để điều hướng sang màn hình Profile Overview
        binding.llSettingRowProfile.setOnClickListener(v ->
                navController.navigate(R.id.action_setting_to_profileOverview)
        );

        // Xử lý click Notifications để điều hướng sang màn hình cài đặt thông báo
        binding.llSettingRowNotifications.setOnClickListener(v ->
                navController.navigate(R.id.action_setting_to_notification)
        );

        // Xử lý click Device để điều hướng sang màn hình thông tin hoặc đăng ký thiết bị
        binding.llSettingRowDevice.setOnClickListener(v ->
                navController.navigate(R.id.action_setting_to_deviceInfo)
        );

        // Xử lý click Logout
        binding.llSettingRowLogout.setOnClickListener(v -> {
            // TODO: gọi API logout hoặc xử lý đăng xuất
            // Sau khi đăng xuất thành công, điều hướng về màn hình đăng nhập
            // navController.navigate(R.id.action_global_loginFragment);
            // 1) Xoá token / session ở đây
            // Ví dụ: SessionManager.clear()

            // 2) Điều hướng về LoginFragment, xóa hết back stack
            NavController nav = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            nav.navigate(R.id.action_global_logout);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
