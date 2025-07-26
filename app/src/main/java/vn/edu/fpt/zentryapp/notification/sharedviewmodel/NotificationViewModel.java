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

    public void loadNotifications() {
        // Chỉ load dữ liệu một lần
        if (isDataLoaded) {
            return;
        }
        
        List<NotificationItem> mockData = new ArrayList<>();
        mockData.add(new NotificationItem("1", "Điểm danh buổi sáng", "Bạn cần điểm danh khuôn mặt trước 9h00 hôm nay.", "Jul 26, 2025", false));
        mockData.add(new NotificationItem("2", "Nhắc lịch học", "Lớp Pháp luật đại cương học bù lúc 15h hôm nay.", "Jul 25, 2025", false));
        mockData.add(new NotificationItem("3", "Bảo trì hệ thống", "Cổng điểm danh sẽ tạm ngưng từ 23h00 đến 01h00.", "Jul 24, 2025", true));
        mockData.add(new NotificationItem("4", "Thông báo điểm danh", "Bạn chưa điểm danh môn Kỹ thuật lập trình sáng nay.", "Jul 26, 2025", false));
        mockData.add(new NotificationItem("5", "Lịch học thay đổi", "Lớp Hệ điều hành chuyển sang phòng 302.", "Jul 26, 2025", true));
        mockData.add(new NotificationItem("6", "Thông báo mới", "Trường phát hành thẻ sinh viên mới cho K65.", "Jul 23, 2025", true));
        mockData.add(new NotificationItem("7", "Tuyển thành viên CLB AI", "CLB AI tuyển thành viên mới đến hết tháng này.", "Jul 22, 2025", false));
        mockData.add(new NotificationItem("8", "Điểm danh muộn", "Bạn vừa điểm danh trễ môn Xác suất Thống kê.", "Jul 25, 2025", true));
        mockData.add(new NotificationItem("9", "Lịch thi giữa kỳ", "Môn Giải tích 2 sẽ thi vào 01/08 tại phòng A101.", "Jul 20, 2025", true));
        mockData.add(new NotificationItem("10", "Nộp học phí", "Hạn nộp học phí đợt 2 là ngày 31/07/2025.", "Jul 18, 2025", false));
        mockData.add(new NotificationItem("11", "Thông báo từ thư viện", "Bạn có sách quá hạn cần trả gấp trước 30/07.", "Jul 17, 2025", false));
        mockData.add(new NotificationItem("12", "Điểm rèn luyện", "Điểm rèn luyện HK2 đã được cập nhật trên hệ thống.", "Jul 16, 2025", true));
        mockData.add(new NotificationItem("13", "Cuộc thi sáng tạo trẻ", "Mời bạn tham gia cuộc thi với giải thưởng hấp dẫn.", "Jul 15, 2025", false));
        mockData.add(new NotificationItem("14", "Thông báo học bổng", "Danh sách xét học bổng HK2 đã được công bố.", "Jul 14, 2025", true));
        mockData.add(new NotificationItem("15", "Chấm dứt học phần", "Bạn đã rút học phần Toán cao cấp.", "Jul 13, 2025", true));

        allNotifications.setValue(mockData);
        isDataLoaded = true; // Đánh dấu đã load dữ liệu
        applyFilter(currentFilter);
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
        // Chỉ cho phép infinite scroll sau khi đã click "See More"
        return seeMoreClicked;
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
