package com.example.zentry_app.ui.faceid;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.edu.fpt.zentryapp.R;


/**
 * Activity to demonstrate the Face ID verification flow with enhanced UI
 */
public class FaceIdVerificationActivity extends AppCompatActivity implements FaceIdManager.OnFaceIdVerificationCompleteListener {

    private FaceIdManager faceIdManager;
    private Button btnStartVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_id_request);

        // Initialize UI components
        btnStartVerification = findViewById(R.id.btnStartVerification);
        
        // Initialize FaceIdManager
        faceIdManager = new FaceIdManager(this);
        faceIdManager.setOnFaceIdVerificationCompleteListener(this);

        // Set up click listeners
        btnStartVerification.setOnClickListener(v -> {
            startFaceIdVerification();
        });
    }

    /**
     * Start the Face ID verification process
     */
    private void startFaceIdVerification() {
        faceIdManager.startFaceIdVerificationFlow();
    }

    @Override
    public void onVerificationComplete(boolean success) {
        if (success) {
            Toast.makeText(this, "Face ID verification setup complete!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Face ID verification setup failed!", Toast.LENGTH_SHORT).show();
        }
    }
}
