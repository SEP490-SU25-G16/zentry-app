package vn.edu.fpt.zentryapp.student.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemStudentReportBinding;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentReport;

public class StudentReportAdapter extends RecyclerView.Adapter<StudentReportAdapter.ViewHolder> {

    private List<StudentReport> reports = new ArrayList<>();
    private OnReportClickListener onReportClickListener;

    public interface OnReportClickListener {
        void onReportClick(StudentReport report);
    }

    public void setOnReportClickListener(OnReportClickListener listener) {
        this.onReportClickListener = listener;
    }

    public void setReports(List<StudentReport> reports) {
        this.reports = reports != null ? reports : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentReportBinding binding = ItemStudentReportBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reports.get(position));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentReportBinding binding;

        public ViewHolder(@NonNull ItemStudentReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onReportClickListener != null) {
                    onReportClickListener.onReportClick(reports.get(position));
                }
            });
        }

        public void bind(StudentReport report) {
            // Set course title (Mathematics - G701)
            binding.tvCourseTitle.setText(report.getCourseTitle());

            // Set lecturer info (Lecturer: Hasha)
            binding.tvLecturerName.setText(report.getLecturerInfo());

            // Set sessions with purple background (15/20 Sessions)
            binding.tvSessions.setText(report.getSessionsText());

            // Set attendance percentage with green background (90%)
            binding.tvAttendancePercentage.setText(report.getAttendancePercentageText());
        }
    }
}
