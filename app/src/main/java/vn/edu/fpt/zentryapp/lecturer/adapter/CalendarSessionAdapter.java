package vn.edu.fpt.zentryapp.lecturer.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemCalendarSessionBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CalendarSession;

public class CalendarSessionAdapter extends RecyclerView.Adapter<CalendarSessionAdapter.CalendarSessionViewHolder> {

    private List<CalendarSession> sessions = new ArrayList<>();
    private OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(CalendarSession session);
    }

    public void setSessions(List<CalendarSession> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarSessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarSessionBinding binding = ItemCalendarSessionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CalendarSessionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarSessionViewHolder holder, int position) {
        holder.bind(sessions.get(position), position == sessions.size() - 1);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class CalendarSessionViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarSessionBinding binding;

        public CalendarSessionViewHolder(ItemCalendarSessionBinding binding) {
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

        public void bind(CalendarSession session, boolean isLastItem) {
            // Set session info
            binding.tvSessionTime.setText(session.getStartTimeDisplay());
            binding.tvSessionDescription.setText(session.getSessionDescription());

            // Set timeline indicator color
            int color = session.getTypeColor();
            binding.viewSessionCircle.getBackground().setTint(color);
            binding.viewSessionLine.setBackgroundColor(color);

            // Hide line for last item
            if (isLastItem) {
                binding.viewSessionLine.setVisibility(View.GONE);
            } else {
                binding.viewSessionLine.setVisibility(View.VISIBLE);
            }

            // Set alpha based on status
            float alpha = "COMPLETED".equals(session.getStatus()) ? 0.6f : 1.0f;
            binding.getRoot().setAlpha(alpha);
        }
    }
}
