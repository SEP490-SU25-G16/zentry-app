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

public class AllNotificationsFragment extends Fragment {

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

        // Observe ALL notifications directly (one source of truth)
        viewModel.allNotifications().observe(getViewLifecycleOwner(), allNotifications -> {
            if (allNotifications != null) {
                Log.d("AllNotificationsFragment", "🔄 AllNotifications changed, auto-refreshing UI with " + allNotifications.size() + " notifications");
                adapter.setItems(allNotifications);
                adapter.notifyDataSetChanged();
                updateEmptyState(allNotifications.isEmpty());
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
                        // ✅ NEW: Sử dụng startActivity để ẩn navbar hoàn toàn
                        android.content.Intent verifyIntent = new android.content.Intent(requireContext(), vn.edu.fpt.zentryapp.faceid.ui.setting.StudentSettingVerifyFaceIdActivity.class);
                        startActivity(verifyIntent);
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
            Log.d("AllNotificationsFragment", "🔄 Refreshing all notifications");
            
            // Get current notifications from ViewModel
            List<NotificationItem> currentNotifications = viewModel.allNotifications().getValue();
            if (currentNotifications != null) {
                Log.d("AllNotificationsFragment", "📊 Refreshing with " + currentNotifications.size() + " notifications");
                
                // Update adapter
                if (adapter != null) {
                    adapter.setItems(currentNotifications);
                    adapter.notifyDataSetChanged();
                    Log.d("AllNotificationsFragment", "✅ Adapter updated successfully");
                }
                
                // Update empty state
                updateEmptyState(currentNotifications.isEmpty());
            } else {
                Log.w("AllNotificationsFragment", "⚠️ No notifications available to refresh");
            }
            
        } catch (Exception e) {
            Log.e("AllNotificationsFragment", "❌ Error refreshing notifications", e);
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
            Log.e("AllNotificationsFragment", "❌ Error updating empty state", e);
        }
    }


}

