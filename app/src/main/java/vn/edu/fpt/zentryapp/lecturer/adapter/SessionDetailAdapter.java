package vn.edu.fpt.zentryapp.lecturer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.ItemSessionDetailBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionDetail;

public class SessionDetailAdapter extends RecyclerView.Adapter<SessionDetailAdapter.SessionDetailViewHolder> {

    private List<SessionDetail> sessions = new ArrayList<>();
    private OnSessionDetailClickListener listener;

    public interface OnSessionDetailClickListener {
        void onSessionDetailClick(SessionDetail session);
    }

    public void setSessions(List<SessionDetail> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnSessionDetailClickListener(OnSessionDetailClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SessionDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSessionDetailBinding binding = ItemSessionDetailBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SessionDetailViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionDetailViewHolder holder, int position) {
        holder.bind(sessions.get(position));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class SessionDetailViewHolder extends RecyclerView.ViewHolder {
        private final ItemSessionDetailBinding binding;

        public SessionDetailViewHolder(ItemSessionDetailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener for entire item
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSessionDetailClick(sessions.get(position));
                }
            });

            // Set click listener for arrow
            binding.ivSessionArrow.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSessionDetailClick(sessions.get(position));
                }
            });
        }

        public void bind(SessionDetail session) {
            binding.tvSessionTitle.setText(session.getSessionTitle());
            binding.tvSessionAttendance.setText(session.getAttendanceSummary());
            binding.tvSessionDate.setText(session.getFormattedDate());

            // Set status-based styling
            setStatusStyling(session.getStatus());
        }

        private void setStatusStyling(String status) {
            switch (status) {
                case "COMPLETED":
                    binding.tvSessionDate.setTextColor(0xFF666666);
                    binding.ivSessionArrow.setVisibility(View.VISIBLE);
                    break;
                case "ONGOING":
                    binding.tvSessionDate.setTextColor(0xFFFF9800); // Orange
                    binding.ivSessionArrow.setVisibility(View.VISIBLE);
                    break;
                case "UPCOMING":
                    binding.tvSessionDate.setTextColor(0xFF2196F3); // Blue
                    binding.tvSessionAttendance.setText("Not started");
                    binding.ivSessionArrow.setVisibility(View.GONE);
                    break;
                case "CANCELLED":
                    binding.tvSessionDate.setTextColor(0xFFE53935); // Red
                    binding.tvSessionAttendance.setText("Cancelled");
                    binding.ivSessionArrow.setVisibility(View.GONE);
                    break;
            }
        }
    }
}
