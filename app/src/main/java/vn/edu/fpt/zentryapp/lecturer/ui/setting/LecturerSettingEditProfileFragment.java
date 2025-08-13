package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerSettingEditProfileBinding;

public class LecturerSettingEditProfileFragment extends Fragment {

    private FragmentLecturerSettingEditProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLecturerSettingEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivEditProfileBack.setOnClickListener(v -> requireActivity().onBackPressed());


        // Xử lý nút Save để lưu thay đổi
        binding.btnEditProfileSave.setOnClickListener(v -> {
            String newPhone = binding.edtEditProfilePhone.getText().toString().trim();

            // Validate số điện thoại (ví dụ: không được để trống)
            if (TextUtils.isEmpty(newPhone)) {
                binding.edtEditProfilePhone.setError("Phone number cannot be empty");
                return;
            }

            // TODO: Gọi API lưu thay đổi profile với số điện thoại mới và avatar nếu có thay đổi

            // Sau khi lưu thành công, quay lại màn hình profile overview
            requireActivity().onBackPressed();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
