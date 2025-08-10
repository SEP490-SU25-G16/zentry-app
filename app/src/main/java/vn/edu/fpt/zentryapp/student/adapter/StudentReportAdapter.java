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

    private List<StudentReport> reports = new ArrayList<>();       // Hiển thị
    private List<StudentReport> allReports = new ArrayList<>();    // Gốc
    private OnReportClickListener onReportClickListener;

    public interface OnReportClickListener {
        void onReportClick(StudentReport report);
    }

    public void setOnReportClickListener(OnReportClickListener listener) {
        this.onReportClickListener = listener;
    }

    public void setReports(List<StudentReport> reports) {
        this.allReports = reports != null ? new ArrayList<>(reports) : new ArrayList<>();
        this.reports = new ArrayList<>(allReports);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            reports = new ArrayList<>(allReports);
        } else {
            String lowerQuery = query.toLowerCase();
            List<StudentReport> filtered = new ArrayList<>();
            for (StudentReport item : allReports) {
                // Lọc theo courseTitle và lecturer (bạn có thể add field khác)
                if (item.getCourseTitle().toLowerCase().contains(lowerQuery) ||
                        item.getLecturerInfo().toLowerCase().contains(lowerQuery)) {
                    filtered.add(item);
                }
            }
            reports = filtered;
        }
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
            binding.tvCourseTitle.setText(report.getCourseTitle());
            binding.tvLecturerName.setText(report.getLecturerInfo());
            binding.tvSessions.setText(report.getSessionsText());
            binding.tvAttendancePercentage.setText(report.getAttendancePercentageText());
        }
    }
}
