package vn.edu.fpt.zentryapp.student.adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
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

        // 🔧 REFACTOR: Load rounds từ API trước khi start BLE service
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startStudentBLEService(Context context, StudentScheduleSession studentScheduleSession) {
            try {
                String userId = authManager.getCurrentUserId();

                if (userId == null) {
                    Log.e(TAG, "Student User ID not available");
                    Toast.makeText(context, "Không thể xác định thông tin sinh viên", Toast.LENGTH_SHORT).show();
                    return;
                }

                loadStudentSessionRounds(context, studentScheduleSession, userId);

            } catch (Exception e) {
                Log.e(TAG, "Failed to start Student BLE service", e);
                Toast.makeText(context, "Lỗi khi tham gia lớp học", Toast.LENGTH_SHORT).show();
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

        /**
         * 🔧 MAP API rounds sang AttendanceModels.AttendanceRound (tương tự lecturer)
         */
        private List<AttendanceModels.AttendanceRound> mapApiRoundsToAttendanceRounds(List<RoundData> apiRounds) {
            List<AttendanceModels.AttendanceRound> rounds = new ArrayList<>();

            if (apiRounds == null || apiRounds.isEmpty()) {
                return rounds;
            }

            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

            for (int i = 0; i < apiRounds.size(); i++) {
                RoundData apiRound = apiRounds.get(i);

                try {
                    Date startTime = isoFormat.parse(apiRound.getStartTime());
                    boolean isLastRound = (i == apiRounds.size() - 1);

                    AttendanceModels.AttendanceRound round = new AttendanceModels.AttendanceRound(
                            apiRound.getRoundId(),        // roundId
                            startTime,                    // executionTime
                            apiRound.getRoundNumber(),    // roundNumber
                            isLastRound                   // isLastRound
                    );

                    rounds.add(round);

                    Log.d(TAG, "Student mapped round " + apiRound.getRoundNumber() +
                            ": " + apiRound.getStartTime() +
                            " (isLast: " + isLastRound + ")");

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing student round " + apiRound.getRoundNumber(), e);
                }
            }

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
                context.startForegroundService(serviceIntent);

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
