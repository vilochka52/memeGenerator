package com.example.memegenerator.ui;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memegenerator.R;
import com.example.memegenerator.data.Project;
import com.example.memegenerator.databinding.ItemProjectBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    public interface Listener {
        void onOpen(Project item);
        void onEdit(Project item);
        void onDelete(Project item);
        void onRename(Project item);
    }

    private final List<Project> items;
    private final Listener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public ProjectAdapter(List<Project> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProjectBinding binding = ItemProjectBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ProjectViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project item = items.get(position);

        String name = item.projectName != null && !item.projectName.trim().isEmpty()
                ? item.projectName
                : holder.itemView.getContext().getString(R.string.new_project);

        String date = dateFormat.format(new Date(item.createdAt));

        holder.binding.topText.setText(name);
        holder.binding.bottomText.setText(R.string.rename_hint);
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

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        final ItemProjectBinding binding;

        ProjectViewHolder(ItemProjectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}