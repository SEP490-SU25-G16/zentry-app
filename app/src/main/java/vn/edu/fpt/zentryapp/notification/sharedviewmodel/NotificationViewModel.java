package vn.edu.fpt.zentryapp.notification.sharedviewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Arrays;
import java.util.List;

import vn.edu.fpt.zentryapp.notification.data.NotificationItem;

public class NotificationViewModel extends ViewModel {
    private final MutableLiveData<List<NotificationItem>> notifications = new MutableLiveData<>();

    public LiveData<List<NotificationItem>> getNotifications() {
        return notifications;
    }

    public void loadNotifications() {
        // Dữ liệu mẫu (sau này thay bằng call API hoặc FCM cache)
        List<NotificationItem> mockData = Arrays.asList(
                new NotificationItem("1", "Chào mừng", "Cảm ơn bạn đã sử dụng Zentry", "2025-07-26 14:30", false),
                new NotificationItem("2", "Cập nhật", "Ứng dụng đã có bản mới", "2025-07-25 09:00", true)
        );
        notifications.setValue(mockData);
    }
}
