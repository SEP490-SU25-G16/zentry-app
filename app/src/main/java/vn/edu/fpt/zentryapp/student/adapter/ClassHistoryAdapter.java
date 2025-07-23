package vn.edu.fpt.zentryapp.student.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemClassSessionSimpleBinding;
import vn.edu.fpt.zentryapp.student.data.model.response.ClassSession;

public class ClassHistoryAdapter extends RecyclerView.Adapter<ClassHistoryAdapter.ViewHolder> {

    private List<ClassSession> sessions = new ArrayList<>();
    private OnSessionClickListener onSessionClickListener;

    public interface OnSessionClickListener {
        void onSessionClick(ClassSession session);
    }

    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.onSessionClickListener = listener;
    }

    public void setSessions(List<ClassSession> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemClassSessionSimpleBinding binding = ItemClassSessionSimpleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassSession session = sessions.get(position);
        holder.bind(session);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemClassSessionSimpleBinding binding;

        public ViewHolder(@NonNull ItemClassSessionSimpleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onSessionClickListener != null) {
                    onSessionClickListener.onSessionClick(sessions.get(position));
                }
            });
        }

        public void bind(ClassSession session) {
            // Set session info
            binding.tvSessionNumber.setText(session.getSessionNumber());
            binding.tvSessionDate.setText(session.getFormattedDate());
            binding.tvAttendanceStatus.setText(session.getMyAttendanceStatus());

            // Set colors based on attendance status
            int color = session.getAttendanceColor();
            binding.viewAttendanceIndicator.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(color));
            binding.tvAttendanceStatus.setTextColor(color);
        }
    }
}
