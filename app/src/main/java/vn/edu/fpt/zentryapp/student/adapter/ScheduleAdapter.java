package vn.edu.fpt.zentryapp.student.adapter;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.MainActivity;
import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.ItemScheduleBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundsResponse;
import vn.edu.fpt.zentryapp.service.AttendanceApiService;
import vn.edu.fpt.zentryapp.service.AttendanceModels;
import vn.edu.fpt.zentryapp.service.BLEAttendanceService;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleSession;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {
    private static final String TAG = "StudentScheduleAdapter";
    private List<StudentScheduleSession> studentScheduleSessions = new ArrayList<>();
    private OnScheduleClickListener onScheduleClickListener;
    private AuthManager authManager;

    public interface OnScheduleClickListener {
        void onScheduleClick(StudentScheduleSession studentScheduleSession);
    }

    public ScheduleAdapter(AuthManager authManager) {
        this.authManager = authManager;
    }

    public void setOnScheduleClickListener(OnScheduleClickListener listener) {
        this.onScheduleClickListener = listener;
    }

    public void setSchedules(List<StudentScheduleSession> studentScheduleSessions) {
        this.studentScheduleSessions = studentScheduleSessions != null ? studentScheduleSessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScheduleBinding binding = ItemScheduleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentScheduleSession studentScheduleSession = studentScheduleSessions.get(position);
        holder.bind(studentScheduleSession);
    }

    @Override
    public int getItemCount() {
        return studentScheduleSessions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemScheduleBinding binding;

        public ViewHolder(@NonNull ItemScheduleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void bind(StudentScheduleSession studentScheduleSession) {
            binding.tvScheduleClassName.setText(studentScheduleSession.getClassNameWithGrade());
            binding.tvScheduleClassTime.setText(studentScheduleSession.getScheduleTime());

            // 🔧 XÁC ĐỊNH có thể join class không dựa trên thời gian
            boolean canJoinClass = canJoinClassNow(studentScheduleSession);

            // Configure click behavior dựa trên timing
            if (canJoinClass) {
                binding.getRoot().setOnClickListener(v -> {
                    showJoinClassConfirmation(v, studentScheduleSession);
                });
                binding.getRoot().setClickable(true);
                binding.getRoot().setFocusable(true);
                binding.getRoot().setAlpha(1.0f);

                // Highlight available classes
                binding.getRoot().setBackgroundColor(0xFFE8F5E8); // Light green
            } else {
                binding.getRoot().setOnClickListener(v -> {
                    Toast.makeText(v.getContext(),
                            getNotAvailableMessage(studentScheduleSession),
                            Toast.LENGTH_SHORT).show();
                });
                binding.getRoot().setClickable(true);
                binding.getRoot().setFocusable(true);
                binding.getRoot().setAlpha(0.6f); // Dim appearance
                binding.getRoot().setBackgroundColor(0xFFFFFFFF); // White
            }
        }

        /**
         * 🔧 XÁC ĐỊNH có thể join class không (similar to lecturer start logic)
         */
        private boolean canJoinClassNow(StudentScheduleSession session) {
            try {
                Date currentTime = new Date();
                Date sessionStart = parseTimeToday(session.getStartTime());
                Date sessionEnd = parseTimeToday(session.getEndTime());

                // 🔧 CHỈ CHO JOIN KHI ĐÚNG GIỜ (trong khoảng thời gian session)
                boolean isInSessionTime = currentTime.getTime() >= sessionStart.getTime() &&
                        currentTime.getTime() <= sessionEnd.getTime();

                return isInSessionTime;

            } catch (Exception e) {
                Log.e(TAG, "Error checking join time", e);
                return false;
            }
        }

        /**
         * 🔧 PARSE time string to Date object for today
         */
        private Date parseTimeToday(String timeStr) throws Exception {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date timeOnly = timeFormat.parse(timeStr);

            Calendar today = Calendar.getInstance();
            Calendar sessionTime = Calendar.getInstance();
            sessionTime.setTime(timeOnly);

            today.set(Calendar.HOUR_OF_DAY, sessionTime.get(Calendar.HOUR_OF_DAY));
            today.set(Calendar.MINUTE, sessionTime.get(Calendar.MINUTE));
            today.set(Calendar.SECOND, sessionTime.get(Calendar.SECOND));

            return today.getTime();
        }

        /**
         * 🔧 GET message khi không thể join
         */
        private String getNotAvailableMessage(StudentScheduleSession session) {
            try {
                Date currentTime = new Date();
                Date sessionStart = parseTimeToday(session.getStartTime());
                Date sessionEnd = parseTimeToday(session.getEndTime());

                if (currentTime.getTime() < sessionStart.getTime()) {
                    return "Class hasn't started yet. Please wait until " + session.getStartTime();
                } else if (currentTime.getTime() > sessionEnd.getTime()) {
                    return "Class has ended.";
                } else {
                    return "Class is not available for joining at the moment.";
                }
            } catch (Exception e) {
                return "Class is not available yet. Please wait until class time.";
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void showJoinClassConfirmation(View view, StudentScheduleSession studentScheduleSession) {
            Dialog dialog = new Dialog(view.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_join_class_confirmation);

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            TextView tvMessage = dialog.findViewById(R.id.tv_message);
            MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
            MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

            tvMessage.setText("Bạn có muốn tham gia lớp học \"" + studentScheduleSession.getClassNameWithGrade() + "\" không?");

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();

                // 🔧 Load rounds từ API trước khi start BLE service
                startStudentBLEService(view.getContext(), studentScheduleSession);

                notifyDataSetChanged();

                if (onScheduleClickListener != null) {
                    onScheduleClickListener.onScheduleClick(studentScheduleSession);
                }
            });

            dialog.setCancelable(true);
            dialog.show();
        }
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startStudentBLEService(Context context, StudentScheduleSession studentScheduleSession) {
            try {
                String userId = authManager.getCurrentUserId();

                if (userId == null) {
                    Log.e(TAG, "Student User ID not available");
                    Toast.makeText(context, "Không thể xác định thông tin sinh viên", Toast.LENGTH_SHORT).show();
                    return;
                }
                MainActivity mainActivity = getMainActivityFromContext(context);

                if (mainActivity != null && mainActivity.hasBLEPermissions()) {
                    // ✅ Có permissions, load rounds và start service
                    loadStudentSessionRounds(context, studentScheduleSession, userId);
                    Log.d(TAG, "✅ Student BLE service starting with permissions");
                } else if (mainActivity != null) {
                    // ❌ Thiếu permissions, request lại
                    Log.w(TAG, "⚠️ Student BLE permissions missing, requesting...");
                    Toast.makeText(context, "🔒 Requesting BLE permissions for attendance...",
                            Toast.LENGTH_SHORT).show();

                    mainActivity.requestBLEPermissions();
                    Toast.makeText(context, "Please grant permissions and try joining class again",
                            Toast.LENGTH_LONG).show();
                } else {
                    // ❌ Không tìm được MainActivity, fallback to static check
                    Log.w(TAG, "⚠️ Cannot find MainActivity, using static permission check");

                    if (hasStaticBLEPermissions(context)) {
                        // Start service với static permission check
                        loadStudentSessionRounds(context, studentScheduleSession, userId);
                        Log.d(TAG, "✅ Student BLE service started with static permission check");
                    } else {
                        // Show settings instruction
                        Toast.makeText(context,
                                "BLE permissions required. Please grant in Settings and try again.",
                                Toast.LENGTH_LONG).show();
                        openAppSettings(context);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to start Student BLE service", e);
                Toast.makeText(context, "Lỗi khi tham gia lớp học", Toast.LENGTH_SHORT).show();
            }
        }

        // ➕ HELPER METHOD để tìm MainActivity từ context chain
        private MainActivity getMainActivityFromContext(Context context) {
            // Try direct cast first
            if (context instanceof MainActivity) {
                return (MainActivity) context;
            }

            // Try to get activity from context wrapper
            while (context instanceof ContextWrapper) {
                if (context instanceof MainActivity) {
                    return (MainActivity) context;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }

            Log.d(TAG, "MainActivity not found in context chain");
            return null;
        }

        // ➕ STATIC PERMISSION CHECK (fallback khi không tìm được MainActivity)
        private boolean hasStaticBLEPermissions(Context context) {
            // Check Location permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "ACCESS_FINE_LOCATION permission missing");
                return false;
            }

            // Check Android 12+ BLE permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "BLUETOOTH_SCAN permission missing");
                    return false;
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "BLUETOOTH_ADVERTISE permission missing");
                    return false;
                }
            }

            Log.d(TAG, "All BLE permissions granted");
            return true;
        }

        // ➕ OPEN APP SETTINGS (fallback khi không thể request permissions)
        private void openAppSettings(Context context) {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);

                Log.d(TAG, "Opened app settings for permission grant");
            } catch (Exception e) {
                Log.e(TAG, "Cannot open app settings", e);
                Toast.makeText(context, "Please manually grant BLE permissions in Settings",
                        Toast.LENGTH_LONG).show();
            }
        }

        /**
         * 🔧 LOAD rounds từ API cho student (tương tự lecturer)
         */
        private void loadStudentSessionRounds(Context context, StudentScheduleSession studentScheduleSession, String userId) {
            Log.d(TAG, "Loading rounds for student session: " + studentScheduleSession.getSessionId());

            // Tạo API service
            AttendanceApiService apiService = ApiClient.getClient(context).create(AttendanceApiService.class);

            apiService.getSessionRounds(studentScheduleSession.getSessionId())
                    .enqueue(new Callback<RoundsResponse>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onResponse(Call<RoundsResponse> call, Response<RoundsResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                RoundsResponse apiResponse = response.body();

                                if (apiResponse.isSuccess()) {
                                    List<AttendanceModels.AttendanceRound> rounds = mapApiRoundsToAttendanceRounds(apiResponse.getData());

                                    if (!rounds.isEmpty()) {
                                        // Start BLE service với rounds thật
                                        startStudentBLEServiceWithRounds(context, studentScheduleSession, userId, rounds);
                                        Log.d(TAG, "✅ Student loaded " + rounds.size() + " rounds from API");
                                    } else {
                                        Log.w(TAG, "⚠️ No rounds found for student, using fallback");
                                    }
                                } else {
                                    Log.e(TAG, "❌ Student API Error: " + apiResponse.getError());
                                }
                            } else {
                                Log.e(TAG, "❌ Student HTTP Error: " + response.code());
                            }
                        }

                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onFailure(Call<RoundsResponse> call, Throwable t) {
                            Log.e(TAG, "❌ Student Network Error", t);
                        }
                    });
        }

        private List<AttendanceModels.AttendanceRound> mapApiRoundsToAttendanceRounds(List<RoundData> apiRounds) {
            List<AttendanceModels.AttendanceRound> rounds = new ArrayList<>();
            if (apiRounds == null || apiRounds.isEmpty()) return rounds;

            // ✅ Setup formatters với timezone rõ ràng
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            SimpleDateFormat vnFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            vnFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

            for (int i = 0; i < apiRounds.size(); i++) {
                RoundData apiRound = apiRounds.get(i);
                try {
                    String utcString = apiRound.getStartTime(); // "2025-07-30T19:26:07Z"

                    // ✅ FIXED: Parse UTC time chính xác
                    Date utcDate = utcFormat.parse(utcString);

                    // ✅ FIXED: Convert sang VN time với simple offset
                    final long VN_OFFSET = 15 * 1000L; // 15 s
                    Date vnDate = new Date(utcDate.getTime() + VN_OFFSET);

                    AttendanceModels.AttendanceRound round = new AttendanceModels.AttendanceRound(
                            apiRound.getRoundId(),
                            vnDate,
                            apiRound.getRoundNumber(),
                            (i == apiRounds.size() - 1)
                    );

                    rounds.add(round);

                    // ✅ Enhanced logging để debug
                    Log.d(TAG, "Round " + apiRound.getRoundNumber() + ":");
                    Log.d(TAG, "  UTC string: " + utcString);
                    Log.d(TAG, "  UTC parsed: " + utcFormat.format(utcDate));
                    Log.d(TAG, "  VN time: " + vnFormat.format(vnDate));
                    Log.d(TAG, "  Offset applied: +7h");

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing round " + apiRound.getRoundNumber(), e);
                }
            }

            Log.d(TAG, "✅ Total rounds mapped: " + rounds.size());
            return rounds;
        }


        /**
         * 🔧 START BLE service với rounds từ API
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startStudentBLEServiceWithRounds(Context context, StudentScheduleSession studentScheduleSession,
                                                      String userId, List<AttendanceModels.AttendanceRound> rounds) {
            try {
                Intent serviceIntent = new Intent(context, BLEAttendanceService.class);
                serviceIntent.setAction("START_ATTENDANCE");
                serviceIntent.putExtra("session", studentScheduleSession);
                serviceIntent.putExtra("userId", userId);
                serviceIntent.putExtra("userRole", "STUDENT"); // 🔧 Student role
                serviceIntent.putExtra("rounds", (Serializable) rounds);
                ContextCompat.startForegroundService(context, serviceIntent);

                Log.d(TAG, "✅ Student BLE Service started with " + rounds.size() +
                        " rounds for session: " + studentScheduleSession.getSessionId());

                Toast.makeText(context, "Đã tham gia lớp học và bắt đầu điểm danh BLE với " + rounds.size() + " rounds",
                        Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to start Student BLE service with API rounds", e);
                Toast.makeText(context, "Lỗi khi tham gia lớp học", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
