package vn.edu.fpt.zentryapp.student.ui.schedule;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayoutMediator;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentScheduleClassDetailBinding;
import vn.edu.fpt.zentryapp.student.ui.schedule.tabs.ClassHistoryFragment;
import vn.edu.fpt.zentryapp.student.ui.schedule.tabs.FinalAttendanceFragment;

public class StudentScheduleClassDetailFragment extends Fragment {

    private FragmentStudentScheduleClassDetailBinding binding;
    private StudentScheduleClassDetailViewModel viewModel;
    private String classId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentScheduleClassDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get classId from arguments or use default
        classId = getArguments() != null ?
                getArguments().getString("classId", "default_class") : "default_class";

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentScheduleClassDetailViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager, classId);

        setupToolbar();
        setupViewPager();
        setupClickListeners();
        observeViewModel();
    }

    private void setupToolbar() {
        binding.ivStudentScheduleClassDetailBack.setOnClickListener(v ->
                requireActivity().onBackPressed());
    }

    private void setupViewPager() {
        String[] tabTitles = new String[]{"History", "Final Attendance"};

        binding.viewPagerStudentScheduleClassDetail.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return ClassHistoryFragment.newInstance(classId);
                    case 1:
                        return FinalAttendanceFragment.newInstance(classId);
                    default:
                        return new Fragment();
                }
            }

            @Override
            public int getItemCount() {
                return tabTitles.length;
            }
        });

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.tabLayoutStudentScheduleClassDetail,
                binding.viewPagerStudentScheduleClassDetail,
                (tab, pos) -> tab.setText(tabTitles[pos])
        ).attach();
    }

    private void setupClickListeners() {

        // Notification button click
        binding.btnStudentScheduleClassDetailNotification.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show();
            viewModel.onNotificationClicked();
        });
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // You can add a loading indicator if needed
            // binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe class detail
        viewModel.classDetail().observe(getViewLifecycleOwner(), classDetail -> {
            if (classDetail != null) {
                binding.tvStudentScheduleClassDetailGrade.setText(classDetail.getGrade());
                binding.tvStudentScheduleClassDetailSubject.setText(classDetail.getSubject());
                binding.tvStudentScheduleClassDetailDurationLabel.setText(classDetail.getDuration());
                Log.d("ClassDetail", "Class loaded: " + classDetail.getSubject() + " - " + classDetail.getGrade());
            }
        });

        // Observe success messages
        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d("ClassDetail", message);
            }
        });

        // Observe error messages
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();

                // Show retry dialog for network errors
                if (error.contains("network") || error.contains("connection")) {
                    showRetryDialog();
                }
            }
        });
    }

    private void showRetryDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Connection Error")
                .setMessage("Unable to load class details. Would you like to retry?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    viewModel.loadClassDetail(classId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Static method to create fragment with arguments
    public static StudentScheduleClassDetailFragment newInstance(String classId) {
        StudentScheduleClassDetailFragment fragment = new StudentScheduleClassDetailFragment();
        Bundle args = new Bundle();
        args.putString("classId", classId);
        fragment.setArguments(args);
        return fragment;
    }
}
