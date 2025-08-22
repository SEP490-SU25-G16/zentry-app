package vn.edu.fpt.zentryapp.student.adapter;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.MainActivity;
import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.ItemStudentScheduleClassSectionBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundDetail;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundsDataResponse;
import vn.edu.fpt.zentryapp.service.AttendanceApiService;
import vn.edu.fpt.zentryapp.service.AttendanceModels;
import vn.edu.fpt.zentryapp.service.BLEAttendanceService;
import vn.edu.fpt.zentryapp.service.ServiceUtils;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleClassSection;

public class StudentScheduleClassSectionAdapter extends RecyclerView.Adapter<StudentScheduleClassSectionAdapter.ViewHolder> {

    private static final String TAG = "StudentScheduleAdapter";

    private static final String PREF_NAME = "StudentJoinedSessions";
    private static final String KEY_JOINED_SESSIONS = "joined_sessions";

    // Session Status Constants (matching API response)
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_COMPLETED = "Completed";
    private static final String STATUS_MISSED = "Missed";

    // Button Actions
    private static final String ACTION_JOIN = "JOIN";
    private static final String ACTION_ONGOING = "ONGOING";
    private static final String ACTION_VIEW = "VIEW";
    private static final String ACTION_UPCOMING = "UPCOMING";
    private static final String ACTION_MISSED = "MISSED";
    private List<StudentScheduleClassSection> sessions = new ArrayList<>();
    private List<StudentScheduleClassSection> allSessions = new ArrayList<>();
    private OnSessionActionListener listener;
    private AuthManager authManager;
    private Context context;

    private Set<String> joinedSessions = new HashSet<>();

    public interface OnSessionActionListener {
        void onSessionClick(StudentScheduleClassSection session);
        void onJoinSession(StudentScheduleClassSection session);
    }

    public StudentScheduleClassSectionAdapter(AuthManager authManager) {
        this.authManager = authManager; // Will be updated when needed
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSessions(List<StudentScheduleClassSection> sessions) {
        this.allSessions = sessions != null ? new ArrayList<>(sessions) : new ArrayList<>();
        this.sessions = new ArrayList<>(allSessions);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            sessions = new ArrayList<>(allSessions);
        } else {
            String lower = query.toLowerCase();
            List<StudentScheduleClassSection> filtered = new ArrayList<>();
            for (StudentScheduleClassSection item : allSessions) {
                if ((item.getCourseName() != null && item.getCourseName().toLowerCase().contains(lower)) ||
                        (item.getSectionCode() != null && item.getSectionCode().toLowerCase().contains(lower)) ||
                        (item.getCourseDisplay() != null && item.getCourseDisplay().toLowerCase().contains(lower))) {
                    filtered.add(item);
                }
            }
            sessions = filtered;
        }
        notifyDataSetChanged();
    }

    public void setOnSessionActionListener(OnSessionActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentScheduleClassSectionBinding binding = ItemStudentScheduleClassSectionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(allSessions.get(position));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    private void loadJoinedSessionsFromPrefs() {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> savedSessions = prefs.getStringSet(KEY_JOINED_SESSIONS, new HashSet<>());

        Log.d(TAG, "Loaded " + savedSessions.size() + " joined sessions from SharedPreferences");
        // Có thể log để debug
        for (String sessionId : savedSessions) {
            Log.d(TAG, "Previously joined session: " + sessionId);
        }
    }

    // ✅ Save joined sessions vào SharedPreferences
    private void saveJoinedSessionToPrefs(String sessionId) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> savedSessions = new HashSet<>(prefs.getStringSet(KEY_JOINED_SESSIONS, new HashSet<>()));

        savedSessions.add(sessionId);
        prefs.edit().putStringSet(KEY_JOINED_SESSIONS, savedSessions).apply();

        Log.d(TAG, "Saved joined session to SharedPreferences: " + sessionId);
    }

    // ✅ Check session đã join chưa từ SharedPreferences
    private boolean isSessionJoinedFromPrefs(String sessionId) {
        if (context == null) return false;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> savedSessions = prefs.getStringSet(KEY_JOINED_SESSIONS, new HashSet<>());

        return savedSessions.contains(sessionId);
    }

    // ✅ Clear old sessions (optional - gọi khi cần cleanup)
    private void clearOldJoinedSessions() {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Log.d(TAG, "Cleared all joined sessions from SharedPreferences");
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentScheduleClassSectionBinding binding;

        public ViewHolder(ItemStudentScheduleClassSectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void bind(StudentScheduleClassSection session) {
            if (context == null) {
                context = itemView.getContext();
                loadJoinedSessionsFromPrefs(); // Load khi lần đầu set context
            }
            // ✅ SIMPLE: Check if session ended and kill BLE service
            checkAndKillBLEServiceIfSessionEnded(session);

            // Set basic session info
            setBasicInfo(session);

            // Configure button based on session status and timing
            configureButton(session);

            // Set card background based on status and timing
            setCardBackground(session);

            // Set click listener
            binding.btnStudentJoinNow.setOnClickListener(v -> handleButtonClick(session));
        }

        private void setBasicInfo(StudentScheduleClassSection session) {
            // Dòng 1: Course Name + Section Code
            binding.tvStudentSessionCourseName.setText(session.getCourseDisplay());

            // Dòng 2: Day + Time Range (chỉ giờ phút, không có giây)
            binding.tvStudentSessionDateTime.setText(session.getDayTimeDisplay());

            // Dòng 3: Building - Room
            binding.tvStudentSessionClassRoom.setText(session.getBuildingRoomDisplay());
        }

        private void configureButton(StudentScheduleClassSection session) {
            String action = determineButtonAction(session);

            switch (action) {
                case ACTION_JOIN:
                    setupButton("Join now", "#3B82F6", true); // Blue
                    break;

                case ACTION_ONGOING:
                    setupButton("Ongoing", "#10B981", true); // Green
                    break;

                case ACTION_VIEW:
                    setupButton("View", "#5265BF", true); // Purple
                    break;

                case ACTION_UPCOMING:
                    setupButton("Upcoming", "#A3BFED", false); // Gray
                    break;

                case ACTION_MISSED:
                    setupButton("Missed", "#FF001E", false); // Red
                    break;
            }
        }

        private String determineButtonAction(StudentScheduleClassSection session) {
            String status = session.getStatus();
            Date currentTime = new Date();

            // Parse session times
            Date startTime = parseSessionTimeForLogic(session.getStartTime());
            Date endTime = parseSessionTimeForLogic(session.getEndTime());

            // Check if session is currently happening (within time range)
            boolean isCurrentlyHappening = startTime != null && endTime != null &&
                    isCurrentTimeInSession(currentTime, startTime, endTime);

            // ✅ Check từ SharedPreferences
            boolean hasJoined = isSessionJoinedFromPrefs(session.getSessionId());
            boolean hasEnded = endTime != null && currentTime.after(endTime);
            switch (status) {
                case STATUS_PENDING:
                    if (hasEnded) {
                        return ACTION_MISSED;
                    }
                    // Pending luôn luôn là UPCOMING, dù có đang trong thời gian
                    return ACTION_UPCOMING;

                case STATUS_ACTIVE:
                    // 2.1 Đã hết giờ
                    if (hasEnded) {
                        return hasJoined ? ACTION_VIEW       // đã Join → chỉ xem lại
                                : ACTION_MISSED;    // chưa Join → Missed
                    }
                    // 2.2 Đang trong giờ
                    if (isCurrentlyHappening) {
                        return hasJoined ? ACTION_ONGOING    // đang học
                                : ACTION_JOIN;      // có thể Join
                    }
                    // 2.3 Chưa tới giờ (hiếm) → VIEW
                    return ACTION_VIEW;

                case STATUS_COMPLETED:
                    return ACTION_VIEW;

                case STATUS_MISSED:
                    return ACTION_MISSED;

                default:
                    return ACTION_UPCOMING;
            }
        }
        /**
         * Kiểm tra session đã kết thúc chưa, nếu có thì kill BLE service
         */
        private void checkAndKillBLEServiceIfSessionEnded(StudentScheduleClassSection session) {
            try {
                String sessionStatus = session.getStatus();
                boolean hasJoined = isSessionJoinedFromPrefs(session.getSessionId());

                boolean shouldKillService = shouldKillServiceForSession(session);

                Log.d(TAG, String.format("checkAndKillBLE - Session: %s, Status: %s, HasJoined: %s, ShouldKill: %s",
                        session.getSessionId(), sessionStatus, hasJoined, shouldKillService));

                // Kill service nếu đã join và thỏa mãn điều kiện
                if (hasJoined && shouldKillService) {
                    Log.d(TAG, "🛑 Session " + session.getSessionId() + " completed early but still in time range");

                    if (ServiceUtils.isServiceRunning(context, BLEAttendanceService.class)) {
                        Log.d(TAG, "🛑 BLE service is running, killing it for early-ended session");
                        ServiceUtils.killServiceIfRunning(context, BLEAttendanceService.class);
                    } else {
                        Log.d(TAG, "ℹ️ BLE service is not running");
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error checking session end", e);
            }
        }

        /**
         * ✅ CORRECT: Chỉ check thời gian hiện tại có trong [start, end] không
         * Logic: Session "Completed" + Current time trong khoảng [start, end] = Kill service
         */
        private boolean shouldKillServiceForSession(StudentScheduleClassSection session) {
            try {
                String status = session.getStatus();
                if (!"completed".equalsIgnoreCase(status)) {
                    return false; // Chỉ check khi status = "Completed"
                }

                Date currentTime = new Date();
                Date startTime = parseSessionTimeForLogic(session.getStartTime());
                Date endTime = parseSessionTimeForLogic(session.getEndTime());

                if (startTime == null || endTime == null) {
                    return false;
                }

                // ✅ ONLY CHECK: Thời gian hiện tại có nằm trong khoảng [start, end] không
                boolean isInSessionTime = isCurrentTimeInSession(currentTime, startTime, endTime);

                Log.d(TAG, String.format("shouldKillService - Status: %s, CurrentTime: %s, StartTime: %s, EndTime: %s, InRange: %s",
                        status,
                        formatTimeForLog(currentTime),
                        formatTimeForLog(startTime),
                        formatTimeForLog(endTime),
                        isInSessionTime));

                return isInSessionTime;

            } catch (Exception e) {
                Log.e(TAG, "Error checking should kill service", e);
                return false;
            }
        }


        /**
         * Helper method để format time cho logging
         */
        private String formatTimeForLog(Date date) {
            if (date == null) return "null";
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return format.format(date);
        }


        // Method để parse thời gian chính xác (bao gồm giây)
        private Date parseSessionTimeForLogic(String timeStr) {
            try {
                SimpleDateFormat timeFormat;

                // Tự động detect format dựa vào độ dài string
                if (timeStr != null && timeStr.length() > 5) {
                    timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                } else {
                    timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                }

                Date timeOnly = timeFormat.parse(timeStr);

                // Tạo Date với ngày hiện tại + thời gian parsed
                java.util.Calendar today = java.util.Calendar.getInstance();
                java.util.Calendar sessionTime = java.util.Calendar.getInstance();
                sessionTime.setTime(timeOnly);

                today.set(java.util.Calendar.HOUR_OF_DAY, sessionTime.get(java.util.Calendar.HOUR_OF_DAY));
                today.set(java.util.Calendar.MINUTE, sessionTime.get(java.util.Calendar.MINUTE));

                // ✅ Giữ nguyên giây từ API nếu có
                if (timeStr != null && timeStr.length() > 5) {
                    today.set(java.util.Calendar.SECOND, sessionTime.get(java.util.Calendar.SECOND));
                } else {
                    today.set(java.util.Calendar.SECOND, 0);
                }

                return today.getTime();

            } catch (Exception e) {
                Log.e(TAG, "Error parsing session time for logic: " + timeStr, e);
                return null;
            }
        }

        // Method để format thời gian cho hiển thị (chỉ giờ:phút)
        private String formatTimeForDisplay(String timeStr) {
            try {
                SimpleDateFormat inputFormat;

                // Parse input format
                if (timeStr != null && timeStr.length() > 5) {
                    inputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                } else {
                    inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                }

                Date time = inputFormat.parse(timeStr);

                // Format lại chỉ giờ:phút cho display
                SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return displayFormat.format(time);

            } catch (Exception e) {
                Log.e(TAG, "Error formatting time for display: " + timeStr, e);
                return timeStr; // fallback to original
            }
        }

        private Date parseSessionTime(String timeStr) {
            try {
                // Assuming time format is "HH:mm" or "HH:mm:ss"
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date timeOnly = timeFormat.parse(timeStr);

                // Create today's date with parsed time
                java.util.Calendar today = java.util.Calendar.getInstance();
                java.util.Calendar sessionTime = java.util.Calendar.getInstance();
                sessionTime.setTime(timeOnly);

                today.set(java.util.Calendar.HOUR_OF_DAY, sessionTime.get(java.util.Calendar.HOUR_OF_DAY));
                today.set(java.util.Calendar.MINUTE, sessionTime.get(java.util.Calendar.MINUTE));
                today.set(java.util.Calendar.SECOND, 0);

                return today.getTime();
            } catch (Exception e) {
                Log.e(TAG, "Error parsing session time: " + timeStr, e);
                return null;
            }
        }

        private boolean hasActiveSessionInList() {
            return allSessions.stream()
                    .anyMatch(session -> STATUS_ACTIVE.equals(session.getStatus()));
        }

        private boolean isCurrentTimeInSession(Date currentTime, Date startTime, Date endTime) {
            return currentTime.getTime() >= startTime.getTime() &&
                    currentTime.getTime() <= endTime.getTime();
        }

        private void setupButton(String text, String colorHex, boolean enabled) {
            binding.btnStudentJoinNow.setText(text);
            binding.btnStudentJoinNow.setEnabled(enabled);

            int color = Color.parseColor(colorHex);
            binding.btnStudentJoinNow.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(color));

            // Set text color - white for enabled, darker for disabled
            binding.btnStudentJoinNow.setTextColor(enabled ? Color.WHITE : Color.parseColor("#94A3B8"));
        }

        private void setCardBackground(StudentScheduleClassSection session) {
            String status = session.getStatus();
            Date currentTime = new Date();
            Date startTime = parseSessionTime(session.getStartTime());
            Date endTime = parseSessionTime(session.getEndTime());

            // Check if session is currently happening
            boolean isCurrentlyHappening = startTime != null && endTime != null &&
                    isCurrentTimeInSession(currentTime, startTime, endTime);

            int backgroundColor;

            switch (status) {
                case STATUS_PENDING:
                    // Pending luôn là màu trắng, dù có đang diễn ra hay không
                    backgroundColor = Color.WHITE;
                    break;

                case STATUS_ACTIVE:
                    if (isCurrentlyHappening) {
                        // Active + đang diễn ra = màu xanh nhạt
                        backgroundColor = Color.parseColor("#DCFCE7"); // Light green
                    } else {
                        // Active + không đang diễn ra = màu trắng
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
        private void handleButtonClick(StudentScheduleClassSection session) {
            String action = determineButtonAction(session);

            switch (action) {
                case ACTION_JOIN:
                    // Active + trong thời gian + chưa join → hiển thị confirm
                    showJoinConfirmation(session);
                    break;

                case ACTION_ONGOING:
                    // Active + trong thời gian + đã join → view
                    if (listener != null) {
                        listener.onSessionClick(session);
                    }
                    break;

                case ACTION_VIEW:
                    // Completed hoặc Active không trong thời gian → view
                    if (listener != null) {
                        listener.onSessionClick(session);
                    }
                    break;

                case ACTION_UPCOMING:
                    // Pending hoặc chưa tới giờ
                    String message;
                    Date currentTime = new Date();
                    Date startTime = parseSessionTime(session.getStartTime());
                    Date endTime = parseSessionTime(session.getEndTime());
                    boolean isCurrentlyHappening = startTime != null && endTime != null &&
                            isCurrentTimeInSession(currentTime, startTime, endTime);

                    if (isCurrentlyHappening && STATUS_PENDING.equals(session.getStatus())) {
                        message = "Lecturer hasn't started the class yet. Please wait for activation.";
                    } else {
                        message = "Class hasn't started yet. Please wait until " + session.getStartTime();
                    }
                    Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();
                    break;

                case ACTION_MISSED:
                    Toast.makeText(itemView.getContext(),
                            "This class has been missed",
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }


        @RequiresApi(api = Build.VERSION_CODES.O)
        private void showJoinConfirmation(StudentScheduleClassSection session) {
            if (!isDeviceRegistered()) {
                Toast.makeText(itemView.getContext(),
                        "Device not registered. Please register your device first in Settings.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (!isFaceIdRegistered()) {
                Toast.makeText(itemView.getContext(),
                        "FaceId not registered. Please register your faceid first in Settings.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Dialog dialog = new Dialog(itemView.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_join_class_confirmation);

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            TextView tvMessage = dialog.findViewById(R.id.tv_message);
            MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
            MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

            String message = String.format("Do you want to join the class \"%s - %s\"?",
                    session.getCourseName(),
                    session.getSectionCode());
            tvMessage.setText(message);

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                joinSession(session);
            });

            dialog.setCancelable(true);
            dialog.show();
        }
        private boolean isDeviceRegistered() {
            boolean registered = authManager.isDeviceRegistered();

            Log.d(TAG, "Device registration check: " + (registered ? "✅ Registered" : "❌ Not Registered"));

            return registered;
        }
        private boolean isFaceIdRegistered() {
            boolean faceIdRegistered = authManager.isFaceIdRegistered();

            Log.d(TAG, "FaceID registration check: " + (faceIdRegistered ? "✅ Registered" : "❌ Not Registered"));

            return faceIdRegistered;
        }

        @SuppressLint("NotifyDataSetChanged")
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void joinSession(StudentScheduleClassSection session) {
            // Load rounds from API first, then start BLE service
            loadSessionRounds(session);

            // Notify listener
            if (listener != null) {
                listener.onJoinSession(session);
            }

            // ✅ Save joined session vào SharedPreferences
            saveJoinedSessionToPrefs(session.getSessionId());

            // Refresh UI để hiển thị trạng thái mới
            notifyDataSetChanged();

            Log.d(TAG, "Student joined session: " + session.getSessionId());
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void loadSessionRounds(StudentScheduleClassSection session) {
            Log.d(TAG, "Loading rounds for student session: " + session.getSessionId());

            AttendanceApiService apiService = ApiClient.getClient(itemView.getContext())
                    .create(AttendanceApiService.class);

            apiService.getListRounds(session.getSessionId())
                    .enqueue(new Callback<RoundsDataResponse>() {
                        @Override
                        public void onResponse(Call<RoundsDataResponse> call, Response<RoundsDataResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                RoundsDataResponse apiResponse = response.body();

                                if (apiResponse.isSuccess()) {
                                    List<AttendanceModels.AttendanceRound> rounds = mapApiDataToAttendanceRounds(apiResponse.getData());

                                    if (!rounds.isEmpty()) {
                                        startBLEServiceWithRounds(session, rounds);
                                        Log.d(TAG, "✅ Student loaded " + rounds.size() + " rounds from API");
                                    } else {
                                        Log.w(TAG, "⚠️ No rounds found for student session");
                                    }
                                } else {
                                    Log.e(TAG, "❌ Student API Error: " + apiResponse.getError());
                                }
                            } else {
                                Log.e(TAG, "❌ Student HTTP Error: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<RoundsDataResponse> call, Throwable t) {
                            Log.e(TAG, "❌ Student Network Error", t);
                        }
                    });
        }

        // ✅ Mapping method - đã tốt rồi
        private List<AttendanceModels.AttendanceRound> mapApiDataToAttendanceRounds(List<RoundDetail> apiData) {
            List<AttendanceModels.AttendanceRound> rounds = new ArrayList<>();

            if (apiData == null || apiData.isEmpty()) {
                return rounds;
            }

            // ✅ Server format: yyyy-MM-dd HH:mm:ss
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            for (int i = 0; i < apiData.size(); i++) {
                RoundDetail apiRound = apiData.get(i);
                try {
                    // ✅ Parse time từ server
                    Date roundTime = format.parse(apiRound.getStartTime());
                    boolean isLastRound = (i == apiData.size() - 1);

                    // ✅ Create AttendanceModels.AttendanceRound object
                    AttendanceModels.AttendanceRound round = new AttendanceModels.AttendanceRound(
                            apiRound.getRoundId(),
                            roundTime,
                            apiRound.getRoundNumber(),
                            isLastRound
                    );

                    rounds.add(round);

                    Log.d(TAG, "✅ Mapped round " + apiRound.getRoundNumber() +
                            " - Time: " + apiRound.getStartTime() + " → " + roundTime.toString() +
                            " - Status: " + apiRound.getStatus() +
                            " - Attendance: " + apiRound.getAttendedCount() + "/" + apiRound.getTotalStudents());

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error mapping round " + apiRound.getRoundNumber() +
                            " with time: " + apiRound.getStartTime(), e);
                }
            }

            Log.d(TAG, "✅ Total rounds mapped: " + rounds.size() + " out of " + apiData.size());
            return rounds;
        }


        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startBLEServiceWithRounds(StudentScheduleClassSection session,
                                               List<AttendanceModels.AttendanceRound> rounds) {
            try {
                String userId = authManager.getCurrentUserId();
                if (userId == null) {
                    Log.e(TAG, "Student User ID not available");
                    return;
                }

                MainActivity mainActivity = getMainActivity();
                if (mainActivity != null && mainActivity.hasBLEPermissions()) {
                    Intent serviceIntent = new Intent(itemView.getContext(), BLEAttendanceService.class);
                    serviceIntent.setAction("START_ATTENDANCE");
                    serviceIntent.putExtra("session", session);
                    serviceIntent.putExtra("userId", userId);
                    serviceIntent.putExtra("userRole", "STUDENT");
                    serviceIntent.putExtra("rounds", (Serializable) rounds);

                    ContextCompat.startForegroundService(itemView.getContext(), serviceIntent);

                    Toast.makeText(itemView.getContext(),
                            "Joined class successfully with " + rounds.size() + " rounds",
                            Toast.LENGTH_LONG).show();

                    Log.d(TAG, "✅ Student BLE Service started with " + rounds.size() + " rounds");

                } else if (mainActivity != null) {
                    Log.w(TAG, "⚠️ Student BLE permissions missing, requesting...");
                    Toast.makeText(itemView.getContext(),
                            "Requesting BLE permissions for attendance...",
                            Toast.LENGTH_SHORT).show();
                    mainActivity.requestBLEPermissions();
                } else {
                    Log.w(TAG, "Cannot find MainActivity for student");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to start Student BLE service with rounds", e);
                Toast.makeText(itemView.getContext(),
                        "Failed to join class",
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
