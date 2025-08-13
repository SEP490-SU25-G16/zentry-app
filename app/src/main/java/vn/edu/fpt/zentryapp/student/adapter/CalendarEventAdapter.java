package vn.edu.fpt.zentryapp.student.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemCalendarSessionBinding;
import vn.edu.fpt.zentryapp.student.data.model.response.CalendarEvent;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.CalendarEventViewHolder> {

    private List<CalendarEvent> events = new ArrayList<>();
    private OnEventClickListener listener;

    // Single pink color for all events
    private static final int PINK_COLOR = 0xFFE91E63; // Material Pink

    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event);
    }

    public void setEvents(List<CalendarEvent> events) {
        this.events = events != null ? events : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarSessionBinding binding = ItemCalendarSessionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CalendarEventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarEventViewHolder holder, int position) {
        holder.bind(events.get(position), position == events.size() - 1);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class CalendarEventViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarSessionBinding binding;

        public CalendarEventViewHolder(ItemCalendarSessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEventClick(events.get(position));
                }
            });
        }

        public void bind(CalendarEvent event, boolean isLastItem) {
            // Set event info
            binding.tvSessionTime.setText(getFormattedTimeRange(event));
            binding.tvSessionDescription.setText(event.getDisplayTitle() + " - " + event.getRoomInfo());

            // Always use pink color for all events
            binding.viewSessionCircle.getBackground().setTint(PINK_COLOR);
            binding.viewSessionLine.setBackgroundColor(PINK_COLOR);

            // Hide line for last item
            if (isLastItem) {
                binding.viewSessionLine.setVisibility(View.GONE);
            } else {
                binding.viewSessionLine.setVisibility(View.VISIBLE);
            }

            // Set alpha based on event status (if you want to differentiate past events)
            float alpha = isPastEvent(event) ? 1.0f : 1.0f;
            binding.getRoot().setAlpha(alpha);
        }

        /**
         * Format time range without seconds
         */
        private String getFormattedTimeRange(CalendarEvent event) {
            String timeRange = event.getTimeRange();

            if (timeRange != null && !timeRange.isEmpty()) {
                // Remove seconds from time format (e.g., "15:22:03" -> "15:22")
                return timeRange.replaceAll(":\\d{2}(?=\\s|$|-)", "");
            }

            return timeRange;
        }

        /**
         * Check if event is in the past
         */
        private boolean isPastEvent(CalendarEvent event) {
            if (event.getEventDate() == null) return false;

            // Simple check - if event date is before today
            long currentTime = System.currentTimeMillis();
            return event.getEventDate().getTime() < currentTime;
        }
    }
}
