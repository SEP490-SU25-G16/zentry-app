package vn.edu.fpt.zentryapp.notification.ui;

import android.os.Bundle;
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
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.notification.adapter.NotificationPagerAdapter;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;

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

        // Load initial data
        viewModel.loadNotifications();
    }

    // Alternative navigation method using FragmentManager
    private void navigateToStudentSettingNotificationFragment() {
        try {
            // Tìm StudentSettingNotificationFragment class với đường dẫn đúng
            Class<?> fragmentClass = Class.forName("vn.edu.fpt.zentryapp.student.ui.setting.StudentSettingNotificationFragment");
            Fragment fragment = (Fragment) fragmentClass.newInstance();
            
            // Tìm container ID
            int containerId = findFragmentContainer();
            
            // Replace current fragment with StudentSettingNotificationFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(containerId, fragment)
                    .addToBackStack(null)
                    .commit();
                    
//            Toast.makeText(getContext(), "Navigating to Notification Settings...", Toast.LENGTH_SHORT).show();
            
        } catch (ClassNotFoundException e) {
            Toast.makeText(getContext(), "StudentSettingNotificationFragment not found", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Could not navigate to settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Helper method to find the fragment container
    private int findFragmentContainer() {
        // Thử tìm container của fragment hiện tại
        View currentView = getView();
        if (currentView != null) {
            ViewGroup parent = (ViewGroup) currentView.getParent();
            if (parent != null) {
                return parent.getId();
            }
        }
        
        // Fallback: thử các container ID có thể tồn tại
        try {
            // Kiểm tra nav_host_fragment nếu tồn tại
            View navHost = requireActivity().findViewById(R.id.nav_host_fragment);
            if (navHost != null) {
                return R.id.nav_host_fragment;
            }
        } catch (Exception e) {
            // ID không tồn tại
        }
        
        // Fallback cuối cùng: sử dụng android.R.id.content
        return android.R.id.content;
    }
}
