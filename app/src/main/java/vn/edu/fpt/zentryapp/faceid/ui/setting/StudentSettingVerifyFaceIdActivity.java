package vn.edu.fpt.zentryapp.faceid.ui.setting;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import vn.edu.fpt.zentryapp.R;

public class StudentSettingVerifyFaceIdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_id_verify);

        // Tạo và hiển thị fragment verify Face ID
        if (savedInstanceState == null) {
            StudentSettingVerifyFaceIdFragment fragment = new StudentSettingVerifyFaceIdFragment();
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
