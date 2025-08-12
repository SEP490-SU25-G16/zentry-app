package vn.edu.fpt.zentryapp.faceid.ui.setting.util;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

public final class ErrorPresenter {
    private ErrorPresenter() {}

    public static void showError(Context context, String title, String message) {
        if (context == null) return;
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    public static void showRetry(Context context, String title, String message,
                                 Runnable onRetry, Runnable onCancel) {
        if (context == null) return;
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Try Again", (d, w) -> { if (onRetry != null) onRetry.run(); })
                .setNegativeButton("Cancel", (d, w) -> { if (onCancel != null) onCancel.run(); })
                .setCancelable(false)
                .show();
    }
}


