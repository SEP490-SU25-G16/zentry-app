package vn.edu.fpt.zentryapp.auth.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentForgotPasswordVerifyCodeBinding;

public class ForgotPasswordVerifyCodeFragment extends Fragment {

    private FragmentForgotPasswordVerifyCodeBinding binding;
    private static final long RESEND_INTERVAL_MS = 60 * 1000; // 60 giây đếm ngược trước khi được resend

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentForgotPasswordVerifyCodeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);

        // Khi người dùng ấn nút back, quay lại màn hình trước đó
        binding.ivVerifyCodeBack.setOnClickListener(v -> navController.navigateUp());

        // Bắt đầu đếm ngược thời gian cho phép gửi lại mã
        startResendCountdown();

        // Khi người dùng ấn nút Verify để xác thực mã OTP
        binding.btnVerifyCodeVerify.setOnClickListener(v -> {
            // Lấy mã OTP người dùng nhập
            String code = binding.pvVerifyCodeInput.getText() != null
                    ? binding.pvVerifyCodeInput.getText().toString().trim()
                    : "";

            // Kiểm tra mã OTP có hợp lệ (không rỗng, đủ 4 ký tự)
            if (TextUtils.isEmpty(code) || code.length() < 4) {
                // Hiển thị lỗi cho người dùng
                binding.tvVerifyCodeResendTimer.setError("Enter valid code");
                return;
            }

            // TODO: Gọi API verify code ở đây

            // Nếu verify thành công → chuyển sang màn hình tạo mật khẩu mới
            navController.navigate(R.id.action_verifyCode_to_createPassword);
        });

        // Khi người dùng ấn vào "Resend Code"
        binding.tvVerifyCodeResendTimer.setOnClickListener(v -> {
            // Chỉ cho phép resend khi nút được kích hoạt (đã hết thời gian đếm ngược)
            if ("Resend Code".equals(binding.tvVerifyCodeResendTimer.getText().toString())) {
                // TODO: Gọi API resend code

                // Bắt đầu lại bộ đếm đếm ngược sau khi gửi lại mã
                startResendCountdown();
            }
        });
    }

    /**
     * Bắt đầu bộ đếm thời gian đếm ngược 60s để khóa nút resend code
     */
    private void startResendCountdown() {
        // Vô hiệu hóa nút resend code khi đang đếm ngược
        binding.tvVerifyCodeResendTimer.setEnabled(false);

        new CountDownTimer(RESEND_INTERVAL_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Cập nhật text hiển thị số giây còn lại để gửi lại mã
                long sec = millisUntilFinished / 1000;
                binding.tvVerifyCodeResendTimer.setText("Resend Code in " + sec + "s");
            }

            @Override
            public void onFinish() {
                // Khi kết thúc đếm ngược, cho phép người dùng gửi lại mã
                binding.tvVerifyCodeResendTimer.setText("Resend Code");
                binding.tvVerifyCodeResendTimer.setEnabled(true);
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
