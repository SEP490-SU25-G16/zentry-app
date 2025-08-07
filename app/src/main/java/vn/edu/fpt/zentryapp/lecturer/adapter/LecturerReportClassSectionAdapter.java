package vn.edu.fpt.zentryapp.lecturer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.ItemReportClassSectionBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerReportClassSection;

public class LecturerReportClassSectionAdapter extends RecyclerView.Adapter<LecturerReportClassSectionAdapter.ClassroomViewHolder> {

    private List<LecturerReportClassSection> classrooms = new ArrayList<>();
    private OnLecturerReportClassSectionClickListener listener;

    public interface OnLecturerReportClassSectionClickListener {
        void onClassroomClick(LecturerReportClassSection classroom);
    }

    public void setClassrooms(List<LecturerReportClassSection> classrooms) {
        this.classrooms = classrooms != null ? classrooms : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnClassroomClickListener(OnLecturerReportClassSectionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClassroomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReportClassSectionBinding binding = ItemReportClassSectionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ClassroomViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassroomViewHolder holder, int position) {
        holder.bind(classrooms.get(position));
    }

    @Override
    public int getItemCount() {
        return classrooms.size();
    }

    class ClassroomViewHolder extends RecyclerView.ViewHolder {
        private final ItemReportClassSectionBinding binding;

        public ClassroomViewHolder(ItemReportClassSectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onClassroomClick(classrooms.get(position));
                }
            });
        }

        public void bind(LecturerReportClassSection classroom) {
            // Bind subject/course name
            binding.tvSubject.setText(classroom.getCourseName());

            // Bind students count
            String studentsText = classroom.getStudentCount() + " Students";
            binding.tvStudentsCount.setText(studentsText);

            // Bind sessions count
            String sessionsText = classroom.getCurrentSessions() + "/" + classroom.getTotalSessions() + " Sessions";
            binding.tvSessionsCount.setText(sessionsText);

            // Bind attendance percentage
            String attendanceText = Math.round(classroom.getAttendancePercentage()) + "%";
            binding.tvAttendancePercent.setText(attendanceText);

            // Set attendance circle color based on percentage
            setAttendanceColor(classroom.getAttendancePercentage());
        }

        private void setAttendanceColor(double attendancePercentage) {
            int colorRes;
            int backgroundRes;

            if (attendancePercentage >= 90) {
                // Green for excellent attendance (90%+)
                colorRes = R.color.attendance_excellent;
                backgroundRes = R.drawable.bg_attendance_excellent;
            } else if (attendancePercentage >= 75) {
                // Orange for good attendance (75-89%)
                colorRes = R.color.attendance_good;
                backgroundRes = R.drawable.bg_attendance_good;
            } else if (attendancePercentage >= 60) {
                // Blue for average attendance (60-74%)
                colorRes = R.color.attendance_average;
                backgroundRes = R.drawable.bg_attendance_average;
            } else {
                // Red for poor attendance (below 60%)
                colorRes = R.color.attendance_poor;
                backgroundRes = R.drawable.bg_attendance_poor;
            }

            // Set background drawable for attendance circle
            binding.tvAttendancePercent.setBackgroundResource(backgroundRes);
        }
    }
}
