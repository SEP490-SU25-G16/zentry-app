package vn.edu.fpt.zentryapp.student.ui.schedule;

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
import vn.edu.fpt.zentryapp.databinding.FragmentStudentScheduleBinding;

public class StudentScheduleFragment extends Fragment {

    private FragmentStudentScheduleBinding binding;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentStudentScheduleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // Xử lý click nút Calendar để điều hướng sang màn hình lịch
        binding.tvStudentScheduleCalendar.setOnClickListener(v ->
                navController.navigate(R.id.action_studentSchedule_to_calendar)
        );

        // Xử lý click card lớp học 1 và icon mũi tên để điều hướng chi tiết lớp học
        View.OnClickListener class1ClickListener = v -> navController.navigate(R.id.action_studentSchedule_to_classDetail);
        binding.cardStudentScheduleClass1.setOnClickListener(class1ClickListener);
        binding.ivStudentScheduleChevron1.setOnClickListener(class1ClickListener);

        // Xử lý click card lớp học 2 và icon mũi tên để điều hướng chi tiết lớp học
        View.OnClickListener class2ClickListener = v -> navController.navigate(R.id.action_studentSchedule_to_classDetail);
        binding.cardStudentScheduleClass2.setOnClickListener(class2ClickListener);
        binding.ivStudentScheduleChevron2.setOnClickListener(class2ClickListener);

        // TODO: Xử lý nút See All (chưa implement)
        binding.tvStudentScheduleSeeAll.setOnClickListener(v -> {
            // TODO: Hiển thị danh sách đầy đủ các lớp học
        });

        // TODO: Xử lý nút thông báo nếu cần
        binding.btnStudentScheduleNotification.setOnClickListener(v -> {
            // TODO: Mở màn hình hoặc dialog thông báo
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
