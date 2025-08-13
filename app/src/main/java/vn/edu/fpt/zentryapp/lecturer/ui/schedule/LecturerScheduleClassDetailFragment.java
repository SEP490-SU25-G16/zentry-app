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
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
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
    // Face ID request lock state
    private static final String PREFS_FACEID_LOCK = "faceid_request_prefs";
    private static final String KEY_LOCK_PREFIX = "lock_until_";
    private long faceRequestLockUntilMs = 0L;
    private boolean isCreatingRequest = false;
	// Store original button colors to restore after unlock
	private ColorStateList originalRequestBtnTint;
	private int originalRequestBtnTextColor;
    private ColorStateList originalEndBtnTint;
    private int originalEndBtnTextColor;

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
        // Capture original colors of the request button for later restore (must happen before any state changes)
        try {
            originalRequestBtnTint = binding.btnScheduleClassDetailRequestFaceId.getBackgroundTintList();
            originalRequestBtnTextColor = binding.btnScheduleClassDetailRequestFaceId.getCurrentTextColor();
            originalEndBtnTint = binding.btnScheduleClassDetailEndSession.getBackgroundTintList();
            originalEndBtnTextColor = binding.btnScheduleClassDetailEndSession.getCurrentTextColor();
        } catch (Exception ignored) {}

        observeViewModel();

        // Restore lock state from prefs and update button
        restoreFaceRequestLock();
        updateRequestButtonEnabled();

        // Apply ended-state UI if session already ended
        applySessionEndedUiIfNeeded();
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

        binding.btnScheduleClassDetailRequestFaceId.setOnClickListener(v -> {
            if (isSessionEndedNow()) {
                // Button will already be disabled/gray; no action
                return;
            }
            showFaceIdRequestDialog();
        });

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

                // If status indicates ended, lock and gray buttons
                try {
                    String status = sessionInfo.getStatus();
                    if (status != null && !"Active".equalsIgnoreCase(status)) {
                        applySessionEndedUi();
                    }
                } catch (Exception ignored) {}
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

        // Persist lock and update button state
        long lockMs = minutes * 60L * 1000L; // minutes to ms
        faceRequestLockUntilMs = System.currentTimeMillis() + lockMs;
        persistFaceRequestLock(faceRequestLockUntilMs);
        updateRequestButtonEnabled();
        // Schedule a re-check to re-enable later
        binding.btnScheduleClassDetailRequestFaceId.postDelayed(this::updateRequestButtonEnabled, lockMs);
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
                isCreatingRequest = loading;
                updateRequestButtonEnabled();
            }
        });
    }

    private void updateRequestButtonEnabled() {
        if (binding == null) return;
        boolean ended = isSessionEndedNow();
        boolean locked = System.currentTimeMillis() < faceRequestLockUntilMs;
        boolean enabled = !isCreatingRequest && !locked && !ended;
        MaterialButton btn = binding.btnScheduleClassDetailRequestFaceId;
        btn.setEnabled(enabled);
        btn.setClickable(enabled);
        btn.setAlpha(enabled ? 1.0f : 0.5f);

        // Visual feedback: gray when locked/disabled, restore original on enable
        if (!enabled) {
            ColorStateList grayTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            btn.setBackgroundTintList(grayTint);
            btn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        } else {
            if (originalRequestBtnTint != null) {
                btn.setBackgroundTintList(originalRequestBtnTint);
            }
            if (originalRequestBtnTextColor != 0) {
                btn.setTextColor(originalRequestBtnTextColor);
            }
        }
    }

    private boolean isSessionEndedNow() {
        boolean ended = false;
        try {
            if (session != null) {
                String status = session.getSessionStatus();
                if (status != null && !"Active".equalsIgnoreCase(status)) ended = true;
                java.util.Date end = session.getEndTimeAsDate();
                if (end != null && new java.util.Date().after(end)) ended = true;
            } else {
                ended = true;
            }
        } catch (Exception ignored) {}
        return ended;
    }

    private void applySessionEndedUiIfNeeded() {
        if (isSessionEndedNow()) {
            applySessionEndedUi();
        }
    }

    private void applySessionEndedUi() {
        if (binding == null) return;
        // Request Face Now button
        MaterialButton reqBtn = binding.btnScheduleClassDetailRequestFaceId;
        reqBtn.setEnabled(false);
        reqBtn.setClickable(false);
        reqBtn.setAlpha(0.5f);
        ColorStateList grayTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        reqBtn.setBackgroundTintList(grayTint);
        reqBtn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        // End Time button
        MaterialButton endBtn = binding.btnScheduleClassDetailEndSession;
        endBtn.setEnabled(false);
        endBtn.setClickable(false);
        endBtn.setAlpha(0.5f);
        endBtn.setBackgroundTintList(grayTint);
        endBtn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
    }

    private String getLockKey() {
        String sessionId = session != null ? String.valueOf(session.getSessionId()) : "global";
        return KEY_LOCK_PREFIX + sessionId;
    }

    private void persistFaceRequestLock(long untilMs) {
        try {
            requireContext().getSharedPreferences(PREFS_FACEID_LOCK, 0)
                    .edit()
                    .putLong(getLockKey(), untilMs)
                    .apply();
        } catch (Exception ignored) {}
    }

    private void restoreFaceRequestLock() {
        try {
            long saved = requireContext().getSharedPreferences(PREFS_FACEID_LOCK, 0)
                    .getLong(getLockKey(), 0L);
            faceRequestLockUntilMs = saved;
            if (System.currentTimeMillis() < faceRequestLockUntilMs) {
                long remaining = faceRequestLockUntilMs - System.currentTimeMillis();
                binding.btnScheduleClassDetailRequestFaceId.postDelayed(this::updateRequestButtonEnabled, remaining);
            }
        } catch (Exception ignored) {}
    }
}