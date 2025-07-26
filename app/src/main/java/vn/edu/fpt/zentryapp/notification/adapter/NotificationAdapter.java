package vn.edu.fpt.zentryapp.notification.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.notification.data.NotificationItem;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<NotificationItem> items;

    public NotificationAdapter(List<NotificationItem> items) {
        this.items = items;
    }

    public void setItems(List<NotificationItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, timestamp;

        public ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            message = v.findViewById(R.id.tvMessage);
            timestamp = v.findViewById(R.id.tvTime);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.message.setText(item.getMessage());
        holder.timestamp.setText(item.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }
}
