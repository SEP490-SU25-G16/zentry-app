package vn.edu.fpt.zentryapp.lecturer.ui.report;

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
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerReportBinding;

public class LecturerReportFragment extends Fragment {

    private FragmentLecturerReportBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLecturerReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = NavHostFragment.findNavController(this);

        // TODO: Khởi tạo và bind dữ liệu thực tế cho các view trong layout
        // Ví dụ:
        // - Hiển thị avatar giảng viên
        // - Hiển thị tên và lời chào cá nhân
        // - Hiển thị danh sách lớp học từ API hoặc database
        // - Xử lý sự kiện click cho nút thông báo (nếu có)

        // Ví dụ xử lý click card lớp học để điều hướng chi tiết báo cáo session
        binding.cardHomeClassroomMathematics.setOnClickListener(v -> {
            Bundle args = new Bundle();
            // Giả sử sessionId = 101L, thực tế lấy từ dữ liệu lớp học
            args.putLong("sessionId", 101L);
            navController.navigate(R.id.action_listSession_to_sessionDetail, args);
        });

        // Tương tự có thể thêm xử lý click cho các card khác nếu có
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
