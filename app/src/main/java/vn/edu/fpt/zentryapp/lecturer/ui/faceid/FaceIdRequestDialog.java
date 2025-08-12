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

    // Single-dialog, multi-step state machine (avoid stacking multiple dialogs)
    private FaceIdRequestListener listener;
    private int minutes = 0;
    private int seconds = 0;
    private Step currentStep = Step.INITIAL;

    private enum Step { INITIAL, TIME_INPUT, PROCESSING, SUCCESS }

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
        // Start with initial step layout
        return inflater.inflate(R.layout.dialog_face_id_request_initial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindInitialStep(view);
    }

    private void bindInitialStep(View root) {
        currentStep = Step.INITIAL;
        View btnContinue = root.findViewById(R.id.btnContinue);
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> showTimeInputStep());
        }
    }

    private void showTimeInputStep() {
        if (getView() == null || getContext() == null) return;
        currentStep = Step.TIME_INPUT;
        // Replace content of existing dialog view
        ViewGroup parent = (ViewGroup) getView();
        parent.removeAllViews();
        LayoutInflater.from(getContext()).inflate(R.layout.dialog_face_id_time_input, parent, true);

        EditText etMinutes = parent.findViewById(R.id.etMinutes);
        EditText etSeconds = parent.findViewById(R.id.etSeconds);
        View btnStartVerification = parent.findViewById(R.id.btnStartVerification);

        TextWatcher minuteWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    try {
                        minutes = Integer.parseInt(s.toString());
                    } catch (NumberFormatException e) { minutes = 0; }
                    if (minutes > 60) { minutes = 60; etMinutes.setText("60"); etMinutes.setSelection(etMinutes.getText().length()); }
                } else minutes = 0;
            }
        };
        TextWatcher secondWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    try {
                        seconds = Integer.parseInt(s.toString());
                    } catch (NumberFormatException e) { seconds = 0; }
                    if (seconds > 59) { seconds = 59; etSeconds.setText("59"); etSeconds.setSelection(etSeconds.getText().length()); }
                } else seconds = 0;
            }
        };
        etMinutes.addTextChangedListener(minuteWatcher);
        etSeconds.addTextChangedListener(secondWatcher);

        btnStartVerification.setOnClickListener(v -> {
            if ((minutes == 0 && seconds == 0) || (minutes == 0 && seconds < 30)) {
                Toast.makeText(requireContext(), "Please set a time of at least 30 seconds", Toast.LENGTH_SHORT).show();
            } else {
                showProcessingStep();
            }
        });
    }

    private void showProcessingStep() {
        if (getView() == null || getContext() == null) return;
        currentStep = Step.PROCESSING;
        ViewGroup parent = (ViewGroup) getView();
        parent.removeAllViews();
        LayoutInflater.from(getContext()).inflate(R.layout.dialog_processing, parent, true);

        // Ensure progress bar visible (handle optional animation view fallback)
        View progressBar = parent.findViewById(R.id.progressBar);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        View animationView = parent.findViewById(R.id.animationView);
        if (animationView != null) animationView.setVisibility(View.GONE);

        // Simulate processing delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;
            // Notify listener
            if (listener != null) {
                int totalSeconds = (minutes * 60) + seconds;
                listener.onFaceIdRequestConfigured(totalSeconds);
            }
            showSuccessStep();
        }, 2000);
    }

    private void showSuccessStep() {
        if (getView() == null || getContext() == null) return;
        currentStep = Step.SUCCESS;
        ViewGroup parent = (ViewGroup) getView();
        parent.removeAllViews();
        LayoutInflater.from(getContext()).inflate(R.layout.dialog_face_id_success, parent, true);
        View btnBack = parent.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> dismiss());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nothing extra to dismiss now (single dialog approach)
    }
}
