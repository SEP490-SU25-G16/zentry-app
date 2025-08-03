package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Objects;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerScheduleClassDetailBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleClassSection;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Round;
import vn.edu.fpt.zentryapp.lecturer.ui.schedule.tabs.LecturerHistoryFragment;
import vn.edu.fpt.zentryapp.lecturer.ui.schedule.tabs.LecturerAttendanceFragment;

public class LecturerScheduleClassDetailFragment extends Fragment implements LecturerHistoryFragment.OnRoundClickListener {

    private FragmentLecturerScheduleClassDetailBinding binding;
    private LecturerScheduleClassDetailViewModel viewModel;
    private NavController navController;
    private LecturerScheduleClassSection session;
    private LecturerHistoryFragment historyFragment;
    private LecturerAttendanceFragment attendanceFragment;
    private GestureDetector gestureDetector;

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

        // Get session object from arguments
        if (getArguments() != null) {
            session = (LecturerScheduleClassSection) getArguments().getSerializable("session");
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerScheduleClassDetailViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager, session);

        setupViewPager();
        setupClickListeners();
        setupDoubleTapDetection();
        observeViewModel();
    }

    private void setupViewPager() {
        String[] tabTitles = {"History", "Attendance"};

        binding.viewPagerScheduleClassDetail.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        historyFragment = LecturerHistoryFragment.newInstance(session.getSessionId());
                        // Set callback trong fragment
                        historyFragment.setOnRoundClickListener(LecturerScheduleClassDetailFragment.this);
                        return historyFragment;
                    case 1:
                        attendanceFragment = LecturerAttendanceFragment.newInstance(session.getSessionId());
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

    @SuppressLint("ClickableViewAccessibility")
    private void setupDoubleTapDetection() {
        gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                // Double tap on attendance tab - switch back to final attendance
                viewModel.loadListFinalAttendances();
                return true;
            }
        });

        // Apply gesture detector to attendance tab
        Objects.requireNonNull(binding.tabLayoutScheduleClassDetail.getTabAt(1)).view.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }

    private void setupClickListeners() {
        binding.ivScheduleClassDetailBack.setOnClickListener(v -> navController.navigateUp());

        binding.btnScheduleClassDetailRequestFaceId.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Create face id", Toast.LENGTH_SHORT).show());

        binding.btnScheduleClassDetailEndSession.setOnClickListener(v ->
                Toast.makeText(requireContext(), "End time", Toast.LENGTH_SHORT).show());

        binding.btnScheduleClassDetailNotification.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModel() {
        // Observe session info
        viewModel.sessionInfo().observe(getViewLifecycleOwner(), sessionInfo -> {
            if (sessionInfo != null) {
                binding.tvScheduleClassDetailCountStudents.setText(sessionInfo.getStudentCountDisplay());
                binding.tvScheduleClassDetailDuration.setText(sessionInfo.getDurationDisplay());
                binding.tvScheduleClassDetailRoom.setText(sessionInfo.getRoom());
                binding.tvScheduleClassDetailCourseName.setText(sessionInfo.getCourseName() + " - " + sessionInfo.getClassName());
            }
        });

        // Observer for button visibility
        viewModel.canAddFaceId().observe(getViewLifecycleOwner(), canAdd ->
                binding.btnScheduleClassDetailRequestFaceId.setEnabled(canAdd));


        // Observer for history rounds
        viewModel.listHistoryRounds().observe(getViewLifecycleOwner(), rounds -> {
            if (rounds != null && historyFragment != null) {
                historyFragment.updateRoundHistory(rounds);
            }
        });

        // Observer for attendance data (both final and round-specific)
        viewModel.listAttendance().observe(getViewLifecycleOwner(), attendanceList -> {
            if (attendanceList != null && attendanceFragment != null) {
                attendanceFragment.updateAttendanceData(attendanceList);
            }
        });

        // Observer for errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRoundClick(Round round) {
        // Khi user click vào round trong History tab
        // Class Detail Fragment sẽ gọi ViewModel để load attendance của round đó
        viewModel.loadListRoundAttendances(round.getRoundId());

        // Chuyển sang tab Attendance để user xem kết quả
        binding.viewPagerScheduleClassDetail.setCurrentItem(1, true);

        Toast.makeText(requireContext(),
                "Loading attendance for Round " + round.getRoundNumber(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
