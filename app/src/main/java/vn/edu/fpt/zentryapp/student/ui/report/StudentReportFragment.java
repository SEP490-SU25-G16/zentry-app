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
import vn.edu.fpt.zentryapp.databinding.FragmentStudentReportBinding;

public class StudentReportFragment extends Fragment {

    private FragmentStudentReportBinding binding;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentStudentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // Xử lý click card lớp học để điều hướng sang danh sách session
        binding.cardStudentReportClass1.setOnClickListener(v -> {
            // TODO: Truyền dữ liệu lớp học nếu cần
            navController.navigate(R.id.action_studentReport_to_listSession);
        });

        // TODO: Xử lý click các card lớp học khác nếu có


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
