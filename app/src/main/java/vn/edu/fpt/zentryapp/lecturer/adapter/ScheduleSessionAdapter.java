package vn.edu.fpt.zentryapp.lecturer.adapter;

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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
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

    // 🔧 CẬP NHẬT: Định nghĩa constants theo backend
    private static final String STATUS_PENDING = "Pending";      // ID: 1
    private static final String STATUS_ACTIVE = "Active";        // ID: 2
    private static final String STATUS_COMPLETED = "Completed";  // ID: 3
    private static final String STATUS_CANCELLED = "Cancelled";  // ID: 4
    private static final String STATUS_ARCHIVED = "Archived";    // ID: 5

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

        /**
         * 🔧 CẬP NHẬT: Hiển thị status chính xác
         */
        private void updateStatusDisplay(LecturerScheduleSession session) {
            String statusText = getStatusDisplayText(session);
            int statusColor = getStatusDisplayColor(session);

            binding.tvSessionStatus.setText(statusText);
            binding.tvSessionStatus.setTextColor(statusColor);
        }

        /**
         * 🔧 XỬ LÝ click button với logic mới
         */
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
         * 🔧 CẬP NHẬT: Xác định action dựa trên status constants đúng
         */
        private String getSessionAction(LecturerScheduleSession session) {
            Date currentTime = new Date();
            Date startTime = session.getStartTime();
            Date endTime = session.getEndTime();

            // Chỉ cho phép start khi đúng giờ (trong khoảng thời gian session)
            boolean isInSessionTime = currentTime.getTime() >= startTime.getTime() &&
                    currentTime.getTime() <= endTime.getTime();
            boolean isAfterStart = currentTime.getTime() >= startTime.getTime();
            boolean isBeforeEnd = currentTime.getTime() <= endTime.getTime();

            String status = session.getStatus();

            if (STATUS_ACTIVE.equals(status)) {
                // Session đã được start (Active)
                if (isAfterStart && isBeforeEnd) {
                    return "VIEW_DETAIL"; // Đang diễn ra → cho xem detail
                } else if (currentTime.getTime() > endTime.getTime()) {
                    return "VIEW_DETAIL"; // Đã kết thúc → cho xem detail
                } else {
                    return "NOT_AVAILABLE"; // Chưa đến giờ nhưng đã start
                }
            } else if (STATUS_COMPLETED.equals(status)) {
                return "VIEW_DETAIL"; // Đã hoàn thành → cho xem detail
            } else if (STATUS_CANCELLED.equals(status) || STATUS_ARCHIVED.equals(status)) {
                return "VIEW_DETAIL"; // Đã hủy/lưu trữ → chỉ cho xem detail
            } else if (STATUS_PENDING.equals(status)) {
                // Session chưa start (Pending) - chỉ cho start khi đúng giờ
                if (isInSessionTime) {
                    return "START"; // Đúng giờ → cho phép start
                } else {
                    return "NOT_AVAILABLE"; // Chưa đến giờ hoặc đã quá giờ
                }
            } else {
                // Trường hợp khác hoặc status không xác định
                if (isInSessionTime) {
                    return "START"; // Có thể start nếu đúng giờ
                } else {
                    return "NOT_AVAILABLE";
                }
            }
        }

        /**
         * 🔧 CẤU HÌNH button dựa trên action
         */
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

        /**
         * 🔧 LÝ DO tại sao không available
         */
        private String getNotAvailableReason(LecturerScheduleSession session) {
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

        /**
         * 🔧 CẬP NHẬT: Status display text theo constants đúng
         */
        private String getStatusDisplayText(LecturerScheduleSession session) {
            String status = session.getStatus();
            Date currentTime = new Date();
            Date startTime = session.getStartTime();
            Date endTime = session.getEndTime();

            if (STATUS_ACTIVE.equals(status)) {
                if (currentTime.getTime() >= startTime.getTime() && currentTime.getTime() <= endTime.getTime()) {
                    return "ONGOING"; // Hiển thị ONGOING khi Active và đang diễn ra
                } else if (currentTime.getTime() > endTime.getTime()) {
                    return "COMPLETED"; // Active nhưng đã kết thúc
                } else {
                    return "ACTIVE"; // Active nhưng chưa đến giờ
                }
            } else if (STATUS_PENDING.equals(status)) {
                if (currentTime.getTime() >= startTime.getTime() && currentTime.getTime() <= endTime.getTime()) {
                    return "READY TO START"; // Pending nhưng đã đến giờ
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
                return status; // Fallback
            }
        }

        /**
         * 🔧 CẬP NHẬT: Status display color theo constants đúng
         */
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

                // 1. Start BLE service ngay lập tức (offline-first)
                startBLEAttendanceService(view.getContext(), session);

                // 2. Gọi API trong background (best effort)
                if (listener != null) {
                    listener.onStartSession(session);
                }

                // 3. Update local session status optimistically
                session.setStatus("Active"); // Optimistic update
                notifyDataSetChanged();

                Log.d(TAG, "Session start initiated: " + session.getSessionId());
                Log.d(TAG, "BLE service started immediately for offline capability");
            });

            dialog.setCancelable(true);
            dialog.show();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startBLEAttendanceService(Context context, LecturerScheduleSession session) {
            try {
                String userId = authManager.getCurrentUserId();
                if (userId == null) {
                    Log.e(TAG, "User ID not available");
                    return;
                }

                // 🔧 GỌI API LẤY ROUNDS THẬT
                loadSessionRounds(context, session, userId);

                Log.d(TAG, "BLE Attendance Service started for session: " + session.getSessionId());

            } catch (Exception e) {
                Log.e(TAG, "Failed to start BLE service", e);
            }
        }

        /**
         * 🔧 THÊM method để load rounds từ API
         */
        private void loadSessionRounds(Context context, LecturerScheduleSession session, String userId) {
            Log.d(TAG, "Loading rounds for session: " + session.getSessionId());

            // Tạo API service
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
                                        // Start BLE service với rounds thật
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

        /**
         * 🔧 MAP API rounds sang AttendanceModels.AttendanceRound
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
                    // Parse start time từ ISO string
                    Date startTime = isoFormat.parse(apiRound.getStartTime());

                    // Xác định nếu đây là round cuối cùng
                    boolean isLastRound = (i == apiRounds.size() - 1);

                    AttendanceModels.AttendanceRound round = new AttendanceModels.AttendanceRound(
                            apiRound.getRoundId(),        // 🔧 THÊM roundId
                            startTime,                    // executionTime
                            apiRound.getRoundNumber(),    // roundNumber
                            isLastRound                   // isLastRound
                    );

                    rounds.add(round);

                    Log.d(TAG, "Mapped round " + apiRound.getRoundNumber() +
                            ": " + apiRound.getStartTime() +
                            " (isLast: " + isLastRound + ")");

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing round " + apiRound.getRoundNumber(), e);
                }
            }

            return rounds;
        }

        /**
         * 🔧 START BLE service với rounds từ API
         */
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
                context.startForegroundService(serviceIntent);

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
