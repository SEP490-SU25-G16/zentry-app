package vn.edu.fpt.zentryapp.faceid.ui.setting;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;

import vn.edu.fpt.zentryapp.R;

public class StudentSettingVerifyFaceIdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_id_verify);

        // ✅ NEW: Tạo và hiển thị fragment verify Face ID với arguments từ intent
        if (savedInstanceState == null) {
            StudentSettingVerifyFaceIdFragment fragment = new StudentSettingVerifyFaceIdFragment();
            
            // Pass verification arguments from intent to fragment
            Bundle args = new Bundle();
            String requestId = getIntent().getStringExtra("requestId");
            String sessionId = getIntent().getStringExtra("sessionId");
            String expiresAt = getIntent().getStringExtra("expiresAt");
            
            if (requestId != null && sessionId != null) {
                args.putString("requestId", requestId);
                args.putString("sessionId", sessionId);
                if (expiresAt != null) {
                    args.putString("expiresAt", expiresAt);
                }
                Log.d("VerifyActivity", "✅ Passing verification args: " + requestId + ", " + sessionId + ", expiresAt: " + expiresAt);
            } else {
                Log.w("VerifyActivity", "⚠️ No verification args found in intent");
            }
            
            fragment.setArguments(args);
            
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        // ✅ NEW: Xử lý back press để quay về setting
        // Finish activity hiện tại để quay về StudentSettingFragment
        finish();
    }
}
