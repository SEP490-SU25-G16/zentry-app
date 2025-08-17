package vn.edu.fpt.zentryapp.student.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentHomeBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.ExamAdapter;
import vn.edu.fpt.zentryapp.lecturer.adapter.SessionAdapter;
import vn.edu.fpt.zentryapp.lecturer.adapter.WeeklyAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ExamModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.WeeklyModel;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import android.content.BroadcastReceiver;
import android.content.Intent;

public class StudentHomeFragment extends Fragment {

    private FragmentStudentHomeBinding binding;
    private StudentHomeViewModel viewModel;
    private NotificationViewModel notificationViewModel;
    private androidx.navigation.NavController navController;

    private int currentExamPosition = 0;
    private int currentWeeklyPosition = 0;
    private boolean isShowingAllSessions = false;
    private java.util.List<SessionModel> cachedSessionList = java.util.Collections.emptyList();

    private static final String TAG = "StudentHomeFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentHomeViewModel.class);
        notificationViewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);

        // Load notifications từ API để có dữ liệu cho badge
        String userId = authManager.getCurrentUserId();
        if (userId != null) {
            notificationViewModel.loadNotifications(userId, requireContext());
        }

        // 🔧 NEW: Register broadcast receiver for real-time notification updates
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(notificationUpdateReceiver, new android.content.IntentFilter("vn.edu.fpt.zentryapp.NOTIFICATIONS_UPDATED"));

        setupClickListeners();
        observeViewModel();

    }



    // 🔧 NEW: Broadcast receiver for real-time notification updates
    private final BroadcastReceiver notificationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            Log.d(TAG, "📢 StudentHome: Received notification update broadcast");
            
            // Force refresh notification count for badge
            String userId = intent.getStringExtra("userId");
            if (userId != null) {
                Log.d(TAG, "🔄 Refreshing notification count for badge update");
                // 🔧 FIX: Use forceRefresh instead of forceRefreshNotifications
                notificationViewModel.forceRefresh(userId, requireContext());
                
                // ✅ NEW: Check if this is a session end notification and handle BLE service stopping
                // This serves as a fallback when FCM fails due to network restrictions
                String notificationBody = intent.getStringExtra("notificationBody");
                if (notificationBody != null && notificationBody.contains("Tiết học đã kết thúc sớm")) {
                    Log.d(TAG, "🛑 Received session end notification, checking if BLE service needs to be stopped");
                    stopBLEAttendanceServiceIfNeeded();
                }
                
                // 🔧 NOTE: StudentHomeFragment doesn't have notification badge UI
                // So we only refresh the data, not update any badge
                Log.d(TAG, "ℹ️ No notification badge UI in StudentHomeFragment");
            }
        }
    };

    /* ---------- Observe ---------- */
    private void observeViewModel() {
        // Keep exam slider (empty for now)
        viewModel.exams().observe(getViewLifecycleOwner(), examList -> {
            if (examList.isEmpty()) {
                binding.rvExams.setVisibility(View.GONE);
                binding.dotsIndicator.setVisibility(View.GONE);
            } else {
                setupExamSlider(examList);
            }
        });

        viewModel.sessions().observe(getViewLifecycleOwner(), sessionList ->
                setupSessions(sessionList, false));

        viewModel.weekly().observe(getViewLifecycleOwner(), this::setupWeeklySlider);

        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Show/hide loading indicator if you have one
            // binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    /* ---------- Exam slider ---------- */
    private void setupExamSlider(java.util.List<ExamModel> examList) {
        binding.rvExams.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        binding.rvExams.setAdapter(new ExamAdapter(examList));
        new PagerSnapHelper().attachToRecyclerView(binding.rvExams);

        buildDotIndicators(examList.size(), binding.dotsIndicator, currentExamPosition);
        binding.rvExams.addOnScrollListener(createScrollListener(position -> {
            currentExamPosition = position;
            updateDotIndicators(binding.dotsIndicator, position);
        }));
    }

    /* ---------- Sessions ---------- */
    private void setupSessions(java.util.List<SessionModel> sessionList, boolean isRebuild) {
        if (sessionList.isEmpty()) {
            binding.sessionsContainer.removeAllViews();
            // Add empty state view
            android.widget.TextView emptyView = new android.widget.TextView(requireContext());
            emptyView.setText("No classes scheduled for today");
            emptyView.setGravity(android.view.Gravity.CENTER);
            binding.sessionsContainer.addView(emptyView);
            binding.btnSeeAllSessions.setVisibility(View.GONE);
            return;
        }

        if (!isRebuild) cachedSessionList = sessionList;
        binding.sessionsContainer.removeAllViews();

        int displayCount = isShowingAllSessions ? sessionList.size() : 1;
        for (int index = 0; index < displayCount; index++) {
            View sessionView = SessionAdapter.createSessionViews(binding.sessionsContainer, sessionList).get(index);
            if (index < displayCount - 1) addBottomMargin(sessionView, 12);
            binding.sessionsContainer.addView(sessionView);
        }

        binding.btnSeeAllSessions.setText(isShowingAllSessions ? "Show Less" : "See All");
        binding.btnSeeAllSessions.setOnClickListener(view -> {
            isShowingAllSessions = !isShowingAllSessions;
            setupSessions(cachedSessionList, true);
        });
    }

    /* ---------- Weekly slider ---------- */
    private void setupWeeklySlider(java.util.List<WeeklyModel> weeklyList) {
        binding.rvWeekly.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        binding.rvWeekly.setAdapter(new WeeklyAdapter(weeklyList));
        new PagerSnapHelper().attachToRecyclerView(binding.rvWeekly);

        buildDotIndicators(weeklyList.size(), binding.weeklyDotsIndicator, currentWeeklyPosition);
        binding.rvWeekly.addOnScrollListener(createScrollListener(position -> {
            currentWeeklyPosition = position;
            updateDotIndicators(binding.weeklyDotsIndicator, position);
        }));
    }

    /* ---------- Utilities ---------- */
    private RecyclerView.OnScrollListener createScrollListener(java.util.function.IntConsumer positionConsumer) {
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int visiblePosition = ((LinearLayoutManager) recyclerView.getLayoutManager())
                            .findFirstCompletelyVisibleItemPosition();
                    if (visiblePosition != RecyclerView.NO_POSITION) {
                        positionConsumer.accept(visiblePosition);
                    }
                }
            }
        };
    }

    private void buildDotIndicators(int dotCount, LinearLayout dotsContainer, int activePosition) {
        dotsContainer.removeAllViews();
        for (int index = 0; index < dotCount; index++) {
            ImageView dotView = createDotView();
            updateDotAppearance(dotView, index == activePosition);
            dotsContainer.addView(dotView);
        }
    }

    private void updateDotIndicators(LinearLayout dotsContainer, int activePosition) {
        for (int index = 0; index < dotsContainer.getChildCount(); index++) {
            updateDotAppearance((ImageView) dotsContainer.getChildAt(index), index == activePosition);
        }
    }

    private ImageView createDotView() {
        ImageView dotView = new ImageView(requireContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(8, 0, 8, 0);
        dotView.setLayoutParams(layoutParams);
        return dotView;
    }

    private void updateDotAppearance(ImageView dotView, boolean isActive) {
        dotView.setBackground(ContextCompat.getDrawable(requireContext(),
                isActive ? R.drawable.dot_active : R.drawable.dot_inactive));
    }

    private void addBottomMargin(View targetView, int marginDp) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) targetView.getLayoutParams();
        layoutParams.bottomMargin = (int) (marginDp * getResources().getDisplayMetrics().density);
        targetView.setLayoutParams(layoutParams);
    }

    private void setupClickListeners() {
        binding.btnSeeAllSessions.setOnClickListener(view -> {
            isShowingAllSessions = !isShowingAllSessions;
            setupSessions(cachedSessionList, true);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // 🔧 NEW: Unregister broadcast receiver
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(notificationUpdateReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering notification receiver", e);
        }
        
        binding = null;
    }
    
    // ✅ NEW: Method to stop BLE attendance service when session ends (fallback for FCM failures)
    private void stopBLEAttendanceServiceIfNeeded() {
        try {
            // Check if BLE service is already stopped via FCM
            if (!vn.edu.fpt.zentryapp.notification.push.FcmMessagingService.isBLEServiceStopped()) {
                Intent serviceIntent = new Intent(requireContext(), vn.edu.fpt.zentryapp.service.BLEAttendanceService.class);
                serviceIntent.setAction("STOP_ATTENDANCE");
                requireContext().startService(serviceIntent);
                Log.d(TAG, "✅ Fragment: Sent STOP_ATTENDANCE intent to BLE service (FCM fallback)");
            } else {
                Log.d(TAG, "ℹ️ Fragment: BLE service already stopped by FCM, no action needed");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Fragment: Error stopping BLE service", e);
        }
    }
}
