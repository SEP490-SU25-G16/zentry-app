package vn.edu.fpt.zentryapp.notification.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.notification.data.NotificationItem;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<NotificationItem> items = new ArrayList<>();
    private final OnItemClickListener listener;
    private boolean showSeeMore = false;
    
    private static final int TYPE_NOTIFICATION = 0;
    private static final int TYPE_SEE_MORE = 1;

    public interface OnItemClickListener {
        void onItemClick(NotificationItem item);
        void onSeeMoreClick();
    }

    public NotificationAdapter(List<NotificationItem> items, OnItemClickListener listener) {
        if (items != null) {
            this.items = new ArrayList<>(items);
        }
        this.listener = listener;
    }

    public void setItems(List<NotificationItem> newItems) {
        if (newItems == null) {
            newItems = new ArrayList<>();
        }
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NotificationDiffCallback(this.items, newItems));
        this.items.clear();
        this.items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }
    
    public void setSeeMoreVisible(boolean visible) {
        if (this.showSeeMore != visible) {
            boolean wasVisible = this.showSeeMore;
            this.showSeeMore = visible;
            
            if (visible && !wasVisible) {
                // Hiển thị see more button
                notifyItemInserted(items.size());
            } else if (!visible && wasVisible) {
                // Ẩn see more button  
                notifyItemRemoved(items.size());
            }
        }
    }

    public NotificationItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position < items.size()) {
            return TYPE_NOTIFICATION;
        } else {
            return TYPE_SEE_MORE;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDate;
        ImageView ivIcon;
        View unreadIndicator;

        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvDate = itemView.findViewById(R.id.tvNotificationDate);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }

        public void bind(final NotificationItem item, final OnItemClickListener listener) {
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }

    public static class SeeMoreViewHolder extends RecyclerView.ViewHolder {
        Button btnSeeMore;

        public SeeMoreViewHolder(View itemView) {
            super(itemView);
            btnSeeMore = itemView.findViewById(R.id.btnSeeMore);
        }

        public void bind(OnItemClickListener listener) {
            btnSeeMore.setOnClickListener(v -> listener.onSeeMoreClick());
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_NOTIFICATION) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_see_more, parent, false);
            return new SeeMoreViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_NOTIFICATION) {
            // Kiểm tra bounds trước khi truy cập items
            if (position < items.size()) {
                NotificationItem item = items.get(position);
                ViewHolder notificationHolder = (ViewHolder) holder;
                notificationHolder.bind(item, listener);

                notificationHolder.tvTitle.setText(item.getTitle());
                notificationHolder.tvMessage.setText(item.getMessage());
                notificationHolder.tvDate.setText(item.getTimestamp());

                // Reset background trước khi set lại để tránh trạng thái cũ
                notificationHolder.itemView.setBackground(null);
                
                if (item.isRead()) {
                    // 🔧 ENHANCED: Read notification styling
                    notificationHolder.unreadIndicator.setVisibility(View.GONE);
                    notificationHolder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    
                    // Dimmed text for read notifications
                    notificationHolder.tvTitle.setTextColor(
                        ContextCompat.getColor(notificationHolder.itemView.getContext(), R.color.notification_read_text)
                    );
                    notificationHolder.tvMessage.setTextColor(
                        ContextCompat.getColor(notificationHolder.itemView.getContext(), R.color.notification_read_text)
                    );
                    notificationHolder.tvDate.setTextColor(
                        ContextCompat.getColor(notificationHolder.itemView.getContext(), R.color.notification_read_text)
                    );
                    
                    // Optional: Add subtle border or background for read notifications
                    notificationHolder.itemView.setBackground(
                        ContextCompat.getDrawable(notificationHolder.itemView.getContext(), R.drawable.notification_read_background)
                    );
                    
                } else {
                    // 🔧 ENHANCED: Unread notification styling
                    notificationHolder.unreadIndicator.setVisibility(View.VISIBLE);
                    
                    // Bold text for unread notifications
                    notificationHolder.tvTitle.setTextColor(
                        ContextCompat.getColor(notificationHolder.itemView.getContext(), R.color.notification_unread_text)
                    );
                    notificationHolder.tvMessage.setTextColor(
                        ContextCompat.getColor(notificationHolder.itemView.getContext(), R.color.notification_unread_text)
                    );
                    notificationHolder.tvDate.setTextColor(
                        ContextCompat.getColor(notificationHolder.itemView.getContext(), R.color.notification_unread_text)
                    );
                    
                    // Bright background for unread notifications
                    notificationHolder.itemView.setBackgroundColor(
                        ContextCompat.getColor(notificationHolder.itemView.getContext(), R.color.notification_unread_background)
                    );
                }
            }
        } else {
            // Bind for see more item
            SeeMoreViewHolder seeMoreHolder = (SeeMoreViewHolder) holder;
            seeMoreHolder.bind(listener);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size() + (showSeeMore ? 1 : 0);
    }

    // DiffUtil inner class
    public static class NotificationDiffCallback extends DiffUtil.Callback {
        private final List<NotificationItem> oldList;
        private final List<NotificationItem> newList;

        public NotificationDiffCallback(List<NotificationItem> oldList, List<NotificationItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList != null ? oldList.size() : 0;
        }

        @Override
        public int getNewListSize() {
            return newList != null ? newList.size() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            NotificationItem oldItem = oldList.get(oldItemPosition);
            NotificationItem newItem = newList.get(newItemPosition);
            
            // So sánh tất cả các thuộc tính quan trọng
            return oldItem.isRead() == newItem.isRead()
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getMessage().equals(newItem.getMessage())
                    && oldItem.getTimestamp().equals(newItem.getTimestamp());
        }
    }
}
