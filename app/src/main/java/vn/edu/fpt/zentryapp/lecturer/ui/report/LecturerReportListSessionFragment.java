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
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerReportListSessionBinding;

public class LecturerReportListSessionFragment extends Fragment {

    private FragmentLecturerReportListSessionBinding binding;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLecturerReportListSessionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivReportBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // TODO: Bind dữ liệu thực tế cho các TextView, ví dụ:
        // binding.tvReportGrade.setText("Grade 07");
        // binding.tvReportSubject.setText("Mathematics");
        // binding.tvReportStudentCount.setText("21 Students");
        // binding.tvReportSessionCount.setText("12/20 Sessions");

        // Xử lý click từng buổi học để điều hướng sang chi tiết buổi học
        binding.llReportSession1.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("sessionId", 1L); // Tham số sessionId tương ứng
            navController.navigate(R.id.action_listSession_to_sessionDetail, args);
        });

        binding.ivReportSession1Arrow.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("sessionId", 1L);
            navController.navigate(R.id.action_listSession_to_sessionDetail, args);
        });

        binding.llReportSession2.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("sessionId", 2L);
            navController.navigate(R.id.action_listSession_to_sessionDetail, args);
        });

        binding.ivReportSession2Arrow.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("sessionId", 2L);
            navController.navigate(R.id.action_listSession_to_sessionDetail, args);
        });

        // TODO: Nếu có nhiều session, nên dùng RecyclerView để linh hoạt hơn
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
