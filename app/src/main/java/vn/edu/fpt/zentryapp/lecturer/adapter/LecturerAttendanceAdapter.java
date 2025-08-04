package vn.edu.fpt.zentryapp.lecturer.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemAttendanceBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Attendance;

public class LecturerAttendanceAdapter extends RecyclerView.Adapter<LecturerAttendanceAdapter.AttendanceViewHolder> {

    private List<Attendance> finalAttendance = new ArrayList<>();
    private OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onStudentClick(Attendance student);
    }

    public void setFinalAttendance(List<Attendance> finalAttendance) {
        this.finalAttendance = finalAttendance != null ? finalAttendance : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnStudentClickListener(OnStudentClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAttendanceBinding binding = ItemAttendanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AttendanceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        // ✅ Pass position + 1 để bắt đầu từ 1 thay vì 0
        holder.bind(finalAttendance.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return finalAttendance.size();
    }

    class AttendanceViewHolder extends RecyclerView.ViewHolder {
        private final ItemAttendanceBinding binding;

        public AttendanceViewHolder(ItemAttendanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    // listener.onStudentClick(finalAttendance.get(position));
                }
            });
        }

        // ✅ Thêm parameter studentNumber
        @SuppressLint("SetTextI18n")
        public void bind(Attendance student, int studentNumber) {
            // ✅ Set student number (1, 2, 3, ...)
            binding.tvStudentNumber.setText(String.valueOf(studentNumber));

            binding.tvStudentName.setText(student.getStudentName());
            binding.tvStudentCode.setText("ID: " + student.getStudentCode());
            binding.tvAttendanceStatus.setText(student.getAttendanceStatus());
            binding.tvAttendanceStatus.setTextColor(student.getAttendanceStatusColor());
        }
    }
}
