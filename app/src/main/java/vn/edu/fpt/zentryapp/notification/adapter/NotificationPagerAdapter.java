package vn.edu.fpt.zentryapp.notification.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import vn.edu.fpt.zentryapp.notification.ui.AllNotificationsFragment;
import vn.edu.fpt.zentryapp.notification.ui.UnreadNotificationsFragment;;

public class NotificationPagerAdapter extends FragmentStateAdapter {

    public NotificationPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }


    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new UnreadNotificationsFragment();
        } else {
            return new AllNotificationsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Tất cả & Chưa đọc
    }
}
