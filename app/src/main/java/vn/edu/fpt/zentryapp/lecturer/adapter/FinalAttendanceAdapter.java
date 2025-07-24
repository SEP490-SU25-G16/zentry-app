package vn.edu.fpt.zentryapp.lecturer.adapter;


import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.ItemFinalAttendanceBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendance;

public class FinalAttendanceAdapter extends RecyclerView.Adapter<FinalAttendanceAdapter.FinalAttendanceViewHolder> {

    private List<FinalAttendance> finalAttendance = new ArrayList<>();
    private OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onStudentClick(FinalAttendance student);
    }

    public void setFinalAttendance(List<FinalAttendance> finalAttendance) {
        this.finalAttendance = finalAttendance != null ? finalAttendance : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnStudentClickListener(OnStudentClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FinalAttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFinalAttendanceBinding binding = ItemFinalAttendanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FinalAttendanceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FinalAttendanceViewHolder holder, int position) {
        holder.bind(finalAttendance.get(position));
    }

    @Override
    public int getItemCount() {
        return finalAttendance.size();
    }

    class FinalAttendanceViewHolder extends RecyclerView.ViewHolder {
        private final ItemFinalAttendanceBinding binding;

        public FinalAttendanceViewHolder(ItemFinalAttendanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStudentClick(finalAttendance.get(position));
                }
            });
        }

        public void bind(FinalAttendance student) {
            binding.tvStudentName.setText(student.getStudentName());
            binding.tvStudentCode.setText("ID: " + student.getStudentCode());
            binding.tvAttendanceStatus.setText(student.getAttendanceStatus());
            binding.tvAttendanceStatus.setTextColor(student.getAttendanceStatusColor());
            binding.tvAttendanceRatio.setText(student.getAttendanceRatio());

        }
    }
}
