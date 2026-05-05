package com.example.messenger;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.GroupViewHolder> {

    private final List<GroupChatsViewModel.GroupChatItem> items = new ArrayList<>();
    private OnGroupClickListener onGroupClickListener;

    public void setItems(List<GroupChatsViewModel.GroupChatItem> groups) {
        items.clear();
        if (groups != null) items.addAll(groups);
        notifyDataSetChanged();
    }

    public void setOnGroupClickListener(OnGroupClickListener onGroupClickListener) {
        this.onGroupClickListener = onGroupClickListener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupChatsViewModel.GroupChatItem item = items.get(position);
        Group group = item.getGroup();

        holder.textViewGroupTitle.setText(TextUtils.isEmpty(group.getTitle()) ? "Без названия" : group.getTitle());
        holder.textViewLastMessage.setText(TextUtils.isEmpty(item.getLastMessage()) ? "Нет сообщений" : item.getLastMessage());
        holder.textViewTime.setText(formatTime(item.getLastMessageTime()));

        if (item.getUnreadCount() > 0) {
            holder.textViewUnreadCount.setVisibility(View.VISIBLE);
            holder.textViewUnreadCount.setText(String.valueOf(item.getUnreadCount()));
        } else {
            holder.textViewUnreadCount.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onGroupClickListener != null) onGroupClickListener.onGroupClick(group);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "";
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView textViewGroupTitle;
        TextView textViewLastMessage;
        TextView textViewTime;
        TextView textViewUnreadCount;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewGroupTitle = itemView.findViewById(R.id.textViewGroupTitle);
            textViewLastMessage = itemView.findViewById(R.id.textViewGroupLastMessage);
            textViewTime = itemView.findViewById(R.id.textViewGroupTime);
            textViewUnreadCount = itemView.findViewById(R.id.textViewGroupUnreadCount);
        }
    }

    interface OnGroupClickListener { void onGroupClick(Group group); }
}
