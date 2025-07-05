package vn.edu.fpt.zentryapp.auth.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentPasswordResetSuccessBinding;

public class PasswordResetSuccessFragment extends Fragment {

    private FragmentPasswordResetSuccessBinding binding;
    private NavController navController;
    private Handler handler;

    // Thời gian đếm ngược tự động chuyển tiếp (3 giây)
    private static final long REDIRECT_DELAY_MS = 3_000L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentPasswordResetSuccessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);
        handler = new Handler(Looper.getMainLooper());

        // Tự động chuyển sang màn hình đăng nhập sau 3 giây
        handler.postDelayed(this::navigateToLogin, REDIRECT_DELAY_MS);

        // Nếu người dùng ấn nút "Sign In", hủy đếm ngược và chuyển ngay
        binding.btnPasswordResetSuccessSignIn.setOnClickListener(v -> {
            handler.removeCallbacksAndMessages(null);
            navigateToLogin();
        });
    }

    /**
     * Hàm điều hướng về màn hình đăng nhập, đồng thời xóa toàn bộ back stack
     */
    private void navigateToLogin() {
        NavOptions navOptions = new NavOptions.Builder()
                // Xóa toàn bộ back stack đến nav_graph_root (bao gồm nó)
                .setPopUpTo(R.id.nav_graph_root, true)
                .build();

        navController.navigate(R.id.loginFragment, null, navOptions);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy callback đếm ngược nếu fragment bị hủy sớm để tránh leak
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        binding = null;
    }
}
