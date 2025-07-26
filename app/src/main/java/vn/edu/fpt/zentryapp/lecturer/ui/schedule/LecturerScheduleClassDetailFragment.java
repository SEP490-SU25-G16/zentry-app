package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayoutMediator;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerScheduleClassDetailBinding;
import vn.edu.fpt.zentryapp.lecturer.ui.schedule.tabs.AttendanceHistoryFragment;
import vn.edu.fpt.zentryapp.lecturer.ui.schedule.tabs.FinalAttendanceFragment;

public class LecturerScheduleClassDetailFragment extends Fragment {

    private FragmentLecturerScheduleClassDetailBinding binding;
    private LecturerScheduleClassDetailViewModel viewModel;
    private NavController navController;
    private String sessionId;
    private AttendanceHistoryFragment historyFragment;
    private FinalAttendanceFragment attendanceFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerScheduleClassDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Get sessionId from arguments
        sessionId = getArguments() != null ? getArguments().getString("sessionId", "") : "SESSION_001";

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerScheduleClassDetailViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager, sessionId);

        setupViewPager();
        setupClickListeners();
        observeViewModel();
    }

    private void setupViewPager() {
        String[] tabTitles = {"History", "Final Attendance"};

        binding.viewPagerScheduleClassDetail.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        historyFragment = AttendanceHistoryFragment.newInstance(sessionId);
                        return historyFragment;
                    case 1:
                        attendanceFragment = FinalAttendanceFragment.newInstance(sessionId);
                        return attendanceFragment;
                    default:
                        return new Fragment();
                }
            }

            @Override
            public int getItemCount() {
                return tabTitles.length;
            }
        });

        new TabLayoutMediator(binding.tabLayoutScheduleClassDetail, binding.viewPagerScheduleClassDetail,
                (tab, pos) -> tab.setText(tabTitles[pos])
        ).attach();
    }

    private void setupClickListeners() {
        binding.ivScheduleClassDetailBack.setOnClickListener(v -> navController.navigateUp());

        binding.btnScheduleClassDetailAdd.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Add new round", Toast.LENGTH_SHORT).show();
        });

        binding.btnScheduleClassDetailNotification.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.sessionInfo().observe(getViewLifecycleOwner(), sessionInfo -> {
            if (sessionInfo != null) {
                binding.tvScheduleClassDetailGrade.setText(sessionInfo.getClassDisplay());
                binding.tvScheduleClassDetailSubject.setText(sessionInfo.getCourseDisplay());
                binding.tvScheduleClassDetailStudentCount.setText(sessionInfo.getStudentCountDisplay());
            }
        });
        // Observer riêng cho button visibility
        viewModel.canAddFaceId().observe(getViewLifecycleOwner(), canAdd -> {
            binding.btnScheduleClassDetailAdd.setVisibility(canAdd ? View.VISIBLE : View.GONE);
        });

        viewModel.attendanceRounds().observe(getViewLifecycleOwner(), rounds -> {
            if (rounds != null && historyFragment != null) {
                historyFragment.updateRoundHistory(rounds);
            }
        });

        viewModel.finalAttendance().observe(getViewLifecycleOwner(), finalAttendance -> {
            if (finalAttendance != null && attendanceFragment != null) {
                attendanceFragment.updateFinalAttendance(finalAttendance);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}