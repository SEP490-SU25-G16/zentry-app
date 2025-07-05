package vn.edu.fpt.zentryapp.auth.ui;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = NavHostFragment.findNavController(this);

        // Xử lý khi người dùng ấn "Forgot Password?"
        binding.tvLoginForgotPassword.setOnClickListener(v ->
                navController.navigate(R.id.action_login_to_selectMethod)
        );

        // Xử lý khi người dùng ấn nút "Sign In"
        binding.btnLoginSignIn.setOnClickListener(v -> {
            // Ẩn thông báo lỗi cũ
            binding.tvLoginPasswordError.setVisibility(View.GONE);

            // Lấy dữ liệu email và password người dùng nhập
            String email = binding.etLoginEmail.getText() != null
                    ? binding.etLoginEmail.getText().toString().trim()
                    : "";
            String pwd = binding.etLoginPassword.getText() != null
                    ? binding.etLoginPassword.getText().toString()
                    : "";

            // Validate email và password không được để trống
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pwd)) {
                binding.tvLoginPasswordError.setText("Email và password không được để trống");
                binding.tvLoginPasswordError.setVisibility(View.VISIBLE);
                return;
            }

            // TODO: Gọi API xác thực người dùng thực tế
            // Ở đây giả lập xác thực với mật khẩu "123456"
            boolean loginSuccess = fakeAuthenticate(email, pwd);

            // Nếu đăng nhập thất bại, hiển thị lỗi
            if (!loginSuccess) {
                binding.tvLoginPasswordError.setText("Email hoặc password không đúng");
                binding.tvLoginPasswordError.setVisibility(View.VISIBLE);
                return;
            }

            // Phân biệt role người dùng theo email
            boolean isLecturer = email.endsWith("@fpt.edu.vn");
            int actionId = isLecturer
                    ? R.id.action_login_to_lecturer
                    : R.id.action_login_to_student;

            // Tạo NavOptions để clear toàn bộ back stack trước khi navigate
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph_root, true) // Xóa toàn bộ stack đến nav_graph_root (bao gồm nó)
                    .build();

            // Điều hướng sang màn hình tương ứng với role
            navController.navigate(actionId, null, navOptions);
        });

        // Xử lý Google Sign-In (chưa implement)
        binding.btnLoginGoogle.setOnClickListener(v -> {
            // TODO: Khởi chạy flow Google Sign-In
        });

        // Đăng ký callback xử lý nút back hệ thống
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (navController.popBackStack()) {
                            // Đã pop thành công fragment trước đó
                        } else {
                            // Nếu không còn fragment nào để pop, gọi back mặc định (thoát app hoặc activity)
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Giải phóng binding để tránh leak bộ nhớ
        binding = null;
    }

    /**
     * Hàm giả lập xác thực người dùng, chỉ dùng demo.
     * @param email Email người dùng nhập
     * @param password Password người dùng nhập
     * @return true nếu password đúng "123456", false nếu sai
     */
    private boolean fakeAuthenticate(String email, String password) {
        return "123456".equals(password);
    }
}