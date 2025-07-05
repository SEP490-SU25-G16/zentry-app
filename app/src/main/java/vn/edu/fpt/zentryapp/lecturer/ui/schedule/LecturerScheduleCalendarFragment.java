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
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerScheduleCalendarBinding;

public class LecturerScheduleCalendarFragment extends Fragment {

    private FragmentLecturerScheduleCalendarBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLecturerScheduleCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = NavHostFragment.findNavController(this);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivScheduleCalendarBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Lắng nghe sự kiện thay đổi ngày trên CalendarView
        binding.calendarView.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            // TODO: Xử lý khi người dùng chọn ngày mới
            // Ví dụ: gọi API hoặc truy vấn dữ liệu nội bộ để lấy lịch sự kiện ngày được chọn
            // Sau đó cập nhật danh sách timeline sự kiện dưới lịch

            // Ví dụ giả lập cập nhật timeline (cần thay bằng dữ liệu thực tế)
            updateTimelineForDate(year, month, dayOfMonth);
        });

        // TODO: Khởi tạo danh sách timeline sự kiện cho ngày hiện tại (hoặc ngày mặc định)
        // Có thể lấy ngày hiện tại từ Calendar hoặc hệ thống
        // updateTimelineForDate(currentYear, currentMonth, currentDay);
    }

    /**
     * Hàm cập nhật danh sách timeline sự kiện theo ngày được chọn
     * @param year năm
     * @param month tháng (0-based, 0 = January)
     * @param dayOfMonth ngày trong tháng
     */
    private void updateTimelineForDate(int year, int month, int dayOfMonth) {
        // TODO: Xóa hoặc cập nhật UI timeline sự kiện theo dữ liệu mới

        // Ví dụ: binding.tvScheduleCalendarItem1Time.setText("08:00");
        // binding.tvScheduleCalendarItem1Description.setText("Math Class: Grade 07");
        // ... cập nhật các item khác hoặc hiển thị danh sách động nếu cần
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
