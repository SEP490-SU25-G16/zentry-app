package vn.edu.fpt.zentryapp.lecturer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemStudentAttendanceBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Student;

public class LecturerReportListStudentOnSessionAdapter
        extends RecyclerView.Adapter<LecturerReportListStudentOnSessionAdapter.ViewHolder> {

    private final List<Student> students = new ArrayList<>();
    private OnAttendanceEditListener listener;
    private boolean canEditAttendance = true;        // <── GIỮ LẠI CỜ NÀY

    /* ---------- Interface ---------- */
    public interface OnAttendanceEditListener {
        void onEditAttendance(Student student);
    }

    /* ---------- Public setters ---------- */
    public void setStudents(List<Student> list) {
        students.clear();
        if (list != null) students.addAll(list);
        notifyDataSetChanged();
    }

    public void setOnAttendanceEditListener(OnAttendanceEditListener l) {
        this.listener = l;
    }

    public void setCanEditAttendance(boolean canEdit) {   // <── KHÔNG BỊ LỖI NỮA
        this.canEditAttendance = canEdit;
        notifyDataSetChanged();
    }

    /* ---------- Boilerplate ---------- */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentAttendanceBinding b =
                ItemStudentAttendanceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        h.bind(students.get(pos));
    }

    @Override
    public int getItemCount() { return students.size(); }

    /* ---------- ViewHolder ---------- */
    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentAttendanceBinding binding;

        ViewHolder(ItemStudentAttendanceBinding b) {
            super(b.getRoot());
            binding = b;
            binding.btnStudentEdit.setOnClickListener(v -> {
                int p = getAdapterPosition();
                if (p != RecyclerView.NO_POSITION && listener != null && canEditAttendance) {
                    listener.onEditAttendance(students.get(p));
                }
            });
        }

        void bind(Student s) {
            binding.tvStudentName.setText(s.getDisplayName());
            binding.tvStudentId.setText("ID: " + s.getStudentCode());

            binding.tvStudentStatus.setText(s.getAttendanceStatus());
            binding.tvStudentStatus.setTextColor(s.getAttendanceStatusColor());

            binding.btnStudentEdit.setEnabled(canEditAttendance);
            binding.btnStudentEdit.setAlpha(canEditAttendance ? 1f : 0.5f);
        }
    }
}
