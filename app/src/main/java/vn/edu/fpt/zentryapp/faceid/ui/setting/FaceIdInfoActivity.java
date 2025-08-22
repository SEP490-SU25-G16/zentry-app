package vn.edu.fpt.zentryapp.faceid.ui.setting;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.ActivityFaceIdInfoBinding;

/**
 * Activity hiển thị thông tin Face ID và cho phép update
 * Được sử dụng khi user bấm Face ID từ setting
 */
public class FaceIdInfoActivity extends AppCompatActivity {
    private static final String TAG = "FaceIdInfoActivity";
    
    private static final String EXTRA_USER_ID = "user_id";
    private static final String EXTRA_USER_NAME = "user_name";
    
    private ActivityFaceIdInfoBinding binding;
    
    public static Intent createIntent(android.content.Context context, String userId, String userName) {
        Intent intent = new Intent(context, FaceIdInfoActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_USER_NAME, userName);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaceIdInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupUI();
        setupClickListeners();
    }
    
    private void setupUI() {
        String userName = getIntent().getStringExtra(EXTRA_USER_NAME);

        
        // Display Face ID registration info
        binding.tvFaceIdStatus.setText("Face ID has been registered successfully!");
        
        // Hiển thị button update
        binding.btnUpdateFaceId.setVisibility(View.VISIBLE);
    }
    
    private void setupClickListeners() {

        
        // Button update Face ID
        binding.btnUpdateFaceId.setOnClickListener(v -> {
            Log.d(TAG, "✅ Update Face ID button clicked");
            try {
                Intent updateIntent = new Intent(this, StudentSettingUpdateFaceIdActivity.class);
                startActivity(updateIntent);
                Log.d(TAG, "✅ Launched StudentSettingUpdateFaceIdActivity");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error launching Update Face ID Activity", e);
                Toast.makeText(this, "Unable to open the Update Face ID screen", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Button back
        binding.ivBack.setOnClickListener(v -> finish());
    }
}
