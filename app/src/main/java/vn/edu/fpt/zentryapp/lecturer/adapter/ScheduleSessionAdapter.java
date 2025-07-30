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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
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
import vn.edu.fpt.zentryapp.databinding.ItemScheduleSessionBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleSession;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundsResponse;
import vn.edu.fpt.zentryapp.service.AttendanceApiService;
import vn.edu.fpt.zentryapp.service.AttendanceModels;
import vn.edu.fpt.zentryapp.service.BLEAttendanceService;

public class ScheduleSessionAdapter extends RecyclerView.Adapter<ScheduleSessionAdapter.ScheduleSessionViewHolder> {

    private static final String TAG = "ScheduleSessionAdapter";

    // Constants theo backend
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_COMPLETED = "Completed";
    private static final String STATUS_CANCELLED = "Cancelled";
    private static final String STATUS_ARCHIVED = "Archived";

    private List<LecturerScheduleSession> sessions = new ArrayList<>();
    private OnSessionActionListener listener;
    private AuthManager authManager;

    public interface OnSessionActionListener {
        void onSessionClick(LecturerScheduleSession session);

        void onStartSession(LecturerScheduleSession session);
    }

    public void setSessions(List<LecturerScheduleSession> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public ScheduleSessionAdapter(AuthManager authManager) {
        this.authManager = authManager;
    }

    public void setOnSessionActionListener(OnSessionActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScheduleSessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScheduleSessionBinding binding = ItemScheduleSessionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ScheduleSessionViewHolder(binding);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ScheduleSessionViewHolder holder, int position) {
        holder.bind(sessions.get(position));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class ScheduleSessionViewHolder extends RecyclerView.ViewHolder {
        private final ItemScheduleSessionBinding binding;

        public ScheduleSessionViewHolder(ItemScheduleSessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void bind(LecturerScheduleSession session) {
            Log.d(TAG, "=== BINDING SESSION ===");
            Log.d(TAG, "Position: " + getAdapterPosition());
            Log.d(TAG, "SessionId: " + session.getSessionId());
            Log.d(TAG, "Status: " + session.getStatus());
            Log.d(TAG, "StartTime: " + session.getStartTime());
            Log.d(TAG, "EndTime: " + session.getEndTime());
            Log.d(TAG, "========================");

            // Set basic info
            binding.tvSessionCourseName.setText(session.getCourseName());
            binding.tvSessionClassRoom.setText(session.getClassRoomDisplay());
            binding.tvSessionDateTime.setText(session.getDateTimeDisplay());

            // Cập nhật status display
            updateStatusDisplay(session);

            // Cập nhật action button logic
            configureActionButton(session);

            // Set click listener với logic mới
            binding.btnSessionAction.setOnClickListener(v -> handleButtonClick(v, session));

            // Set card styling based on status
            setCardStyling(session);
        }

        private void updateStatusDisplay(LecturerScheduleSession session) {
            String statusText = getStatusDisplayText(session);
            int statusColor = getStatusDisplayColor(session);

            binding.tvSessionStatus.setText(statusText);
            binding.tvSessionStatus.setTextColor(statusColor);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void handleButtonClick(View view, LecturerScheduleSession session) {
            String action = getSessionAction(session);

            switch (action) {
                case "START":
                    showStartClassConfirmation(view, session);
                    break;
                case "VIEW_DETAIL":
                    if (listener != null) {
                        listener.onSessionClick(session);
                    }
                    break;
                case "NOT_AVAILABLE":
                default:
                    Log.d(TAG, "Action not available for session: " + session.getSessionId());
                    break;
            }
        }

        /**
         * 🔧 LOGIC MỚI: Tìm session đúng dựa trên thời gian hiện tại
         */
        /**
         * 🔧 LOGIC ĐÚNG: Đọc status từ session list
         */
        private String getSessionAction(LecturerScheduleSession session) {
            Log.d(TAG, "=== GET SESSION ACTION ===");
            Log.d(TAG, "Checking session: " + session.getSessionId());

            // 🎯 BƯỚC 1: Kiểm tra trong danh sách sessions xem có session nào Active không
            boolean hasActiveSession = hasActiveSessionInList();

            if (hasActiveSession) {
                // Nếu có session Active, tìm session đó
                LecturerScheduleSession activeSession = findActiveSession();
                if (activeSession != null && session.getSessionId().equals(activeSession.getSessionId())) {
                    Log.d(TAG, "→ This is the ACTIVE session → VIEW_DETAIL");
                    return "VIEW_DETAIL";
                } else {
                    Log.d(TAG, "→ Not the active session → NOT_AVAILABLE");
                    return "NOT_AVAILABLE";
                }
            }

            // 🎯 BƯỚC 2: Không có session Active, kiểm tra session Pending trong thời gian hiện tại
            String rawStatus = getRawStatus(session);
            Log.d(TAG, "Session raw status: " + rawStatus);

            if ("Pending".equals(rawStatus)) {
                boolean isInCurrentTime = isSessionInCurrentTime(session);
                Log.d(TAG, "Is Pending session in current time: " + isInCurrentTime);

                if (isInCurrentTime) {
                    Log.d(TAG, "→ Pending session in current time → START");
                    return "START";
                } else {
                    Log.d(TAG, "→ Pending session not in current time → NOT_AVAILABLE");
                    return "NOT_AVAILABLE";
                }
            }

            // 🎯 BƯỚC 3: Các trường hợp khác
            if ("Completed".equals(rawStatus) ||
                    "Cancelled".equals(rawStatus) ||
                    "Archived".equals(rawStatus)) {
                Log.d(TAG, "→ Completed/Cancelled/Archived → VIEW_DETAIL");
                return "VIEW_DETAIL";
            }

            Log.d(TAG, "→ Unknown status → NOT_AVAILABLE");
            return "NOT_AVAILABLE";
        }

        /**
         * 🔧 KIỂM TRA có session Active trong danh sách không
         */
        private boolean hasActiveSessionInList() {
            for (LecturerScheduleSession session : sessions) {
                String rawStatus = getRawStatus(session);
                if ("Active".equals(rawStatus)) {
                    Log.d(TAG, "Found Active session in list: " + session.getSessionId());
                    return true;
                }
            }
            Log.d(TAG, "No Active session found in list");
            return false;
        }

        /**
         * 🔧 TÌM session Active trong danh sách
         */
        private LecturerScheduleSession findActiveSession() {
            for (LecturerScheduleSession session : sessions) {
                String rawStatus = getRawStatus(session);
                if ("Active".equals(rawStatus)) {
                    return session;
                }
            }
            return null;
        }

        /**
         * 🔧 LẤY raw status từ JSON (không phải display text)
         */
        /**
         * 🔧 LẤY raw status từ session list trong JSON
         */
        private String getRawStatus(LecturerScheduleSession session) {
            // Tìm session trong danh sách gốc dựa trên sessionId
            String sessionId = session.getSessionId();

            Log.d(TAG, "Getting raw status for session: " + sessionId);

            // Lặp qua danh sách sessions để tìm session với ID tương ứng
            for (LecturerScheduleSession s : sessions) {
                if (sessionId.equals(s.getSessionId())) {
                    // Lấy status từ field, không phải từ display method
                    String rawStatus = s.getStatus(); // Direct access to field
                    Log.d(TAG, "Found raw status: " + rawStatus + " for session: " + sessionId);
                    return rawStatus;
                }
            }

            // Fallback: nếu không tìm thấy, dùng display status và map
            String displayStatus = session.getStatus();
            Log.d(TAG, "Fallback to display status: " + displayStatus);

            // Map display status về raw status
            if ("ONGOING".equals(displayStatus)) {
                return "Active"; // ONGOING thường là Active session
            } else if ("READY TO START".equals(displayStatus)) {
                return "Pending";
            } else if ("COMPLETED".equals(displayStatus)) {
                return "Completed";
            } else if ("CANCELLED".equals(displayStatus)) {
                return "Cancelled";
            } else if ("ARCHIVED".equals(displayStatus)) {
                return "Archived";
            }

            return displayStatus; // Trả về như cũ nếu không map được
        }


        /**
         * 🔧 KIỂM TRA session có trong thời gian hiện tại không
         */
        private boolean isSessionInCurrentTime(LecturerScheduleSession session) {
            Date currentTime = new Date();
            Date startTime = session.getStartTime();
            Date endTime = session.getEndTime();

            boolean isInTime = currentTime.getTime() >= startTime.getTime() &&
                    currentTime.getTime() <= endTime.getTime();

            Log.d(TAG, "Session time check:");
            Log.d(TAG, "  Current: " + currentTime);
            Log.d(TAG, "  Start: " + startTime);
            Log.d(TAG, "  End: " + endTime);
            Log.d(TAG, "  Is in time: " + isInTime);

            return isInTime;
        }

        /**
         * 🔧 CẬP NHẬT findCurrentSession() để loại bỏ logic cũ
         */
        private LecturerScheduleSession findCurrentSession() {
            // Method này không còn cần thiết với logic mới
            // Nhưng giữ lại để tương thích với code khác
            return findActiveSession();
        }

        /**
         * 🔧 KIỂM TRA session có đang diễn ra không
         */
        private boolean isSessionInCurrentTime(LecturerScheduleSession session, Date currentTime) {
            try {
                // Parse UTC time từ server
                Date sessionStart = parseUTCToLocal(session.getStartTime());
                Date sessionEnd = parseUTCToLocal(session.getEndTime());

                // Kiểm tra thời gian hiện tại có nằm trong khoảng session không
                boolean isInTime = currentTime.getTime() >= sessionStart.getTime() &&
                        currentTime.getTime() <= sessionEnd.getTime();

                Log.d(TAG, "Session " + session.getSessionId() + ":");
                Log.d(TAG, "  Start: " + sessionStart);
                Log.d(TAG, "  End: " + sessionEnd);
                Log.d(TAG, "  Is in time: " + isInTime);

                return isInTime;

            } catch (Exception e) {
                Log.e(TAG, "Error checking session time", e);
                return false;
            }
        }

        /**
         * 🔧 PARSE UTC time sang local time
         */
        private Date parseUTCToLocal(Date utcDate) {
            // Nếu đã là local time thì return luôn
            return utcDate;
        }

        private void configureActionButton(LecturerScheduleSession session) {
            String action = getSessionAction(session);

            switch (action) {
                case "START":
                    binding.btnSessionAction.setText("Start Class");
                    binding.btnSessionAction.setVisibility(View.VISIBLE);
                    binding.btnSessionAction.setEnabled(true);
                    binding.btnSessionAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Green
                    break;

                case "VIEW_DETAIL":
                    binding.btnSessionAction.setText("View Detail");
                    binding.btnSessionAction.setVisibility(View.VISIBLE);
                    binding.btnSessionAction.setEnabled(true);
                    binding.btnSessionAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF2196F3)); // Blue
                    break;

                case "NOT_AVAILABLE":
                default:
                    String statusText = getNotAvailableReason(session);
                    binding.btnSessionAction.setText(statusText);
                    binding.btnSessionAction.setVisibility(View.VISIBLE);
                    binding.btnSessionAction.setEnabled(false);
                    binding.btnSessionAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF9E9E9E)); // Grey
                    break;
            }
        }

        private String getNotAvailableReason(LecturerScheduleSession session) {
            // Tìm session hiện tại
            LecturerScheduleSession currentSession = findCurrentSession();

            if (currentSession == null) {
                return "No Active Session";
            }

            if (!session.getSessionId().equals(currentSession.getSessionId())) {
                return "Not Current Session";
            }

            Date currentTime = new Date();
            Date startTime = session.getStartTime();
            Date endTime = session.getEndTime();
            String status = session.getStatus();

            if (STATUS_CANCELLED.equals(status)) {
                return "Cancelled";
            } else if (STATUS_ARCHIVED.equals(status)) {
                return "Archived";
            } else if (currentTime.getTime() < startTime.getTime()) {
                return "Not Started";
            } else if (currentTime.getTime() > endTime.getTime()) {
                return "Session Ended";
            } else {
                return "Not Available";
            }
        }

        private String getStatusDisplayText(LecturerScheduleSession session) {
            String status = session.getStatus();
            Date currentTime = new Date();
            Date startTime = session.getStartTime();
            Date endTime = session.getEndTime();

            if (STATUS_ACTIVE.equals(status)) {
                if (currentTime.getTime() >= startTime.getTime() && currentTime.getTime() <= endTime.getTime()) {
                    return "ONGOING";
                } else if (currentTime.getTime() > endTime.getTime()) {
                    return "COMPLETED";
                } else {
                    return "ACTIVE";
                }
            } else if (STATUS_PENDING.equals(status)) {
                if (currentTime.getTime() >= startTime.getTime() && currentTime.getTime() <= endTime.getTime()) {
                    return "READY TO START";
                } else {
                    return "PENDING";
                }
            } else if (STATUS_COMPLETED.equals(status)) {
                return "COMPLETED";
            } else if (STATUS_CANCELLED.equals(status)) {
                return "CANCELLED";
            } else if (STATUS_ARCHIVED.equals(status)) {
                return "ARCHIVED";
            } else {
                return status;
            }
        }

        private int getStatusDisplayColor(LecturerScheduleSession session) {
            String displayStatus = getStatusDisplayText(session);

            switch (displayStatus) {
                case "ONGOING":
                case "ACTIVE":
                    return 0xFF4CAF50; // Green
                case "READY TO START":
                    return 0xFFFF9800; // Orange
                case "PENDING":
                    return 0xFF2196F3; // Blue
                case "COMPLETED":
                    return 0xFF757575; // Grey
                case "CANCELLED":
                    return 0xFFF44336; // Red
                case "ARCHIVED":
                    return 0xFF9E9E9E; // Light Grey
                default:
                    return 0xFF757575; // Default grey
            }
        }

        // Các method khác giữ nguyên...
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void showStartClassConfirmation(View view, LecturerScheduleSession session) {
            Dialog dialog = new Dialog(view.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_start_class_confirmation);

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            TextView tvMessage = dialog.findViewById(R.id.tv_message);
            MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
            MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

            tvMessage.setText("Bạn có chắc chắn muốn bắt đầu lớp học \"" + session.getCourseName() + "\" không?");

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();

                startBLEAttendanceService(view.getContext(), session);

                if (listener != null) {
                    listener.onStartSession(session);
                }

                session.setStatus(STATUS_ACTIVE);
                notifyDataSetChanged();

                Log.d(TAG, "Session start initiated: " + session.getSessionId());
                Log.d(TAG, "BLE service started immediately for offline capability");
            });

            dialog.setCancelable(true);
            dialog.show();
        }

        // Các method còn lại giữ nguyên...
        // (startBLEAttendanceService, loadSessionRounds, mapApiRoundsToAttendanceRounds,
        //  startBLEServiceWithRounds, setCardStyling)

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startBLEAttendanceService(Context context, LecturerScheduleSession session) {
            try {
                String userId = authManager.getCurrentUserId();
                if (userId == null) {
                    Log.e(TAG, "User ID not available");
                    return;
                }

                // ➕ GET ACTIVITY FROM CONTEXT
                MainActivity mainActivity = getMainActivityFromContext(context);

                if (mainActivity != null && mainActivity.hasBLEPermissions()) {
                    // ✅ Có permissions, start service
                    loadSessionRounds(context, session, userId);
                    Log.d(TAG, "✅ BLE Attendance Service started with permissions");
                } else if (mainActivity != null) {
                    // ❌ Thiếu permissions, request lại
                    Log.w(TAG, "⚠️ BLE permissions missing, requesting...");
                    Toast.makeText(context, "Requesting BLE permissions for attendance...",
                            Toast.LENGTH_SHORT).show();

                    mainActivity.requestBLEPermissions();
                    Toast.makeText(context, "Please try starting class again after granting permissions",
                            Toast.LENGTH_LONG).show();
                } else {
                    // ❌ Không tìm được MainActivity, start anyway
                    Log.w(TAG, "⚠️ Cannot find MainActivity, starting service without permission check");
                    loadSessionRounds(context, session, userId);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to start BLE service", e);
            }
        }

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

            return null;
        }

        private void loadSessionRounds(Context context, LecturerScheduleSession session, String userId) {
            Log.d(TAG, "Loading rounds for session: " + session.getSessionId());

            AttendanceApiService apiService = ApiClient.getClient(context).create(AttendanceApiService.class);

            apiService.getSessionRounds(session.getSessionId())
                    .enqueue(new Callback<RoundsResponse>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onResponse(Call<RoundsResponse> call, Response<RoundsResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                RoundsResponse apiResponse = response.body();

                                if (apiResponse.isSuccess()) {
                                    List<AttendanceModels.AttendanceRound> rounds = mapApiRoundsToAttendanceRounds(apiResponse.getData());

                                    if (!rounds.isEmpty()) {
                                        startBLEServiceWithRounds(context, session, userId, rounds);
                                        Log.d(TAG, "✅ Loaded " + rounds.size() + " rounds from API");
                                    } else {
                                        Log.w(TAG, "⚠️ No rounds found, using fallback");
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
                        public void onFailure(Call<RoundsResponse> call, Throwable t) {
                            Log.e(TAG, "❌ Network Error", t);
                        }
                    });
        }

        private List<AttendanceModels.AttendanceRound> mapApiRoundsToAttendanceRounds(List<RoundData> apiRounds) {
            List<AttendanceModels.AttendanceRound> rounds = new ArrayList<>();

            if (apiRounds == null || apiRounds.isEmpty()) {
                return rounds;
            }

            // ✅ CORRECT: Explicit timezone handling
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Parse as UTC

            SimpleDateFormat vnFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            vnFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")); // Display as VN

            for (int i = 0; i < apiRounds.size(); i++) {
                RoundData apiRound = apiRounds.get(i);

                try {
                    // Parse UTC time
                    Date utcTime = utcFormat.parse(apiRound.getStartTime());

                    // ✅ MANUAL conversion với Calendar
                    Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    utcCal.setTime(utcTime);

                    Calendar vnCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                    vnCal.setTimeInMillis(utcCal.getTimeInMillis());

                    Date vietnamTime = vnCal.getTime();

                    boolean isLastRound = (i == apiRounds.size() - 1);

                    AttendanceModels.AttendanceRound round = new AttendanceModels.AttendanceRound(
                            apiRound.getRoundId(),
                            vietnamTime, // Đã convert đúng sang VN time
                            apiRound.getRoundNumber(),
                            isLastRound
                    );

                    rounds.add(round);

                    Log.d(TAG, "Mapped round " + apiRound.getRoundNumber() + ":");
                    Log.d(TAG, "  API UTC time: " + apiRound.getStartTime());
                    Log.d(TAG, "  Vietnam time: " + vnFormat.format(vietnamTime));
                    Log.d(TAG, "  Is last: " + isLastRound);

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing round " + apiRound.getRoundNumber(), e);
                }
            }

            Log.d(TAG, "✅ Total rounds mapped: " + rounds.size());
            return rounds;
        }


        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startBLEServiceWithRounds(Context context, LecturerScheduleSession session,
                                               String userId, List<AttendanceModels.AttendanceRound> rounds) {
            try {
                Intent serviceIntent = new Intent(context, BLEAttendanceService.class);
                serviceIntent.setAction("START_ATTENDANCE");
                serviceIntent.putExtra("session", session);
                serviceIntent.putExtra("userId", userId);
                serviceIntent.putExtra("userRole", "LECTURER");
                serviceIntent.putExtra("rounds", (Serializable) rounds);
                ContextCompat.startForegroundService(context, serviceIntent);

                Log.d(TAG, "✅ BLE Attendance Service started with " + rounds.size() +
                        " rounds for session: " + session.getSessionId());

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to start BLE service with API rounds", e);
            }
        }

        private void setCardStyling(LecturerScheduleSession session) {
            float alpha = 1.0f;
            int backgroundColor = 0xFFFFFFFF; // Default white

            String displayStatus = getStatusDisplayText(session);

            switch (displayStatus) {
                case "ONGOING":
                case "ACTIVE":
                    backgroundColor = 0xFFE8F5E8; // Light green
                    break;
                case "READY TO START":
                    backgroundColor = 0xFFFFF3E0; // Light orange
                    break;
                case "COMPLETED":
                    alpha = 0.7f;
                    backgroundColor = 0xFFF5F5F5; // Light grey
                    break;
                case "CANCELLED":
                    alpha = 0.6f;
                    backgroundColor = 0xFFFFEBEE; // Light red
                    break;
                case "ARCHIVED":
                    alpha = 0.5f;
                    backgroundColor = 0xFFF5F5F5; // Light grey
                    break;
                default:
                    backgroundColor = 0xFFFFFFFF; // White
                    break;
            }

            binding.getRoot().setCardBackgroundColor(backgroundColor);
            binding.getRoot().setAlpha(alpha);
        }
    }
}
