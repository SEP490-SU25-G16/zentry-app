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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.ItemScheduleBinding;
import vn.edu.fpt.zentryapp.service.AttendanceModels;
import vn.edu.fpt.zentryapp.service.BLEAttendanceService;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleSession;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

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

            // Set clickable state and visual feedback
            boolean isClickable = studentScheduleSession.isClickable();
            StudentScheduleSession.ScheduleStatus status = studentScheduleSession.getStatus();

            // Configure click behavior
            if (isClickable) {
                binding.getRoot().setOnClickListener(v -> {
                    // 🔧 Hiển thị dialog confirm thay vì navigate trực tiếp
                    showJoinClassConfirmation(v, studentScheduleSession);
                });
                binding.getRoot().setClickable(true);
                binding.getRoot().setFocusable(true);
                binding.getRoot().setAlpha(1.0f);
            } else {
                binding.getRoot().setOnClickListener(v -> {
                    // Show message when not clickable
                    Toast.makeText(v.getContext(),
                            "Class is not available yet. Please wait until class time.",
                            Toast.LENGTH_SHORT).show();
                });
                binding.getRoot().setClickable(true);
                binding.getRoot().setFocusable(true);
                binding.getRoot().setAlpha(0.6f); // Dim appearance
            }
        }

        // 🔧 THÊM method hiển thị dialog confirm
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void showJoinClassConfirmation(View view, StudentScheduleSession studentScheduleSession) {
            // Tạo dialog
            Dialog dialog = new Dialog(view.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_join_class_confirmation);

            // Làm cho dialog có background trong suốt
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // Tìm các view trong dialog
            TextView tvMessage = dialog.findViewById(R.id.tv_message);
            MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
            MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

            // Set message
            tvMessage.setText("Bạn có muốn tham gia lớp học \"" + studentScheduleSession.getClassNameWithGrade() + "\" không?");

            // Set click listeners
            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();

                // 🔧 Bắt đầu BLE service cho student
                startStudentBLEService(view.getContext(), studentScheduleSession);

                // Mark as joined (có thể cần thêm field này vào Schedule model)
                // schedule.setJoined(true);
                notifyDataSetChanged();

                // Callback để navigate hoặc update UI
                if (onScheduleClickListener != null) {
                    onScheduleClickListener.onScheduleClick(studentScheduleSession);
                }
            });

            // Cho phép hủy khi ấn ra ngoài
            dialog.setCancelable(true);
            dialog.show();
        }

        // 🔧 THÊM method start BLE service cho student
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void startStudentBLEService(Context context, StudentScheduleSession studentScheduleSession) {
            try {
                // Lấy user ID từ AuthManager
                String userId = authManager.getCurrentUserId();

                if (userId == null) {
                    Log.e("ScheduleAdapter", "Student User ID not available");
                    Toast.makeText(context, "Không thể xác định thông tin sinh viên", Toast.LENGTH_SHORT).show();
                    return;
                }
                // TODO: get round api for student

                // Tạo rounds cho student (giống lecturer)
                List<AttendanceModels.AttendanceRound> rounds = Arrays.asList(
                        new AttendanceModels.AttendanceRound(new Date(System.currentTimeMillis() + 30000), 1, false),
                        new AttendanceModels.AttendanceRound(new Date(System.currentTimeMillis() + 60000), 2, false),
                        new AttendanceModels.AttendanceRound(new Date(System.currentTimeMillis() + 90000), 3, true)
                );

                // Start BLE service
                Intent serviceIntent = new Intent(context, BLEAttendanceService.class);
                serviceIntent.setAction("START_ATTENDANCE");
                serviceIntent.putExtra("session", studentScheduleSession);
                serviceIntent.putExtra("userId", userId);
                serviceIntent.putExtra("userRole", "STUDENT"); // 🔧 Đánh dấu là student
                serviceIntent.putExtra("rounds", (Serializable) rounds);
                context.startForegroundService(serviceIntent);

                Log.d("ScheduleAdapter", "Student BLE Service started - Schedule: " + studentScheduleSession.getClassNameWithGrade() + ", User: " + userId);

                Toast.makeText(context, "Đã tham gia lớp học và bắt đầu điểm danh BLE", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e("ScheduleAdapter", "Failed to start Student BLE service", e);
                Toast.makeText(context, "Lỗi khi tham gia lớp học", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
