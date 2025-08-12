package vn.edu.fpt.zentryapp.notification.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.notification.adapter.NotificationPagerAdapter;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;
import vn.edu.fpt.zentryapp.student.ui.setting.StudentSettingNotificationFragment;

public class NotificationFragment extends Fragment {

    private ImageView btnBack, btnSettings;
    private TextView tvGoToHistory;
    private TabLayout tabNotification;
    private ViewPager2 viewPagerNotification;
    private View emptyStateView;
    private NotificationViewModel viewModel;

    private final String[] tabTitles = new String[]{"All", "Unread"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View bindings
        tabNotification = view.findViewById(R.id.tabNotification);
        viewPagerNotification = view.findViewById(R.id.viewPagerNotification);
        emptyStateView = view.findViewById(R.id.notificationEmptyState);
        tvGoToHistory = view.findViewById(R.id.tvNotificationHistory);
        btnBack = view.findViewById(R.id.btnBack);
        btnSettings = view.findViewById(R.id.btnSettings);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);

        // Set adapter for ViewPager2
        NotificationPagerAdapter pagerAdapter = new NotificationPagerAdapter(this);
        viewPagerNotification.setAdapter(pagerAdapter);

        // Link TabLayout with ViewPager2
        new TabLayoutMediator(tabNotification, viewPagerNotification,
                (tab, position) -> {
                    tab.setText(tabTitles[position]);
                }).attach();

        // Navigation actions
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        btnSettings.setOnClickListener(v -> {
            // Use FragmentManager for manual navigation - more reliable
            navigateToStudentSettingNotificationFragment();
        });
        tvGoToHistory.setOnClickListener(v ->
                Toast.makeText(getContext(), "Navigate to historical notifications...", Toast.LENGTH_SHORT).show());

        // Load initial data from API with userId and context
        String userId = vn.edu.fpt.zentryapp.auth.client.AuthManager.getInstance(requireContext()).getCurrentUserId();
        viewModel.loadNotifications(userId, requireContext());
        
        // Đánh dấu tất cả thông báo là đã seen khi vào màn hình notification
        viewModel.markAllAsSeen();
    }

    // Alternative navigation method using FragmentManager
    private void navigateToStudentSettingNotificationFragment() {
        try {
            Log.d("NotificationFragment", "Navigating to StudentSettingNotificationFragment");
            
            // Tạo fragment với source là NotificationFragment
            Fragment settingsFragment = StudentSettingNotificationFragment.newInstance(
                    StudentSettingNotificationFragment.SOURCE_NOTIFICATION);
            
            // Sử dụng FragmentTransaction để replace và add to back stack
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(((ViewGroup)getView().getParent()).getId(), settingsFragment)
                    .addToBackStack(null) // Sử dụng null để thêm vào back stack mặc định
                    .commit();
                    
        } catch (Exception e) {
            Log.e("NotificationFragment", "Navigation error", e);
            Toast.makeText(getContext(), "Không thể mở cài đặt thông báo", Toast.LENGTH_SHORT).show();
        }
    }
}
