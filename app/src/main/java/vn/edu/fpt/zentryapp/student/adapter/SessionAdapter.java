package vn.edu.fpt.zentryapp.student.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemSessionStudentBinding;
import vn.edu.fpt.zentryapp.student.data.model.response.Session;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {

    private List<Session> sessions = new ArrayList<>();
    private OnSessionClickListener onSessionClickListener;

    public interface OnSessionClickListener {
        void onSessionClick(Session session);
    }


    public void setSessions(List<Session> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSessionStudentBinding binding = ItemSessionStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Session session = sessions.get(position);
        holder.bind(session, position == sessions.size() - 1); // Check if last item
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSessionStudentBinding binding;

        public ViewHolder(@NonNull ItemSessionStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

//            binding.getRoot().setOnClickListener(v -> {
//                int position = getAdapterPosition();
//                if (position != RecyclerView.NO_POSITION && onSessionClickListener != null) {
//                    onSessionClickListener.onSessionClick(sessions.get(position));
//                }
//            });
        }

        public void bind(Session session, boolean isLastItem) {
            binding.tvSessionTitle.setText(session.getTitle());
            binding.tvSessionAttendance.setText(session.getAttendanceStatus());
            binding.tvSessionDate.setText(session.getDate());

            // Set attendance status color
            binding.tvSessionAttendance.setTextColor(session.getAttendanceColor());

            // Hide divider for last item
            binding.viewSessionDivider.setVisibility(isLastItem ?
                    android.view.View.GONE : android.view.View.VISIBLE);
        }
    }
}
