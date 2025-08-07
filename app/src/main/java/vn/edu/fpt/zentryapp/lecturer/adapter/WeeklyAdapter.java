package vn.edu.fpt.zentryapp.lecturer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemWeeklyBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.WeeklyModel;

public class WeeklyAdapter extends RecyclerView.Adapter<WeeklyAdapter.ViewHolder> {
    private final List<WeeklyModel> list;
    public WeeklyAdapter(List<WeeklyModel> l) { list = l; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new ViewHolder(ItemWeeklyBinding.inflate(
                LayoutInflater.from(p.getContext()), p, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder h, int i) { h.bind(list.get(i)); }
    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemWeeklyBinding b;
        ViewHolder(ItemWeeklyBinding b){ super(b.getRoot()); this.b = b; }
        void bind(WeeklyModel m){
            b.tvSubject.setText(m.subject);
            b.tvPresented.setText(m.presented);
            b.tvSessions.setText(m.sessions);
            b.tvAttendance.setText(m.attendance);
        }
    }
}
