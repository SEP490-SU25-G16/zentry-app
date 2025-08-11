package vn.edu.fpt.zentryapp.notification.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItem implements Serializable {
    private String id;
    private String title;
    private String message;
    private String timestamp;
    private boolean isRead;
    private boolean isSeen; // Đánh dấu thông báo đã được seen (xem qua) chưa
}
