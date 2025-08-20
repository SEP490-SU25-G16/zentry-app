package vn.edu.fpt.zentryapp.lecturer.adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
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
import vn.edu.fpt.zentryapp.databinding.ItemLecturerScheduleClassSectionBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleClassSection;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundDetail;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundsDataResponse;
import vn.edu.fpt.zentryapp.service.AttendanceApiService;
import vn.edu.fpt.zentryapp.service.AttendanceModels;
import vn.edu.fpt.zentryapp.service.BLEAttendanceService;

public class LecturerScheduleClassSectionAdapter extends RecyclerView.Adapter<LecturerScheduleClassSectionAdapter.ViewHolder> {

    private static final String TAG = "LecturerScheduleAdapter";

    // Session Status Constants (matching API response)
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_COMPLETED = "Completed";
    private static final String STATUS_MISSED = "Missed";

    // Button Actions
    private static final String ACTION_START = "START";
    private static final String ACTION_ONGOING = "ONGOING";
    private static final String ACTION_VIEW = "VIEW";
    private static final String ACTION_UPCOMING = "UPCOMING";
    private static final String ACTION_MISSED = "MISSED";

    private List<LecturerScheduleClassSection> sessions = new ArrayList<>();
    private OnSessionActionListener listener;
    private AuthManager authManager;

    public interface OnSessionActionListener {
        void onSessionClick(LecturerScheduleClassSection session);
        void onStartSession(LecturerScheduleClassSection session);
    }

    public LecturerScheduleClassSectionAdapter(AuthManager authManager) {
        this.authManager = authManager;
    }

    public void setSessions(List<LecturerScheduleClassSection> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnSessionActionListener(OnSessionActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLecturerScheduleClassSectionBinding binding = ItemLecturerScheduleClassSectionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(sessions.get(position));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemLecturerScheduleClassSectionBinding binding;

        public ViewHolder(ItemLecturerScheduleClassSectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void bind(LecturerScheduleClassSection session) {
            // Set basic session info with correct format
            setBasicInfo(session);

            // Configure button based on session status and timing
            configureButton(session);

            // Set card background based on status and timing
            setCardBackground(session);
            // Set click listener
            binding.btnStartNow.setOnClickListener(v -> handleButtonClick(session));
        }

        private void setBasicInfo(LecturerScheduleClassSection session) {
            // Dòng 1: Course Name + Section Code
            String courseDisplay = session.getCourseName() + " - " + session.getSectionCode();
            binding.tvSessionCourseName.setText(courseDisplay);

            // Dòng 2: Weekday + Start Time - End Time (đã parse đẹp)
            binding.tvSessionDateTime.setText(session.getWeekdayTimeDisplay());

            // Dòng 3: Building - Room
            binding.tvSessionClassRoom.setText(session.getBuildingRoomDisplay());
        }

        private void configureButton(LecturerScheduleClassSection session) {
            String action = determineButtonAction(session);

            switch (action) {
                case ACTION_START:
                    setupButton("Start now", "#3B82F6", true); // Blue
                    break;

                case ACTION_ONGOING:
                    setupButton("On going", "#10B981", true); // Green
                    break;

                case ACTION_VIEW:
                    setupButton("View", "#6366F1", true); // Purple
                    break;

                case ACTION_UPCOMING:
                    setupButton("Upcoming", "#A3BFED", false); // Gray
                    break;

                case ACTION_MISSED:
                    setupButton("Missed", "#EF4444", false); // Red
                    break;
            }
        }

        private String determineButtonAction(LecturerScheduleClassSection session) {
            String status = session.getSessionStatus();
            Date currentTime = new Date();
            Date startTime = session.getStartTimeAsDate();
            Date endTime = session.getEndTimeAsDate();

            // Check if session is currently happening (within time range)
            boolean isCurrentlyHappening = startTime != null && endTime != null &&
                    isCurrentTimeInSession(currentTime, startTime, endTime);

            // ✅ HARDCODE: Check if session has ended (past end time)
            boolean hasEnded = endTime != null && currentTime.getTime() > endTime.getTime();

            // Check if there's any active session
            boolean hasActiveSession = hasActiveSessionInList();

            switch (status) {
                case STATUS_PENDING:
                    if (hasEnded) {                       // quá giờ  → MISSED
                        return ACTION_MISSED;
                    }
                    if (isCurrentlyHappening) {           // đang tới giờ → START
                        return ACTION_START;
                    }
                    if (hasActiveSession) {               // có lớp Active khác → UPCOMING
                        return ACTION_UPCOMING;
                    }
                    return ACTION_UPCOMING;               // mặc định

                case STATUS_ACTIVE:
                    // ✅ HARDCODE: Nếu Active nhưng đã quá giờ kết thúc → VIEW
                    if (hasEnded) {
                        return ACTION_VIEW;
                    }
                    // Nếu vẫn đang trong thời gian hoặc chưa tới giờ → ONGOING
                    return ACTION_ONGOING;

                case STATUS_COMPLETED:
                    return ACTION_VIEW;

                case STATUS_MISSED:
                    return ACTION_MISSED;

                default:
                    return ACTION_UPCOMING;
            }
        }

        private boolean hasActiveSessionInList() {
            return sessions.stream()
                    .anyMatch(session -> STATUS_ACTIVE.equals(session.getSessionStatus()));
        }

        private boolean isCurrentTimeInSession(Date currentTime, Date startTime, Date endTime) {
            return currentTime.getTime() >= startTime.getTime() &&
                    currentTime.getTime() <= endTime.getTime();
        }

        private void setupButton(String text, String colorHex, boolean enabled) {
            binding.btnStartNow.setText(text);
            binding.btnStartNow.setEnabled(enabled);

            int color = Color.parseColor(colorHex);
            binding.btnStartNow.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(color));

            // Set text color - white for enabled, darker for disabled
            binding.btnStartNow.setTextColor(enabled ? Color.WHITE : Color.parseColor("#94A3B8"));
        }

        private void setCardBackground(LecturerScheduleClassSection session) {
            String status = session.getSessionStatus();
            Date currentTime = new Date();
            Date startTime = session.getStartTimeAsDate();
            Date endTime = session.getEndTimeAsDate();

            // Check if session is currently happening
            boolean isCurrentlyHappening = startTime != null && endTime != null &&
                    isCurrentTimeInSession(currentTime, startTime, endTime);

            // ✅ HARDCODE: Check if session has ended
            boolean hasEnded = endTime != null && currentTime.getTime() > endTime.getTime();

            int backgroundColor;

            switch (status) {
                case STATUS_PENDING:
                    backgroundColor = Color.WHITE;
                    break;

                case STATUS_ACTIVE:
                    // ✅ HARDCODE: Active nhưng đã kết thúc → màu như Completed
                    if (hasEnded) {
                        backgroundColor = Color.parseColor("#F8FAFC"); // Very light gray (như Completed)
                    } else if (isCurrentlyHappening) {
                        backgroundColor = Color.parseColor("#DCFCE7"); // Light green
                    } else {
                        backgroundColor = Color.WHITE;
                    }
                    break;

                case STATUS_COMPLETED:
                    backgroundColor = Color.parseColor("#F8FAFC"); // Very light gray
                    break;

                case STATUS_MISSED:
                    backgroundColor = Color.parseColor("#FFF1F2"); // Light red
                    break;

                default:
                    backgroundColor = Color.WHITE;
                    break;
            }

            binding.getRoot().setCardBackgroundColor(backgroundColor);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void handleButtonClick(LecturerScheduleClassSection session) {
            String action = determineButtonAction(session);

            switch (action) {
                case ACTION_START:
                    showStartConfirmation(session);
                    break;

                case ACTION_ONGOING:
                case ACTION_VIEW:
                    if (listener != null) {
                        listener.onSessionClick(session);
                    }
                    break;

                case ACTION_UPCOMING:
                case ACTION_MISSED:
                    // Show info toast
                    String message = action.equals(ACTION_UPCOMING)
                            ? "Class hasn't started yet"
                            : "This class has been missed";
                    Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void showStartConfirmation(LecturerScheduleClassSection session) {
            if (!isDeviceRegistered()) {
                Toast.makeText(itemView.getContext(),
                        "Device not registered. Please register your device first in Settings.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            Dialog dialog = new Dialog(itemView.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_start_class_confirmation);

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            TextView tvMessage = dialog.findViewById(R.id.tv_message);
            MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
            MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

            String message = String.format("Are you sure you want to start the class \"%s - %s\"?",
                    session.getCourseName(),
                    session.getSectionCode());
            tvMessage.setText(message);

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                startSession(session);
            });

            dialog.setCancelable(true);
            dialog.show();
        }

        private boolean isDeviceRegistered() {
            boolean registered = authManager.isDeviceRegistered();

            Log.d(TAG, "Device registration check: " + (registered ? "✅ Registered" : "❌ Not Registered"));

            return registered;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startSession(LecturerScheduleClassSection session) {
            // ⭐ KEY ADDITION: Load rounds from API first, then start BLE service
            startBLEAttendanceService(session);

            // Notify listener
            if (listener != null) {
                listener.onStartSession(session);
            }

            // Update session status and refresh UI
            session.setSessionStatus(STATUS_ACTIVE);
            notifyDataSetChanged();

            Log.d(TAG, "Session started: " + session.getSessionId());
        }

        // ⭐ RESTORED: Load rounds from API before starting BLE service
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startBLEAttendanceService(LecturerScheduleClassSection session) {
            try {
                String userId = authManager.getCurrentUserId();

                MainActivity mainActivity = getMainActivity();

                if (mainActivity != null && mainActivity.hasBLEPermissions()) {
                    // ✅ Có permissions, load rounds và start service
                    loadSessionRounds(session, userId);
                    Log.d(TAG, "✅ BLE service starting with permissions");
                } else if (mainActivity != null) {
                    // ❌ Thiếu permissions, request lại
                    Log.w(TAG, "⚠️ BLE permissions missing, requesting...");

                    mainActivity.requestBLEPermissions();
                } else {
                    // ❌ Không tìm được MainActivity, fallback
                    Log.w(TAG, "⚠️ Cannot find MainActivity, starting service without permission check");
                    loadSessionRounds(session, userId);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to start BLE service", e);
            }
        }

        // ⭐ RESTORED: Load session rounds from API
        private void loadSessionRounds(LecturerScheduleClassSection session, String userId) {
            Log.d(TAG, "Loading rounds for session: " + session.getSessionId());

            AttendanceApiService apiService = ApiClient.getClient(itemView.getContext())
                    .create(AttendanceApiService.class);

            apiService.getListRounds(session.getSessionId())
                    .enqueue(new Callback<RoundsDataResponse>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onResponse(Call<RoundsDataResponse> call, Response<RoundsDataResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                RoundsDataResponse apiResponse = response.body();

                                if (apiResponse.isSuccess()) {
                                    // ✅ FIXED: Map từ RoundsDataResponse thay vì RoundsResponse
                                    List<AttendanceModels.AttendanceRound> rounds =
                                            mapApiDataToAttendanceRounds(apiResponse.getData());

                                    if (!rounds.isEmpty()) {
                                        startBLEServiceWithRounds(session, userId, rounds);
                                        Log.d(TAG, "✅ Loaded " + rounds.size() + " rounds from API");
                                    } else {
                                        Log.w(TAG, "⚠️ No rounds found, starting service without rounds");
                                    }
                                } else {
                                    Log.e(TAG, "❌ API Error: " + apiResponse.getError());
                                }
                            } else {
                                Log.e(TAG, "❌ HTTP Error: " + response.code());
                            }
                        }

                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onFailure(Call<RoundsDataResponse> call, Throwable t) {
                            Log.e(TAG, "❌ Network Error", t);
                        }
                    });
        }


        private List<AttendanceModels.AttendanceRound> mapApiDataToAttendanceRounds(List<RoundDetail> apiRounds) {
            List<AttendanceModels.AttendanceRound> rounds = new ArrayList<>();

            if (apiRounds == null || apiRounds.isEmpty()) {
                return rounds;
            }

            // ✅ Format đúng theo server: yyyy-MM-dd HH:mm:ss
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            for (int i = 0; i < apiRounds.size(); i++) {
                RoundDetail apiRound = apiRounds.get(i);

                try {
                    // Parse StartTime
                    Date roundTime = format.parse(apiRound.getStartTime());
                    boolean isLastRound = (i == apiRounds.size() - 1);

                    AttendanceModels.AttendanceRound round = new AttendanceModels.AttendanceRound(
                            apiRound.getRoundId(),
                            roundTime,
                            apiRound.getRoundNumber(),
                            isLastRound
                    );

                    rounds.add(round);

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parsing round " + apiRound.getRoundNumber() +
                            " with time: " + apiRound.getStartTime(), e);
                }
            }

            Log.d(TAG, "✅ Total rounds mapped: " + rounds.size() + " out of " + apiRounds.size());
            return rounds;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startBLEServiceWithRounds(LecturerScheduleClassSection session,
                                               String userId,
                                               List<AttendanceModels.AttendanceRound> rounds) {
            try {
                Intent serviceIntent = new Intent(itemView.getContext(), BLEAttendanceService.class);
                serviceIntent.setAction("START_ATTENDANCE");
                serviceIntent.putExtra("session", session);
                serviceIntent.putExtra("userId", userId);
                serviceIntent.putExtra("userRole", "LECTURER");
                serviceIntent.putExtra("rounds", (Serializable) rounds);

                ContextCompat.startForegroundService(itemView.getContext(), serviceIntent);

                Toast.makeText(itemView.getContext(),
                        "Class started successfully with " + rounds.size() + " rounds",
                        Toast.LENGTH_LONG).show();

                Log.d(TAG, "✅ BLE Attendance Service started with " + rounds.size() +
                        " rounds for session: " + session.getSessionId());

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to start BLE service with API rounds", e);
                Toast.makeText(itemView.getContext(), "Failed to start attendance service",
                        Toast.LENGTH_SHORT).show();
            }
        }

        private MainActivity getMainActivity() {
            Context context = itemView.getContext();

            if (context instanceof MainActivity) {
                return (MainActivity) context;
            }

            while (context instanceof ContextWrapper) {
                if (context instanceof MainActivity) {
                    return (MainActivity) context;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }

            return null;
        }
    }
}
