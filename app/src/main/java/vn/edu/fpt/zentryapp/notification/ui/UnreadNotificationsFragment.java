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

        viewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);

        adapter = new NotificationAdapter(new ArrayList<>(), new NotificationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(NotificationItem item) {
                viewModel.markAsRead(item.getId());
            }

            @Override
            public void onSeeMoreClick() {
                viewModel.loadMoreNotifications();
            }
        });
        recyclerView.setAdapter(adapter);

        // Add scroll listener for infinite scroll (chỉ khi đã click "See More")
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && viewModel != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    // Load more khi gần cuối danh sách và đã click "See More" trước đó
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2 && 
                        firstVisibleItemPosition >= 0 && 
                        viewModel.canLoadMoreByScroll()) {
                        viewModel.loadMoreNotifications();
                    }
                }
            }
        });

        viewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            if (notifications != null) {
                adapter.setItems(notifications);
            }
        });
        
        // Observe see more button visibility
        viewModel.shouldShowSeeMoreButton().observe(getViewLifecycleOwner(), shouldShow -> {
            adapter.setSeeMoreVisible(shouldShow);
        });

        // Load dữ liệu trước khi apply filter
        viewModel.loadNotifications();
        // Reset pagination và apply filter khi fragment được tạo
        viewModel.resetPagination();
        viewModel.applyFilter(NotificationViewModel.FilterType.UNREAD);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh filter khi fragment được hiển thị lại
        if (viewModel != null) {
            viewModel.resetPagination();
            viewModel.applyFilter(NotificationViewModel.FilterType.UNREAD);
        }
    }
}
