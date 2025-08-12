package vn.edu.fpt.zentryapp.notification.sharedviewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import android.util.Log;
import android.content.Context;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.notification.data.api.NotificationApiService;
import vn.edu.fpt.zentryapp.notification.data.model.NotificationDto;

import lombok.Getter;
import vn.edu.fpt.zentryapp.notification.data.NotificationItem;

public class NotificationViewModel extends ViewModel {

    private final MutableLiveData<List<NotificationItem>> allNotifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<NotificationItem>> filteredNotifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> showSeeMoreButton = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> unseenCount = new MutableLiveData<>(0);
    private boolean isDataLoaded = false;
    private String lastLoadedUserId = null;
    
    // Pagination constants
    private static final int INITIAL_LOAD_COUNT = 6; // Số thông báo load lần đầu
    private static final int PAGE_SIZE = 4; // Số thông báo load mỗi lần "See More"
    private int currentDisplayCount = INITIAL_LOAD_COUNT; // Số thông báo hiện đang hiển thị
    private boolean seeMoreClicked = false; // Flag để biết đã click "See More" chưa

    public enum FilterType {
        ALL,
        UNREAD
    }

    @Getter
    private FilterType currentFilter = FilterType.ALL;

    public LiveData<List<NotificationItem>> getNotifications() {
        return filteredNotifications;
    }
    
    public LiveData<Boolean> shouldShowSeeMoreButton() {
        return showSeeMoreButton;
    }
    
    public LiveData<Integer> getUnseenCount() {
        return unseenCount;
    }

    public void loadNotifications() {
        if (isDataLoaded) return;

        String userId = null;
        try {
            // NotificationViewModel không có context, nên cần context từ nơi gọi (fragment/activity)
            // Ở đây fallback: dùng ApiClient.getClient(null) vốn yêu cầu context cho AuthInterceptor.
            // Lấy userId từ AuthManager cần context → tạm bỏ qua và để caller truyền userId vào hàm mới.
        } catch (Exception ignored) {}

        if (userId == null || userId.isEmpty()) {
            allNotifications.setValue(new ArrayList<>());
            applyFilter(currentFilter);
            isDataLoaded = true;
            updateUnseenCount();
            return;
        }

        // No context here; skip calling API in this overload
        // Caller should use loadNotifications(userId, context)
        NotificationApiService api = ApiClient.getClient(null).create(NotificationApiService.class);
        api.getNotifications(userId).enqueue(new Callback<List<NotificationDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<NotificationDto>> call, @NonNull Response<List<NotificationDto>> response) {
                List<NotificationItem> items = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    for (NotificationDto dto : response.body()) {
                        items.add(new NotificationItem(
                                dto.getId(),
                                dto.getTitle(),
                                dto.getBody(),
                                dto.getCreatedAt(),
                                dto.isRead(),
                                false,
                                dto.getData()
                        ));
                    }
                } else {
                    Log.e("NotificationVM", "HTTP Error loading notifications: " + response.code());
                }

                allNotifications.setValue(items);
                applyFilter(currentFilter);
                isDataLoaded = true;
                updateUnseenCount();
            }

            @Override
            public void onFailure(@NonNull Call<List<NotificationDto>> call, @NonNull Throwable t) {
                Log.e("NotificationVM", "Network error loading notifications", t);
                allNotifications.setValue(new ArrayList<>());
                applyFilter(currentFilter);
                isDataLoaded = true;
                updateUnseenCount();
            }
        });
    }

    // Overload cho phép caller truyền vào userId (lấy từ AuthManager ở layer có context)
    public void loadNotifications(String userId) {
        // Reload if user changes
        if (lastLoadedUserId == null || !lastLoadedUserId.equals(userId)) {
            clearDataForNewUser();
        } else if (isDataLoaded) {
            return;
        }
        if (userId == null || userId.isEmpty()) {
            allNotifications.setValue(new ArrayList<>());
            applyFilter(currentFilter);
            isDataLoaded = true;
            updateUnseenCount();
            return;
        }

        NotificationApiService api = ApiClient.getClient(null).create(NotificationApiService.class);
        api.getNotifications(userId).enqueue(new Callback<List<NotificationDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<NotificationDto>> call, @NonNull Response<List<NotificationDto>> response) {
                List<NotificationItem> items = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    for (NotificationDto dto : response.body()) {
                        items.add(new NotificationItem(
                                dto.getId(),
                                dto.getTitle(),
                                dto.getBody(),
                                dto.getCreatedAt(),
                                dto.isRead(),
                                false,
                                dto.getData()
                        ));
                    }
                } else {
                    Log.e("NotificationVM", "HTTP Error loading notifications: " + response.code());
                }

                allNotifications.setValue(items);
                applyFilter(currentFilter);
                isDataLoaded = true;
                lastLoadedUserId = userId;
                updateUnseenCount();
            }

            @Override
            public void onFailure(@NonNull Call<List<NotificationDto>> call, @NonNull Throwable t) {
                Log.e("NotificationVM", "Network error loading notifications", t);
                allNotifications.setValue(new ArrayList<>());
                applyFilter(currentFilter);
                isDataLoaded = true;
                lastLoadedUserId = userId;
                updateUnseenCount();
            }
        });
    }

    // Preferred overload: caller supplies context (for AuthInterceptor) and userId
    public void loadNotifications(String userId, Context context) {
        // Reload if user changes
        if (lastLoadedUserId == null || !lastLoadedUserId.equals(userId)) {
            clearDataForNewUser();
        } else if (isDataLoaded) {
            return;
        }
        if (userId == null || userId.isEmpty()) {
            allNotifications.setValue(new ArrayList<>());
            applyFilter(currentFilter);
            isDataLoaded = true;
            updateUnseenCount();
            return;
        }

        NotificationApiService api = ApiClient.getClient(context).create(NotificationApiService.class);
        api.getNotifications(userId).enqueue(new Callback<List<NotificationDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<NotificationDto>> call, @NonNull Response<List<NotificationDto>> response) {
                List<NotificationItem> items = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    for (NotificationDto dto : response.body()) {
                        items.add(new NotificationItem(
                                dto.getId(),
                                dto.getTitle(),
                                dto.getBody(),
                                dto.getCreatedAt(),
                                dto.isRead(),
                                false,
                                dto.getData()
                        ));
                    }
                } else {
                    Log.e("NotificationVM", "HTTP Error loading notifications: " + response.code());
                }

                allNotifications.setValue(items);
                applyFilter(currentFilter);
                isDataLoaded = true;
                lastLoadedUserId = userId;
                updateUnseenCount();
            }

            @Override
            public void onFailure(@NonNull Call<List<NotificationDto>> call, @NonNull Throwable t) {
                Log.e("NotificationVM", "Network error loading notifications", t);
                allNotifications.setValue(new ArrayList<>());
                applyFilter(currentFilter);
                isDataLoaded = true;
                lastLoadedUserId = userId;
                updateUnseenCount();
            }
        });
    }

    public void applyFilter(FilterType filter) {
        currentFilter = filter;
        List<NotificationItem> all = allNotifications.getValue();

        if (all == null || all.isEmpty()) {
            filteredNotifications.setValue(new ArrayList<>());
            showSeeMoreButton.setValue(false);
            return;
        }

        List<NotificationItem> filtered;
        if (filter == FilterType.UNREAD) {
            filtered = all.stream().filter(n -> !n.isRead()).collect(Collectors.toList());
        } else {
            filtered = new ArrayList<>(all);
        }

        // Apply pagination
        applyPagination(filtered);
    }
    
    private void applyPagination(List<NotificationItem> fullList) {
        if (fullList.isEmpty()) {
            filteredNotifications.setValue(new ArrayList<>());
            showSeeMoreButton.setValue(false);
            return;
        }
        
        // Giới hạn số lượng hiển thị dựa trên currentDisplayCount
        int itemsToShow = Math.min(currentDisplayCount, fullList.size());
        List<NotificationItem> paginatedList = fullList.subList(0, itemsToShow);
        
        filteredNotifications.setValue(paginatedList);
        
        // Hiển thị nút "See More" nếu còn thông báo chưa hiển thị và chưa click "See More"
        boolean hasMoreItems = fullList.size() > itemsToShow;
        showSeeMoreButton.setValue(hasMoreItems && !seeMoreClicked);
    }
    
    public void loadMoreNotifications() {
        currentDisplayCount += PAGE_SIZE;
        seeMoreClicked = true; // Đánh dấu đã click "See More"
        applyFilter(currentFilter); // Re-apply filter với display count mới
    }
    
    public void resetPagination() {
        currentDisplayCount = INITIAL_LOAD_COUNT;
        seeMoreClicked = false;
    }

    private void clearDataForNewUser() {
        isDataLoaded = false;
        allNotifications.setValue(new ArrayList<>());
        filteredNotifications.setValue(new ArrayList<>());
        resetPagination();
        updateUnseenCount();
    }
    
    public boolean canLoadMoreByScroll() {
        return seeMoreClicked;
    }
    
    /**
     * Đánh dấu tất cả thông báo là đã seen khi người dùng vào màn hình notification
     */
    public void markAllAsSeen() {
        List<NotificationItem> all = allNotifications.getValue();
        if (all == null) return;
        
        boolean hasChanges = false;
        for (NotificationItem item : all) {
            if (!item.isSeen()) {
                item.setSeen(true);
                hasChanges = true;
            }
        }
        
        if (hasChanges) {
            allNotifications.setValue(new ArrayList<>(all));
            applyFilter(currentFilter);
            updateUnseenCount();
        }
    }


    public void deleteNotification(String id) {
        List<NotificationItem> current = allNotifications.getValue();
        if (current != null) {
            boolean changed = current.removeIf(n -> n.getId().equals(id));
            if (changed) {
                allNotifications.setValue(current);
                applyFilter(currentFilter);
            }
        }
    }

    public void markAsRead(String id) {
        List<NotificationItem> current = allNotifications.getValue();
        if (current != null) {
            boolean changed = false;
            for (NotificationItem n : current) {
                if (n.getId().equals(id) && !n.isRead()) {
                    n.setRead(true);  // ✅ mutation trực tiếp object gốc
                    changed = true;
                    break;
                }
            }
            if (changed) {
                allNotifications.setValue(current); // trigger lại observers
                applyFilter(currentFilter);         // 🔁 apply lại filter hiện tại
            }
        }
    }

    private void updateUnseenCount() {
        List<NotificationItem> all = allNotifications.getValue();
        if (all == null) {
            unseenCount.setValue(0);
            return;
        }
        int count = 0;
        for (NotificationItem item : all) {
            if (!item.isSeen()) { // Đếm thông báo chưa được seen
                count++;
            }
        }
        unseenCount.setValue(count);
    }

    // 🧠 Helper: kiểm tra 2 list có giống nhau không (tránh re-render)
    private boolean isSameList(List<NotificationItem> oldList, List<NotificationItem> newList) {
        if (oldList == null || newList == null) return false;
        if (oldList.size() != newList.size()) return false;
        for (int i = 0; i < oldList.size(); i++) {
            if (!oldList.get(i).getId().equals(newList.get(i).getId()) ||
                    oldList.get(i).isRead() != newList.get(i).isRead()) {
                return false;
            }
        }
        return true;
    }

}
