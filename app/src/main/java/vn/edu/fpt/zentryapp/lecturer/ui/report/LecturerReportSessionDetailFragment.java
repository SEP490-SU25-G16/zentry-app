package vn.edu.fpt.zentryapp.lecturer.ui.report;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerReportSessionDetailBinding;

/**
 * Fragment hiển thị chi tiết một session báo cáo, với nút Back.
 */
public class LecturerReportSessionDetailFragment extends Fragment {

    private FragmentLecturerReportSessionDetailBinding binding;
    private long sessionId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLecturerReportSessionDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivSessionDetailBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Lấy argument sessionId truyền từ fragment trước
        if (getArguments() != null) {
            sessionId = getArguments().getLong("sessionId", 0L);
        }

        // TODO: Load dữ liệu chi tiết session từ API hoặc database dựa trên sessionId
        // Hiển thị thông tin chi tiết session (ví dụ tạm thời)
        binding.tvSessionDetailGrade.setText("Grade " + sessionId);
        binding.tvSessionDetailSubject.setText("Session - " + sessionId);
        binding.tvSessionDetailAttendanceCount.setText("19/21 - Attendance");

        // TODO: Load danh sách học sinh và trạng thái điểm danh
        // Hiển thị danh sách học sinh trong llSessionDetailStudentList
        // Mỗi item gồm avatar, tên, mã ID, trạng thái điểm danh, nút chỉnh sửa

        // TODO: Xử lý sự kiện click nút chỉnh sửa điểm danh cho từng học sinh
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
