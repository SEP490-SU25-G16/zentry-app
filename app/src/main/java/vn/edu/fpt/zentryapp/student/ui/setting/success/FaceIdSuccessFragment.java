package vn.edu.fpt.zentryapp.student.ui.setting.success;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import vn.edu.fpt.zentryapp.R;

/**
 * Fragment hiển thị màn hình Face ID Success đơn giản
 * Chỉ hiển thị thông báo thành công, không có button update
 * Sử dụng layout riêng fragment_face_id_success.xml
 */
public class FaceIdSuccessFragment extends Fragment {

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// ✅ NEW: Sử dụng layout riêng cho Fragment thay vì layout của Activity
		return inflater.inflate(R.layout.fragment_face_id_success, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		NavController navController = NavHostFragment.findNavController(this);
		View ivBack = view.findViewById(R.id.ivBack);

		if (ivBack != null) {
			ivBack.setOnClickListener(v -> navController.popBackStack());
		}
		
		// ✅ NEW: Fragment này chỉ hiển thị thông báo thành công
		// Không có button update face id
		// Button update sẽ được xử lý bởi FaceIdSuccessActivity
	}
}


