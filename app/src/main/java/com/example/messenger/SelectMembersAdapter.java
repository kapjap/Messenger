package com.example.messenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectMembersAdapter extends RecyclerView.Adapter<SelectMembersAdapter.MemberViewHolder> {

    private final List<User> users = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();

    public void setUsers(List<User> list) {
        users.clear();
        if (list != null) users.addAll(list);
        notifyDataSetChanged();
    }

    public List<String> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    public void setPreselectedIds(Set<String> ids) {
        selectedIds.clear();
        if (ids != null) selectedIds.addAll(ids);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = users.get(position);
        String fullName = (user.getName() + " " + user.getLastName()).trim();
        holder.textViewName.setText(fullName.isEmpty() ? "Пользователь" : fullName);
        holder.textViewEmail.setText(user.getEmail());

        String userId = user.getId();
        boolean checked = userId != null && selectedIds.contains(userId);
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(checked);

        View.OnClickListener toggle = v -> {
            if (userId == null) return;
            if (selectedIds.contains(userId)) {
                selectedIds.remove(userId);
                holder.checkBox.setChecked(false);
            } else {
                selectedIds.add(userId);
                holder.checkBox.setChecked(true);
            }
        };

        holder.itemView.setOnClickListener(toggle);
        holder.checkBox.setOnClickListener(toggle);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewEmail;
        CheckBox checkBox;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewMemberName);
            textViewEmail = itemView.findViewById(R.id.textViewMemberEmail);
            checkBox = itemView.findViewById(R.id.checkBoxMember);
        }
    }
}
