package vn.edu.fpt.zentryapp.notification.data.model;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class NotificationDto {
    @SerializedName("Id")
    private String id;

    @SerializedName("RecipientUserId")
    private String recipientUserId;

    @SerializedName("Title")
    private String title;

    @SerializedName("Body")
    private String body;

    @SerializedName("CreatedAt")
    private String createdAt;

    @SerializedName("IsRead")
    private boolean isRead;

    @SerializedName("Data")
    private String data; // raw JSON string
}


