package vn.edu.fpt.zentryapp.auth.ui;

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
import vn.edu.fpt.zentryapp.databinding.FragmentForgotPasswordSelectMethodBinding;

public class ForgotPasswordSelectMethodFragment extends Fragment {

    private FragmentForgotPasswordSelectMethodBinding binding;

    // Biến lưu trạng thái phương thức được chọn, ví dụ "email"
    private String selectedMethod = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentForgotPasswordSelectMethodBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);

        // Khi người dùng ấn nút back, quay lại màn hình trước đó
        binding.ivForgotPasswordBack.setOnClickListener(v -> navController.navigateUp());

        // Khi người dùng chọn phương thức Email
        binding.cardForgotPasswordOptionEmail.setOnClickListener(v -> {
            // Đánh dấu phương thức Email được chọn
            selectedMethod = "email";

            // Thay đổi trạng thái UI để hiển thị đã chọn
            binding.cardForgotPasswordOptionEmail.setChecked(true);

            // Nếu có nhiều phương thức, ở đây sẽ bỏ chọn các phương thức khác
            // Ví dụ: binding.cardOptionSms.setChecked(false);
            // Hiện tại chỉ có 1 phương thức nên không cần xử lý thêm
        });

        // Khi người dùng ấn nút Continue
        binding.btnForgotPasswordContinue.setOnClickListener(v -> {
            // Kiểm tra đã chọn ít nhất 1 phương thức chưa
            if (selectedMethod == null) {
                // TODO: Hiển thị cảnh báo yêu cầu chọn phương thức (ví dụ Toast hoặc Snackbar)
                return;
            }

            // Nếu đã chọn phương thức, chuyển sang màn hình nhập mã xác thực (Verify Code)
            navController.navigate(R.id.action_selectMethod_to_verifyCode);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
