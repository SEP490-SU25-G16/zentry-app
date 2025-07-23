package vn.edu.fpt.zentryapp.student.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemCalendarEventBinding;
import vn.edu.fpt.zentryapp.student.data.model.response.CalendarEvent;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.ViewHolder> {

    private List<CalendarEvent> events = new ArrayList<>();
    private OnEventClickListener onEventClickListener;

    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event);
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.onEventClickListener = listener;
    }

    public void setEvents(List<CalendarEvent> events) {
        this.events = events != null ? events : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarEventBinding binding = ItemCalendarEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarEvent event = events.get(position);
        holder.bind(event, position == events.size() - 1); // Check if last item
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarEventBinding binding;

        public ViewHolder(@NonNull ItemCalendarEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onEventClickListener != null) {
                    onEventClickListener.onEventClick(events.get(position));
                }
            });
        }

        public void bind(CalendarEvent event, boolean isLastItem) {
            binding.tvEventTime.setText(event.getTime());
            binding.tvEventDescription.setText(event.getDisplayDescription());

            // Set timeline indicator color
            try {
                int color = Color.parseColor(event.getColor());
                GradientDrawable circleDrawable = new GradientDrawable();
                circleDrawable.setShape(GradientDrawable.OVAL);
                circleDrawable.setColor(color);
                binding.viewEventCircle.setBackground(circleDrawable);
                binding.viewEventLine.setBackgroundColor(color);
            } catch (IllegalArgumentException e) {
                // Fallback to default color
                binding.viewEventCircle.setBackgroundColor(Color.parseColor("#FF4081"));
                binding.viewEventLine.setBackgroundColor(Color.parseColor("#FF4081"));
            }

            // Hide line for last item
            binding.viewEventLine.setVisibility(isLastItem ? View.GONE : View.VISIBLE);
        }
    }
}
