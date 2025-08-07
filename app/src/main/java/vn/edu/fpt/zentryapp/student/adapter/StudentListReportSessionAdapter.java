package vn.edu.fpt.zentryapp.student.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.ItemSessionStudentBinding;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentSession;

public class StudentListReportSessionAdapter extends RecyclerView.Adapter<StudentListReportSessionAdapter.ViewHolder> {

    private List<StudentSession> studentSessions = new ArrayList<>();

    public void setSessions(List<StudentSession> studentSessions) {
        this.studentSessions = studentSessions != null ? studentSessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSessionStudentBinding binding = ItemSessionStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentSession studentSession = studentSessions.get(position);
        holder.bind(studentSession, position == studentSessions.size() - 1);
    }

    @Override
    public int getItemCount() {
        return studentSessions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSessionStudentBinding binding;

        public ViewHolder(@NonNull ItemSessionStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(StudentSession studentSession, boolean isLastItem) {
            binding.tvSessionTitle.setText(studentSession.getTitle());
            binding.tvSessionAttendance.setText(studentSession.getAttendanceStatus());
            binding.tvSessionDate.setText(studentSession.getDate());

            // Set attendance background và text color linh hoạt
            setAttendanceStyle(studentSession);

            // Hide divider for last item
            binding.viewSessionDivider.setVisibility(isLastItem ?
                    android.view.View.GONE : android.view.View.VISIBLE);
        }

        private void setAttendanceStyle(StudentSession studentSession) {
            String status = studentSession.getAttendanceStatus().toLowerCase();

            switch (status) {
                case "attended":
                case "present":
                    // Background xanh, text trắng
                    binding.tvSessionAttendance.setBackground(
                            ContextCompat.getDrawable(binding.getRoot().getContext(),
                                    R.drawable.rounded_green_background));
                    binding.tvSessionAttendance.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(),
                                    android.R.color.white));
                    break;

                case "absent":
                case "absented":
                    // Background đỏ, text trắng
                    binding.tvSessionAttendance.setBackground(
                            ContextCompat.getDrawable(binding.getRoot().getContext(),
                                    R.drawable.rounded_red_background));
                    binding.tvSessionAttendance.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(),
                                    android.R.color.white));
                    break;

                default:
                    // Background xám, text trắng (cho trường hợp khác)
                    binding.tvSessionAttendance.setBackground(
                            ContextCompat.getDrawable(binding.getRoot().getContext(),
                                    R.drawable.rounded_gray_background));
                    binding.tvSessionAttendance.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(),
                                    android.R.color.white));
                    break;
            }
        }
    }
}
