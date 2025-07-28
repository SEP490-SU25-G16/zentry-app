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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.ItemScheduleSessionBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleSession;
import vn.edu.fpt.zentryapp.service.AttendanceModels;
import vn.edu.fpt.zentryapp.service.BLEAttendanceService;

public class ScheduleSessionAdapter extends RecyclerView.Adapter<ScheduleSessionAdapter.ScheduleSessionViewHolder> {

    private List<LecturerScheduleSession> sessions = new ArrayList<>();
    private OnSessionActionListener listener;
    private AuthManager authManager; // 🔧 THÊM AuthManager

    public interface OnSessionActionListener {
        void onSessionClick(LecturerScheduleSession session);
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
            binding.tvSessionStatus.setText(session.getStatusText());
            binding.tvSessionStatus.setTextColor(session.getStatusColor());

            // Configure action button
            configureActionButton(session);

            // Chỉ giữ lại click listener cho button
            binding.btnSessionAction.setOnClickListener(v -> {
                if (session.isCanStartInstant() && listener != null) {
                    // Hiển thị popup xác nhận cho Start Class
                    showStartClassConfirmation(v, session);
                } else if (session.isCanViewDetail() && listener != null) {
                    listener.onSessionClick(session);
                }
            });


            // Set card styling based on status
            setCardStyling(session);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void showStartClassConfirmation(View view, LecturerScheduleSession session) {
            // Tạo dialog
            Dialog dialog = new Dialog(view.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_start_class_confirmation);

            // Làm cho dialog có background trong suốt để hiển thị CardView đẹp
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // Tìm các view trong dialog
            TextView tvMessage = dialog.findViewById(R.id.tv_message);
            MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
            MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

            // Set message
            tvMessage.setText("Bạn có chắc chắn muốn bắt đầu lớp học \"" + session.getCourseName() + "\" không?");

            // Set click listeners
            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();

                // **ĐÂY LÀ CHỖ BỐ TRÍ CODE START BLE SERVICE**
                startBLEAttendanceService(view.getContext(), session);

                // Đánh dấu session đã started (nếu cần)
                session.setCanViewDetail(true);
                notifyDataSetChanged();

                if (listener != null) {
                    listener.onSessionClick(session);
                }
            });

            // Cho phép hủy khi ấn ra ngoài
            dialog.setCancelable(true);
            // Hiển thị dialog
            dialog.show();
        }

        // Thêm method helper để start BLE service
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startBLEAttendanceService(Context context, LecturerScheduleSession session) {
            try {
                // 🔧 Lấy user ID từ AuthManager
                String userId = authManager.getCurrentUserId(); // Hoặc method tương tự
                if (userId == null) {
                    Log.e("ScheduleSessionAdapter", "User ID not available");
                    return;
                }
                // TODO: Get list round from all API
                // Test data
                // Tạo rounds cho test (3 rounds: 30s, 60s, 90s)
                List<AttendanceModels.AttendanceRound> rounds = Arrays.asList(
                        new AttendanceModels.AttendanceRound(new Date(System.currentTimeMillis() + 30000), 1, false),
                        new AttendanceModels.AttendanceRound(new Date(System.currentTimeMillis() + 60000), 2, false),
                        new AttendanceModels.AttendanceRound(new Date(System.currentTimeMillis() + 90000), 3, true)
                );

                // Start BLE service for student
                Intent serviceIntent = new Intent(context, BLEAttendanceService.class);
                serviceIntent.setAction("START_ATTENDANCE"); // 2 thằng này phải giống nhau mới start được
                serviceIntent.putExtra("session", session);
                serviceIntent.putExtra("userId", userId);
                serviceIntent.putExtra("rounds", (Serializable) rounds);
                context.startForegroundService(serviceIntent);

                Log.d("ScheduleSessionAdapter", "BLE Attendance Service started for session: " + session.getSessionId());

            } catch (Exception e) {
                Log.e("ScheduleSessionAdapter", "Failed to start BLE service", e);
            }
        }


        private void configureActionButton(LecturerScheduleSession session) {
            if (session.isCanStartInstant()) {
                binding.btnSessionAction.setText("Start Class");
                binding.btnSessionAction.setVisibility(View.VISIBLE);
                binding.btnSessionAction.setEnabled(true);
                binding.btnSessionAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Green
            } else if (session.isCanViewDetail()) {
                binding.btnSessionAction.setText("View Detail");
                binding.btnSessionAction.setVisibility(View.VISIBLE);
                binding.btnSessionAction.setEnabled(true);
                binding.btnSessionAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF2196F3)); // Blue
            } else {
                binding.btnSessionAction.setText("Not Available");
                binding.btnSessionAction.setVisibility(View.VISIBLE);
                binding.btnSessionAction.setEnabled(false);
                binding.btnSessionAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF9E9E9E)); // Grey
            }
        }

        private void setCardStyling(LecturerScheduleSession session) {
            float alpha = 1.0f;

            if ("COMPLETED".equals(session.getStatus()) || "CANCELLED".equals(session.getStatus())) {
                alpha = 0.7f; // Slightly transparent for past sessions
            } else if ("ONGOING".equals(session.getStatus())) {
                // Highlight ongoing sessions
                binding.getRoot().setCardBackgroundColor(0xFFE8F5E8); // Light green
            }

            binding.getRoot().setAlpha(alpha);
        }
    }
}
