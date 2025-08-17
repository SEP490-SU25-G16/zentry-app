package vn.edu.fpt.zentryapp.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import vn.edu.fpt.zentryapp.R;


/**
 * Activity to show the success screen after Face ID verification setup
 */
public class FaceIdSuccessActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ImageView ivSuccessIcon;
    private Button btnContinue;
    private Button btnTestFaceId;
    private CardView cardSuccess;
    private LinearLayout llBottomButtons;

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
        btnContinue = findViewById(R.id.btnUpdateFaceId); // Sử dụng button có sẵn trong layout
        cardSuccess = findViewById(R.id.cardSuccess);
        llBottomButtons = findViewById(R.id.llBottomButtons);
        
        // Ẩn button update vì đây là màn hình success sau khi thiết lập verification
        if (btnContinue != null) {
            btnContinue.setText("Continue");
        }
    }
    
    /**
     * Set up click listeners
     */
    private void setupClickListeners() {
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> onBackPressed());
        }
        
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                // Navigate back to the main flow
                finish();
            });
        }
        
        // Ẩn test button vì không cần thiết trong màn hình này
        btnTestFaceId = null;
    }
    
    /**
     * Apply enhanced visual effects
     */
    private void applyEnhancedVisuals() {
        if (btnContinue != null) {
            // Use green gradient background for success button
            btnContinue.setBackgroundResource(R.drawable.bg_button_success);
        }
        
        // Apply animation to success icon
        if (ivSuccessIcon != null) {
            ivSuccessIcon.setScaleX(0f);
            ivSuccessIcon.setScaleY(0f);
            ivSuccessIcon.setAlpha(0f);
            ivSuccessIcon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(500)
                    .start();
        }
                
        // Apply animation to the success card
        if (cardSuccess != null) {
            cardSuccess.setAlpha(0f);
            cardSuccess.setTranslationY(100f);
            cardSuccess.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setStartDelay(300)
                    .start();
        }
    }
    
    /**
     * Start a verification test (for demonstration purposes)
     * Note: This method is no longer used in the current implementation
     */
    private void startTestVerification() {
        // In a real application, this would start a Face ID verification test
        // For demonstration, we'll just navigate back to the verification activity
        Intent intent = new Intent(this, FaceIdVerificationActivity.class);
        startActivity(intent);
    }
}
