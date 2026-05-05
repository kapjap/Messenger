package com.example.messenger;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private static final String EMPTY_MESSAGE_HINT = "Начните общение";

    private List<UsersVIewModel.ChatPreview> chats = new ArrayList<>();
    private OnUserClickListener onUserClickListener;

    public void setChats(List<UsersVIewModel.ChatPreview> chats) {
        this.chats = chats;
        notifyDataSetChanged();
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.onUserClickListener = listener;
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UsersVIewModel.ChatPreview preview = chats.get(position);
        User user = preview.getUser();

        String fullName = (user.getName() + " " + user.getLastName()).trim();
        holder.textViewUserName.setText(fullName.isEmpty() ? "Пользователь" : fullName);

        String message = preview.getLastMessage();
        holder.textViewLastMessage.setText(TextUtils.isEmpty(message) ? EMPTY_MESSAGE_HINT : message);

        holder.textViewLastMessageTime.setText(formatTime(preview.getLastMessageTime()));

        boolean isOnline = user.isOnline();
        int bkResId = isOnline ? R.drawable.circle_green : R.drawable.circle_red;
        Drawable background = ContextCompat.getDrawable(holder.itemView.getContext(), bkResId);
        holder.onlineStatus.setBackground(background);
        holder.textViewStatus.setText(isOnline ? "Online" : "Offline");

        int unreadCount = preview.getUnreadCount();
        if (unreadCount > 0) {
            holder.textViewUnreadCount.setVisibility(View.VISIBLE);
            holder.textViewUnreadCount.setText(String.valueOf(unreadCount));
        } else {
            holder.textViewUnreadCount.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onUserClickListener != null) {
                onUserClickListener.onUserClick(user);
            }
        });
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "";
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUserName;
        TextView textViewStatus;
        TextView textViewLastMessage;
        TextView textViewLastMessageTime;
        TextView textViewUnreadCount;
        View onlineStatus;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUserName = itemView.findViewById(R.id.textViewUserName);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            textViewLastMessage = itemView.findViewById(R.id.textViewLastMessage);
            textViewLastMessageTime = itemView.findViewById(R.id.textViewLastMessageTime);
            textViewUnreadCount = itemView.findViewById(R.id.textViewUnreadCount);
            onlineStatus = itemView.findViewById(R.id.onlineStatus);
        }
    }

    interface OnUserClickListener {
        void onUserClick(User user);
    }
}
