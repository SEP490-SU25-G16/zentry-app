package vn.edu.fpt.zentryapp.student.ui.setting;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingProfileOverviewBinding;

public class StudentSettingProfileOverviewFragment extends Fragment {

    private FragmentStudentSettingProfileOverviewBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentStudentSettingProfileOverviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivStudentSettingProfileOverviewBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // TODO: Hiển thị dữ liệu profile lấy từ args, ViewModel hoặc API
        // Ví dụ:
        // binding.tvStudentSettingProfileOverviewFullName.setText(userFullName);
        // binding.tvStudentSettingProfileOverviewUsername.setText(username);
        // binding.tvStudentSettingProfileOverviewDOB.setText(dateOfBirth);
        // binding.tvStudentSettingProfileOverviewEmail.setText(email);
        // binding.tvStudentSettingProfileOverviewPhone.setText(phone);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
