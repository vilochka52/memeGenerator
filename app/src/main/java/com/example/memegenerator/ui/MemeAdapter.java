package com.example.memegenerator.ui;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memegenerator.data.Meme;
import com.example.memegenerator.databinding.ItemMemeBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemeAdapter extends RecyclerView.Adapter<MemeAdapter.MemeViewHolder> {

    public interface Listener {
        void onOpen(Meme item);
        void onEdit(Meme item);
        void onDelete(Meme item);
        void onRename(Meme item);
    }

    private final List<Meme> items;
    private final Listener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public MemeAdapter(List<Meme> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMemeBinding binding = ItemMemeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MemeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MemeViewHolder holder, int position) {
        Meme item = items.get(position);

        String name = item.projectName != null && !item.projectName.trim().isEmpty()
                ? item.projectName
                : "Без названия";

        String date = dateFormat.format(new Date(item.createdAt));

        holder.binding.topText.setText(name);
        holder.binding.bottomText.setText("Нажми, чтобы переименовать");
        holder.binding.dateText.setText(date);

        if (item.previewImagePath != null && !item.previewImagePath.trim().isEmpty()) {
            holder.binding.thumb.setImageURI(Uri.parse(item.previewImagePath));
        } else {
            holder.binding.thumb.setImageDrawable(null);
        }

        holder.binding.historyCard.setOnClickListener(v -> listener.onOpen(item));

        holder.binding.editButton.setOnClickListener(v -> listener.onEdit(item));

        holder.binding.deleteButton.setOnClickListener(v -> listener.onDelete(item));

        holder.binding.topText.setOnClickListener(v -> listener.onRename(item));
        holder.binding.bottomText.setOnClickListener(v -> listener.onRename(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MemeViewHolder extends RecyclerView.ViewHolder {
        final ItemMemeBinding binding;

        MemeViewHolder(ItemMemeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}