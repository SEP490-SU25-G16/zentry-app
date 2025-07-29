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
import androidx.viewpager2.widget.ViewPager2;

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
    private String sessionId;

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

        // 🔧 LẤY data từ arguments
        Bundle args = getArguments();
        if (args != null) {
            sessionId = args.getString("sessionId", "");

            // 🔧 HIỂN THỊ thông tin cơ bản ngay lập tức từ arguments
            displayBasicInfo(args);
        } else {
            Log.w("ClassDetail", "No arguments provided, using default sessionId");
            sessionId = "default_session";
        }

        // Initialize ViewModel - chỉ cần sessionId để load attendance data
        viewModel = new ViewModelProvider(this).get(StudentScheduleClassDetailViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager, sessionId);

        setupToolbar();
        setupViewPager();
        setupClickListeners();
        observeViewModel();
    }

    /**
     * 🔧 SETUP toolbar với back navigation
     */
    private void setupToolbar() {
        binding.ivStudentScheduleClassDetailBack.setOnClickListener(v ->
                requireActivity().onBackPressed());
    }

    /**
     * 🔧 HIỂN THỊ thông tin cơ bản từ arguments (không cần API)
     */
    private void displayBasicInfo(Bundle args) {
        String courseName = args.getString("courseName", "Unknown Course");
        String sectionCode = args.getString("sectionCode", "Unknown Section");
        String room = args.getString("room", "Unknown Room");
        String lecturer = args.getString("lecturer", "Unknown Lecturer");
        String startTime = args.getString("startTime", "");
        String endTime = args.getString("endTime", "");
        String dayOfWeek = args.getString("dayOfWeek", "");

        // Set UI elements immediately
        binding.tvStudentScheduleClassDetailSubject.setText(courseName);
        binding.tvStudentScheduleClassDetailGrade.setText(sectionCode);

        // Format duration string
        String durationText = formatDurationText(dayOfWeek, startTime, endTime, room, lecturer);
        binding.tvStudentScheduleClassDetailDurationLabel.setText(durationText);

        Log.d("ClassDetail", "Basic info displayed: " + courseName + " - " + sectionCode);
    }

    /**
     * 🔧 FORMAT duration text với thông tin đầy đủ
     */
    private String formatDurationText(String dayOfWeek, String startTime, String endTime, String room, String lecturer) {
        StringBuilder sb = new StringBuilder();

        // Time info
        if (!dayOfWeek.isEmpty() && !startTime.isEmpty() && !endTime.isEmpty()) {
            sb.append(dayOfWeek).append(" ").append(startTime).append(" - ").append(endTime);
        }

        // Room info
        if (!room.isEmpty()) {
            if (sb.length() > 0) sb.append(" at ");
            sb.append(room);
        }

        // Lecturer info
        if (!lecturer.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Lecturer: ").append(lecturer);
        }

        return sb.toString();
    }

    /**
     * 🔧 SETUP ViewPager với tabs
     */
    private void setupViewPager() {
        String[] tabTitles = new String[]{"History", "Final Attendance"};

        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        // 🔧 PASS sessionId thay vì classId
                        return ClassHistoryFragment.newInstance(sessionId);
                    case 1:
                        return FinalAttendanceFragment.newInstance(sessionId);
                    default:
                        return new Fragment();
                }
            }

            @Override
            public int getItemCount() {
                return tabTitles.length;
            }
        };

        binding.viewPagerStudentScheduleClassDetail.setAdapter(adapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.tabLayoutStudentScheduleClassDetail,
                binding.viewPagerStudentScheduleClassDetail,
                (tab, pos) -> tab.setText(tabTitles[pos])
        ).attach();

        // 🔧 THÊM listener để cập nhật chiều cao khi chuyển tab
        binding.viewPagerStudentScheduleClassDetail.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Request layout lại để tính toán chiều cao mới
                binding.viewPagerStudentScheduleClassDetail.requestLayout();
                Log.d("ClassDetail", "Switched to tab: " + tabTitles[position]);
            }
        });
    }

    /**
     * 🔧 SETUP click listeners
     */
    private void setupClickListeners() {
        // Notification button click
        binding.btnStudentScheduleClassDetailNotification.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show();
            viewModel.onNotificationClicked();
        });
    }

    /**
     * 🔧 OBSERVE ViewModel LiveData
     */
    private void observeViewModel() {
        // 🔧 CHỈ observe loading state cho attendance data
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Optional: show loading indicator for attendance data
            Log.d("ClassDetail", "Loading attendance data: " + isLoading);
        });

        // 🔧 BỎ observe classDetail vì đã có data từ arguments

    }

    /**
     * 🔧 STATIC method để tạo fragment với sessionId
     */
    public static StudentScheduleClassDetailFragment newInstance(String sessionId) {
        StudentScheduleClassDetailFragment fragment = new StudentScheduleClassDetailFragment();
        Bundle args = new Bundle();
        args.putString("sessionId", sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 🔧 THÊM method để tạo fragment với full arguments
     */
    public static StudentScheduleClassDetailFragment newInstanceWithData(
            String sessionId, String courseName, String sectionCode,
            String room, String lecturer, String startTime, String endTime, String dayOfWeek) {

        StudentScheduleClassDetailFragment fragment = new StudentScheduleClassDetailFragment();
        Bundle args = new Bundle();

        // Core session info
        args.putString("sessionId", sessionId);
        args.putString("courseName", courseName);
        args.putString("sectionCode", sectionCode);
        args.putString("room", room);
        args.putString("lecturer", lecturer);

        // Timing info
        args.putString("startTime", startTime);
        args.putString("endTime", endTime);
        args.putString("dayOfWeek", dayOfWeek);

        fragment.setArguments(args);
        return fragment;
    }
}
