package vn.edu.fpt.zentryapp.lecturer.adapter;


import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.ItemStudentAttendanceBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Student;

public class StudentAttendanceAdapter extends RecyclerView.Adapter<StudentAttendanceAdapter.StudentAttendanceViewHolder> {

    private List<Student> students = new ArrayList<>();
    private OnAttendanceEditListener listener;
    private boolean canEditAttendance = true;

    public interface OnAttendanceEditListener {
        void onEditAttendance(Student student);
    }

    public void setStudents(List<Student> students) {
        this.students = students != null ? students : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCanEditAttendance(boolean canEdit) {
        this.canEditAttendance = canEdit;
        notifyDataSetChanged();
    }

    public void setOnAttendanceEditListener(OnAttendanceEditListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentAttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentAttendanceBinding binding = ItemStudentAttendanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StudentAttendanceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentAttendanceViewHolder holder, int position) {
        holder.bind(students.get(position));
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    class StudentAttendanceViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentAttendanceBinding binding;

        public StudentAttendanceViewHolder(ItemStudentAttendanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener for edit button
            binding.btnStudentEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null && canEditAttendance) {
                    listener.onEditAttendance(students.get(position));
                }
            });
        }

        public void bind(Student student) {
            // Set student info
            binding.tvStudentName.setText(student.getDisplayName());
            binding.tvStudentId.setText("ID: " + student.getStudentCode());

            // Set attendance status
            binding.tvStudentStatus.setText(student.getAttendanceStatus());
            binding.tvStudentStatus.setTextColor(student.getAttendanceStatusColor());

            // Enable/disable edit button based on time constraint
            binding.btnStudentEdit.setEnabled(canEditAttendance);
            binding.btnStudentEdit.setAlpha(canEditAttendance ? 1.0f : 0.5f);

            // Show edit restriction tooltip
            if (!canEditAttendance) {
                binding.btnStudentEdit.setOnLongClickListener(v -> {
                    android.widget.Toast.makeText(v.getContext(),
                            "Editing disabled after 24 hours", android.widget.Toast.LENGTH_SHORT).show();
                    return true;
                });
            }
        }
    }
}
