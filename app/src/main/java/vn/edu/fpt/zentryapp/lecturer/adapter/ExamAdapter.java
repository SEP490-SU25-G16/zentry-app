package vn.edu.fpt.zentryapp.lecturer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.edu.fpt.zentryapp.databinding.ItemExamBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ExamModel;

public class ExamAdapter extends RecyclerView.Adapter<ExamAdapter.ViewHolder> {
    private final List<ExamModel> list;
    public ExamAdapter(List<ExamModel> l) { list = l; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new ViewHolder(ItemExamBinding.inflate(
                LayoutInflater.from(p.getContext()), p, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder h, int i) { h.bind(list.get(i)); }
    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemExamBinding b;
        ViewHolder(ItemExamBinding b) { super(b.getRoot()); this.b = b; }
        void bind(ExamModel m){
            b.tvExamTitle.setText(m.title);
            b.tvExamDescription.setText(m.description);
            b.tvExamDate.setText(m.date);
        }
    }
}
