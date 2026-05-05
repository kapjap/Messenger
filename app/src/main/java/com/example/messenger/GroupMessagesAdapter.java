package com.example.messenger;

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

public class GroupMessagesAdapter extends RecyclerView.Adapter<GroupMessagesAdapter.GroupMessageViewHolder> {

    private static final int VIEW_TYPE_MY = 1;
    private static final int VIEW_TYPE_OTHER = 2;

    private final List<GroupMessage> messages = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public GroupMessagesAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<GroupMessage> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        GroupMessage message = messages.get(position);
        return currentUserId != null && currentUserId.equals(message.getSenderId()) ? VIEW_TYPE_MY : VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public GroupMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == VIEW_TYPE_MY
                ? R.layout.my_group_message_item
                : R.layout.other_group_message_item;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new GroupMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupMessageViewHolder holder, int position) {
        GroupMessage message = messages.get(position);
        holder.textViewMessage.setText(message.getText());
        holder.textViewSenderName.setText(message.getSenderName() != null ? message.getSenderName() : "");

        long ts = message.getTimestamp();
        holder.textViewTime.setText(ts > 0 ? timeFormat.format(new Date(ts)) : "--:--");
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class GroupMessageViewHolder extends RecyclerView.ViewHolder {

        TextView textViewSenderName;
        TextView textViewMessage;
        TextView textViewTime;

        GroupMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
        }
    }
}
