package vn.edu.fpt.zentryapp.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import vn.edu.fpt.zentryapp.R;


/**
 * Activity to show the success screen after Face ID verification setup
 */
public class FaceIdSuccessActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ImageView ivSuccessIcon;
    private Button btnContinue;
    private Button btnTestFaceId;
    private ConstraintLayout clSuccessContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_id_success);

        // Initialize UI components
        initializeUI();
        
        // Set up click listeners
        setupClickListeners();
        
        // Apply enhanced visual effects
        applyEnhancedVisuals();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeUI() {
        ivBack = findViewById(R.id.ivBack);
        ivSuccessIcon = findViewById(R.id.ivSuccessIcon);
        btnContinue = findViewById(R.id.btnContinue);
        clSuccessContainer = findViewById(R.id.clSuccessContainer);
    }
    
    /**
     * Set up click listeners
     */
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());
        
        btnContinue.setOnClickListener(v -> {
            // Navigate back to the main flow
            finish();
        });
        
        btnTestFaceId.setOnClickListener(v -> {
            // Start a verification test (for demonstration purposes)
            startTestVerification();
        });
    }
    
    /**
     * Apply enhanced visual effects
     */
    private void applyEnhancedVisuals() {
        // Use green gradient background for success button
        btnContinue.setBackgroundResource(R.drawable.button_green_gradient);
        
        // Use ripple effect for buttons
        btnContinue.setBackgroundResource(R.drawable.ripple_green_gradient);
        
        // Apply animation to success icon
        ivSuccessIcon.setScaleX(0f);
        ivSuccessIcon.setScaleY(0f);
        ivSuccessIcon.setAlpha(0f);
        ivSuccessIcon.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(500)
                .start();
                
        // Apply animation to the success container
        clSuccessContainer.setAlpha(0f);
        clSuccessContainer.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(300)
                .start();
    }
    
    /**
     * Start a verification test (for demonstration purposes)
     */
    private void startTestVerification() {
        // In a real application, this would start a Face ID verification test
        // For demonstration, we'll just navigate back to the verification activity
        Intent intent = new Intent(this, FaceIdVerificationActivity.class);
        startActivity(intent);
    }
}
