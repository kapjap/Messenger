package com.example.messenger;

import static java.lang.Math.max;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FavoriteMessagesActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFavorites;
    private TextView textViewEmpty;
    private FavoriteAdapter adapter;
    private FavoritesViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favorite_messages);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, max(systemBars.bottom, ime.bottom));
            return insets;
        });

        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        adapter = new FavoriteAdapter(item -> viewModel.removeFromFavorites(item.getMessageId()));
        recyclerViewFavorites.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getFavorites().observe(this, items -> {
            adapter.setItems(items);
            boolean isEmpty = items == null || items.isEmpty();
            textViewEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerViewFavorites.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {

        interface OnRemoveClickListener {
            void onRemoveClick(FavoritesViewModel.FavoriteItem item);
        }

        private final List<FavoritesViewModel.FavoriteItem> items = new ArrayList<>();
        private final OnRemoveClickListener onRemoveClickListener;
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        FavoriteAdapter(OnRemoveClickListener onRemoveClickListener) {
            this.onRemoveClickListener = onRemoveClickListener;
        }

        void setItems(List<FavoritesViewModel.FavoriteItem> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_favorite_message, parent, false);
            return new FavoriteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
            FavoritesViewModel.FavoriteItem item = items.get(position);
            holder.textViewCompanion.setText(item.getCompanionName() != null && !item.getCompanionName().trim().isEmpty()
                    ? item.getCompanionName() : "Собеседник");
            holder.textViewMessage.setText(item.getText() != null ? item.getText() : "");
            holder.textViewTime.setText(item.getTimestamp() > 0
                    ? timeFormat.format(new Date(item.getTimestamp())) : "--");
            holder.imageViewRemove.setOnClickListener(v -> onRemoveClickListener.onRemoveClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class FavoriteViewHolder extends RecyclerView.ViewHolder {
            TextView textViewCompanion;
            TextView textViewMessage;
            TextView textViewTime;
            ImageView imageViewRemove;

            FavoriteViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewCompanion = itemView.findViewById(R.id.textViewCompanion);
                textViewMessage = itemView.findViewById(R.id.textViewMessage);
                textViewTime = itemView.findViewById(R.id.textViewTime);
                imageViewRemove = itemView.findViewById(R.id.imageViewRemove);
            }
        }
    }
}
