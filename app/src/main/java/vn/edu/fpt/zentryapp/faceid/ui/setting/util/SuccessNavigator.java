package vn.edu.fpt.zentryapp.faceid.ui.setting.util;

import android.content.Context;
import android.content.Intent;

import vn.edu.fpt.zentryapp.faceid.ui.setting.success.FaceIdSuccessActivity;

public final class SuccessNavigator {
    private SuccessNavigator() {}

    public static void navigateToSuccess(Context context,
                                         String userId,
                                         String successMessage,
                                         String bitmapPath) {
        if (context == null) return;
        Intent successIntent = FaceIdSuccessActivity.createIntent(context, userId, successMessage, bitmapPath);
        context.startActivity(successIntent);
    }
}


