package com.example.messenger;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GroupMembersAdapter extends RecyclerView.Adapter<GroupMembersAdapter.MemberViewHolder> {

    public static class MemberItem {
        private final String uid;
        private final String fullName;
        private final String email;
        private final boolean isAdmin;

        public MemberItem(String uid, String fullName, String email, boolean isAdmin) {
            this.uid = uid;
            this.fullName = fullName;
            this.email = email;
            this.isAdmin = isAdmin;
        }

        public String getUid() { return uid; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public boolean isAdmin() { return isAdmin; }
    }

    private final List<MemberItem> items = new ArrayList<>();

    public void setItems(List<MemberItem> members) {
        items.clear();
        if (members != null) items.addAll(members);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        MemberItem item = items.get(position);
        holder.textViewMemberName.setText(TextUtils.isEmpty(item.getFullName()) ? "Без имени" : item.getFullName());
        holder.textViewMemberEmail.setText(TextUtils.isEmpty(item.getEmail()) ? item.getUid() : item.getEmail());
        holder.textViewAdminBadge.setVisibility(item.isAdmin() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewMemberName;
        private final TextView textViewMemberEmail;
        private final TextView textViewAdminBadge;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMemberName = itemView.findViewById(R.id.textViewMemberName);
            textViewMemberEmail = itemView.findViewById(R.id.textViewMemberEmail);
            textViewAdminBadge = itemView.findViewById(R.id.textViewMemberAdmin);
        }
    }
}
