package vn.edu.fpt.zentryapp.lecturer.ui.faceid;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import vn.edu.fpt.zentryapp.R;

public class FaceIdRequestDialog extends DialogFragment {

    private Dialog currentDialog;
    private FaceIdRequestListener listener;
    private int minutes = 0;
    private int seconds = 0;

    public interface FaceIdRequestListener {
        void onFaceIdRequestConfigured(int totalSeconds);
    }

    public void setFaceIdRequestListener(FaceIdRequestListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_face_id_request_initial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup initial dialog using findViewById
        view.findViewById(R.id.btnContinue).setOnClickListener(v -> showTimeInputDialog());
    }

    private void showTimeInputDialog() {
        // Create time input dialog
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_face_id_time_input);
        
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        // Find views directly instead of using binding
        EditText etMinutes = dialog.findViewById(R.id.etMinutes);
        EditText etSeconds = dialog.findViewById(R.id.etSeconds);
        View btnStartVerification = dialog.findViewById(R.id.btnStartVerification);
        
        // Add text watchers for validation
        etMinutes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    minutes = Integer.parseInt(s.toString());
                    if (minutes > 60) {
                        etMinutes.setText("60");
                        minutes = 60;
                    }
                } else {
                    minutes = 0;
                }
            }
        });
        
        etSeconds.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    seconds = Integer.parseInt(s.toString());
                    if (seconds > 59) {
                        etSeconds.setText("59");
                        seconds = 59;
                    }
                } else {
                    seconds = 0;
                }
            }
        });
        
        // Setup start verification button
        btnStartVerification.setOnClickListener(v -> {
            if ((minutes == 0 && seconds == 0) || (minutes == 0 && seconds < 30)) {
                Toast.makeText(requireContext(), "Please set a time of at least 30 seconds", Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
                processVerification();
            }
        });
        
        dialog.setCancelable(true);
        dialog.show();
        currentDialog = dialog;
    }
    
    private void processVerification() {
        // Show a loading dialog or progress indicator
        Dialog progressDialog = new Dialog(requireContext());
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setContentView(R.layout.dialog_processing);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setCancelable(false);
        
        // Handle the case where Lottie might not be available
        try {
            // Try to find the Lottie animation view
            View animationView = progressDialog.findViewById(R.id.animationView);
            if (animationView != null && animationView.getVisibility() == View.VISIBLE) {
                // Animation view is available, hide the progress bar
                progressDialog.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                animationView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            // Fallback to the progress bar if there's any issue with Lottie
            progressDialog.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }
        
        progressDialog.show();
        
        // Simulate processing (replace with actual implementation)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            progressDialog.dismiss();
            showSuccessDialog();
            
            // Notify listener with total seconds
            if (listener != null) {
                int totalSeconds = (minutes * 60) + seconds;
                listener.onFaceIdRequestConfigured(totalSeconds);
            }
        }, 2000);
    }
    
    private void showSuccessDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_face_id_success);
        
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        // Find views directly instead of using binding
        View btnBack = dialog.findViewById(R.id.btnBack);
        
        // Setup back button
        btnBack.setOnClickListener(v -> {
            dialog.dismiss();
            dismiss();
        });
        
        dialog.setCancelable(false);
        dialog.show();
        currentDialog = dialog;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Close any open dialogs
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }
}
