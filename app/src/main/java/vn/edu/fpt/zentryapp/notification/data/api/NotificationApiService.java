package vn.edu.fpt.zentryapp.notification.data.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import vn.edu.fpt.zentryapp.notification.data.model.NotificationDto;

/** Notification API */
public interface NotificationApiService {
    @GET("api/notifications")
    Call<List<NotificationDto>> getNotifications(@Query("userId") String userId);
}


