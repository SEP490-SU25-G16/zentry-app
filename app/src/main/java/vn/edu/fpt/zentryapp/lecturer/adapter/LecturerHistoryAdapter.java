package vn.edu.fpt.zentryapp.lecturer.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemRoundBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Round;

public class LecturerHistoryAdapter extends RecyclerView.Adapter<LecturerHistoryAdapter.RoundViewHolder> {

    private List<Round> rounds = new ArrayList<>();
    private OnRoundClickListener listener;

    public interface OnRoundClickListener {
        void onRoundClick(Round round);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setRounds(List<Round> rounds) {
        this.rounds = rounds != null ? rounds : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnRoundClickListener(OnRoundClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRoundBinding binding = ItemRoundBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RoundViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoundViewHolder holder, int position) {
        holder.bind(rounds.get(position), position + 1); // Truyền số thứ tự
    }

    @Override
    public int getItemCount() {
        return rounds.size();
    }

    class RoundViewHolder extends RecyclerView.ViewHolder {
        private final ItemRoundBinding binding;

        public RoundViewHolder(ItemRoundBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRoundClick(rounds.get(position));
                }
            });
        }

        public void bind(Round round, int roundNumber) {
            // Set số thứ tự trong circle
            binding.tvRoundNumber.setText(String.valueOf(roundNumber));

            // Set title
            binding.tvRoundTitle.setText(round.getRoundTitle());

            // Set subtitle với attendance info
            binding.tvRoundSubtitle.setText(round.getAttendanceDisplay());
        }
    }
}
