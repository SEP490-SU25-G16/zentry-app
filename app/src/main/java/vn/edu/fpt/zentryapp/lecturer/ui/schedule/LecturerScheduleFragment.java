package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

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
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerScheduleBinding;

public class LecturerScheduleFragment extends Fragment {

    private FragmentLecturerScheduleBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLecturerScheduleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);

        // Xử lý click nút Calendar để điều hướng sang màn hình lịch
        binding.tvScheduleCalendar.setOnClickListener(v ->
                navController.navigate(R.id.action_schedule_to_calendar)
        );

        // Xử lý click card lớp học 1 để điều hướng sang chi tiết lớp học với classId = 1
        binding.cardScheduleClass1.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("classId", 1L);
            navController.navigate(R.id.action_schedule_to_classDetail, args);
        });

        // Xử lý click card lớp học 2 để điều hướng sang chi tiết lớp học với classId = 2
        binding.cardScheduleClass2.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("classId", 2L);
            navController.navigate(R.id.action_schedule_to_classDetail, args);
        });

        // TODO: Xử lý nút Start Instant Class (hiện tại chưa implement)
        binding.btnScheduleStartClass.setOnClickListener(v -> {
            // TODO: Thực hiện bắt đầu lớp học ngay lập tức
        });

        // TODO: Xử lý nút See All (hiện tại chưa implement)
        binding.tvScheduleSeeAll.setOnClickListener(v -> {
            // TODO: Hiển thị danh sách đầy đủ các lớp học
        });

        // TODO: Xử lý nút thông báo nếu cần
        binding.btnScheduleNotification.setOnClickListener(v -> {
            // TODO: Mở màn hình hoặc dialog thông báo
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
