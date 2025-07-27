package vn.edu.fpt.zentryapp.lecturer.adapter;


import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemAttendanceRoundBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceRound;

public class AttendanceRoundAdapter extends RecyclerView.Adapter<AttendanceRoundAdapter.RoundViewHolder> {

    private List<AttendanceRound> rounds = new ArrayList<>();
    private OnRoundClickListener listener;

    public interface OnRoundClickListener {
        void onRoundClick(AttendanceRound round);
    }

    public void setRounds(List<AttendanceRound> rounds) {
        this.rounds = rounds != null ? rounds : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnRoundClickListener(OnRoundClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAttendanceRoundBinding binding = ItemAttendanceRoundBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RoundViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoundViewHolder holder, int position) {
        holder.bind(rounds.get(position));
    }

    @Override
    public int getItemCount() {
        return rounds.size();
    }

    class RoundViewHolder extends RecyclerView.ViewHolder {
        private final ItemAttendanceRoundBinding binding;

        public RoundViewHolder(ItemAttendanceRoundBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRoundClick(rounds.get(position));
                }
            });
        }

        public void bind(AttendanceRound round) {
            binding.tvRoundTitle.setText(round.getRoundTitle());
            binding.tvRoundDate.setText(round.getFormattedDate());
            binding.tvRoundTime.setText(round.getFormattedTime());
            binding.tvRoundAttendance.setText(round.getAttendanceDisplay());

            // Set progress bar
            binding.progressRoundAttendance.setProgress(round.getAttendancePercentage());
        }
    }
}
