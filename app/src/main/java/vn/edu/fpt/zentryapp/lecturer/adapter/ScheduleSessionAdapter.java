package vn.edu.fpt.zentryapp.lecturer.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.ItemScheduleSessionBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ScheduleSession;

public class ScheduleSessionAdapter extends RecyclerView.Adapter<ScheduleSessionAdapter.ScheduleSessionViewHolder> {

    private List<ScheduleSession> sessions = new ArrayList<>();
    private OnSessionActionListener listener;

    public interface OnSessionActionListener {
        void onSessionClick(ScheduleSession session);

        void onStartInstantClick(ScheduleSession session);
    }

    public void setSessions(List<ScheduleSession> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnSessionActionListener(OnSessionActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScheduleSessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScheduleSessionBinding binding = ItemScheduleSessionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ScheduleSessionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleSessionViewHolder holder, int position) {
        holder.bind(sessions.get(position));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class ScheduleSessionViewHolder extends RecyclerView.ViewHolder {
        private final ItemScheduleSessionBinding binding;

        public ScheduleSessionViewHolder(ItemScheduleSessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ScheduleSession session) {
            // Set basic info
            binding.tvSessionCourseName.setText(session.getCourseName());
            binding.tvSessionClassRoom.setText(session.getClassRoomDisplay());
            binding.tvSessionDateTime.setText(session.getDateTimeDisplay());
            binding.tvSessionStatus.setText(session.getStatusText());
            binding.tvSessionStatus.setTextColor(session.getStatusColor());

            // Configure action button
            configureActionButton(session);

            // Set click listeners
            binding.getRoot().setOnClickListener(v -> {
                if (session.isCanViewDetail() && listener != null) {
                    listener.onSessionClick(session);
                }
            });

            binding.btnSessionAction.setOnClickListener(v -> {
                if (session.isCanStartInstant() && listener != null) {
                    listener.onStartInstantClick(session);
                }
            });

            // Set card styling based on status
            setCardStyling(session);
        }

        private void configureActionButton(ScheduleSession session) {
            if (session.isCanStartInstant()) {
                binding.btnSessionAction.setText("Start Class");
                binding.btnSessionAction.setVisibility(View.VISIBLE);
                binding.btnSessionAction.setEnabled(true);
                binding.btnSessionAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Green
            } else if (session.isCanViewDetail()) {
                binding.btnSessionAction.setText("View Detail");
                binding.btnSessionAction.setVisibility(View.VISIBLE);
                binding.btnSessionAction.setEnabled(true);
                binding.btnSessionAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF2196F3)); // Blue
            } else {
                binding.btnSessionAction.setText("Not Available");
                binding.btnSessionAction.setVisibility(View.VISIBLE);
                binding.btnSessionAction.setEnabled(false);
                binding.btnSessionAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF9E9E9E)); // Grey
            }
        }

        private void setCardStyling(ScheduleSession session) {
            float alpha = 1.0f;

            if ("COMPLETED".equals(session.getStatus()) || "CANCELLED".equals(session.getStatus())) {
                alpha = 0.7f; // Slightly transparent for past sessions
            } else if ("ONGOING".equals(session.getStatus())) {
                // Highlight ongoing sessions
                binding.getRoot().setCardBackgroundColor(0xFFE8F5E8); // Light green
            }

            binding.getRoot().setAlpha(alpha);
        }
    }
}
