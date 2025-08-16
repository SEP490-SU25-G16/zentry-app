package vn.edu.fpt.zentryapp.notification.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.notification.adapter.NotificationAdapter;
import vn.edu.fpt.zentryapp.notification.data.NotificationItem;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class UnreadNotificationsFragment extends Fragment {

    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Use shared ViewModel from parent NotificationFragment
        viewModel = new ViewModelProvider(getParentFragment()).get(NotificationViewModel.class);

        adapter = new NotificationAdapter(new ArrayList<>(), new NotificationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(NotificationItem item) {
                viewModel.markAsRead(item.getId(), requireContext());
                handleNotificationClick(item);
            }

            @Override
            public void onSeeMoreClick() {
                // Optional: implement local pagination if needed
            }
        });
        recyclerView.setAdapter(adapter);

        // Add scroll listener for infinite scroll (optional if implementing local pagination)
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // No-op for now
            }
        });

        // Observe ALL notifications and locally filter UNREAD
        viewModel.allNotifications().observe(getViewLifecycleOwner(), allNotifications -> {
            if (allNotifications != null) {
                List<NotificationItem> unread = new java.util.ArrayList<>();
                for (NotificationItem n : allNotifications) {
                    if (!n.isRead()) unread.add(n);
                }
                Log.d("UnreadNotificationsFragment", "🔄 Unread list recomputed: " + unread.size());
                adapter.setItems(unread);
                adapter.notifyDataSetChanged();
                updateEmptyState(unread.isEmpty());
            }
        });

        // Removed: per-fragment load/reset/applyFilter (handled by parent)
    }

    private void handleNotificationClick(NotificationItem item) {
        if (item == null) return;
        try {
            String raw = item.getRawData();
            if (raw != null) {
                org.json.JSONObject json = new org.json.JSONObject(raw);
                String type = json.optString("type", "");
                if ("FACE_VERIFICATION_REQUEST".equalsIgnoreCase(type)) {
                    String deeplink = json.optString("deeplink", "");
                    if (!deeplink.isEmpty()) {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(deeplink));
                        startActivity(intent);
                    } else {
                        androidx.navigation.NavController navController = androidx.navigation.fragment.NavHostFragment.findNavController(this);
                        navController.navigate(vn.edu.fpt.zentryapp.R.id.studentSettingVerifyFaceIdFragment);
                    }
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh filter khi fragment được hiển thị lại
        // Removed: per-fragment load/reset/applyFilter (handled by parent)
    }

    // 🔧 NEW: Method to refresh notifications
    public void refreshNotifications() {
        try {
            Log.d("UnreadNotificationsFragment", "🔄 Refreshing unread notifications");
            
            // Get current unread notifications from ViewModel
            List<NotificationItem> currentUnreadNotifications = viewModel.getNotifications().getValue();
            if (currentUnreadNotifications != null) {
                Log.d("UnreadNotificationsFragment", "📊 Refreshing with " + currentUnreadNotifications.size() + " unread notifications");
                
                // Update adapter
                if (adapter != null) {
                    adapter.setItems(currentUnreadNotifications);
                    adapter.notifyDataSetChanged();
                    Log.d("UnreadNotificationsFragment", "✅ Adapter updated successfully");
                }
                
                // Update empty state
                updateEmptyState(currentUnreadNotifications.isEmpty());
            } else {
                Log.w("UnreadNotificationsFragment", "⚠️ No unread notifications available to refresh");
            }
            
        } catch (Exception e) {
            Log.e("UnreadNotificationsFragment", "❌ Error refreshing notifications", e);
        }
    }

    // 🔧 NEW: Method to update empty state
    private void updateEmptyState(boolean isEmpty) {
        try {
            View emptyStateView = getView().findViewById(R.id.notificationEmptyState);
            if (emptyStateView != null) {
                emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e("UnreadNotificationsFragment", "❌ Error updating empty state", e);
        }
    }
}
