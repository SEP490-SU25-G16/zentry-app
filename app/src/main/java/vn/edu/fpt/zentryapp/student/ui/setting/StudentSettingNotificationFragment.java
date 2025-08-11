package vn.edu.fpt.zentryapp.student.ui.setting;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingNotificationBinding;

/**
 * Fragment cài đặt thông báo cho sinh viên
 * Có thể được truy cập từ 2 flow:
 * 1. Từ NotificationFragment
 * 2. Từ StudentSettingFragment
 */
public class StudentSettingNotificationFragment extends Fragment {

    private FragmentStudentSettingNotificationBinding binding;
    private SharedPreferences preferences;
    
    // Key để xác định nguồn gốc navigation
    public static final String ARG_SOURCE = "source_fragment";
    public static final String SOURCE_NOTIFICATION = "notification_fragment";
    public static final String SOURCE_SETTINGS = "settings_fragment";

    private static final String PREF_SOUND = "sound_enabled";
    private static final String PREF_VIBRATE = "vibrate_enabled";
    private static final String PREF_GENERAL = "general_notif_enabled";
    
    // Factory method để tạo instance với source
    public static StudentSettingNotificationFragment newInstance(String source) {
        StudentSettingNotificationFragment fragment = new StudentSettingNotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SOURCE, source);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSettingNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // Khởi tạo SharedPreferences
            preferences = requireContext().getSharedPreferences("app_prefs", 0);
            
            // Xác định nguồn gốc navigation
            String source = SOURCE_SETTINGS; // Mặc định là từ Settings
            if (getArguments() != null && getArguments().containsKey(ARG_SOURCE)) {
                source = getArguments().getString(ARG_SOURCE, SOURCE_SETTINGS);
            }
            
            final String navigationSource = source;
            Log.d("StudentSettingNotif", "Source: " + navigationSource);

            // Xử lý nút back toolbar dựa vào nguồn gốc
            binding.ivStudentSettingNotificationBack.setOnClickListener(v -> {
                try {
                    Log.d("StudentSettingNotif", "Back button clicked, source: " + navigationSource);
                    
                    if (SOURCE_NOTIFICATION.equals(navigationSource)) {
                        // Nếu đến từ NotificationFragment, sử dụng popBackStack
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        // Nếu đến từ StudentSettingFragment, sử dụng action cụ thể để quay về
                        NavController navController = NavHostFragment.findNavController(this);
                        navController.navigate(R.id.action_notifications_to_settings);
                    }
                } catch (Exception e) {
                    Log.e("StudentSettingNotif", "Error navigating back: ", e);
                    // Fallback an toàn
                    try {
                        NavHostFragment.findNavController(this).navigateUp();
                    } catch (Exception ex) {
                        Log.e("StudentSettingNotif", "Error on navigateUp fallback: ", ex);
                    }
                }
            });

            // Load trạng thái switch từ SharedPreferences
            boolean soundOn = preferences.getBoolean(PREF_SOUND, true);
            boolean vibrateOn = preferences.getBoolean(PREF_VIBRATE, true);
            boolean generalOn = preferences.getBoolean(PREF_GENERAL, true);

            binding.switchStudentSettingNotificationSound.setChecked(soundOn);
            binding.switchStudentSettingNotificationVibrate.setChecked(vibrateOn);
            binding.switchStudentSettingNotificationGeneral.setChecked(generalOn);

            // Lắng nghe thay đổi trạng thái switch và lưu lại
            CompoundButton.OnCheckedChangeListener listener = (button, isChecked) -> {
                int id = button.getId();

                SharedPreferences.Editor editor = preferences.edit();

                if (id == binding.switchStudentSettingNotificationSound.getId()) {
                    // Lưu trạng thái âm thanh thông báo
                    editor.putBoolean(PREF_SOUND, isChecked);
                } else if (id == binding.switchStudentSettingNotificationVibrate.getId()) {
                    // Lưu trạng thái rung thông báo
                    editor.putBoolean(PREF_VIBRATE, isChecked);
                } else if (id == binding.switchStudentSettingNotificationGeneral.getId()) {
                    // Lưu trạng thái thông báo chung
                    editor.putBoolean(PREF_GENERAL, isChecked);
                }
                editor.apply();
            };

            binding.switchStudentSettingNotificationSound.setOnCheckedChangeListener(listener);
            binding.switchStudentSettingNotificationVibrate.setOnCheckedChangeListener(listener);
            binding.switchStudentSettingNotificationGeneral.setOnCheckedChangeListener(listener);
        } catch (Exception e) {
            Log.e("StudentSettingNotif", "Error in onViewCreated", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
