package com.example.zentry_app.ui.faceid;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;

import vn.edu.fpt.zentryapp.R;

/**
 * Manager class to handle the Face ID verification flow with enhanced UI
 */
public class FaceIdManager {

    private final Activity activity;
    private Dialog currentDialog;
    private int verificationTimeInSeconds = 30; // Default time
    private OnFaceIdVerificationCompleteListener listener;

    /**
     * Interface for Face ID verification completion callback
     */
    public interface OnFaceIdVerificationCompleteListener {
        void onVerificationComplete(boolean success);
    }

    public FaceIdManager(Activity activity) {
        this.activity = activity;
    }

    /**
     * Set the listener for verification completion
     */
    public void setOnFaceIdVerificationCompleteListener(OnFaceIdVerificationCompleteListener listener) {
        this.listener = listener;
    }

    /**
     * Start the Face ID verification flow
     */
    public void startFaceIdVerificationFlow() {
        showInitialRequestDialog();
    }

    /**
     * Show the initial request dialog
     */
    private void showInitialRequestDialog() {
        dismissCurrentDialog();

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_face_id_request_initial, null);
        Button btnContinue = dialogView.findViewById(R.id.btnContinue);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        builder.setCancelable(true);

        currentDialog = builder.create();
        currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        currentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        currentDialog.show();

        btnContinue.setOnClickListener(v -> {
            currentDialog.dismiss();
            showTimeInputDialog();
        });
    }

    /**
     * Show the time input dialog
     */
    private void showTimeInputDialog() {
        dismissCurrentDialog();

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_face_id_time_input, null);
        EditText etMinutes = dialogView.findViewById(R.id.etMinutes);
        EditText etSeconds = dialogView.findViewById(R.id.etSeconds);
        MaterialButton btnStartVerification = dialogView.findViewById(R.id.btnStartVerification);
        TextView tvMinimumTimeNote = dialogView.findViewById(R.id.tvMinimumTimeNote);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        builder.setCancelable(true);

        currentDialog = builder.create();
        currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        currentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        currentDialog.show();

        btnStartVerification.setOnClickListener(v -> {
            // Validate input
            int minutes = 0;
            int seconds = 0;

            if (!etMinutes.getText().toString().isEmpty()) {
                minutes = Integer.parseInt(etMinutes.getText().toString());
            }

            if (!etSeconds.getText().toString().isEmpty()) {
                seconds = Integer.parseInt(etSeconds.getText().toString());
            }

            int totalSeconds = (minutes * 60) + seconds;

            if (totalSeconds < 30) {
                tvMinimumTimeNote.setTextColor(activity.getResources().getColor(android.R.color.holo_red_light));
                return;
            }

            verificationTimeInSeconds = totalSeconds;
            currentDialog.dismiss();
            showProcessingDialog();
        });
    }

    /**
     * Show the processing dialog
     */
    private void showProcessingDialog() {
        dismissCurrentDialog();

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_processing, null);
        TextView tvProcessingMessage = dialogView.findViewById(R.id.tvProcessingMessage);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        builder.setCancelable(false);

        currentDialog = builder.create();
        currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        currentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        currentDialog.show();

        // Simulate processing time
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            tvProcessingMessage.setText("Creating verification session...");
            
            // Simulate second stage of processing
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                tvProcessingMessage.setText("Finalizing settings...");
                
                // Show success after delay
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    currentDialog.dismiss();
                    showSuccessDialog();
                }, 1500);
            }, 1500);
        }, 1500);
    }

    /**
     * Show the success dialog
     */
    private void showSuccessDialog() {
        dismissCurrentDialog();

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_face_id_success, null);
        Button btnBack = dialogView.findViewById(R.id.btnBack);
        TextView tvSuccessDetail = dialogView.findViewById(R.id.tvSuccessDetail);
        
        // Format time display
        String timeDisplay;
        if (verificationTimeInSeconds >= 60) {
            int minutes = verificationTimeInSeconds / 60;
            int seconds = verificationTimeInSeconds % 60;
            timeDisplay = minutes + " minute" + (minutes > 1 ? "s" : "") + 
                          (seconds > 0 ? " and " + seconds + " second" + (seconds > 1 ? "s" : "") : "");
        } else {
            timeDisplay = verificationTimeInSeconds + " seconds";
        }
        
        tvSuccessDetail.setText("Students will now be able to submit their face ID within the next " + timeDisplay);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        builder.setCancelable(false);

        currentDialog = builder.create();
        currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        currentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        currentDialog.show();

        btnBack.setOnClickListener(v -> {
            currentDialog.dismiss();
            
            // You can either go to the success activity or just call the listener
            // Option 1: Go to success activity
            activity.startActivity(new android.content.Intent(activity, FaceIdSuccessActivity.class));
            
            // Option 2: Just call the listener
            if (listener != null) {
                listener.onVerificationComplete(true);
            }
        });
    }

    /**
     * Dismiss the current dialog if it exists
     */
    private void dismissCurrentDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }
}
