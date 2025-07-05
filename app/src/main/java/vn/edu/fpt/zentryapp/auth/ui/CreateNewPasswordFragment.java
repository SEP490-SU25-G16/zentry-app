package vn.edu.fpt.zentryapp.auth.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentCreateNewPasswordBinding;

public class CreateNewPasswordFragment extends Fragment {

    private FragmentCreateNewPasswordBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentCreateNewPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);

        // Khi người dùng ấn nút back, quay lại màn hình trước đó
        binding.ivCreatePasswordBack.setOnClickListener(v -> navController.navigateUp());

        // Khi người dùng ấn nút "Verify"
        binding.btnCreatePasswordContinue.setOnClickListener(v -> {
            // Xóa các thông báo lỗi cũ
            binding.tilCreatePasswordNew.setError(null);
            binding.tilCreatePasswordReType.setError(null);

            // Lấy giá trị từ hai trường nhập mật khẩu
            String newPwd = binding.etCreatePasswordNew.getText() != null
                    ? binding.etCreatePasswordNew.getText().toString().trim()
                    : "";
            String rePwd = binding.etCreatePasswordReType.getText() != null
                    ? binding.etCreatePasswordReType.getText().toString().trim()
                    : "";

            // Kiểm tra trường mật khẩu mới có rỗng không
            if (TextUtils.isEmpty(newPwd)) {
                binding.tilCreatePasswordNew.setError("Password không được để trống");
                return;
            }
            // Kiểm tra trường nhập lại mật khẩu có rỗng không
            if (TextUtils.isEmpty(rePwd)) {
                binding.tilCreatePasswordReType.setError("Nhập lại password");
                return;
            }
            // Nếu hai mật khẩu không trùng nhau, hiển thị lỗi
            if (!newPwd.equals(rePwd)) {
                binding.tilCreatePasswordReType.setError("Password không khớp");
                return;
            }

            // TODO: Gọi API để cập nhật mật khẩu mới
            // Nếu thành công, chuyển sang màn hình thông báo thành công
            navController.navigate(R.id.action_createPassword_to_success);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
