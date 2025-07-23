package vn.edu.fpt.zentryapp.student.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemScheduleBinding;
import vn.edu.fpt.zentryapp.student.data.model.response.Schedule;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<Schedule> schedules = new ArrayList<>();
    private OnScheduleClickListener onScheduleClickListener;

    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule);
    }

    public void setOnScheduleClickListener(OnScheduleClickListener listener) {
        this.onScheduleClickListener = listener;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules != null ? schedules : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScheduleBinding binding = ItemScheduleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);
        holder.bind(schedule);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemScheduleBinding binding;

        public ViewHolder(@NonNull ItemScheduleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Schedule schedule) {
            binding.tvScheduleClassName.setText(schedule.getClassNameWithGrade());
            binding.tvScheduleClassTime.setText(schedule.getScheduleTime());

            // Set clickable state and visual feedback
            boolean isClickable = schedule.isClickable();
            Schedule.ScheduleStatus status = schedule.getStatus();

            // Configure click behavior
            if (isClickable) {
                binding.getRoot().setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && onScheduleClickListener != null) {
                        onScheduleClickListener.onScheduleClick(schedules.get(position));
                    }
                });
                binding.getRoot().setClickable(true);
                binding.getRoot().setFocusable(true);
                binding.getRoot().setAlpha(1.0f);
            } else {
                binding.getRoot().setOnClickListener(v -> {
                    // Show message when not clickable
                    Toast.makeText(v.getContext(),
                            "Class is not available yet. Please wait until class time.",
                            Toast.LENGTH_SHORT).show();
                });
                binding.getRoot().setClickable(true);
                binding.getRoot().setFocusable(true);
                binding.getRoot().setAlpha(0.6f); // Dim appearance
            }
        }
    }
}
