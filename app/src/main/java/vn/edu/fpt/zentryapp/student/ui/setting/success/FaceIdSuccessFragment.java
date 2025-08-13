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

public class FaceIdSuccessFragment extends Fragment {

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_face_id_success, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		NavController navController = NavHostFragment.findNavController(this);
		View btnUpdateFaceId = view.findViewById(R.id.btnUpdateFaceId);
		View ivBack = view.findViewById(R.id.ivBack);

		if (ivBack != null) {
			ivBack.setOnClickListener(v -> navController.popBackStack());
		}
		if (btnUpdateFaceId != null) {
			btnUpdateFaceId.setOnClickListener(v -> navController.navigate(R.id.action_studentSetting_to_updateFaceId));
		}
	}
}


