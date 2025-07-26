package vn.edu.fpt.zentryapp.notification.data;

public class NotificationItem {
    private String id;
    private String title;
    private String message;
    private String timestamp;
    private boolean isRead;

    public NotificationItem(String id, String title, String message, String timestamp, boolean isRead) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }

    public void setRead(boolean read) { isRead = read; }
}
