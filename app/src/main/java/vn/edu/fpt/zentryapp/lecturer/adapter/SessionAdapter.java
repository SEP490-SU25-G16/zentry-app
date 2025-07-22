package vn.edu.fpt.zentryapp.lecturer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemSessionBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Session;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private List<Session> sessions = new ArrayList<>();
    private OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(Session session);
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSessionBinding binding = ItemSessionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SessionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        holder.bind(sessions.get(position));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {
        private final ItemSessionBinding binding;

        public SessionViewHolder(ItemSessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSessionClick(sessions.get(position));
                }
            });
        }

        public void bind(Session session) {
            binding.tvSessionTitle.setText(session.getCourseName());
            binding.tvSessionClassInfo.setText(session.getClassInfo());
            binding.tvSessionTime.setText(session.getTimeRange());
            binding.tvSessionAttendance.setText(session.getAttendanceSummary());
            binding.tvSessionTaskSummary.setText(session.getTaskSummary());
            binding.pbSessionProgress.setProgress(session.getTaskCompletionPercentage());

            // Set status indicator color
            setStatusIndicator(session.getStatus());
        }

        private void setStatusIndicator(String status) {
            int color;
            switch (status) {
                case "COMPLETED":
                    color = 0xFF4CAF50; // Green
                    break;
                case "ONGOING":
                    color = 0xFFFF9800; // Orange
                    break;
                case "SCHEDULED":
                    color = 0xFF2196F3; // Blue
                    break;
                default:
                    color = 0xFF9E9E9E; // Grey
            }
            // You can add a status indicator view to the layout and set its color
            // binding.viewStatusIndicator.setBackgroundColor(color);
        }
    }
}