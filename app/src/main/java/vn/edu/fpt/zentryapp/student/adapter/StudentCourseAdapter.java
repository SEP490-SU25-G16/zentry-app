package vn.edu.fpt.zentryapp.student.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.ItemStudentCourseBinding;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentCourse;

public class StudentCourseAdapter extends RecyclerView.Adapter<StudentCourseAdapter.ViewHolder> {

    private List<StudentCourse> courses = new ArrayList<>();
    private OnCourseClickListener onCourseClickListener;

    public interface OnCourseClickListener {
        void onCourseClick(StudentCourse course);
    }

    public void setOnCourseClickListener(OnCourseClickListener listener) {
        this.onCourseClickListener = listener;
    }

    public void setCourses(List<StudentCourse> courses) {
        this.courses = courses != null ? courses : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentCourseBinding binding = ItemStudentCourseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentCourse course = courses.get(position);
        holder.bind(course);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentCourseBinding binding;

        public ViewHolder(@NonNull ItemStudentCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onCourseClickListener != null) {
                    onCourseClickListener.onCourseClick(courses.get(position));
                }
            });
        }

        public void bind(StudentCourse course) {
            binding.tvCourseTitle.setText(course.getName());
            binding.tvCourseInfo.setText(course.getClassInfo());
            binding.tvAttendance.setText(course.getAttendanceText());
            binding.tvTaskProgress.setText(course.getTaskProgressText());

            // Set progress
            int progressPercentage = course.getProgressPercentage();
            binding.pbProgress.setProgress(progressPercentage);
            binding.tvProgressPercentage.setText(progressPercentage + "%");

            // Set attendance color based on rate
            if (course.getAttendanceRate() >= 90) {
                binding.tvAttendance.setTextColor(binding.getRoot().getContext().getColor(android.R.color.holo_green_dark));
            } else if (course.getAttendanceRate() >= 75) {
                binding.tvAttendance.setTextColor(binding.getRoot().getContext().getColor(android.R.color.holo_orange_dark));
            } else {
                binding.tvAttendance.setTextColor(binding.getRoot().getContext().getColor(android.R.color.holo_red_dark));
            }
        }

    }
}

