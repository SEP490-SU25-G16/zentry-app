package vn.edu.fpt.zentryapp.lecturer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemSessionDetailBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.OverviewSession;

public class LecturerListSessionDetailAdapter extends RecyclerView.Adapter<LecturerListSessionDetailAdapter.SessionDetailViewHolder> {

    private List<OverviewSession> sessions = new ArrayList<>();
    private OnSessionDetailClickListener listener;

    public interface OnSessionDetailClickListener {
        void onSessionDetailClick(OverviewSession session);
    }

    public void setSessions(List<OverviewSession> sessions) {
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

        }

        public void bind(OverviewSession session) {
            binding.tvSessionTitle.setText(session.getSessionTitle());
            binding.tvSessionAttendance.setText(session.getAttendanceSummary());
            binding.tvSessionDate.setText(session.getFormattedDate());
        }
    }
}
