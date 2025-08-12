package vn.edu.fpt.zentryapp.notification.push;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.notification.data.api.NotificationApiService;
import vn.edu.fpt.zentryapp.notification.data.model.NotificationDto;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Background service to refresh notifications via API when push arrives.
 */
public class NotificationRefreshService extends JobIntentService {
    private static final String TAG = "NotifRefreshService";
    private static final int JOB_ID = 1001;
    private static final String EXTRA_USER_ID = "user_id";

    public static void enqueueWork(Context context, String userId) {
        Intent i = new Intent(context, NotificationRefreshService.class);
        i.putExtra(EXTRA_USER_ID, userId);
        enqueueWork(context, NotificationRefreshService.class, JOB_ID, i);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String userId = intent.getStringExtra(EXTRA_USER_ID);
        if (userId == null) return;

        NotificationApiService api = ApiClient.getClient(getApplicationContext()).create(NotificationApiService.class);
        api.getNotifications(userId).enqueue(new Callback<List<NotificationDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<NotificationDto>> call, @NonNull Response<List<NotificationDto>> response) {
                // No direct UI reference here; UI observes ViewModel. Optionally, send a local broadcast to prompt VM reload.
                Log.d(TAG, "Notifications refreshed via background service");
            }

            @Override
            public void onFailure(@NonNull Call<List<NotificationDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to refresh notifications", t);
            }
        });
    }
}


