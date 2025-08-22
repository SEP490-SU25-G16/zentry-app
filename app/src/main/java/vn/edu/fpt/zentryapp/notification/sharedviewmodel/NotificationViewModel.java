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
import org.json.JSONException;

public class NotificationViewModel extends ViewModel {

    private final MutableLiveData<List<NotificationItem>> allNotifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<NotificationItem>> filteredNotifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> showSeeMoreButton = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> unseenCount = new MutableLiveData<>(0);
    private boolean isDataLoaded = false;
    private String lastLoadedUserId = null;
    
    // Pagination constants
    private static final int INITIAL_LOAD_COUNT = 6; // Số thông báo load lần đầu
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
    
    public LiveData<List<NotificationItem>> allNotifications() {
        return allNotifications;
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
                        // ✅ NEW: Extract expiration from notification data
                        String expiresAt = extractExpirationFromData(dto.getData());
                        
                        items.add(new NotificationItem(
                                dto.getId(),
                                dto.getTitle(),
                                dto.getBody(),
                                dto.getCreatedAt(),
                                dto.isRead(),
                                false,
                                dto.getData(),
                                expiresAt // ✅ NEW: 8th parameter
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
                        // ✅ NEW: Extract expiration from notification data
                        String expiresAt = extractExpirationFromData(dto.getData());
                        
                        items.add(new NotificationItem(
                                dto.getId(),
                                dto.getTitle(),
                                dto.getBody(),
                                dto.getCreatedAt(),
                                dto.isRead(),
                                false,
                                dto.getData(),
                                expiresAt // ✅ NEW: 8th parameter
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
    
    // ✅ NEW: Helper method to extract expiration from notification data
    private String extractExpirationFromData(String data) {
        if (data == null || data.isEmpty()) {
            android.util.Log.d("NotificationVM", "🔍 No data to extract expiration from");
            return null;
        }
        
        try {
            org.json.JSONObject json = new org.json.JSONObject(data);
            String type = json.optString("type", "");
            android.util.Log.d("NotificationVM", "🔍 Notification type: " + type);
            
            // Only extract expiration for Face ID verification requests
            if ("FACE_VERIFICATION_REQUEST".equalsIgnoreCase(type)) {
                String expiresAt = json.optString("expiresAt", "");
                android.util.Log.d("NotificationVM", "🔍 Raw expiresAt from JSON: " + expiresAt);
                
                if (expiresAt != null && !expiresAt.isEmpty()) {
                    android.util.Log.d("NotificationVM", "✅ Extracted expiration for Face ID request: " + expiresAt);
                    return expiresAt;
                } else {
                    android.util.Log.w("NotificationVM", "⚠️ Face ID request but no expiration found");
                }
            } else {
                android.util.Log.d("NotificationVM", "🔍 Not a Face ID request, no expiration needed");
            }
            
            return null; // No expiration for other notification types
            
        } catch (JSONException e) {
            android.util.Log.w("NotificationVM", "⚠️ Failed to parse notification data for expiration: " + data, e);
            return null;
        }
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

    // 🔧 NEW: Mark all notifications as seen and read via API
    public void markAllAsSeen(Context context) {
        // Update local state immediately
        markAllAsSeen();
        
        // Also mark all as read via API if we have userId
        if (lastLoadedUserId != null && !lastLoadedUserId.isEmpty()) {
            markAllAsRead(context);
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
                updateUnseenCount();               // 🔁 update unseen count
            }
        }
    }

    // 🔧 NEW: Mark single notification as read via API
    public void markAsRead(String id, Context context) {
        if (id == null || id.isEmpty()) return;
        
        // Update local state immediately for better UX
        markAsRead(id);
        
        // Call API to persist the change
        NotificationApiService api = ApiClient.getClient(context).create(NotificationApiService.class);
        api.markNotificationAsRead(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("NotificationVM", "✅ Marked notification " + id + " as read via API");
                } else {
                    Log.e("NotificationVM", "❌ Failed to mark notification as read: " + response.code());
                    // Optionally revert local state on API failure
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("NotificationVM", "❌ Network error marking notification as read", t);
                // Optionally revert local state on network failure
            }
        });
    }

    // 🔧 NEW: Mark all notifications as read via API
    public void markAllAsRead(Context context) {
        String userId = lastLoadedUserId;
        if (userId == null || userId.isEmpty()) return;
        
        // Update local state immediately for better UX
        markAllAsReadLocal();
        
        // Call API to persist the change
        NotificationApiService api = ApiClient.getClient(context).create(NotificationApiService.class);
        api.markAllNotificationsAsRead(userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("NotificationVM", "✅ Marked all notifications as read via API");
                } else {
                    Log.e("NotificationVM", "❌ Failed to mark all notifications as read: " + response.code());
                    // Optionally revert local state on API failure
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("NotificationVM", "❌ Network error marking all notifications as read", t);
                // Optionally revert local state on network failure
            }
        });
    }

    // 🔧 NEW: Mark all notifications as read locally (without API call)
    private void markAllAsReadLocal() {
        List<NotificationItem> current = allNotifications.getValue();
        if (current == null) return;
        
        boolean changed = false;
        for (NotificationItem n : current) {
            if (!n.isRead()) {
                n.setRead(true);
                changed = true;
            }
        }
        
        if (changed) {
            allNotifications.setValue(new ArrayList<>(current));
            applyFilter(currentFilter);
            updateUnseenCount();
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

    // 🔧 NEW: Force refresh method that actually works
    public void forceRefresh(String userId, Context context) {
        Log.d("NotificationVM", "🔄 FORCE REFRESH: Bypassing cache and calling API directly");
        
        // Reset all flags to force reload
        isDataLoaded = false;
        lastLoadedUserId = null;
        
        // Clear current data immediately
        allNotifications.setValue(new ArrayList<>());
        filteredNotifications.setValue(new ArrayList<>());
        unseenCount.setValue(0);
        
        // Reset pagination
        resetPagination();
        
        // 🔧 FIX: Call API directly without going through loadNotifications
        if (userId != null && !userId.isEmpty()) {
            NotificationApiService api = ApiClient.getClient(context).create(NotificationApiService.class);
            api.getNotifications(userId).enqueue(new Callback<List<NotificationDto>>() {
                @Override
                public void onResponse(@NonNull Call<List<NotificationDto>> call, @NonNull Response<List<NotificationDto>> response) {
                    List<NotificationItem> items = new ArrayList<>();
                    if (response.isSuccessful() && response.body() != null) {
                        for (NotificationDto dto : response.body()) {
                            // ✅ NEW: Extract expiration from notification data
                            String expiresAt = extractExpirationFromData(dto.getData());
                            
                            items.add(new NotificationItem(
                                    dto.getId(),
                                    dto.getTitle(),
                                    dto.getBody(),
                                    dto.getCreatedAt(),
                                    dto.isRead(),
                                    false,
                                    dto.getData(),
                                    expiresAt // ✅ NEW: 8th parameter
                            ));
                        }
                        Log.d("NotificationVM", "✅ FORCE REFRESH: loaded " + items.size() + " notifications");
                    } else {
                        Log.e("NotificationVM", "❌ FORCE REFRESH HTTP Error: " + response.code());
                    }

                    // 🔧 FIX: Update data in correct order to trigger observers
                    Log.d("NotificationVM", "🔄 Updating data in correct order...");
                    
                    // 1. Update allNotifications first
                    allNotifications.setValue(items);
                    Log.d("NotificationVM", "✅ allNotifications updated with " + items.size() + " items");
                    
                    // 2. Apply filter to update filteredNotifications
                    applyFilter(currentFilter);
                    Log.d("NotificationVM", "✅ filter applied, filteredNotifications updated");
                    
                    // 3. Update flags
                    isDataLoaded = true;
                    lastLoadedUserId = userId;
                    
                    // 4. Update unseen count
                    updateUnseenCount();
                    Log.d("NotificationVM", "✅ unseenCount updated: " + unseenCount.getValue());
                    
                    Log.d("NotificationVM", "✅ FORCE REFRESH completed successfully");
                }

                @Override
                public void onFailure(@NonNull Call<List<NotificationDto>> call, @NonNull Throwable t) {
                    Log.e("NotificationVM", "❌ FORCE REFRESH failed", t);
                }
            });
        }
    }

}
