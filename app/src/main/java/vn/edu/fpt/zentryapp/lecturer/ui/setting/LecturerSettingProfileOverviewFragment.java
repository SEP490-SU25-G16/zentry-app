package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerSettingProfileOverviewBinding;


public class LecturerSettingProfileOverviewFragment extends Fragment {

    private FragmentLecturerSettingProfileOverviewBinding binding;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLecturerSettingProfileOverviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivProfileOverviewBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // TODO: Hiển thị dữ liệu profile lấy từ args, ViewModel hoặc API
        // Ví dụ:
        // binding.tvProfileOverviewFullName.setText(userFullName);
        // binding.tvProfileOverviewUsername.setText(username);
        // binding.tvProfileOverviewDOB.setText(dateOfBirth);
        // binding.tvProfileOverviewEmail.setText(email);
        // binding.tvProfileOverviewPhone.setText(phone);

        // Xử lý nút Edit Profile để điều hướng sang màn hình chỉnh sửa profile
//        binding.btnEditProfile.setOnClickListener(v ->
//                navController.navigate(R.id.action_profileOverview_to_editProfile)
//        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
