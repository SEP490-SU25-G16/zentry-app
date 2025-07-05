package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerSettingNotificationBinding;

public class LecturerSettingNotificationFragment extends Fragment {

    private FragmentLecturerSettingNotificationBinding binding;
    private SharedPreferences preferences;

    private static final String PREF_SOUND = "sound_enabled";
    private static final String PREF_VIBRATE = "vibrate_enabled";
    private static final String PREF_GENERAL = "general_notif_enabled";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerSettingNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo SharedPreferences (ví dụ dùng mặc định của app)
        preferences = requireContext().getSharedPreferences("app_prefs", 0);

        // Xử lý nút back toolbar
        binding.ivNotificationBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Load trạng thái switch từ SharedPreferences
        boolean soundOn = preferences.getBoolean(PREF_SOUND, true);
        boolean vibrateOn = preferences.getBoolean(PREF_VIBRATE, true);
        boolean generalOn = preferences.getBoolean(PREF_GENERAL, true);

        binding.switchNotificationSound.setChecked(soundOn);
        binding.switchNotificationVibrate.setChecked(vibrateOn);
        binding.switchNotificationGeneral.setChecked(generalOn);

        // Lắng nghe thay đổi trạng thái switch và lưu lại
        CompoundButton.OnCheckedChangeListener listener = (button, isChecked) -> {
            int id = button.getId();

            SharedPreferences.Editor editor = preferences.edit();

            if (id == binding.switchNotificationSound.getId()) {
                // Lưu trạng thái âm thanh thông báo
                editor.putBoolean(PREF_SOUND, isChecked);
            } else if (id == binding.switchNotificationVibrate.getId()) {
                // Lưu trạng thái rung thông báo
                editor.putBoolean(PREF_VIBRATE, isChecked);
            } else if (id == binding.switchNotificationGeneral.getId()) {
                // Lưu trạng thái thông báo chung
                editor.putBoolean(PREF_GENERAL, isChecked);
            }
            editor.apply();
        };

        binding.switchNotificationSound.setOnCheckedChangeListener(listener);
        binding.switchNotificationVibrate.setOnCheckedChangeListener(listener);
        binding.switchNotificationGeneral.setOnCheckedChangeListener(listener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
