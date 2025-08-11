package vn.edu.fpt.zentryapp.notification.sharedviewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import vn.edu.fpt.zentryapp.notification.data.NotificationItem;

public class NotificationViewModel extends ViewModel {

    private final MutableLiveData<List<NotificationItem>> allNotifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<NotificationItem>> filteredNotifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> showSeeMoreButton = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> unseenCount = new MutableLiveData<>(0);
    private boolean isDataLoaded = false;
    
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
        // Chỉ load dữ liệu một lần
        if (isDataLoaded) {
            return;
        }
        
        // Tạo dữ liệu mock
        List<NotificationItem> mockData = new ArrayList<>();
        
        // Mock notifications với một số chưa được seen
        mockData.add(new NotificationItem("1", "Yêu cầu điểm danh", 
            "Vui lòng thực hiện điểm danh bằng khuôn mặt cho buổi học SEP490 - Session 1", 
            "2025-01-28 08:30", false, false)); // Chưa read, chưa seen
        
        mockData.add(new NotificationItem("2", "Vượt quá số buổi vắng", 
            "Bạn đã vắng 3/4 buổi học cho môn SEP490. Vui lòng liên hệ giảng viên.", 
            "2025-01-27 14:20", false, false)); // Chưa read, chưa seen
        
        mockData.add(new NotificationItem("3", "Thay đổi lịch học", 
            "Lịch học môn SEP490 buổi 5 đã được chuyển từ 7:30 sang 9:00 ngày 30/01/2025", 
            "2025-01-26 16:45", true, false)); // Đã read, chưa seen
        
        mockData.add(new NotificationItem("4", "Điểm danh thành công", 
            "Bạn đã điểm danh thành công cho buổi học SEP490 - Session 2", 
            "2025-01-25 08:35", true, true)); // Đã read, đã seen
        
        mockData.add(new NotificationItem("5", "Nhắc nhở điểm danh", 
            "Còn 15 phút nữa hết thời gian điểm danh cho buổi học PRN231", 
            "2025-01-25 07:45", false, false)); // Chưa read, chưa seen
        
        mockData.add(new NotificationItem("6", "Cập nhật điểm số", 
            "Điểm Assignment 1 môn SEP490 đã được cập nhật: 8.5/10", 
            "2025-01-24 15:30", true, true)); // Đã read, đã seen
        
        mockData.add(new NotificationItem("7", "Thông báo nghỉ học", 
            "Buổi học PRN231 ngày 25/01/2025 được chuyển sang ngày 26/01/2025", 
            "2025-01-23 10:15", true, false)); // Đã read, chưa seen
        
        mockData.add(new NotificationItem("8", "Yêu cầu xác thực Face ID", 
            "Hệ thống yêu cầu bạn cập nhật dữ liệu Face ID để cải thiện độ chính xác", 
            "2025-01-22 09:00", false, false)); // Chưa read, chưa seen
        
        mockData.add(new NotificationItem("9", "Điểm danh muộn", 
            "Bạn đã điểm danh muộn 5 phút cho buổi học SWD392", 
            "2025-01-21 08:05", true, true)); // Đã read, đã seen
        
        mockData.add(new NotificationItem("10", "Thông báo bài tập mới", 
            "Bài tập Assignment 2 cho môn SEP490 đã được giao, hạn nộp: 05/02/2025", 
            "2025-01-20 16:00", true, false)); // Đã read, chưa seen
        
        mockData.add(new NotificationItem("11", "Cảnh báo vắng học", 
            "Bạn đã vắng 2 buổi học liên tiếp cho môn PRN231", 
            "2025-01-19 14:30", false, false)); // Chưa read, chưa seen
        
        mockData.add(new NotificationItem("12", "Điểm danh thành công", 
            "Bạn đã điểm danh thành công cho buổi học SWD392 - Session 3", 
            "2025-01-18 08:32", true, true)); // Đã read, đã seen
        
        mockData.add(new NotificationItem("13", "Thay đổi phòng học", 
            "Phòng học môn SEP490 buổi 8 đã được chuyển từ 501 sang 601", 
            "2025-01-17 12:00", true, false)); // Đã read, chưa seen
        
        mockData.add(new NotificationItem("14", "Nhắc nhở nộp bài", 
            "Còn 2 ngày để nộp Assignment 1 cho môn SWD392", 
            "2025-01-16 18:45", true, true)); // Đã read, đã seen
        
        mockData.add(new NotificationItem("15", "Điểm danh bằng khuôn mặt", 
            "Vui lòng thực hiện điểm danh bằng khuôn mặt cho buổi học PRN231 - Session 4", 
            "2025-01-15 07:30", false, false)); // Chưa read, chưa seen

        allNotifications.setValue(mockData);
        applyFilter(currentFilter); // Áp dụng bộ lọc hiện tại
        isDataLoaded = true; // Đánh dấu đã load dữ liệu
        updateUnseenCount(); // Cập nhật số lượng thông báo chưa seen
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
