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

public class StudentHomeFragment extends Fragment {

    private FragmentStudentHomeBinding binding;
    private StudentHomeViewModel viewModel;

    private int currentExamPosition = 0;
    private int currentWeeklyPosition = 0;
    private boolean isShowingAllSessions = false;
    private java.util.List<SessionModel> cachedSessionList = java.util.Collections.emptyList();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(StudentHomeViewModel.class);

        // Initialize ViewModel with dependencies
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);

        // Load real data instead of mock
        viewModel.loadStudentHomeData();

        observeViewModel();
    }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
