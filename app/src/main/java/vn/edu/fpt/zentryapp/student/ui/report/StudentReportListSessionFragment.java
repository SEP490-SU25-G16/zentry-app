package vn.edu.fpt.zentryapp.student.ui.report;

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
import vn.edu.fpt.zentryapp.databinding.FragmentStudentReportListSessionBinding;

public class StudentReportListSessionFragment extends Fragment {

    private FragmentStudentReportListSessionBinding binding;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentStudentReportListSessionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivStudentReportListSessionBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // TODO: Bind dữ liệu thực tế cho các TextView, ví dụ:
        // binding.tvStudentReportListSessionGrade.setText("Grade 07");
        // binding.tvStudentReportListSessionSubject.setText("Mathematics");
        // binding.tvStudentReportListSessionCount.setText("12/20 Sessions");

        // TODO: Xử lý click từng buổi học để điều hướng sang chi tiết buổi học
        binding.llStudentReportListSession1.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("sessionId", 1L);
            navController.navigate(R.id.action_listSession_to_sessionDetail, args);
        });

        binding.llStudentReportListSession2.setOnClickListener(v -> {
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
