package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import vn.edu.fpt.zentryapp.lecturer.ui.faceid.FaceIdRequestDialog;

import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Objects;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerScheduleClassDetailBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleClassSection;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Round;
import vn.edu.fpt.zentryapp.lecturer.ui.schedule.tabs.LecturerHistoryFragment;
import vn.edu.fpt.zentryapp.lecturer.ui.schedule.tabs.LecturerAttendanceFragment;

import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;

public class LecturerScheduleClassDetailFragment extends Fragment implements LecturerHistoryFragment.OnRoundClickListener {

    private FragmentLecturerScheduleClassDetailBinding binding;
    private LecturerScheduleClassDetailViewModel viewModel;
    private NotificationViewModel notificationViewModel;
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
        notificationViewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager, session);


        // Load notifications từ API để có dữ liệu cho badge
        String userId = authManager.getCurrentUserId();
        if (userId != null) {
            notificationViewModel.loadNotifications(userId, requireContext());
        }

        setupViewPager();
        setupClickListeners();
        setupDoubleTapDetection();
        observeViewModel();
    }

    private void setupViewPager() {
        String[] tabTitles = {"History", "Attendance"};
        binding.viewPagerScheduleClassDetail.setOffscreenPageLimit(2);
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

        binding.btnScheduleClassDetailRequestFaceId.setOnClickListener(v -> showFaceIdRequestDialog());

        binding.btnScheduleClassDetailEndSession.setOnClickListener(v -> showEndSessionConfirmation());

        binding.btnScheduleClassDetailNotification.setOnClickListener(v -> {
            // Navigate đến NotificationFragment
            try {
                if (navController.getCurrentDestination() != null && 
                    navController.getCurrentDestination().getAction(R.id.action_scheduleClassDetail_to_notification) != null) {
                    navController.navigate(R.id.action_scheduleClassDetail_to_notification);
                } else {
                    // Fallback navigation if action isn't available
                    android.util.Log.w("LecturerScheduleClassDetail", "Navigation action not available, using fallback");
                    Toast.makeText(requireContext(), "Đang chuyển đến thông báo...", Toast.LENGTH_SHORT).show();
                    
                    // Try to find notificationFragment by ID
                    navController.navigate(R.id.notificationFragment);
                }
            } catch (Exception e) {
                android.util.Log.e("LecturerScheduleClassDetail", "Navigation error: ", e);
                Toast.makeText(requireContext(), "Chức năng thông báo đang được phát triển", Toast.LENGTH_SHORT).show();
            }
        });
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
        viewModel.listFinalAttendance().observe(getViewLifecycleOwner(), attendanceList -> {
            if (attendanceList != null && attendanceFragment != null) {
                attendanceFragment.updateAttendanceData(attendanceList);
            }
        });

        // Observe notification unseen count để hiển thị badge
        notificationViewModel.getUnseenCount().observe(getViewLifecycleOwner(), unseenCount -> {
            if (unseenCount != null && unseenCount > 0) {
                binding.tvNotificationBadge.setVisibility(View.VISIBLE);
                binding.tvNotificationBadge.setText(String.valueOf(unseenCount));
            } else {
                binding.tvNotificationBadge.setVisibility(View.GONE);
            }
        });

        viewModel.listRoundAttendance().observe(getViewLifecycleOwner(), attendanceList -> {
            if (attendanceList != null && attendanceFragment != null) {
                // Get current round number
                Integer roundNumber = viewModel.currentRoundNumber().getValue();
                if (roundNumber != null) {
                    attendanceFragment.updateRoundAttendanceData(attendanceList, roundNumber);
                }
            }
        });

        // Observer for errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.isEndingSession().observe(getViewLifecycleOwner(), isEnding -> {
            binding.btnScheduleClassDetailEndSession.setEnabled(!isEnding);
            binding.btnScheduleClassDetailEndSession.setText(isEnding ? "Ending..." : "End Time");
        });

        viewModel.endSessionResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                // Show success message
                Toast.makeText(requireContext(),
                        result.getMessage() != null ? result.getMessage() : "Session ended successfully",
                        Toast.LENGTH_LONG).show();
                // Navigate back to schedule
                navController.navigateUp();
            }
        });
    }

    private void showEndSessionConfirmation() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_end_session_confirmation);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvMessage = dialog.findViewById(R.id.tv_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

        // Set message with session info
        String message = String.format(
                "Are you sure you want to end the session?\n\n" +
                        "Course: %s\n" +
                        "This will stop the attendance service and finalize all completed rounds.",
                session != null ? session.getCourseName() + " - " + session.getSectionCode() : "Current Session"
        );
        tvMessage.setText(message);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.endSession();
        });

        dialog.setCancelable(true);
        dialog.show();
    }


    @Override
    public void onRoundClick(Round round) {
        // Khi user click vào round trong History tab
        // Class Detail Fragment sẽ gọi ViewModel để load attendance của round đó
        viewModel.loadListRoundAttendances(round.getRoundId());

        binding.viewPagerScheduleClassDetail.post(() -> {
            // ✅ 3. Sau đó mới load data
            viewModel.loadListRoundAttendances(round.getRoundId());
            binding.viewPagerScheduleClassDetail.setCurrentItem(1, true);
        });    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    private void showFaceIdRequestDialog() {
        FaceIdRequestDialog dialog = new FaceIdRequestDialog();
        dialog.setFaceIdRequestListener(this::scheduleFaceIdVerification);
        dialog.show(getChildFragmentManager(), "FaceIdRequestDialog");
    }
    
    private void scheduleFaceIdVerification(int totalSeconds) {
        int minutes = (totalSeconds + 59) / 60; // round up to minutes
        if (minutes <= 0) minutes = 1;
        String title = "Yêu cầu xác thực Face ID";
        String body = "Vui lòng xác thực khuôn mặt để tiếp tục.";

        Toast.makeText(requireContext(), "Đang gửi yêu cầu Face ID...", Toast.LENGTH_SHORT).show();
        viewModel.createFaceIdRequest(minutes, title, body);
        // Attach observers if not yet (idempotent safety)
        attachFaceIdObservers();
    }

    private boolean faceIdObserversAttached = false;
    private void attachFaceIdObservers() {
        if (faceIdObserversAttached) return;
        faceIdObserversAttached = true;
        viewModel.faceIdRequestSuccess().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.faceIdRequestError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                Toast.makeText(requireContext(), "Face ID request error: " + err, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.isCreatingFaceIdRequest().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null) {
                binding.btnScheduleClassDetailRequestFaceId.setEnabled(!loading);
            }
        });
    }
}